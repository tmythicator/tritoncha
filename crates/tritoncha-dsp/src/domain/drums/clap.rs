use super::cymbals::PercussionParams;
use super::{DrumMode, MIN_AUDIBLE_VELOCITY};
use crate::core::math::{soft_clip, t60_decay_coeff, xorshift32_norm};
use crate::domain::effects::StateVariableFilter;
use crate::domain::{DrumVelocity, DEFAULT_SAMPLE_RATE};

/// Hand Clap Voice with multi-burst envelope.
#[derive(Clone)]
pub struct ClapVoice {
    pub active: bool,
    env: f32,
    vel: f32,
    filter: StateVariableFilter,
    noise_seed: u32,
    cutoff_hz: f32,
    resonance: f32,
    decay_coeff: f32,
    drive: f32,
    pub mode: DrumMode,
}

impl ClapVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            env: 0.0,
            vel: 0.0,
            filter: StateVariableFilter::new(),
            noise_seed: 0x33b9_1a7f,
            cutoff_hz: 1200.0,
            resonance: 0.70,
            decay_coeff: 0.9982,
            drive: 1.0,
            mode: DrumMode::default(),
        }
    }

    pub fn set_params(&mut self, params: impl Into<PercussionParams>) {
        let p = params.into();
        self.cutoff_hz = *p.cutoff;
        self.resonance = *p.resonance;
        self.decay_coeff = t60_decay_coeff(*p.decay, DEFAULT_SAMPLE_RATE);
        self.drive = *p.drive;
        self.mode = p.mode;
    }

    pub fn trigger(&mut self, vel: impl Into<DrumVelocity>) {
        self.active = true;
        self.env = 1.0;
        self.vel = *vel.into();
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);
        let noise = match self.mode {
            DrumMode::Natural => noise_raw * 0.9,
            DrumMode::Idm => (noise_raw * 6.0).round() / 6.0,
            DrumMode::Industrial => soft_clip(noise_raw * 2.2),
            DrumMode::Analog => noise_raw,
        };
        let filtered = self
            .filter
            .process_bp(noise, self.cutoff_hz, self.resonance, sample_rate);
        let sig = soft_clip(filtered * self.drive) * self.env * self.vel * 0.8;

        self.env *= self.decay_coeff;
        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.env = 0.0;
        self.filter.reset();
    }
}

impl Default for ClapVoice {
    fn default() -> Self {
        Self::new()
    }
}
