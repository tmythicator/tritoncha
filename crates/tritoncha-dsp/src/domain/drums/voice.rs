//! Polyphonic layered percussion voice orchestrator (Exciter -> Physical Body -> Shaper).

use crate::core::math::{sin_phase, soft_clip, t60_decay_coeff, wrap_phase, xorshift32_norm};
use crate::domain::drums::bodies::cymbal::{configure_cymbal, is_edge_crash, render_cymbal};
use crate::domain::drums::bodies::idiophone::{configure_idiophone_freqs, render_idiophone};
use crate::domain::drums::bodies::membrane::render_membrane;
use crate::domain::drums::bodies::shell::{configure_shell_modes, render_shell};
use crate::domain::drums::bodies::{CachedModalFilter, NUM_MODES};
use crate::domain::drums::patch::{NoiseFilterMode, PercBodyType, PercPatch};
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;
use crate::domain::effects::StateVariableFilter;

/// Single polyphonic layered percussion voice (Exciter -> Physical Body -> Shaper).
#[derive(Clone)]
pub struct PercVoice {
    pub active: bool,
    pub choke_group: u8,
    pub age: u64,

    // Cached voice configuration for current hit
    body_type: PercBodyType,
    base_pitch_hz: f32,
    click_freq_hz: f32,
    click_level: f32,
    fm_depth: f32,
    fm_ratio: f32,
    noise_mix: f32,
    noise_cutoff_hz: f32,
    noise_q: f32,
    noise_filter_mode: NoiseFilterMode,
    drive: f32,
    fold: f32,
    crush: f32,
    vel: f32,

    // Runtime oscillators & envelopes
    phase: f32,
    fm_phase: f32,
    click_phase: f32,
    metallic_phases: [f32; NUM_MODES],
    metallic_freqs: [f32; NUM_MODES],
    modes: [CachedModalFilter; NUM_MODES],

    pitch_env: f32,
    pitch_decay: f32,
    body_env: f32,
    body_decay: f32,
    noise_env: f32,
    noise_decay: f32,
    click_env: f32,
    click_decay: f32,
    exciter_env: f32,
    exciter_decay: f32,

    // Multi-burst exciter state (clap / burst)
    bursts_remaining: u8,
    burst_timer: usize,
    burst_spacing_samples: usize,

    // Smooth choke dampening state
    choking: bool,
    choke_env: f32,
    choke_decay: f32,

    body_filter: StateVariableFilter,
    noise_filter: StateVariableFilter,
    noise_seed: u32,
}

impl Default for PercVoice {
    fn default() -> Self {
        Self::new()
    }
}

impl PercVoice {
    pub fn new() -> Self {
        Self::with_seed(0x5a82_7999)
    }

    pub fn with_seed(seed: u32) -> Self {
        Self {
            active: false,
            choke_group: 0,
            age: 0,
            body_type: PercBodyType::Membrane,
            base_pitch_hz: 52.0,
            click_freq_hz: 2400.0,
            click_level: 0.0,
            fm_depth: 0.0,
            fm_ratio: 2.5,
            noise_mix: 0.0,
            noise_cutoff_hz: 3200.0,
            noise_q: 0.45,
            noise_filter_mode: NoiseFilterMode::Highpass,
            drive: 1.0,
            fold: 0.0,
            crush: 0.0,
            vel: 0.0,
            phase: 0.0,
            fm_phase: 0.0,
            click_phase: 0.0,
            metallic_phases: [0.0; NUM_MODES],
            metallic_freqs: [300.0; NUM_MODES],
            modes: [CachedModalFilter::default(); NUM_MODES],
            pitch_env: 0.0,
            pitch_decay: 0.99,
            body_env: 0.0,
            body_decay: 0.999,
            noise_env: 0.0,
            noise_decay: 0.999,
            click_env: 0.0,
            click_decay: 0.98,
            exciter_env: 0.0,
            exciter_decay: 0.95,
            bursts_remaining: 0,
            burst_timer: 0,
            burst_spacing_samples: 0,
            choking: false,
            choke_env: 1.0,
            choke_decay: 0.98,
            body_filter: StateVariableFilter::new(),
            noise_filter: StateVariableFilter::new(),
            noise_seed: seed.max(1),
        }
    }

