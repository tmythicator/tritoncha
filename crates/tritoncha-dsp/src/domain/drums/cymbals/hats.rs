//! Hi-hat voice modeling closed, open, and pedal hi-hats.

use crate::core::math::{soft_clip, t60_decay_coeff, xorshift32_norm};
use crate::domain::drums::{
    DrumMode, MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP,
};
use crate::domain::effects::StateVariableFilter;
use crate::engine::DEFAULT_SAMPLE_RATE;

/// Hi-hat synthesis parameters.
#[derive(Clone, Copy, Debug)]
pub struct HatParams {
    pub cutoff_hz: f32,
    pub decay_closed_s: f32,
    pub decay_open_s: f32,
    pub mode: f32,
}

impl From<[f32; 7]> for HatParams {
    #[inline(always)]
    fn from(p: [f32; 7]) -> Self {
        Self {
            cutoff_hz: p[0],
            decay_closed_s: p[1],
            decay_open_s: p[2],
            mode: p[6],
        }
    }
}

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
    pub mode: DrumMode,
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
            mode: DrumMode::default(),
        }
    }

    pub fn set_params(&mut self, params: impl Into<HatParams>) {
        let p = params.into();
        if p.cutoff_hz > 0.0 {
            self.cutoff_hz = p.cutoff_hz.clamp(3000.0, 14000.0);
        }
        if p.decay_closed_s > 0.0 {
            self.decay_closed =
                t60_decay_coeff(p.decay_closed_s.clamp(0.01, 0.5), DEFAULT_SAMPLE_RATE)
                    .clamp(0.980, 0.999);
        }
        if p.decay_open_s > 0.0 {
            self.decay_open = t60_decay_coeff(p.decay_open_s.clamp(0.05, 1.5), DEFAULT_SAMPLE_RATE)
                .clamp(0.990, 0.9999);
        }
        if p.mode >= 0.0 {
            self.mode = DrumMode::from(p.mode);
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
            self.click_env *= 0.965;
            c
        } else {
            0.0
        };

        let noise = match self.mode {
            DrumMode::Natural => noise_raw * 0.85,
            DrumMode::Idm => (noise_raw * 8.0).round() / 8.0,
            DrumMode::Industrial => soft_clip(noise_raw * 2.4),
            DrumMode::Analog => noise_raw,
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
