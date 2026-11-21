//! Layered parametric percussion engine (Exciter -> Body -> Shaper) with polyphonic voice pool.
//! Zero-allocation, sample-rate invariant, coefficient-cached real-time DSP.

use std::f32::consts::TAU;

use crate::core::math::{
    sin_phase, soft_clip, t60_decay_coeff, wrap_phase, xorshift32_norm, LN_MIN_60DB,
};
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;
use crate::domain::effects::StateVariableFilter;

pub const PERC_PATCH_PARAM_COUNT: usize = 24;
pub const MAX_PERC_VOICES: usize = 16;
const NUM_MODES: usize = 6;

/// Base circular membrane / plate partial ratios (scaled by `modal_spread`).
const BASE_MODAL_RATIOS: [f32; NUM_MODES] = [1.0, 1.587, 2.140, 2.653, 3.156, 3.891];
const BASE_MODAL_GAINS: [f32; NUM_MODES] = [0.55, 0.38, 0.28, 0.20, 0.14, 0.10];
const BASE_MODAL_DECAY_SCALES: [f32; NUM_MODES] = [1.0, 0.82, 0.65, 0.48, 0.35, 0.24];

/// Inharmonic ratios for 6-oscillator metallic cluster (808/909 cymbals, hats, cowbell).
const METALLIC_RATIOS: [f32; NUM_MODES] = [1.0, 1.447, 1.617, 1.926, 2.502, 2.917];

/// Primary resonant body topology for a percussion voice.
#[repr(u8)]
#[derive(Copy, Clone, Debug, PartialEq, Eq, Default)]
pub enum PercBodyType {
    #[default]
    PitchedOsc = 0,
    ModalBank = 1,
    MetallicCluster = 2,
    NoiseOnly = 3,
}

impl From<u8> for PercBodyType {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => PercBodyType::ModalBank,
            2 => PercBodyType::MetallicCluster,
            3 => PercBodyType::NoiseOnly,
            _ => PercBodyType::PitchedOsc,
        }
    }
}

/// Filter topology for the secondary noise layer.
#[repr(u8)]
#[derive(Copy, Clone, Debug, PartialEq, Eq, Default)]
pub enum NoiseFilterMode {
    #[default]
    Highpass = 0,
    Bandpass = 1,
    Lowpass = 2,
}

impl From<u8> for NoiseFilterMode {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => NoiseFilterMode::Bandpass,
            2 => NoiseFilterMode::Lowpass,
            _ => NoiseFilterMode::Highpass,
        }
    }
}

/// Declarative, flat parameter contract for any percussion instrument.
/// All times are in seconds; 0.0 is always a genuine zero (no sentinel overloading).
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct PercPatch {
    pub body_type: PercBodyType,
    pub pitch_hz: f32,
    pub pitch_drop: f32,
    pub pitch_decay_s: f32,
    pub decay_s: f32,
    pub click_level: f32,
    pub click_freq_hz: f32,
    pub burst_count: u8,
    pub burst_spacing_s: f32,
    pub fm_depth: f32,
    pub fm_ratio: f32,
    pub noise_mix: f32,
    pub noise_decay_s: f32,
    pub noise_cutoff_hz: f32,
    pub noise_q: f32,
    pub noise_filter_mode: NoiseFilterMode,
    pub modal_spread: f32,
    pub drive: f32,
    pub fold: f32,
    pub crush: f32,
    pub vel_to_pitch: f32,
    pub vel_to_tone: f32,
    pub variation: f32,
    pub choke_group: u8,
}

impl Default for PercPatch {
    fn default() -> Self {
        Self {
            body_type: PercBodyType::PitchedOsc,
            pitch_hz: 52.0,
            pitch_drop: 160.0,
            pitch_decay_s: 0.035,
            decay_s: 0.25,
            click_level: 0.30,
            click_freq_hz: 2400.0,
            burst_count: 1,
            burst_spacing_s: 0.011,
            fm_depth: 0.0,
            fm_ratio: 2.5,
            noise_mix: 0.0,
            noise_decay_s: 0.15,
            noise_cutoff_hz: 3200.0,
            noise_q: 0.45,
            noise_filter_mode: NoiseFilterMode::Highpass,
            modal_spread: 1.0,
            drive: 1.4,
            fold: 0.0,
            crush: 0.0,
            vel_to_pitch: 0.15,
            vel_to_tone: 0.35,
            variation: 0.10,
            choke_group: 0,
        }
    }
}

