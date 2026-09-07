//! Membrane drum voices (toms, kicks, and tuned sub-percussion).

use crate::dsp::math::{soft_clip, wrap_phase};
use crate::synth::drums::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use std::f32::consts::PI;

// Tuned frequency ranges for membrane toms
pub const TOM_HIGH_START_HZ: f32 = 240.0;
pub const TOM_HIGH_MIN_HZ: f32 = 170.0;
pub const TOM_MID_START_HZ: f32 = 180.0;
pub const TOM_MID_MIN_HZ: f32 = 120.0;
pub const TOM_LOW_START_HZ: f32 = 120.0;
pub const TOM_LOW_MIN_HZ: f32 = 75.0;

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
    pub mode: u8,
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
            mode: 0,
        }
    }

    pub fn trigger(&mut self, vel: f32, sample_rate: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.freq = self.start_pitch_hz;
        self.env = 1.0;
        self.vel = v;
        let _ = sample_rate;
    }

    pub fn trigger_freq(&mut self, vel: f32, freq: f32, sample_rate: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.freq = freq.max(self.min_pitch_hz);
        self.env = 1.0;
        self.vel = v;
        let _ = sample_rate;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let sine_val = match self.mode {
            1 => {
                // Natural acoustic tom: dual-membrane resonance
                let head1 = (self.phase * 2.0 * PI).sin();
                let head2 = (self.phase * 1.42 * 2.0 * PI).sin() * 0.35;
                head1 + head2
            }
            2 => {
                // IDM laser tom: FM chirped sweep
                let chirp = (self.phase * 2.5 * 2.0 * PI).sin() * 0.5 * self.env;
                ((self.phase + chirp) * 2.0 * PI).sin()
            }
            3 => {
                // Industrial tom: saturated fold
                let raw = (self.phase * 2.0 * PI).sin();
                (raw * 2.2).sin()
            }
            _ => (self.phase * 2.0 * PI).sin(),
        };
        let sig = soft_clip(sine_val * self.drive_gain) * self.env * self.vel * 1.25;

        self.phase = wrap_phase(self.phase + self.freq / sample_rate);

        // Exponential pitch descent
        self.freq += (self.min_pitch_hz - self.freq) * self.pitch_decay_coeff;
        // Exponential amplitude decay
        self.env *= self.amp_decay_coeff;

        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn set_params(
        &mut self,
        start_pitch: f32,
        min_pitch: f32,
        pitch_decay: f32,
        decay_s: f32,
        drive: f32,
        mode: f32,
    ) {
        if start_pitch > 0.0 {
            self.start_pitch_hz = start_pitch.clamp(30.0, 900.0);
        }
        if min_pitch > 0.0 {
            self.min_pitch_hz = min_pitch.clamp(20.0, 600.0);
        }
        if pitch_decay > 0.0 {
            self.pitch_decay_coeff = pitch_decay.clamp(0.001, 0.15);
        }
        if decay_s > 0.0 {
            let samples = (decay_s.clamp(0.02, 3.0) * 48000.0).max(50.0);
            self.amp_decay_coeff = (-6.90775 / samples).exp().clamp(0.980, 0.9999);
        }
        if drive > 0.0 {
            self.drive_gain = drive.clamp(0.2, 4.0);
        }
        if mode >= 0.0 {
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.env = 0.0;
    }
}
