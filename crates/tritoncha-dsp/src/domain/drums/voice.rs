//! Polyphonic layered percussion voice orchestrator (Exciter -> Physical Body -> Shaper).

use crate::core::math::{sin_phase, soft_clip, t60_decay_coeff, wrap_phase, xorshift32_norm};
use crate::domain::drums::bodies::cymbal::{configure_cymbal, render_cymbal, CymbalStrikeSpec};
use crate::domain::drums::bodies::idiophone::{configure_idiophone_freqs, render_idiophone};
use crate::domain::drums::bodies::membrane::render_membrane;
use crate::domain::drums::bodies::shell::{configure_shell_modes, render_shell};
use crate::domain::drums::bodies::ResonatorBank;
use crate::domain::drums::patch::{ChokeGroup, NoiseFilterSpec, PercBodyType, PercPatch};
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;
use crate::domain::effects::StateVariableFilter;

const SHAPER_CRUSH_MAX_BITS: f32 = 12.0;
const SHAPER_CRUSH_SPAN_BITS: f32 = 9.0;
const SHAPER_DRIVE_COMP_SLOPE: f32 = 0.22;
const SHAPER_FOLD_PHASE_SCALE: f32 = 0.35;

const MIN_SAMPLE_RATE_HZ: f32 = 8000.0;
const NYQUIST_RATIO: f32 = 0.45;
const CLICK_DECAY_S: f32 = 0.004;
const CHOKE_DECAY_S: f32 = 0.004;
const EXCITER_BASE_DECAY_S: f32 = 0.0025;
const EXCITER_CRASH_DECAY_SPAN_S: f32 = 0.0095;
const MEMBRANE_FULL_GAIN_MAX_HZ: f32 = 220.0;
const MEMBRANE_FULL_GAIN_MAX_NOISE: f32 = 0.35;
const UPPER_PERC_LEVEL_TRIM: f32 = 0.72;
const OUTPUT_HEADROOM_LIMIT: f32 = 1.25;

/// Exponential decay envelope value + per-sample T60 decay coefficient.
#[derive(Copy, Clone, Debug, Default)]
struct DecayEnv {
    val: f32,
    decay: f32,
}

impl DecayEnv {
    #[inline(always)]
    fn trigger(&mut self, initial: f32, decay_s: f32, sr: f32) {
        self.val = initial;
        self.decay = t60_decay_coeff(decay_s, sr);
    }

    #[inline(always)]
    fn is_audible(&self) -> bool {
        self.val > MIN_AUDIBLE_VELOCITY
    }

    #[inline(always)]
    fn step(&mut self) -> f32 {
        let cur = self.val;
        self.val *= self.decay;
        cur
    }
}

/// Output nonlinear waveshaper (Drive + Envelope-Bounded Wavefolder + Bitcrusher).
#[derive(Copy, Clone, Debug)]
pub struct PercShaper {
    pub drive: f32,
    pub fold: f32,
    pub crush: f32,
}

impl Default for PercShaper {
    fn default() -> Self {
        Self {
            drive: 1.0,
            fold: 0.0,
            crush: 0.0,
        }
    }
}

impl PercShaper {
    #[inline(always)]
    pub fn process(&self, mut sig: f32) -> f32 {
        if self.crush > 0.0 {
            let steps = 2.0_f32.powf(SHAPER_CRUSH_MAX_BITS - SHAPER_CRUSH_SPAN_BITS * self.crush);
            sig = (sig * steps).round() / steps;
        }
        let drive_comp = 1.0 / (1.0 + (self.drive - 1.0).max(0.0) * SHAPER_DRIVE_COMP_SLOPE);
        let driven = soft_clip(sig * self.drive) * drive_comp;
        if self.fold > 0.0 {
            let folded = sin_phase(sig * self.drive * SHAPER_FOLD_PHASE_SCALE) * driven.abs();
            driven * (1.0 - self.fold) + folded * self.fold
        } else {
            driven
        }
    }
}

/// Single polyphonic layered percussion voice (Exciter -> Physical Body -> Shaper).
#[derive(Clone)]
pub struct PercVoice {
    pub active: bool,
    pub choke_group: ChokeGroup,
    pub age: u64,

    body_type: PercBodyType,
    base_pitch_hz: f32,
    click_freq_hz: f32,
    click_level: f32,
    fm_depth: f32,
    fm_ratio: f32,
    noise_mix: f32,
    noise_filter_spec: NoiseFilterSpec,
    shaper: PercShaper,
    vel: f32,

    phase: f32,
    fm_phase: f32,
    click_phase: f32,
    bank: ResonatorBank,

    pitch: DecayEnv,
    body: DecayEnv,
    noise: DecayEnv,
    click: DecayEnv,
    exciter: DecayEnv,
    choke: DecayEnv,

