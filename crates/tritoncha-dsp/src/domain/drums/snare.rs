//! Snare drum voice combining body membrane resonance with filtered wire noise.

use super::{
    DrumMode, DRUM_SNARE_BODY, DRUM_SNARE_CRACK, DRUM_SNARE_GHOST, DRUM_SNARE_RIM, DRUM_SNARE_WIRE,
    MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP,
};
use crate::core::math::{sin_phase, soft_clip, wrap_phase, xorshift32_norm};
use crate::domain::effects::StateVariableFilter;

// Default Snare Voice Tuning Constants
pub const DEFAULT_SNARE_BASE_FREQ: f32 = 185.0;
pub const DEFAULT_SNARE_TONE_DECAY: f32 = 0.9985;
pub const DEFAULT_SNARE_NOISE_DECAY: f32 = 0.9991;
pub const DEFAULT_SNARE_CUTOFF_HZ: f32 = 2400.0;
pub const DEFAULT_SNARE_RESONANCE: f32 = 0.45;
pub const DEFAULT_SNARE_TONE_GAIN: f32 = 0.65;
pub const DEFAULT_SNARE_NOISE_GAIN: f32 = 0.85;
pub const DEFAULT_SNARE_SEED: u32 = 0x19a3b5c7;

// Mode 1: Natural (Acoustic wooden shell with 185 Hz fundamental + 330 Hz harmonic)
pub const SNARE_NATURAL_HEAD1_GAIN: f32 = 0.70;
pub const SNARE_NATURAL_HEAD2_MULT: f32 = 1.78;
pub const SNARE_NATURAL_HEAD2_GAIN: f32 = 0.35;
pub const SNARE_NATURAL_BP_CUTOFF_MULT: f32 = 0.85;
pub const SNARE_NATURAL_BP_Q: f32 = 0.35;
pub const SNARE_NATURAL_NOISE_MULT: f32 = 0.90;

// Mode 2: IDM (Chirped metallic ring modulation and crisp micro-burst)
pub const SNARE_IDM_CHIRP_DEPTH: f32 = 0.50;
pub const SNARE_IDM_TONE_GAIN_MULT: f32 = 1.25;
pub const SNARE_IDM_BP_CUTOFF_MULT: f32 = 1.35;
pub const SNARE_IDM_BP_MAX_CUTOFF: f32 = 14000.0;
pub const SNARE_IDM_BP_Q: f32 = 0.85;
pub const SNARE_IDM_NOISE_GAIN_MULT: f32 = 1.25;

// Mode 3: Industrial (Overdriven wire noise and saturated body)
pub const SNARE_IND_OVERDRIVE: f32 = 2.0;
pub const SNARE_IND_BP_Q: f32 = 0.65;
pub const SNARE_IND_NOISE_BOOST: f32 = 1.8;

/// Parameters defining acoustic and articulation characteristics for a snare hit style.
#[derive(Debug, Clone, Copy)]
pub struct SnareStylePreset {
    pub env_tone: f32,
    pub env_noise: f32,
    pub cutoff_hz: f32,
    pub resonance: f32,
    pub base_freq: f32,
    pub noise_decay: f32,
}

pub const STYLE_CRACK: SnareStylePreset = SnareStylePreset {
    env_tone: 1.2,
    env_noise: 1.35,
    cutoff_hz: 3800.0,
    resonance: 0.65,
    base_freq: 220.0,
    noise_decay: 0.9988,
};

pub const STYLE_WIRE: SnareStylePreset = SnareStylePreset {
    env_tone: 0.35,
    env_noise: 1.15,
    cutoff_hz: 5200.0,
    resonance: 0.35,
    base_freq: 175.0,
    noise_decay: 0.9993,
};

pub const STYLE_BODY: SnareStylePreset = SnareStylePreset {
    env_tone: 1.3,
    env_noise: 0.4,
    cutoff_hz: 1600.0,
    resonance: 0.55,
    base_freq: 160.0,
    noise_decay: 0.9985,
};

pub const STYLE_GHOST: SnareStylePreset = SnareStylePreset {
    env_tone: 0.45,
    env_noise: 0.55,
    cutoff_hz: 2800.0,
    resonance: 0.3,
    base_freq: 190.0,
    noise_decay: 0.9980,
};

pub const STYLE_RIM: SnareStylePreset = SnareStylePreset {
    env_tone: 1.4,
    env_noise: 0.2,
    cutoff_hz: 4200.0,
    resonance: 0.85,
    base_freq: 420.0,
    noise_decay: 0.9975,
};

pub const STYLE_DEFAULT: SnareStylePreset = SnareStylePreset {
    env_tone: 1.0,
    env_noise: 1.0,
    cutoff_hz: DEFAULT_SNARE_CUTOFF_HZ,
    resonance: DEFAULT_SNARE_RESONANCE,
    base_freq: DEFAULT_SNARE_BASE_FREQ,
    noise_decay: DEFAULT_SNARE_NOISE_DECAY,
};

/// Snare Drum Voice combining body membrane resonance with filtered wire noise.
#[derive(Clone)]
pub struct SnareVoice {
    pub active: bool,
    phase: f32,
    env_tone: f32,
    env_noise: f32,
    vel: f32,
    filter: StateVariableFilter,
    noise_seed: u32,
    tone_decay: f32,
    noise_decay: f32,
    cutoff_hz: f32,
    resonance: f32,
    base_freq: f32,
    tone_gain: f32,
    noise_gain: f32,
    pub mode: DrumMode,
}

