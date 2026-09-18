//! Membrane drum voices (toms, kicks, and tuned sub-percussion).

use super::{DrumMode, MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use crate::core::math::{soft_clip, t60_decay_coeff, wrap_phase};
use crate::engine::DEFAULT_SAMPLE_RATE;
use std::f32::consts::PI;

// Tuned frequency ranges for membrane toms
pub const TOM_HIGH_START_HZ: f32 = 240.0;
pub const TOM_HIGH_MIN_HZ: f32 = 170.0;
pub const TOM_MID_START_HZ: f32 = 180.0;
pub const TOM_MID_MIN_HZ: f32 = 120.0;
pub const TOM_LOW_START_HZ: f32 = 120.0;
pub const TOM_LOW_MIN_HZ: f32 = 80.0;
pub const TOM_DEFAULT_PITCH_DECAY: f32 = 0.015;
pub const TOM_DEFAULT_AMP_DECAY: f32 = 0.9992;
pub const TOM_DEFAULT_DRIVE: f32 = 1.0;

/// Membrane drum synthesis parameters.
#[derive(Clone, Copy, Debug)]
pub struct MembraneParams {
    pub start_pitch_hz: f32,
    pub min_pitch_hz: f32,
    pub pitch_decay: f32,
    pub decay_s: f32,
    pub drive: f32,
    pub mode: f32,
}

impl From<[f32; 7]> for MembraneParams {
    #[inline(always)]
    fn from(p: [f32; 7]) -> Self {
        Self {
            start_pitch_hz: p[0],
            min_pitch_hz: p[1],
            pitch_decay: p[2],
            decay_s: p[3],
            drive: p[4],
            mode: p[6],
        }
    }
}

/// Membrane Drum Voice modeling toms and tuned sub-percussion.
#[derive(Clone)]
pub struct MembraneVoice {
    pub active: bool,
    phase: f32,
    freq: f32,
    env: f32,
    vel: f32,
    start_pitch_hz: f32,
    min_pitch_hz: f32,
    pitch_decay_coeff: f32,
    amp_decay_coeff: f32,
    drive_gain: f32,
    pub mode: DrumMode,
}

impl MembraneVoice {
    pub fn new(
        start_pitch_hz: f32,
        min_pitch_hz: f32,
        pitch_decay_coeff: f32,
        amp_decay_coeff: f32,
        drive_gain: f32,
    ) -> Self {
        Self {
            active: false,
            phase: 0.0,
            freq: min_pitch_hz,
            env: 0.0,
            vel: 0.0,
            start_pitch_hz,
            min_pitch_hz,
            pitch_decay_coeff,
            amp_decay_coeff,
            drive_gain,
            mode: DrumMode::default(),
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.freq = self.start_pitch_hz;
        self.env = 1.0;
        self.vel = v;
    }

    pub fn trigger_freq(&mut self, vel: f32, freq: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.freq = freq.max(self.min_pitch_hz);
        self.env = 1.0;
        self.vel = v;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let sine_val = match self.mode {
            DrumMode::Natural => {
                let head1 = (self.phase * 2.0 * PI).sin();
                let head2 = (self.phase * 1.42 * 2.0 * PI).sin() * 0.35;
                head1 + head2
            }
            DrumMode::Idm => {
                let chirp = (self.phase * 2.5 * 2.0 * PI).sin() * 0.5 * self.env;
                ((self.phase + chirp) * 2.0 * PI).sin()
            }
            DrumMode::Industrial => {
                let raw = (self.phase * 2.0 * PI).sin();
                (raw * 2.2).sin()
            }
            DrumMode::Analog => (self.phase * 2.0 * PI).sin(),
        };
        let sig = soft_clip(sine_val * self.drive_gain) * self.env * self.vel * 1.25;

        self.phase = wrap_phase(self.phase + self.freq / sample_rate);

        self.freq += (self.min_pitch_hz - self.freq) * self.pitch_decay_coeff;
        self.env *= self.amp_decay_coeff;

        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn set_params(&mut self, params: impl Into<MembraneParams>) {
        let p = params.into();
        if p.start_pitch_hz > 0.0 {
            self.start_pitch_hz = p.start_pitch_hz.clamp(30.0, 900.0);
        }
        if p.min_pitch_hz > 0.0 {
            self.min_pitch_hz = p.min_pitch_hz.clamp(20.0, 600.0);
        }
        if p.pitch_decay > 0.0 {
            self.pitch_decay_coeff = p.pitch_decay.clamp(0.001, 0.15);
        }
        if p.decay_s > 0.0 {
            self.amp_decay_coeff = t60_decay_coeff(p.decay_s.clamp(0.02, 3.0), DEFAULT_SAMPLE_RATE)
                .clamp(0.980, 0.9999);
        }
        if p.drive > 0.0 {
            self.drive_gain = p.drive.clamp(0.2, 4.0);
        }
        if p.mode >= 0.0 {
            self.mode = DrumMode::from(p.mode);
        }
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.env = 0.0;
    }
}
