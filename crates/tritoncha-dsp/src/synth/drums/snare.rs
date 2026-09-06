//! Snare drum voice combining body membrane resonance with filtered wire noise.

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, wrap_phase, xorshift32_norm};
use crate::synth::drums::{
    DRUM_SNARE_BODY, DRUM_SNARE_CRACK, DRUM_SNARE_GHOST, DRUM_SNARE_RIM, DRUM_SNARE_WIRE,
    MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP,
};
use std::f32::consts::PI;

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
    pub mode: u8,
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
            noise_seed: 0x19a3b5c7,
            tone_decay: 0.9985,
            noise_decay: 0.9991,
            cutoff_hz: 2400.0,
            resonance: 0.45,
            base_freq: 185.0,
            tone_gain: 0.65,
            noise_gain: 0.85,
            mode: 0,
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
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn trigger_styled(&mut self, vel: f32, style: i32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.vel = v;

        match style {
            DRUM_SNARE_CRACK => {
                self.env_tone = 1.2;
                self.env_noise = 1.35;
                self.cutoff_hz = 3800.0;
                self.resonance = 0.65;
                self.base_freq = 220.0;
                self.noise_decay = 0.9988;
            }
            DRUM_SNARE_WIRE => {
                self.env_tone = 0.35;
                self.env_noise = 1.15;
                self.cutoff_hz = 5200.0;
                self.resonance = 0.35;
                self.base_freq = 175.0;
                self.noise_decay = 0.9993;
            }
            DRUM_SNARE_BODY => {
                self.env_tone = 1.3;
                self.env_noise = 0.4;
                self.cutoff_hz = 1600.0;
                self.resonance = 0.55;
                self.base_freq = 160.0;
                self.noise_decay = 0.9985;
            }
            DRUM_SNARE_GHOST => {
                self.env_tone = 0.45;
                self.env_noise = 0.55;
                self.cutoff_hz = 2800.0;
                self.resonance = 0.3;
                self.base_freq = 190.0;
                self.noise_decay = 0.9980;
            }
            DRUM_SNARE_RIM => {
                self.env_tone = 1.4;
                self.env_noise = 0.2;
                self.cutoff_hz = 4200.0;
                self.resonance = 0.85;
                self.base_freq = 420.0;
                self.noise_decay = 0.9975;
            }
            _ => {
                self.env_tone = 1.0;
                self.env_noise = 1.0;
                self.cutoff_hz = 2400.0;
                self.resonance = 0.45;
                self.base_freq = 185.0;
                self.noise_decay = 0.9991;
            }
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let (tone, noise) = match self.mode {
            1 => {
                // Natural: acoustic wooden shell (185 Hz fundamental + 330 Hz harmonic)
                let tone1 = (self.phase * 2.0 * PI).sin() * 0.70;
                let tone2 = (self.phase * 1.78 * 2.0 * PI).sin() * 0.35;
                let t = (tone1 + tone2) * self.env_tone * self.tone_gain;
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered =
                    self.filter
                        .process_bp(noise_raw, self.cutoff_hz * 0.85, 0.35, sample_rate);
                let n = noise_filtered * self.env_noise * self.noise_gain * 0.90;
                (t, n)
            }
            2 => {
                // IDM: chirped metallic ring modulation + crisp high-frequency micro-burst
                let chirp = (self.phase * 2.0 * PI * PI).sin() * self.env_tone * 0.50;
                let t = ((self.phase + chirp) * 2.0 * PI).sin()
                    * self.env_tone
                    * (self.tone_gain * 1.25);
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered = self.filter.process_bp(
                    noise_raw,
                    (self.cutoff_hz * 1.35).min(14000.0),
                    0.85,
                    sample_rate,
                );
                let n = noise_filtered * self.env_noise * (self.noise_gain * 1.25);
                (t, n)
            }
            3 => {
                // Industrial: overdriven, crunching wire noise and saturated body
                let raw_tone = (self.phase * 2.0 * PI).sin() * self.env_tone * self.tone_gain;
                let t = soft_clip(raw_tone * 2.0);
                let noise_raw = xorshift32_norm(&mut self.noise_seed);
                let noise_filtered =
                    self.filter
                        .process_bp(noise_raw, self.cutoff_hz, 0.65, sample_rate);
                let n = soft_clip(noise_filtered * self.env_noise * self.noise_gain * 1.8);
                (t, n)
            }
            _ => {
                // Analog (default 808/909):
                let t = (self.phase * 2.0 * PI).sin() * self.env_tone * self.tone_gain;
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