impl SnareVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            phase: 0.0,
            env_tone: 0.0,
            env_noise: 0.0,
            vel: 0.0,
            filter: StateVariableFilter::new(),
            noise_seed: DEFAULT_SNARE_SEED,
            tone_decay: DEFAULT_SNARE_TONE_DECAY,
            noise_decay: DEFAULT_SNARE_NOISE_DECAY,
            cutoff_hz: DEFAULT_SNARE_CUTOFF_HZ,
            resonance: DEFAULT_SNARE_RESONANCE,
            base_freq: DEFAULT_SNARE_BASE_FREQ,
            tone_gain: DEFAULT_SNARE_TONE_GAIN,
            noise_gain: DEFAULT_SNARE_NOISE_GAIN,
            mode: DrumMode::default(),
        }
    }

    pub fn set_params(
        &mut self,
        base_freq: f32,
        tone_decay: f32,
        noise_decay: f32,
        cutoff_hz: f32,
        snappy: f32,
        mode: f32,
    ) {
        if base_freq > 0.0 {
            self.base_freq = base_freq.clamp(80.0, 500.0);
        }
        if tone_decay > 0.0 {
            self.tone_decay = tone_decay.clamp(0.990, 0.9999);
        }
        if noise_decay > 0.0 {
            self.noise_decay = noise_decay.clamp(0.990, 0.9999);
        }
        if cutoff_hz > 0.0 {
            self.cutoff_hz = cutoff_hz.clamp(800.0, 12000.0);
        }
        if snappy >= 0.0 {
            self.noise_gain = snappy.clamp(0.0, 2.5);
        }
        if mode >= 0.0 {
            self.mode = DrumMode::from(mode);
        }
    }

    pub fn trigger_styled(&mut self, vel: f32, style: i32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.vel = v;

        let preset = match style {
            DRUM_SNARE_CRACK => STYLE_CRACK,
            DRUM_SNARE_WIRE => STYLE_WIRE,
            DRUM_SNARE_BODY => STYLE_BODY,
            DRUM_SNARE_GHOST => STYLE_GHOST,
            DRUM_SNARE_RIM => STYLE_RIM,
            _ => STYLE_DEFAULT,
        };

        self.env_tone = preset.env_tone;
        self.env_noise = preset.env_noise;
        self.cutoff_hz = preset.cutoff_hz;
        self.resonance = preset.resonance;
        self.base_freq = preset.base_freq;
        self.noise_decay = preset.noise_decay;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let (tone, noise) = match self.mode {
            DrumMode::Natural => {
                // Natural: acoustic wooden shell (185 Hz fundamental + 330 Hz harmonic)
                let tone1 = sin_phase(self.phase) * SNARE_NATURAL_HEAD1_GAIN;
                let tone2 =
                    sin_phase(self.phase * SNARE_NATURAL_HEAD2_MULT) * SNARE_NATURAL_HEAD2_GAIN;
                let t = (tone1 + tone2) * self.env_tone * self.tone_gain;
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered = self.filter.process_bp(
                    noise_raw,
                    self.cutoff_hz * SNARE_NATURAL_BP_CUTOFF_MULT,
                    SNARE_NATURAL_BP_Q,
                    sample_rate,
                );
                let n =
                    noise_filtered * self.env_noise * self.noise_gain * SNARE_NATURAL_NOISE_MULT;
                (t, n)
            }
            DrumMode::Idm => {
                // IDM: chirped metallic ring modulation + crisp high-frequency micro-burst
                let chirp = (self.phase * std::f32::consts::TAU * std::f32::consts::PI).sin()
                    * self.env_tone
                    * SNARE_IDM_CHIRP_DEPTH;
                let t = sin_phase(self.phase + chirp)
                    * self.env_tone
                    * (self.tone_gain * SNARE_IDM_TONE_GAIN_MULT);
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered = self.filter.process_bp(
                    noise_raw,
                    (self.cutoff_hz * SNARE_IDM_BP_CUTOFF_MULT).min(SNARE_IDM_BP_MAX_CUTOFF),
                    SNARE_IDM_BP_Q,
                    sample_rate,
                );
                let n =
                    noise_filtered * self.env_noise * (self.noise_gain * SNARE_IDM_NOISE_GAIN_MULT);
                (t, n)
            }
            DrumMode::Industrial => {
                // Industrial: overdriven, crunching wire noise and saturated body
                let raw_tone = sin_phase(self.phase) * self.env_tone * self.tone_gain;
                let t = soft_clip(raw_tone * SNARE_IND_OVERDRIVE);
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered =
                    self.filter
                        .process_bp(noise_raw, self.cutoff_hz, SNARE_IND_BP_Q, sample_rate);
                let n = soft_clip(
                    noise_filtered * self.env_noise * self.noise_gain * SNARE_IND_NOISE_BOOST,
                );
                (t, n)
            }
            DrumMode::Analog => {
                // Analog (default 808/909):
                let t = sin_phase(self.phase) * self.env_tone * self.tone_gain;
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered =
                    self.filter
                        .process_bp(noise_raw, self.cutoff_hz, self.resonance, sample_rate);
                let n = noise_filtered * self.env_noise * self.noise_gain;
                (t, n)
            }
        };

        let sig = soft_clip(tone + noise) * self.vel;

        self.phase = wrap_phase(self.phase + self.base_freq / sample_rate);
        self.env_tone *= self.tone_decay;
        self.env_noise *= self.noise_decay;

        if self.env_tone < MIN_AUDIBLE_VELOCITY && self.env_noise < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.env_tone = 0.0;
        self.env_noise = 0.0;
        self.filter.reset();
    }
}

impl Default for SnareVoice {
    fn default() -> Self {
        Self::new()
    }
}
