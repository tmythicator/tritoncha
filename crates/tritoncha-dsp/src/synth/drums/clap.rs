use super::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, xorshift32_norm};

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
    pub mode: u8,
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
            mode: 0,
        }
    }

    pub fn set_params(
        &mut self,
        cutoff_hz: f32,
        resonance: f32,
        decay_s: f32,
        drive: f32,
        mode: f32,
    ) {
        if cutoff_hz > 0.0 {
            self.cutoff_hz = cutoff_hz.clamp(200.0, 8000.0);
        }
        if resonance >= 0.0 {
            self.resonance = resonance.clamp(0.1, 0.95);
        }
        if decay_s > 0.0 {
            let samples = (decay_s.clamp(0.05, 1.5) * 48000.0).max(50.0);
            self.decay_coeff = (-6.90775 / samples).exp().clamp(0.980, 0.9995);
        }
        if drive > 0.0 {
            self.drive = drive.clamp(0.2, 3.0);
        }
        if mode >= 0.0 {
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.env = 1.0;
        self.vel = v;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);
        let noise = match self.mode {
            1 => noise_raw * 0.9,
            2 => (noise_raw * 6.0).round() / 6.0,
            3 => soft_clip(noise_raw * 2.2),
            _ => noise_raw,
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