    /// Triggers this voice with a `PercPatch`, velocity, optional melodic pitch override, and exact sample rate.
    pub fn trigger(
        &mut self,
        patch: &PercPatch,
        vel: f32,
        note_freq_hz: Option<f32>,
        sample_rate: f32,
    ) {
        let sr = sample_rate.max(8000.0);
        let v = vel.clamp(0.05, 1.2);

        // Seeded per-hit humanization ("round-robin" micro-variation)
        let var = patch.variation.clamp(0.0, 1.0);
        let r_pitch = xorshift32_norm(&mut self.noise_seed) * 0.06 * var;
        let r_decay = xorshift32_norm(&mut self.noise_seed) * 0.12 * var;
        let r_tone = xorshift32_norm(&mut self.noise_seed) * 0.10 * var;

        // First-class pitch: optional melodic note overrides base pitch; velocity adds strike tension
        let raw_pitch = note_freq_hz.unwrap_or(patch.pitch_hz);
        let vel_pitch_mult = 1.0 + patch.vel_to_pitch * 0.18 * (v - 0.5);
        let eff_pitch = (raw_pitch * vel_pitch_mult * (1.0 + r_pitch)).clamp(20.0, sr * 0.45);

        let eff_decay = (patch.decay_s * (1.0 + r_decay)).clamp(0.005, 6.0);
        let eff_noise_decay = (patch.noise_decay_s * (1.0 + r_decay)).clamp(0.005, 4.0);

        // Velocity -> tone brightness scaling (monotonic with velocity when vel_to_tone > 0)
        let tone_scale = (1.0 - patch.vel_to_tone * 0.55) + patch.vel_to_tone * 0.95 * v.min(1.0);
        let eff_noise_cutoff =
            (patch.noise_cutoff_hz * tone_scale * (1.0 + r_tone)).clamp(150.0, sr * 0.45);

        self.active = true;
        self.choke_group = patch.choke_group;
        self.choking = false;
        self.choke_env = 1.0;

        self.body_type = patch.body_type;
        self.base_pitch_hz = eff_pitch;
        self.click_freq_hz = (patch.click_freq_hz * tone_scale).clamp(400.0, sr * 0.45);
        self.click_level = patch.click_level;
        self.fm_depth = patch.fm_depth;
        self.fm_ratio = patch.fm_ratio;
        self.noise_mix = patch.noise_mix;
        self.noise_cutoff_hz = eff_noise_cutoff;
        self.noise_q = patch.noise_q;
        self.noise_filter_mode = patch.noise_filter_mode;
        self.drive = patch.drive;
        self.fold = patch.fold;
        self.crush = patch.crush;
        self.vel = v;

        // Reset phases & filters
        self.phase = 0.12; // Non-zero onset for crisp punch transient
        self.fm_phase = 0.0;
        self.click_phase = 0.0;
        self.metallic_phases = [0.0; NUM_MODES];
        self.body_filter.reset();
        self.noise_filter.reset();

        // Sample-rate-invariant envelope coefficients
        self.pitch_env = patch.pitch_drop;
        self.pitch_decay = t60_decay_coeff(patch.pitch_decay_s, sr);
        self.body_env = 1.0;
        self.body_decay = t60_decay_coeff(eff_decay, sr);
        self.noise_env = if patch.noise_mix > 0.0 { 1.0 } else { 0.0 };
        self.noise_decay = t60_decay_coeff(eff_noise_decay, sr);
        self.click_env = if patch.click_level > 0.0 { 1.0 } else { 0.0 };
        self.click_decay = t60_decay_coeff(0.004, sr);
        self.exciter_env = 1.0;
        self.exciter_decay = t60_decay_coeff(0.0025, sr);
        self.choke_decay = t60_decay_coeff(0.004, sr);

        // Multi-burst exciter setup (clap / burst)
        let bc = patch.burst_count.clamp(1, 6);
        self.bursts_remaining = bc.saturating_sub(1);
        self.burst_spacing_samples = ((patch.burst_spacing_s * sr) as usize).max(16);
        self.burst_timer = self.burst_spacing_samples;

        // Precompute physical body resonators once on trigger
        let spread = patch.modal_spread.clamp(0.5, 3.0);
        match patch.body_type {
            PercBodyType::Shell => {
                configure_shell_modes(&mut self.modes, eff_pitch, eff_decay, spread, sr);
            }
            PercBodyType::Cymbal => {
                let edge_crash = is_edge_crash(patch.noise_mix, patch.choke_group);
                if edge_crash {
                    self.exciter_decay = t60_decay_coeff(0.18, sr);
                }
                configure_cymbal(
                    &mut self.modes,
                    &mut self.metallic_freqs,
                    eff_pitch,
                    eff_decay,
                    spread,
                    edge_crash,
                    sr,
                );
            }
            PercBodyType::Idiophone => {
                configure_idiophone_freqs(&mut self.metallic_freqs, eff_pitch, sr);
            }
            PercBodyType::Membrane | PercBodyType::Burst => {}
        }
    }

