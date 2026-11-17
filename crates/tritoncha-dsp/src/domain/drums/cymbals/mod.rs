//! Cymbals and metallic percussion synthesis modules (ride, bell, crashes, splash, china, cowbell, hats).

pub mod bell;
pub mod hats;
pub mod metallic;
pub mod ride;

pub use bell::RideBellVoice;
pub use hats::{HatParams, NoiseHatVoice};
pub use metallic::MetallicVoice;
pub use ride::RideVoice;

use crate::core::math::{soft_clip, LN_MIN_60DB};
use crate::domain::drums::{opt_param, DrumMode};
use crate::domain::{AdsrTime, CutoffHz, Drive, Resonance};
use std::f32::consts::PI;

#[derive(Clone, Copy)]
pub(crate) struct ModalFilter {
    pub(crate) y1: f32,
    y2: f32,
    base_freq: f32,
    decay_s: f32,
    gain: f32,
}

impl ModalFilter {
    pub(crate) const fn new(base_freq: f32, decay_s: f32, gain: f32) -> Self {
        Self {
            y1: 0.0,
            y2: 0.0,
            base_freq,
            decay_s,
            gain,
        }
    }

    #[inline(always)]
    pub(crate) fn reset(&mut self) {
        self.y1 = 0.0;
        self.y2 = 0.0;
    }

    #[inline(always)]
    pub(crate) fn process(
        &mut self,
        input: f32,
        pitch_ratio: f32,
        decay_scale: f32,
        sample_rate: f32,
    ) -> f32 {
        let f = (self.base_freq * pitch_ratio).clamp(30.0, sample_rate * 0.45);
        let theta = 2.0 * PI * f / sample_rate;
        let decay_samples = (self.decay_s * decay_scale * sample_rate).max(20.0);
        let r = (LN_MIN_60DB / decay_samples).exp().clamp(0.85, 0.99995);

        let a1 = 2.0 * r * theta.cos();
        let a2 = -(r * r);

        let y0 = soft_clip(a1 * self.y1 + a2 * self.y2) + input * self.gain;
        self.y2 = self.y1;
        self.y1 = y0;
        y0
    }
}

/// Unified parameter configuration for resonant percussion models (cymbals, metallic, clap).
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct PercussionParams {
    pub cutoff: CutoffHz,
    pub resonance: Resonance,
    pub decay: AdsrTime,
    pub drive: Drive,
    pub mode: DrumMode,
}

impl Default for PercussionParams {
    fn default() -> Self {
        Self {
            cutoff: 3000.0.into(),
            resonance: 0.5.into(),
            decay: 1.0.into(),
            drive: 1.0.into(),
            mode: DrumMode::default(),
        }
    }
}

impl From<[f32; 7]> for PercussionParams {
    #[inline(always)]
    fn from(p: [f32; 7]) -> Self {
        let d = Self::default();
        Self {
            cutoff: opt_param(p[0], d.cutoff),
            resonance: opt_param(p[1], d.resonance),
            decay: opt_param(p[2], d.decay),
            drive: opt_param(p[3], d.drive),
            mode: p[6].into(),
        }
    }
}

// Inharmonic metallic frequency banks for cymbals and percussion (B20 bronze modal models)
pub const CRASH_16_FREQS: [f32; 6] = [240.0, 360.0, 560.0, 890.0, 1420.0, 2260.0];
pub const CRASH_17_FREQS: [f32; 6] = [215.0, 325.0, 505.0, 810.0, 1290.0, 2050.0];
pub const CRASH_18_FREQS: [f32; 6] = [190.0, 290.0, 450.0, 720.0, 1150.0, 1840.0];
pub const SPLASH_FREQS: [f32; 6] = [480.0, 740.0, 1160.0, 1780.0, 2750.0, 4120.0];
pub const CHINA_FREQS: [f32; 6] = [230.0, 310.0, 440.0, 610.0, 890.0, 1340.0];
pub const COWBELL_FREQS: [f32; 2] = [587.0, 845.0];

// Cymbal filter cutoffs
pub const CRASH_16_CUTOFF_HZ: f32 = 1900.0;
pub const CRASH_17_CUTOFF_HZ: f32 = 1650.0;
pub const CRASH_18_CUTOFF_HZ: f32 = 1400.0;
pub const SPLASH_CUTOFF_HZ: f32 = 3200.0;
pub const CHINA_CUTOFF_HZ: f32 = 1800.0;
pub const COWBELL_BANDPASS_HZ: f32 = 820.0;

#[inline(always)]
pub fn new_crash_16() -> MetallicVoice<6> {
    MetallicVoice::new(
        CRASH_16_FREQS,
        CRASH_16_CUTOFF_HZ,
        0.20,
        0.99987,
        false,
        1.05,
        0.74,
    )
}

#[inline(always)]
pub fn new_crash_17() -> MetallicVoice<6> {
    MetallicVoice::new(
        CRASH_17_FREQS,
        CRASH_17_CUTOFF_HZ,
        0.20,
        0.99989,
        false,
        1.05,
        0.76,
    )
}

#[inline(always)]
pub fn new_crash_18() -> MetallicVoice<6> {
    MetallicVoice::new(
        CRASH_18_FREQS,
        CRASH_18_CUTOFF_HZ,
        0.20,
        0.99991,
        false,
        1.05,
        0.78,
    )
}

#[inline(always)]
pub fn new_splash() -> MetallicVoice<6> {
    MetallicVoice::new(
        SPLASH_FREQS,
        SPLASH_CUTOFF_HZ,
        0.25,
        0.99940,
        false,
        0.95,
        0.82,
    )
}

#[inline(always)]
pub fn new_china() -> MetallicVoice<6> {
    MetallicVoice::new(
        CHINA_FREQS,
        CHINA_CUTOFF_HZ,
        0.45,
        0.99984,
        false,
        1.35,
        0.80,
    )
}

#[inline(always)]
pub fn new_cowbell() -> MetallicVoice<2> {
    MetallicVoice::new(
        COWBELL_FREQS,
        COWBELL_BANDPASS_HZ,
        0.85,
        0.9985,
        true,
        1.4,
        0.0,
    )
}
