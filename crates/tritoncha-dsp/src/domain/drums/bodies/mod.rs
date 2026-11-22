//! Zero-allocation physical percussion body kernels:
//! - `membrane`: Tensioned drumhead + air column (kicks, toms, snare heads).
//! - `shell`: Rigid wooden drum shell / rim hoop modal resonator bank.
//! - `cymbal`: Unified bronze cymbal disc (bell ping <-> bow ride <-> clamped hat <-> edge crash).
//! - `idiophone`: Rigid dual-mode hollow metal/wood cavity (808 cowbell, woodblock, agogo).

pub mod cymbal;
pub mod idiophone;
pub mod membrane;
pub mod shell;

use std::f32::consts::TAU;

use crate::core::math::{soft_clip, LN_MIN_60DB};
use crate::domain::effects::StateVariableFilter;

pub const NUM_MODES: usize = 6;
pub const PLATE_FDN_LINES: usize = 4;
pub const PLATE_FDN_BUF_SIZE: usize = 256;

/// Zero-allocation 4-line Householder Feedback Delay Network simulating a hammered bronze cymbal plate.
/// Creates thousands of dense, inharmonic metallic partials with natural high-frequency damping and zero howling.
#[derive(Clone)]
pub struct BronzePlateDiffuser {
    bufs: [[f32; PLATE_FDN_BUF_SIZE]; PLATE_FDN_LINES],
    lens: [usize; PLATE_FDN_LINES],
    pos: [usize; PLATE_FDN_LINES],
    damp: [f32; PLATE_FDN_LINES],
    feedback: f32,
    damp_coeff: f32,
}

impl Default for BronzePlateDiffuser {
    fn default() -> Self {
        Self::new()
    }
}

impl BronzePlateDiffuser {
    pub fn new() -> Self {
        Self {
            bufs: [[0.0; PLATE_FDN_BUF_SIZE]; PLATE_FDN_LINES],
            lens: [71, 107, 151, 211],
            pos: [0; PLATE_FDN_LINES],
            damp: [0.0; PLATE_FDN_LINES],
            feedback: 0.92,
            damp_coeff: 0.35,
        }
    }

    /// Configures prime delay lengths scaled by cymbal diameter (pitch_hz & modal_spread) and T60 decay.
    #[inline(always)]
    pub fn configure(&mut self, pitch_hz: f32, decay_s: f32, spread: f32, sample_rate: f32) {
        const BASE_PRIMES: [f32; PLATE_FDN_LINES] = [67.0, 101.0, 149.0, 211.0];
        let sr_scale = sample_rate / 48000.0;
        let size_scale = (400.0 / pitch_hz.clamp(160.0, 1400.0)).clamp(0.45, 1.45);
        let spread_warp = (spread / 1.55).clamp(0.7, 1.45);

        let mut avg_len = 0.0_f32;
        for (i, &base_prime) in BASE_PRIMES.iter().enumerate() {
            let warp = 1.0 + (i as f32 - 1.5) * 0.08 * (spread_warp - 1.0);
            let len = (base_prime * sr_scale * size_scale * warp)
                .round()
                .clamp(23.0, (PLATE_FDN_BUF_SIZE - 1) as f32) as usize;
            self.lens[i] = len;
            avg_len += len as f32;
        }
        avg_len /= PLATE_FDN_LINES as f32;

        // Sample-rate-invariant feedback gain targeting T60 = decay_s
        let decay_samples = (decay_s.clamp(0.08, 3.0) * sample_rate).max(avg_len * 2.0);
        self.feedback = (LN_MIN_60DB * avg_len / decay_samples)
            .exp()
            .clamp(0.65, 0.992);
        // Larger/darker cymbals damp highs slightly faster
        self.damp_coeff = (0.18 + 0.14 * (size_scale - 0.5).max(0.0)).clamp(0.15, 0.45);
    }

