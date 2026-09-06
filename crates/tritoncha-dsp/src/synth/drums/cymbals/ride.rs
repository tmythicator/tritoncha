//! Physical modeling acoustic ride cymbal voice.
//! Integrates stick tip ping definition, low bronze plate body resonance,
//! and a 4-stage Schroeder allpass diffuser network simulating the turbulent wash of B20 bronze.

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, xorshift32_norm};
use crate::synth::drums::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use std::f32::consts::PI;

/// Second-order resonant modal filter for plate vibration modes.
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
        let f = (self.base_freq * pitch_ratio).clamp(30.0, sample_rate * 0.45);
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

/// Fixed-delay Schroeder allpass diffuser unit for bronze plate dispersion.
#[derive(Clone)]
struct AllpassUnit<const D: usize> {
    buf: [f32; D],
    idx: usize,
}

impl<const D: usize> AllpassUnit<D> {
    fn new() -> Self {
        Self {
            buf: [0.0; D],
            idx: 0,
        }
    }

    fn reset(&mut self) {
        self.buf = [0.0; D];
        self.idx = 0;
    }

    #[inline(always)]
    fn process(&mut self, input: f32, g: f32) -> f32 {
        let delayed = self.buf[self.idx];
        let out = -g * input + delayed;
        self.buf[self.idx] = input + g * out;
        self.idx += 1;
        if self.idx >= D {
            self.idx = 0;
        }
        out
    }
}

/// Physical Modeling Ride Cymbal Voice (Bow, Ping, Body, and Shimmer Wash).
#[derive(Clone)]
pub struct RideVoice {
    pub active: bool,
    modes: [ModalFilter; 4],
    diffuser_1: AllpassUnit<19>,
    diffuser_2: AllpassUnit<37>,
    diffuser_3: AllpassUnit<61>,
    diffuser_4: AllpassUnit<97>,
    wash_feedback: f32,
    wash_damp: f32,
    wash_env: f32,
    wash_attack: f32,
    wash_decay: f32,
    click_env: f32,
    ping_env: f32,
    vel: f32,
    noise_seed: u32,
    click_filter: StateVariableFilter,
    wash_filter: StateVariableFilter,
    output_filter: StateVariableFilter,
    decay_scale: f32,
    tune_ratio: f32,
    drive: f32,
    pub mode: u8,
}