    /// Initiates a smooth 4ms choke dampening ramp on this voice.
    #[inline(always)]
    pub fn choke(&mut self, sample_rate: f32) {
        if self.active && !self.choking {
            self.choking = true;
            self.choke_decay = t60_decay_coeff(0.004, sample_rate.max(8000.0));
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        // Stage 1: Multi-burst exciter scheduler (for multi-tap claps)
        if self.bursts_remaining > 0 {
            if self.burst_timer == 0 {
                self.bursts_remaining -= 1;
                self.burst_timer = self.burst_spacing_samples;
                self.noise_env = 1.0;
                self.exciter_env = 1.0;
                if self.click_level > 0.0 {
                    self.click_env = 0.85;
                }
            } else {
                self.burst_timer -= 1;
            }
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);

        // Stage 1b: Stick/beater click transient
        let click_sig = if self.click_env > MIN_AUDIBLE_VELOCITY {
            let c = sin_phase(self.click_phase) * self.click_env * self.click_level;
            self.click_phase = wrap_phase(self.click_phase + self.click_freq_hz / sample_rate);
            self.click_env *= self.click_decay;
            c
        } else {
            0.0
        };

        // Stage 2: Physical Resonant Body
        let cur_freq = (self.base_pitch_hz + self.pitch_env).clamp(20.0, sample_rate * 0.45);
        let body_sig = match self.body_type {
            PercBodyType::Membrane => render_membrane(
                &mut self.phase,
                &mut self.fm_phase,
                cur_freq,
                self.fm_depth,
                self.fm_ratio,
                self.body_env,
                sample_rate,
            ),
            PercBodyType::Shell => render_shell(
                &mut self.modes,
                &mut self.exciter_env,
                self.exciter_decay,
                noise_raw,
            ),
            PercBodyType::Cymbal => render_cymbal(
                &mut self.modes,
                &mut self.metallic_phases,
                &self.metallic_freqs,
                &mut self.body_filter,
                self.base_pitch_hz,
                self.noise_mix,
                self.noise_cutoff_hz,
                self.noise_q,
                self.noise_filter_mode,
                self.choke_group,
                self.body_env,
                &mut self.exciter_env,
                self.exciter_decay,
                noise_raw,
                sample_rate,
            ),
            PercBodyType::Idiophone => render_idiophone(
                &mut self.metallic_phases,
                &self.metallic_freqs,
                &mut self.body_filter,
                self.noise_cutoff_hz,
                self.noise_q,
                self.noise_filter_mode,
                self.body_env,
                sample_rate,
            ),
            PercBodyType::Burst => 0.0,
        };

        // Stage 2b: Secondary filtered noise layer (snare wires, cymbal wash, clap tail)
        let noise_sig = if self.noise_env > MIN_AUDIBLE_VELOCITY && self.noise_mix > 0.0 {
            let filtered = match self.noise_filter_mode {
                NoiseFilterMode::Highpass => self.noise_filter.process_hp(
                    noise_raw,
                    self.noise_cutoff_hz,
                    self.noise_q,
                    sample_rate,
                ),
                NoiseFilterMode::Bandpass => self.noise_filter.process_bp(
                    noise_raw,
                    self.noise_cutoff_hz,
                    self.noise_q,
                    sample_rate,
                ),
                NoiseFilterMode::Lowpass => self.noise_filter.process_lp(
                    noise_raw,
                    self.noise_cutoff_hz,
                    self.noise_q,
                    sample_rate,
                ),
            };
            let n = filtered * self.noise_env * self.noise_mix;
            self.noise_env *= self.noise_decay;
            n
        } else {
            0.0
        };

        // Stage 3: Composable Shaper (Drive + Wavefolder + Bitcrusher)
        let mut sig = body_sig + noise_sig + click_sig;
        if self.crush > 0.0 {
            let steps = 2.0_f32.powf(12.0 - 9.0 * self.crush);
            sig = (sig * steps).round() / steps;
        }
        let driven = soft_clip(sig * self.drive);
        let shaped = if self.fold > 0.0 {
            let folded = sin_phase(sig * self.drive * 0.45);
            driven * (1.0 - self.fold) + folded * self.fold
        } else {
            driven
        };

        // Apply velocity and smooth choke envelope (single saturation stage preserves punch & sub weight)
        let out = shaped.clamp(-1.25, 1.25) * self.vel * self.choke_env;

        // Advance envelopes
        self.pitch_env *= self.pitch_decay;
        self.body_env *= self.body_decay;

        if self.choking {
            self.choke_env *= self.choke_decay;
            if self.choke_env < MIN_AUDIBLE_VELOCITY {
                self.active = false;
            }
        } else if self.bursts_remaining == 0
            && self.body_env < MIN_AUDIBLE_VELOCITY
            && self.noise_env < MIN_AUDIBLE_VELOCITY
            && self.click_env < MIN_AUDIBLE_VELOCITY
        {
            self.active = false;
        }

        out
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.choking = false;
        self.choke_env = 1.0;
        self.body_env = 0.0;
        self.noise_env = 0.0;
        self.click_env = 0.0;
        self.exciter_env = 0.0;
        self.bursts_remaining = 0;
        self.body_filter.reset();
        self.noise_filter.reset();
        for m in &mut self.modes {
            m.reset();
        }
    }
}
