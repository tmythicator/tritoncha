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

pub const NUM_MODES: usize = 6;

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
