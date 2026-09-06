//! Inharmonic metallic cymbal and percussion voice.

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, wrap_phase, xorshift32_norm};
use crate::synth::drums::{MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use std::f32::consts::PI;

/// Metallic Inharmonic Cymbal and Percussion Voice.
#[derive(Clone)]
pub struct MetallicVoice<const N: usize> {
    pub active: bool,
    env: f32,
    click_env: f32,
    impact_env: f32,
    vel: f32,
    phases: [f32; N],
    freqs: [f32; N],
    decay_coeff: f32,
    filter: StateVariableFilter,
    noise_filter: StateVariableFilter,
    cutoff_hz: f32,
    resonance: f32,
    is_bandpass: bool,
    drive: f32,
    noise_seed: u32,
    noise_mix: f32,
    pub mode: u8,
}

impl<const N: usize> MetallicVoice<N> {
    pub fn new(
        freqs: [f32; N],
        cutoff_hz: f32,
        resonance: f32,
        decay_coeff: f32,
        is_bandpass: bool,
        drive: f32,
        noise_mix: f32,
    ) -> Self {
        Self {
            active: false,
            env: 0.0,
            click_env: 0.0,
            impact_env: 0.0,
            vel: 0.0,
            phases: [0.0; N],
            freqs,
            decay_coeff,
            filter: StateVariableFilter::new(),
            noise_filter: StateVariableFilter::new(),
            cutoff_hz,
            resonance,
            is_bandpass,
            drive,
            noise_seed: 0x4a71b2d9,
            noise_mix,
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
            self.cutoff_hz = cutoff_hz.clamp(100.0, 18000.0);
        }
        if resonance >= 0.0 {
            self.resonance = resonance.clamp(0.05, 0.95);
        }
        if decay_s > 0.0 {
            let samples = (decay_s.clamp(0.02, 5.0) * 48000.0).max(50.0);
            self.decay_coeff = (-6.90775 / samples).exp().clamp(0.980, 0.99998);
        }
        if drive > 0.0 {
            self.drive = drive.clamp(0.1, 5.0);
        }
        if mode >= 0.0 {
            self.mode = (mode.round() as u8).clamp(0, 3);
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.env = 1.0;
        self.click_env = 1.0;
        self.impact_env = 1.0;
        self.vel = v;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        // Acoustic stick tip strike transient for crisp wooden impact definition (ride and bell)
        let click = if self.click_env > 0.001 {
            let click_freq = (self.freqs[0] * 2.5).clamp(3500.0, 8500.0);
            let click_phase = self.phases[0] * (click_freq / self.freqs[0]);
            let c = (click_phase * 2.0 * PI).sin() * self.click_env * 0.45;
            self.click_env *= 0.975; // ~4ms wooden stick tip crack
            c
        } else {
            0.0
        };

        // Explosive wideband impact burst for crashes, splash, and china
        let impact = self.impact_env;
        self.impact_env *= 0.9955; // ~50ms decay of the stick edge impact blast

        let mut sum = 0.0;
        match self.mode {
            1 => {
                // Natural: Organic Inharmonic Bronze Plate + Aerodynamic Sizzle Wash.
                // Pairwise non-linear phase-modulation creates dense Bessel sidebands.
                let mut fm_sum = 0.0;
                let pairs = N / 2;
                for i in 0..pairs {
                    let c_idx = i * 2;
                    let m_idx = i * 2 + 1;
                    let p_c = self.phases[c_idx];
                    let p_m = self.phases[m_idx];

                    // Dynamic modulation index: high energy at impact, settling into warm singing shimmer
                    let beta = 1.35 + 0.75 * impact;
                    let mod_sig = (p_m * 2.0 * PI).sin();
                    let carrier_sig = (p_c * 2.0 * PI + beta * mod_sig).sin();
                    fm_sum += carrier_sig;

                    self.phases[c_idx] =
                        wrap_phase(self.phases[c_idx] + self.freqs[c_idx] / sample_rate);
                    self.phases[m_idx] =
                        wrap_phase(self.phases[m_idx] + self.freqs[m_idx] / sample_rate);
                }
                if N % 2 == 1 {
                    let last_idx = N - 1;
                    let p_last = self.phases[last_idx];
                    let mod_sig = (self.phases[0] * 2.0 * PI).sin();
                    let s = (p_last * 2.0 * PI + 1.2 * mod_sig).sin();
                    fm_sum += s;
                    self.phases[last_idx] =
                        wrap_phase(self.phases[last_idx] + self.freqs[last_idx] / sample_rate);
                }
                let norm = (pairs + (N % 2)).max(1) as f32;
                fm_sum /= norm;

                // Dedicated acoustic cymbal sizzle wash (highpass-filtered noise)
                let raw_noise = xorshift32_norm(&mut self.noise_seed);
                let sizzle = self
                    .noise_filter
                    .process_hp(raw_noise, 3200.0, 0.22, sample_rate);
                let explosive_sizzle = sizzle * (1.0 + 1.8 * impact);

                // Blend inharmonic bronze modes with aerodynamic wash
                let wash = self.noise_mix.clamp(0.0, 0.95);
                sum = fm_sum * (1.0 - wash) + explosive_sizzle * wash;
                sum += click;
            }
            2 => {
                // IDM: ring-modulated microtonal ping + laser chirp
                for i in 0..N {
                    let square_val = if self.phases[i] < 0.5 { 1.0 } else { -1.0 };
                    sum += square_val;
                    self.phases[i] =
                        wrap_phase(self.phases[i] + (self.freqs[i] * 1.15) / sample_rate);
                }
                sum /= N as f32;
                let carrier = (self.phases[0] * 3.7 * 2.0 * PI).sin();
                sum *= carrier;
                let raw_noise = xorshift32_norm(&mut self.noise_seed);
                let sizzle = self
                    .noise_filter
                    .process_hp(raw_noise, 4200.0, 0.3, sample_rate);
                sum = sum * 0.7 + sizzle * 0.3 * (1.0 + impact);
                sum += click;
            }
            3 => {
                // Industrial: wavefolded sheet metal clang + distorted wash
                for i in 0..N {
                    let square_val = if self.phases[i] < 0.5 { 1.0 } else { -1.0 };
                    sum += square_val;
                    self.phases[i] = wrap_phase(self.phases[i] + self.freqs[i] / sample_rate);
                }
                sum /= N as f32;
                let raw_noise = xorshift32_norm(&mut self.noise_seed);
                let sizzle = self
                    .noise_filter
                    .process_hp(raw_noise, 2200.0, 0.5, sample_rate);
                sum = sum * 0.45 + sizzle * 0.55 * (1.0 + 1.2 * impact);
                sum = (sum * 2.6).sin();
                sum += click;
            }
            _ => {
                // Analog (default): Multi-oscillator inharmonic metal with XOR ring-modulation + noise
                let mut ring = 1.0;
                let mut pulse_sum = 0.0;
                for i in 0..N {
                    let pw = 0.25 + 0.08 * (i as f32);
                    let pulse_val = if self.phases[i] < pw { 1.0 } else { -1.0 };
                    ring *= pulse_val;
                    pulse_sum += pulse_val;
                    self.phases[i] = wrap_phase(self.phases[i] + self.freqs[i] / sample_rate);
                }
                let metal = ring * 0.55 + (pulse_sum / N as f32) * 0.45;
                let raw_noise = xorshift32_norm(&mut self.noise_seed);
                let sizzle = self
                    .noise_filter
                    .process_hp(raw_noise, 3200.0, 0.25, sample_rate);
                sum =
                    metal * (1.0 - self.noise_mix) + sizzle * self.noise_mix * (1.0 + 0.8 * impact);
                sum += click;
            }
        }

        let filtered = if self.is_bandpass {
            self.filter
                .process_bp(sum, self.cutoff_hz, self.resonance, sample_rate)
        } else {
            self.filter
                .process_hp(sum, self.cutoff_hz, self.resonance, sample_rate)
        };

        let sig = soft_clip(filtered * self.drive) * self.env * self.vel;
        self.env *= self.decay_coeff;

        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.env = 0.0;
        self.click_env = 0.0;
        self.impact_env = 0.0;
        self.phases = [0.0; N];
        self.filter.reset();
        self.noise_filter.reset();
    }
}
