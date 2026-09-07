//! Hi-hat voice modeling closed, open, and pedal hi-hats.

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, xorshift32_norm};
use crate::synth::drums::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};

/// Noise Hi-Hat Voice (Closed and Open).
#[derive(Clone)]
pub struct NoiseHatVoice {
    pub active: bool,
    env: f32,
    decay: f32,
    click_env: f32,
    vel: f32,
    filter: StateVariableFilter,
    noise_seed: u32,
    cutoff_hz: f32,
    decay_closed: f32,
    decay_open: f32,
    pub mode: u8,
}

impl NoiseHatVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            env: 0.0,
            decay: 0.994,
            click_env: 0.0,
            vel: 0.0,
            filter: StateVariableFilter::new(),
            noise_seed: 0x5a827999,
            cutoff_hz: 7200.0,
            decay_closed: 0.9940,
            decay_open: 0.9992,
            mode: 0,
        }
    }

    pub fn set_params(
        &mut self,
        cutoff_hz: f32,
        decay_closed_s: f32,
        decay_open_s: f32,
        mode: f32,
    ) {
        if cutoff_hz > 0.0 {
            self.cutoff_hz = cutoff_hz.clamp(3000.0, 14000.0);
        }
        if decay_closed_s > 0.0 {
            let samples = (decay_closed_s.clamp(0.01, 0.5) * 48000.0).max(10.0);
            self.decay_closed = (-6.90775 / samples).exp().clamp(0.980, 0.999);
        }
        if decay_open_s > 0.0 {
            let samples = (decay_open_s.clamp(0.05, 1.5) * 48000.0).max(50.0);
            self.decay_open = (-6.90775 / samples).exp().clamp(0.990, 0.9999);
        }
        if mode >= 0.0 {
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn trigger(&mut self, vel: f32, open: bool) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.env = 1.0;
        self.click_env = 1.0;
        self.vel = v;
        self.decay = if open {
            self.decay_open
        } else {
            self.decay_closed
        };
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);
        let click = if self.click_env > 0.001 {
            let c = noise_raw * self.click_env * 0.55;
            self.click_env *= 0.965; // ~4ms sharp stick tip transient
            c
        } else {
            0.0
        };

        let noise = match self.mode {
            1 => noise_raw * 0.85,
            2 => (noise_raw * 8.0).round() / 8.0,
            3 => soft_clip(noise_raw * 2.4),
            _ => noise_raw,
        };
        let filtered = self
            .filter
            .process_hp(noise, self.cutoff_hz, 0.60, sample_rate);
        let sig = (filtered + click) * self.env * self.vel * 0.80;

        self.env *= self.decay;
        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.env = 0.0;
        self.click_env = 0.0;
        self.filter.reset();
    }
}

impl Default for NoiseHatVoice {
    fn default() -> Self {
        Self::new()
    }
}