impl RideVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            modes: [
                // Mode 0: Deep plate rumble and fundamental
                ModalFilter::new(235.0, 2.20, 0.32),
                // Mode 1: Mid-body warmth
                ModalFilter::new(385.0, 1.80, 0.35),
                // Mode 2: Bow ping acoustic focus
                ModalFilter::new(885.0, 1.40, 0.55),
                // Mode 3: Upper harmonic bow ping
                ModalFilter::new(1820.0, 1.10, 0.40),
            ],
            diffuser_1: AllpassUnit::new(),
            diffuser_2: AllpassUnit::new(),
            diffuser_3: AllpassUnit::new(),
            diffuser_4: AllpassUnit::new(),
            wash_feedback: 0.0,
            wash_damp: 0.0,
            wash_env: 0.0,
            wash_attack: 0.0,
            wash_decay: 0.99988,
            click_env: 0.0,
            ping_env: 0.0,
            vel: 0.0,
            noise_seed: 0x72a5_4931,
            click_filter: StateVariableFilter::new(),
            wash_filter: StateVariableFilter::new(),
            output_filter: StateVariableFilter::new(),
            decay_scale: 1.0,
            tune_ratio: 1.0,
            drive: 1.15,
            mode: 1, // Default to Natural 22" B20 acoustic ride
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
            self.tune_ratio = (cutoff_hz / 950.0).clamp(0.5, 2.0);
        }
        if resonance >= 0.0 {
            self.drive = (1.0 + resonance * 1.5).clamp(0.5, 3.5);
        }
        if decay_s > 0.0 {
            let samples = (decay_s.clamp(0.2, 5.0) * 48000.0).max(50.0);
            self.wash_decay = (-6.90775 / samples).exp().clamp(0.995, 0.99998);
            self.decay_scale = (decay_s / 2.0).clamp(0.2, 3.0);
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
        self.ping_env = 1.0;
        self.wash_env = 0.2; // Start with fast initial excitation
        self.wash_attack = 1.0; // Bloom over 20-30ms
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise_raw = xorshift32_norm(&mut self.noise_seed);

        // 1. Stick Tip Ping Transient (~3.5ms crisp wooden contact)
        let click = if self.click_env > 0.001 {
            let filtered_click = self.click_filter.process_bp(
                noise_raw,
                (4700.0 * self.tune_ratio).clamp(2000.0, 9500.0),
                0.70,
                sample_rate,
            );
            let c = filtered_click * self.click_env * 0.42;
            self.click_env *= 0.972;
            c
        } else {
            0.0
        };

        // 2. Mode characteristics
        let (mode_decay, mode_wash_gain, mode_ping_gain, mode_color) = match self.mode {
            0 => (self.decay_scale * 0.8, 0.35, 0.8, 0.9), // Analog style
            2 => (self.decay_scale * 0.65, 0.25, 1.1, 0.7), // Dark / Dry Jazz ride (dry ping, short wash)
            3 => (self.decay_scale * 1.35, 0.55, 0.9, 1.25), // Bright Sizzle / Rivet ride
            _ => (self.decay_scale, 0.40, 0.95, 1.0),       // Natural B20 22" acoustic ride
        };

        // 3. Ping excitation into modal filters
        let ping_exciter = if self.ping_env > 0.001 {
            let p = self.ping_env * (1.0 + 0.3 * noise_raw) * self.vel * 0.45;
            self.ping_env *= 0.965;
            p
        } else {
            0.0
        };

        // 4. Modal Resonators for plate body and bow ping
        let mut modal_sum = 0.0;
        for (i, m) in self.modes.iter_mut().enumerate() {
            let factor = match i {
                0 => mode_decay * 1.3,
                1 => mode_decay * 1.1,
                2 => mode_decay * 0.9,
                _ => mode_decay * 0.7,
            };
            modal_sum += m.process(ping_exciter, self.tune_ratio, factor, sample_rate);
        }

        // 5. Turbulent Wash Diffusion Network (Schroeder Allpass Diffusers)
        // Wash envelope blooms over ~25ms then decays smoothly
        if self.wash_attack > 0.001 {
            self.wash_env += (1.0 - self.wash_env) * 0.0045; // Smooth attack bloom
            self.wash_attack *= 0.9985;
        } else {
            self.wash_env *= self.wash_decay;
        }

        let wash_in = (noise_raw * 0.35 + click * 0.6) * self.wash_env + self.wash_feedback * 0.58;
        let d1 = self.diffuser_1.process(wash_in, 0.60);
        let d2 = self.diffuser_2.process(d1, 0.55);
        let d3 = self.diffuser_3.process(d2, 0.50);
        let d4 = self.diffuser_4.process(d3, 0.45);

        // Feedback damping (frequency-dependent decay of the wash)
        self.wash_damp = self.wash_damp * 0.45 + d4 * 0.55;
        self.wash_feedback = self.wash_damp;

        let filtered_wash = self.wash_filter.process_bp(
            d4,
            (6200.0 * mode_color * self.tune_ratio).clamp(2500.0, 12000.0),
            0.40,
            sample_rate,
        );

        // 6. Combine Stick Ping, Modal Plate Body, and Diffused Bronze Wash
        let total_sig = (modal_sum * mode_ping_gain + filtered_wash * mode_wash_gain + click)
            * self.vel
            * self.drive;
        let out = self.output_filter.process_lp(
            soft_clip(total_sig),
            (11500.0 * mode_color).clamp(4000.0, sample_rate * 0.46),
            0.15,
            sample_rate,
        );

        // Termination check
        if self.wash_env < MIN_AUDIBLE_VELOCITY && self.ping_env <= 0.001 && self.click_env <= 0.001
        {
            let max_state = self.modes.iter().fold(0.0f32, |acc, m| acc.max(m.y1.abs()));
            if max_state < MIN_AUDIBLE_VELOCITY && out.abs() < MIN_AUDIBLE_VELOCITY {
                self.active = false;
            }
        }

        out
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.click_env = 0.0;
        self.ping_env = 0.0;
        self.wash_env = 0.0;
        self.wash_attack = 0.0;
        self.wash_feedback = 0.0;
        self.wash_damp = 0.0;
        for m in &mut self.modes {
            m.reset();
        }
        self.diffuser_1.reset();
        self.diffuser_2.reset();
        self.diffuser_3.reset();
        self.diffuser_4.reset();
        self.click_filter.reset();
        self.wash_filter.reset();
        self.output_filter.reset();
    }
}

impl Default for RideVoice {
    fn default() -> Self {
        Self::new()
    }
}