impl From<[f32; PERC_PATCH_PARAM_COUNT]> for PercPatch {
    #[inline(always)]
    fn from(p: [f32; PERC_PATCH_PARAM_COUNT]) -> Self {
        Self {
            body_type: PercBodyType::from(p[0].round().max(0.0) as u8),
            pitch_hz: p[1].clamp(20.0, 12000.0),
            pitch_drop: p[2].clamp(0.0, 2000.0),
            pitch_decay_s: p[3].clamp(0.002, 0.5),
            decay_s: p[4].clamp(0.005, 6.0),
            click_level: p[5].clamp(0.0, 1.0),
            click_freq_hz: p[6].clamp(500.0, 12000.0),
            burst_count: (p[7].round() as i32).clamp(1, 6) as u8,
            burst_spacing_s: p[8].clamp(0.003, 0.030),
            fm_depth: p[9].clamp(0.0, 2.0),
            fm_ratio: p[10].clamp(0.5, 8.0),
            noise_mix: p[11].clamp(0.0, 1.0),
            noise_decay_s: p[12].clamp(0.005, 4.0),
            noise_cutoff_hz: p[13].clamp(200.0, 16000.0),
            noise_q: p[14].clamp(0.05, 0.95),
            noise_filter_mode: NoiseFilterMode::from(p[15].round().max(0.0) as u8),
            modal_spread: p[16].clamp(0.5, 3.0),
            drive: p[17].clamp(0.5, 8.0),
            fold: p[18].clamp(0.0, 1.0),
            crush: p[19].clamp(0.0, 1.0),
            vel_to_pitch: p[20].clamp(0.0, 1.0),
            vel_to_tone: p[21].clamp(0.0, 1.0),
            variation: p[22].clamp(0.0, 1.0),
            choke_group: (p[23].round() as i32).clamp(0, 15) as u8,
        }
    }
}

/// Coefficient-cached 2nd-order resonant modal filter (zero transcendental math per sample).
#[derive(Clone, Copy, Default)]
struct CachedModalFilter {
    y1: f32,
    y2: f32,
    a1: f32,
    a2: f32,
    gain: f32,
}

impl CachedModalFilter {
    #[inline(always)]
    fn configure(&mut self, freq_hz: f32, decay_s: f32, gain: f32, sample_rate: f32) {
        let f = freq_hz.clamp(25.0, sample_rate * 0.45);
        let theta = TAU * f / sample_rate;
        let decay_samples = (decay_s * sample_rate).max(20.0);
        let r = (LN_MIN_60DB / decay_samples).exp().clamp(0.85, 0.99995);
        self.a1 = 2.0 * r * theta.cos();
        self.a2 = -(r * r);
        self.gain = gain;
        self.y1 = 0.0;
        self.y2 = 0.0;
    }

    #[inline(always)]
    fn process(&mut self, input: f32) -> f32 {
        let y0 = soft_clip(self.a1 * self.y1 + self.a2 * self.y2) + input * self.gain;
        self.y2 = self.y1;
        self.y1 = y0;
        y0
    }

    #[inline(always)]
    fn reset(&mut self) {
        self.y1 = 0.0;
        self.y2 = 0.0;
    }
}