    bursts_remaining: u8,
    burst_timer: usize,
    burst_spacing_samples: usize,
    choking: bool,

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
            choke_group: ChokeGroup::NONE,
            age: 0,
            body_type: PercBodyType::Membrane,
            base_pitch_hz: 52.0,
            click_freq_hz: 2400.0,
            click_level: 0.0,
            fm_depth: 0.0,
            fm_ratio: 2.5,
            noise_mix: 0.0,
            noise_filter_spec: NoiseFilterSpec::default(),
            shaper: PercShaper::default(),
            vel: 0.0,
            phase: 0.0,
            fm_phase: 0.0,
            click_phase: 0.0,
            bank: ResonatorBank::new(),
            pitch: DecayEnv::default(),
            body: DecayEnv::default(),
            noise: DecayEnv::default(),
            click: DecayEnv::default(),
            exciter: DecayEnv::default(),
            choke: DecayEnv {
                val: 1.0,
                decay: 0.98,
            },
            bursts_remaining: 0,
            burst_timer: 0,
            burst_spacing_samples: 0,
            choking: false,
            noise_filter: StateVariableFilter::new(),
            noise_seed: seed.max(1),
        }
    }

    /// Triggers this voice with a `PercPatch`, velocity, optional melodic pitch override, and sample rate.
    pub fn trigger(
        &mut self,
        patch: &PercPatch,
        vel: f32,
        note_freq_hz: Option<f32>,
        sample_rate: f32,
    ) {
        let sr = sample_rate.max(MIN_SAMPLE_RATE_HZ);
        let nyquist = sr * NYQUIST_RATIO;
        let v = vel.clamp(0.05, 1.2);

        let var = *patch.variation;
        let r_pitch = xorshift32_norm(&mut self.noise_seed) * 0.06 * var;
        let r_decay = xorshift32_norm(&mut self.noise_seed) * 0.12 * var;
        let r_tone = xorshift32_norm(&mut self.noise_seed) * 0.10 * var;

        let raw_pitch = note_freq_hz.unwrap_or(*patch.pitch_hz);
        let vel_pitch_mult = 1.0 + *patch.vel_to_pitch * 0.18 * (v - 0.5);
        let eff_pitch = (raw_pitch * vel_pitch_mult * (1.0 + r_pitch)).clamp(20.0, nyquist);
        let eff_decay = (*patch.decay_s * (1.0 + r_decay)).clamp(0.005, 6.0);
        let eff_noise_decay = (*patch.noise_decay_s * (1.0 + r_decay)).clamp(0.005, 4.0);

        let vt = *patch.vel_to_tone;
        let tone_scale = (1.0 - vt * 0.55) + vt * 0.95 * v.min(1.0);
        let eff_noise_cutoff =
            (*patch.noise_cutoff_hz * tone_scale * (1.0 + r_tone)).clamp(150.0, nyquist);

        self.active = true;
        self.choke_group = patch.choke_group;
        self.choking = false;
        self.choke.trigger(1.0, CHOKE_DECAY_S, sr);

        self.body_type = patch.body_type;
        self.base_pitch_hz = eff_pitch;
        self.click_freq_hz = (*patch.click_freq_hz * tone_scale).clamp(400.0, nyquist);
        self.click_level = *patch.click_level;
        self.fm_depth = *patch.fm_depth;
        self.fm_ratio = *patch.fm_ratio;
        self.noise_mix = *patch.noise_mix;
        self.noise_filter_spec = NoiseFilterSpec {
            cutoff_hz: eff_noise_cutoff,
            q: *patch.noise_q,
            mode: patch.noise_filter_mode,
        };
        self.shaper = PercShaper {
            drive: *patch.drive,
            fold: *patch.fold,
            crush: *patch.crush,
        };
        self.vel = v;

        self.phase = 0.12;
        self.fm_phase = 0.0;
        self.click_phase = 0.0;
        self.bank.reset_trigger();
        self.noise_filter.reset();

        self.pitch
            .trigger(*patch.pitch_drop, *patch.pitch_decay_s, sr);
        self.body.trigger(1.0, eff_decay, sr);
        self.noise.trigger(
            if self.noise_mix > 0.0 { 1.0 } else { 0.0 },
            eff_noise_decay,
            sr,
        );
        self.click.trigger(
            if self.click_level > 0.0 { 1.0 } else { 0.0 },
            CLICK_DECAY_S,
            sr,
        );
        self.exciter.trigger(1.0, EXCITER_BASE_DECAY_S, sr);

        self.bursts_remaining = patch.burst_count.saturating_sub(1);
        self.burst_spacing_samples = ((*patch.burst_spacing_s * sr) as usize).max(16);
        self.burst_timer = self.burst_spacing_samples;

        let spread = *patch.modal_spread;
        match patch.body_type {
            PercBodyType::Shell => {
                configure_shell_modes(&mut self.bank.modes, eff_pitch, eff_decay, spread, sr);
            }
            PercBodyType::Cymbal => {
                let strike = self.cymbal_strike_spec();
                if !strike.is_clamped_hat() {
                    let exc_s =
                        EXCITER_BASE_DECAY_S + EXCITER_CRASH_DECAY_SPAN_S * strike.crash_blend();
                    self.exciter.trigger(1.0, exc_s, sr);
                }
                configure_cymbal(
                    &mut self.bank,
                    eff_pitch,
                    eff_decay,
                    spread,
                    strike.is_clamped_hat(),
                    sr,
                );
            }
            PercBodyType::Idiophone => {
                configure_idiophone_freqs(&mut self.bank.metallic_freqs, eff_pitch, sr);
            }
            PercBodyType::Membrane | PercBodyType::Burst => {}
        }
    }

    #[inline(always)]
    fn cymbal_strike_spec(&self) -> CymbalStrikeSpec {
        CymbalStrikeSpec {
            base_pitch_hz: self.base_pitch_hz,
            noise_mix: self.noise_mix,
            filter: self.noise_filter_spec,
            choke_group: self.choke_group,
        }
    }

    /// Initiates a smooth 4ms choke dampening ramp on this voice.
    #[inline(always)]
    pub fn choke(&mut self, sample_rate: f32) {
        if self.active && !self.choking {
            self.choking = true;
            self.choke.decay = t60_decay_coeff(CHOKE_DECAY_S, sample_rate.max(MIN_SAMPLE_RATE_HZ));
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        if self.bursts_remaining > 0 {
            if self.burst_timer == 0 {
                self.bursts_remaining -= 1;
                self.burst_timer = self.burst_spacing_samples;
                self.noise.val = 1.0;
                self.exciter.val = 1.0;
                if self.click_level > 0.0 {
                    self.click.val = 0.85;
                }
            } else {
                self.burst_timer -= 1;
            }
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);

        let click_sig = if self.click.is_audible() {
            let c = sin_phase(self.click_phase) * self.click.step() * self.click_level;
            self.click_phase = wrap_phase(self.click_phase + self.click_freq_hz / sample_rate);
            c
        } else {
            0.0
        };

        let cur_freq =
            (self.base_pitch_hz + self.pitch.step()).clamp(20.0, sample_rate * NYQUIST_RATIO);
        let body_env = self.body.step();
        let body_sig = match self.body_type {
            PercBodyType::Membrane => render_membrane(
                &mut self.phase,
                &mut self.fm_phase,
                cur_freq,
                self.fm_depth,
                self.fm_ratio,
                body_env,
                sample_rate,
            ),
            PercBodyType::Shell => render_shell(
                &mut self.bank.modes,
                &mut self.exciter.val,
                self.exciter.decay,
                noise_raw,
            ),
            PercBodyType::Cymbal => {
                let strike = self.cymbal_strike_spec();
                render_cymbal(
                    &strike,
                    &mut self.bank,
                    body_env,
                    &mut self.exciter.val,
                    self.exciter.decay,
                    noise_raw,
                    sample_rate,
                )
            }
            PercBodyType::Idiophone => render_idiophone(
                &mut self.bank,
                &self.noise_filter_spec,
                body_env,
                sample_rate,
            ),
            PercBodyType::Burst => 0.0,
        };

        let noise_sig = if self.noise.is_audible() && self.noise_mix > 0.0 {
            self.noise_filter_spec
                .apply(&mut self.noise_filter, noise_raw, sample_rate)
                * self.noise.step()
                * self.noise_mix
        } else {
            0.0
        };

        let shaped = self.shaper.process(body_sig + noise_sig + click_sig);
        let level_trim = if self.body_type == PercBodyType::Membrane
            && self.base_pitch_hz < MEMBRANE_FULL_GAIN_MAX_HZ
            && self.noise_mix < MEMBRANE_FULL_GAIN_MAX_NOISE
        {
            1.0
        } else {
            UPPER_PERC_LEVEL_TRIM
        };
        let out = shaped.clamp(-OUTPUT_HEADROOM_LIMIT, OUTPUT_HEADROOM_LIMIT)
            * self.vel
            * self.choke.val
            * level_trim;

        if self.choking {
            self.choke.step();
            if !self.choke.is_audible() {
                self.active = false;
            }
        } else if self.bursts_remaining == 0
            && !self.body.is_audible()
            && !self.noise.is_audible()
            && !self.click.is_audible()
        {
            self.active = false;
        }

        out
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.choking = false;
        self.choke.val = 1.0;
        self.body.val = 0.0;
        self.noise.val = 0.0;
        self.click.val = 0.0;
        self.exciter.val = 0.0;
        self.bursts_remaining = 0;
        self.bank.reset();
        self.noise_filter.reset();
    }
}
