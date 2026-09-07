//! Physical modeling modal synthesis for ride cymbal bell (cup).
//! Simulates non-linear strike deformation, stick tip contact transient,
//! and five inharmonic modal plate resonances of B20 bronze.

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, xorshift32_norm};
use crate::synth::drums::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use std::f32::consts::PI;

/// Second-order resonant modal filter for acoustic cymbal cups.
#[derive(Clone, Copy)]
struct ModalFilter {
    y1: f32,
    y2: f32,
    base_freq: f32,
    decay_s: f32,
    gain: f32,
}

impl ModalFilter {
    fn new(base_freq: f32, decay_s: f32, gain: f32) -> Self {
        Self {
            y1: 0.0,
            y2: 0.0,
            base_freq,
            decay_s,
            gain,
        }
    }

    fn reset(&mut self) {
        self.y1 = 0.0;
        self.y2 = 0.0;
    }

    #[inline(always)]
    fn process(&mut self, input: f32, pitch_ratio: f32, decay_scale: f32, sample_rate: f32) -> f32 {
        let f = (self.base_freq * pitch_ratio).clamp(40.0, sample_rate * 0.46);
        let theta = 2.0 * PI * f / sample_rate;
        let decay_samples = (self.decay_s * decay_scale * sample_rate).max(20.0);
        let r = (-6.90775 / decay_samples).exp().clamp(0.85, 0.99995);

        let a1 = 2.0 * r * theta.cos();
        let a2 = -(r * r);

        let y0 = soft_clip(a1 * self.y1 + a2 * self.y2) + input * self.gain;
        self.y2 = self.y1;
        self.y1 = y0;
        y0
    }
}

/// Ride Cymbal Bell (Cup) Physical Modeling Voice.
/// Utilizes a 5-mode modal resonator bank calibrated to acoustic B20 bronze cup geometry,
/// coupled with a stick tip contact transient and strike pitch deflection.
#[derive(Clone)]
pub struct RideBellVoice {
    pub active: bool,
    modes: [ModalFilter; 5],
    click_env: f32,
    impulse_env: f32,
    pitch_env: f32,
    vel: f32,
    noise_seed: u32,
    click_filter: StateVariableFilter,
    tone_filter: StateVariableFilter,
    decay_scale: f32,
    tune_ratio: f32,
    drive: f32,
    pub mode: u8,
}

impl RideBellVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            modes: [
                // Mode 0: Deep cup body resonance (gives the bell acoustic weight)
                ModalFilter::new(610.0, 1.75, 0.38),
                // Mode 1: Primary singing bell ping (the focal melodic ping)
                ModalFilter::new(1385.0, 1.45, 0.72),
                // Mode 2: Secondary inharmonic overtone
                ModalFilter::new(2620.0, 0.85, 0.44),
                // Mode 3: Upper bronze chime
                ModalFilter::new(4120.0, 0.42, 0.28),
                // Mode 4: High metallic sheen
                ModalFilter::new(6350.0, 0.18, 0.18),
            ],
            click_env: 0.0,
            impulse_env: 0.0,
            pitch_env: 0.0,
            vel: 0.0,
            noise_seed: 0x61c8_8647,
            click_filter: StateVariableFilter::new(),
            tone_filter: StateVariableFilter::new(),
            decay_scale: 1.0,
            tune_ratio: 1.0,
            drive: 1.2,
            mode: 1, // Default to Natural acoustic B20 bell
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
            // Map cutoff around default 1385Hz center
            self.tune_ratio = (cutoff_hz / 1385.0).clamp(0.6, 1.8);
        }
        if resonance >= 0.0 {
            // Resonance boosts drive and clarity
            self.drive = (1.0 + resonance * 1.5).clamp(0.5, 3.5);
        }
        if decay_s > 0.0 {
            self.decay_scale = (decay_s / 1.45).clamp(0.15, 3.5);
        }
        if drive > 0.0 {
            self.drive = drive.clamp(0.2, 4.0);
        }
        if mode >= 0.0 {
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.vel = v;
        self.click_env = 1.0;
        self.impulse_env = 1.0;
        self.pitch_env = 1.0;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);

        // 1. Wooden stick tip strike transient (~2.5ms sharp click)
        let click = if self.click_env > 0.001 {
            let click_filtered = self.click_filter.process_bp(
                noise_raw,
                (5400.0 * self.tune_ratio).clamp(2500.0, 9500.0),
                0.70,
                sample_rate,
            );
            let c = click_filtered * self.click_env * 0.40;
            self.click_env *= 0.968;
            c
        } else {
            0.0
        };

        // 2. Physical strike deformation: momentary pitch drop (+4.8% on impact down to 0)
        let pitch_mod = if self.pitch_env > 0.001 {
            let p = 1.0 + 0.048 * self.pitch_env;
            self.pitch_env *= 0.985; // ~5ms relaxation
            p
        } else {
            1.0
        };
        let effective_tune = self.tune_ratio * pitch_mod;

        // Mode adjustments
        let (mode_decay, mode_drive, mode_color) = match self.mode {
            0 => (self.decay_scale * 0.85, self.drive * 1.3, 0.9), // Analog / 808 flavor
            2 => (self.decay_scale * 0.65, self.drive * 1.0, 0.6), // Dark / Dry Jazz bell
            3 => (self.decay_scale * 1.45, self.drive * 1.25, 1.2), // Bright Cutting Rock bell
            _ => (self.decay_scale, self.drive, 1.0),              // Natural B20 Raw Bell
        };

        // 3. Exciter impulse window (~2.0ms) injected into modal filters
        let exciter = if self.impulse_env > 0.001 {
            let e = (self.impulse_env + 0.35 * noise_raw * self.impulse_env) * self.vel * 0.5;
            self.impulse_env *= 0.958;
            e
        } else {
            0.0
        };

        // 4. Modal Resonator Bank (5 inharmonic modes)
        let mut modal_sum = 0.0;
        for (i, m) in self.modes.iter_mut().enumerate() {
            let mode_decay_factor = match i {
                0 => mode_decay * 1.2, // Body hum sustains longest
                1 => mode_decay * 1.0, // Ping
                2 => mode_decay * 0.75,
                3 => mode_decay * 0.50,
                _ => mode_decay * 0.30, // High sparkle fades rapidly
            };
            modal_sum += m.process(exciter, effective_tune, mode_decay_factor, sample_rate);
        }

        // 5. Output shaping and warm acoustic saturation
        let shaped = self.tone_filter.process_lp(
            modal_sum,
            (9500.0 * mode_color * self.tune_ratio).clamp(3000.0, sample_rate * 0.45),
            0.15,
            sample_rate,
        );
        let out = soft_clip((shaped * mode_drive + click) * self.vel);

        // Turn off when energy decays below audible threshold
        if self.impulse_env <= 0.001 && self.click_env <= 0.001 && out.abs() < MIN_AUDIBLE_VELOCITY
        {
            let max_state = self.modes.iter().fold(0.0f32, |acc, m| acc.max(m.y1.abs()));
            if max_state < MIN_AUDIBLE_VELOCITY {
                self.active = false;
            }
        }

        out
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.click_env = 0.0;
        self.impulse_env = 0.0;
        self.pitch_env = 0.0;
        for m in &mut self.modes {
            m.reset();
        }
        self.click_filter.reset();
        self.tone_filter.reset();
    }
}

impl Default for RideBellVoice {
    fn default() -> Self {
        Self::new()
    }
}