/// Single polyphonic layered percussion voice (Exciter -> Body -> Shaper).
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

    // Multi-burst exciter state (clap)
    bursts_remaining: u8,
    burst_timer: usize,
    burst_spacing_samples: usize,

    // Smooth choke dampening state
    choking: bool,
    choke_env: f32,
    choke_decay: f32,

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
            body_type: PercBodyType::PitchedOsc,
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

        // Reset phases & filter
        self.phase = 0.12; // Non-zero onset for crisp punch transient
        self.fm_phase = 0.0;
        self.click_phase = 0.0;
        self.metallic_phases = [0.0; NUM_MODES];
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

        // Multi-burst exciter setup (clap)
        let bc = patch.burst_count.clamp(1, 6);
        self.bursts_remaining = bc.saturating_sub(1);
        self.burst_spacing_samples = ((patch.burst_spacing_s * sr) as usize).max(16);
        self.burst_timer = self.burst_spacing_samples;

        // Precompute modal resonator bank or metallic cluster frequencies once on trigger
        let spread = patch.modal_spread.clamp(0.5, 3.0);
        for i in 0..NUM_MODES {
            let ratio = 1.0 + (BASE_MODAL_RATIOS[i] - 1.0) * spread;
            let mode_freq = eff_pitch * ratio;
            let mode_decay = eff_decay * BASE_MODAL_DECAY_SCALES[i];
            self.modes[i].configure(mode_freq, mode_decay, BASE_MODAL_GAINS[i], sr);

            let metal_ratio = 1.0 + (METALLIC_RATIOS[i] - 1.0) * spread;
            self.metallic_freqs[i] = (eff_pitch * metal_ratio).clamp(40.0, sr * 0.45);
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

        // Stage 2: Primary Resonant Body
        let cur_freq = (self.base_pitch_hz + self.pitch_env).clamp(20.0, sample_rate * 0.45);
        let body_sig = match self.body_type {
            PercBodyType::PitchedOsc => {
                let fm = if self.fm_depth > 0.0 {
                    let f = sin_phase(self.fm_phase) * self.fm_depth * self.body_env;
                    self.fm_phase =
                        wrap_phase(self.fm_phase + (cur_freq * self.fm_ratio) / sample_rate);
                    f
                } else {
                    0.0
                };
                let osc = sin_phase(self.phase + fm);
                self.phase = wrap_phase(self.phase + cur_freq / sample_rate);
                osc * self.body_env
            }
            PercBodyType::ModalBank => {
                let exciter = if self.exciter_env > MIN_AUDIBLE_VELOCITY {
                    let e = (self.exciter_env + 0.4 * noise_raw * self.exciter_env) * 0.45;
                    self.exciter_env *= self.exciter_decay;
                    e
                } else {
                    0.0
                };
                let mut sum = 0.0;
                for m in &mut self.modes {
                    sum += m.process(exciter);
                }
                sum * self.body_env
            }
            PercBodyType::MetallicCluster => {
                let mut ring = 1.0_f32;
                let mut pulse_sum = 0.0_f32;
                for i in 0..NUM_MODES {
                    let pw = 0.28 + 0.06 * (i as f32);
                    let pulse = if self.metallic_phases[i] < pw {
                        1.0
                    } else {
                        -1.0
                    };
                    ring *= pulse;
                    pulse_sum += pulse;
                    self.metallic_phases[i] =
                        wrap_phase(self.metallic_phases[i] + self.metallic_freqs[i] / sample_rate);
                }
                let metal = ring * 0.5 + (pulse_sum / NUM_MODES as f32) * 0.5;
                metal * self.body_env * 0.65
            }
            PercBodyType::NoiseOnly => 0.0,
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

        // Apply velocity and smooth choke envelope
        let out = soft_clip(shaped) * self.vel * self.choke_env;

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
        self.noise_filter.reset();
        for m in &mut self.modes {
            m.reset();
        }
    }
}

/// Fixed-capacity polyphonic percussion voice pool with choke groups and LRU voice stealing.
#[derive(Clone)]
pub struct PercVoicePool {
    voices: [PercVoice; MAX_PERC_VOICES],
    age_counter: u64,
}

impl Default for PercVoicePool {
    fn default() -> Self {
        Self::new()
    }
}

impl PercVoicePool {
    pub fn new() -> Self {
        Self {
            voices: std::array::from_fn(|i| {
                PercVoice::with_seed(0x5a82_7999 ^ (i as u32).wrapping_mul(2_654_435_761))
            }),
            age_counter: 0,
        }
    }

    /// Triggers a percussion hit, smoothly choking any voices in the same non-zero choke group.
    pub fn trigger(
        &mut self,
        patch: &PercPatch,
        vel: f32,
        note_freq_hz: Option<f32>,
        sample_rate: f32,
    ) {
        if patch.choke_group > 0 {
            for v in &mut self.voices {
                if v.active && v.choke_group == patch.choke_group {
                    v.choke(sample_rate);
                }
            }
        }

        // Allocate first inactive voice, or steal oldest active voice (LRU)
        let slot = self
            .voices
            .iter()
            .position(|v| !v.active)
            .unwrap_or_else(|| {
                self.voices
                    .iter()
                    .enumerate()
                    .min_by_key(|(_, v)| v.age)
                    .map(|(i, _)| i)
                    .unwrap_or(0)
            });

        self.age_counter = self.age_counter.wrapping_add(1);
        self.voices[slot].age = self.age_counter;
        self.voices[slot].trigger(patch, vel, note_freq_hz, sample_rate);
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        let mut sum = 0.0_f32;
        for v in &mut self.voices {
            if v.active {
                sum += v.process(sample_rate);
            }
        }
        sum
    }

    pub fn active_count(&self) -> usize {
        self.voices.iter().filter(|v| v.active).count()
    }

    pub fn reset(&mut self) {
        for v in &mut self.voices {
            v.reset();
        }
    }
}
