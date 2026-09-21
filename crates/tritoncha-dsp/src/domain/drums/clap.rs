use super::{DrumMode, MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use crate::core::math::{soft_clip, t60_decay_coeff, xorshift32_norm};
use crate::domain::effects::StateVariableFilter;
use crate::engine::DEFAULT_SAMPLE_RATE;

/// Hand clap synthesis parameters.
#[derive(Clone, Copy, Debug)]
pub struct ClapParams {
    pub cutoff_hz: f32,
    pub resonance: f32,
    pub decay_s: f32,
    pub drive: f32,
    pub mode: f32,
}

impl From<[f32; 7]> for ClapParams {
    #[inline(always)]
    fn from(p: [f32; 7]) -> Self {
        Self {
            cutoff_hz: p[0],
            resonance: p[1],
            decay_s: p[2],
            drive: p[3],
            mode: p[6],
        }
    }
}

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

    pub fn set_params(&mut self, params: impl Into<ClapParams>) {
        let p = params.into();
        if p.cutoff_hz > 0.0 {
            self.cutoff_hz = p.cutoff_hz.clamp(200.0, 8000.0);
        }
        if p.resonance >= 0.0 {
            self.resonance = p.resonance.clamp(0.1, 0.95);
        }
        if p.decay_s > 0.0 {
            self.decay_coeff = t60_decay_coeff(p.decay_s.clamp(0.05, 1.5), DEFAULT_SAMPLE_RATE)
                .clamp(0.980, 0.9995);
        }
        if p.drive > 0.0 {
            self.drive = p.drive.clamp(0.2, 3.0);
        }
        if p.mode >= 0.0 {
            self.mode = DrumMode::from(p.mode);
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
