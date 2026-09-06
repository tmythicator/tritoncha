//! Kick drum physical and analog modeling voice.

use crate::dsp::math::{soft_clip, wrap_phase};
use crate::synth::drums::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use std::f32::consts::PI;

/// High-Impact Analog and Club Kick Drum Voice.
/// Features dual-stage pitch sweep, beater click transient, asymmetric 2nd harmonic warmth,
/// and independent punch and sub-bass decay envelopes.
#[derive(Clone)]
pub struct KickVoice {
    pub active: bool,
    phase: f32,
    click_phase: f32,
    pitch_fast: f32,
    env_punch: f32,
    env_body: f32,
    env_click: f32,
    vel: f32,
    base_pitch_hz: f32,
    pitch_drop: f32,
    pitch_decay_coeff: f32,
    punch_decay: f32,
    body_decay: f32,
    click_decay: f32,
    click_level: f32,
    drive: f32,
    pub mode: u8,
}

impl KickVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            phase: 0.0,
            click_phase: 0.0,
            pitch_fast: 0.0,
            env_punch: 0.0,
            env_body: 0.0,
            env_click: 0.0,
            vel: 0.0,
            base_pitch_hz: 48.0,      // Deep sub fundamental
            pitch_drop: 180.0,        // Snap from 228 Hz down to 48 Hz
            pitch_decay_coeff: 0.040, // Drops fast in ~3-4ms (no lingering bubble pitch sweep)
            punch_decay: 0.9965,      // Snappy punch envelope (~15ms)
            body_decay: 0.99955,      // Deep sub sustain (~280ms)
            click_decay: 0.988,       // Transient snap (~2.5ms)
            click_level: 0.35,        // Tactile beater knock
            drive: 1.6,               // Warm 2nd harmonic analog tape drive
            mode: 0,
        }
    }

    #[allow(clippy::too_many_arguments)]
    pub fn set_params(
        &mut self,
        base_pitch: f32,
        pitch_drop: f32,
        pitch_decay: f32,
        decay_s: f32,
        click: f32,
        drive: f32,
        mode: f32,
    ) {
        if base_pitch > 0.0 {
            self.base_pitch_hz = base_pitch.clamp(30.0, 120.0);
        }
        if pitch_drop >= 0.0 {
            self.pitch_drop = pitch_drop.clamp(0.0, 500.0);
        }
        if pitch_decay > 0.0 {
            self.pitch_decay_coeff = pitch_decay.clamp(0.005, 0.20);
        }
        if decay_s > 0.0 {
            let samples = (decay_s.clamp(0.05, 1.5) * 48000.0).max(100.0);
            self.body_decay = (-6.90775 / samples).exp().clamp(0.990, 0.99995);
        }
        if click >= 0.0 {
            self.click_level = click.clamp(0.0, 2.0);
        }
        if drive > 0.0 {
            self.drive = drive.clamp(0.5, 4.0);
        }
        if mode >= 0.0 {
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn trigger(&mut self, vel: f32, sample_rate: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        // Start oscillator slightly offset from zero for instant pressure wave
        self.phase = 0.15;
        self.click_phase = 0.0;
        self.pitch_fast = self.pitch_drop;
        self.env_punch = 1.0;
        self.env_body = 1.0;
        self.env_click = 1.0;
        self.vel = v;
        let _ = sample_rate;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        // Instantaneous frequency: sharp transient drop + rock-solid sub fundamental
        let freq = self.base_pitch_hz + self.pitch_fast;

        let (body_sig, click_sig, click_hz, pitch_drop_mult) = match self.mode {
            1 => {
                // Natural / Acoustic mode:
                // Organic dual-membrane beating: primary head + resonant head
                let head1 = (self.phase * 2.0 * PI).sin();
                let head2 = (self.phase * 1.34 * 2.0 * PI).sin() * 0.40;
                let warm = soft_clip((head1 + head2) * self.drive);
                let click_osc = (self.click_phase * 2.0 * PI).sin();
                let c_sig = click_osc * self.env_click * self.click_level * 0.75;
                let b_sig = warm * (self.env_punch * 0.55 + self.env_body * 0.85);
                (b_sig, c_sig, 1300.0, 0.75) // wooden beater click ~1300 Hz
            }
            2 => {
                // IDM / Glitch mode:
                // Micro-tuned laser pitch snap + FM chirped transient
                let fm = (self.click_phase * 3.8 * 2.0 * PI).sin() * self.env_punch * 0.65;
                let raw = ((self.phase + fm) * 2.0 * PI).sin();
                let warm = soft_clip(raw * (self.drive * 1.35));
                let click_osc = (self.click_phase * 2.0 * PI).sin();
                let c_sig = click_osc * self.env_click * self.click_level * 1.45;
                let b_sig = warm * (self.env_punch * 0.90 + self.env_body * 0.60);
                (b_sig, c_sig, 4800.0, 1.80) // laser chirp click ~4800 Hz
            }
            3 => {
                // Industrial mode:
                // Heavy wavefolded overdrive and distortion
                let raw = (self.phase * 2.0 * PI).sin();
                let folded = (raw * (self.drive * 2.2)).sin();
                let warm = soft_clip(folded * 1.4 + 0.3 * raw * raw);
                let click_osc = if (self.click_phase * 2.0 * PI).sin() > 0.0 {
                    1.0
                } else {
                    -1.0
                };
                let c_sig = click_osc * self.env_click * self.click_level * 1.25;
                let b_sig = warm * (self.env_punch * 0.85 + self.env_body * 0.95);
                (b_sig, c_sig, 2400.0, 1.0)
            }
            _ => {
                // Analog (default 808/909):
                let raw = (self.phase * 2.0 * PI).sin();
                let warm = soft_clip(raw * self.drive + 0.22 * raw * raw);
                let click_osc = (self.click_phase * 2.0 * PI).sin();
                let c_sig = click_osc * self.env_click * self.click_level;
                let b_sig = warm * (self.env_punch * 0.60 + self.env_body * 0.80);
                (b_sig, c_sig, 2400.0, 1.0)
            }
        };

        let sig = soft_clip(body_sig + click_sig) * self.vel * 1.35;

        self.phase = wrap_phase(self.phase + freq / sample_rate);
        self.click_phase = wrap_phase(self.click_phase + click_hz / sample_rate);
        self.pitch_fast *= 1.0 - (self.pitch_decay_coeff * pitch_drop_mult);
        self.env_punch *= self.punch_decay;
        self.env_body *= self.body_decay;
        self.env_click *= self.click_decay;

        if self.env_body < MIN_AUDIBLE_VELOCITY && self.env_punch < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.click_phase = 0.0;
        self.pitch_fast = 0.0;
        self.env_punch = 0.0;
        self.env_body = 0.0;
        self.env_click = 0.0;
    }
}

impl Default for KickVoice {
    fn default() -> Self {
        Self::new()
    }
}