    #[inline(always)]
    pub fn process(&mut self, input: f32) -> f32 {
        let mut y = [0.0_f32; PLATE_FDN_LINES];
        for (i, out_y) in y.iter_mut().enumerate() {
            let raw = self.bufs[i][self.pos[i]];
            // One-pole lowpass damping in each line: highs decay naturally faster than mid-bronze body
            self.damp[i] += (raw - self.damp[i]) * (1.0 - self.damp_coeff);
            *out_y = self.damp[i];
        }

        // 4x4 Householder orthogonal scattering matrix (energy-preserving: y_i - 0.5 * sum(y))
        let half_sum = 0.5 * (y[0] + y[1] + y[2] + y[3]);
        const IN_SIGNS: [f32; PLATE_FDN_LINES] = [1.0, -1.0, 1.0, -1.0];

        for i in 0..PLATE_FDN_LINES {
            let scattered = (y[i] - half_sum) * self.feedback;
            // Subtle odd-harmonic nonlinearity models von Karman bronze plate energy transfer
            let next = soft_clip(scattered + input * IN_SIGNS[i] * 0.45);
            self.bufs[i][self.pos[i]] = next;
            self.pos[i] += 1;
            if self.pos[i] >= self.lens[i] {
                self.pos[i] = 0;
            }
        }

        (y[0] - y[1] + y[2] - y[3]) * 0.45
    }

    #[inline(always)]
    pub fn reset(&mut self) {
        for b in &mut self.bufs {
            b.fill(0.0);
        }
        self.pos = [0; PLATE_FDN_LINES];
        self.damp = [0.0; PLATE_FDN_LINES];
    }
}

/// Shared modal resonator bank, metallic oscillator state, bronze plate FDN, and body SVF filter.
#[derive(Clone)]
pub struct ResonatorBank {
    pub modes: [CachedModalFilter; NUM_MODES],
    pub metallic_phases: [f32; NUM_MODES],
    pub metallic_freqs: [f32; NUM_MODES],
    pub plate_fdn: BronzePlateDiffuser,
    pub body_filter: StateVariableFilter,
}

impl Default for ResonatorBank {
    fn default() -> Self {
        Self::new()
    }
}

impl ResonatorBank {
    pub fn new() -> Self {
        Self {
            modes: [CachedModalFilter::default(); NUM_MODES],
            metallic_phases: [0.0; NUM_MODES],
            metallic_freqs: [300.0; NUM_MODES],
            plate_fdn: BronzePlateDiffuser::new(),
            body_filter: StateVariableFilter::new(),
        }
    }

    #[inline(always)]
    pub fn reset_trigger(&mut self) {
        self.metallic_phases = [0.0; NUM_MODES];
        self.plate_fdn.reset();
        self.body_filter.reset();
    }

    #[inline(always)]
    pub fn reset(&mut self) {
        self.reset_trigger();
        for m in &mut self.modes {
            m.reset();
        }
    }
}

/// Coefficient-cached 2nd-order resonant modal filter (zero transcendental math per sample).
#[derive(Clone, Copy, Default)]
pub struct CachedModalFilter {
    y1: f32,
    y2: f32,
    a1: f32,
    a2: f32,
    gain: f32,
}

impl CachedModalFilter {
    #[inline(always)]
    pub fn configure(&mut self, freq_hz: f32, decay_s: f32, gain: f32, sample_rate: f32) {
        let f = freq_hz.clamp(25.0, sample_rate * 0.45);
        let theta = TAU * f / sample_rate;
        let decay_samples = (decay_s * sample_rate).max(20.0);
        let r = (LN_MIN_60DB / decay_samples).exp().clamp(0.85, 0.99995);
        self.a1 = 2.0 * r * theta.cos();
        self.a2 = -(r * r);
        self.gain = gain;
        self.y1 = 0.0;
        self.y2 = 0.0;
    }

    #[inline(always)]
    pub fn process(&mut self, input: f32) -> f32 {
        let y0 = soft_clip(self.a1 * self.y1 + self.a2 * self.y2) + input * self.gain;
        self.y2 = self.y1;
        self.y1 = y0;
        y0
    }

    #[inline(always)]
    pub fn reset(&mut self) {
        self.y1 = 0.0;
        self.y2 = 0.0;
    }
}
