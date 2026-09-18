//! Stereo VCA-style studio bus compressor with soft-knee dynamic gain reduction.

use crate::core::math::{db_to_gain, gain_to_db, lerp};
use crate::engine::DEFAULT_SAMPLE_RATE;

pub const DEFAULT_COMP_THRESHOLD_DB: f32 = -12.0;
pub const DEFAULT_COMP_RATIO: f32 = 4.0;
pub const DEFAULT_COMP_ATTACK_SEC: f32 = 0.010;
pub const DEFAULT_COMP_RELEASE_SEC: f32 = 0.100;
pub const DEFAULT_COMP_MAKEUP_DB: f32 = 2.5;
pub const DEFAULT_COMP_KNEE_DB: f32 = 6.0;

pub const MIN_THRESHOLD_DB: f32 = -60.0;
pub const MAX_THRESHOLD_DB: f32 = 0.0;
pub const MIN_RATIO: f32 = 1.0;
pub const MAX_RATIO: f32 = 20.0;
pub const MIN_ATTACK_SEC: f32 = 0.0005;
pub const MAX_ATTACK_SEC: f32 = 0.200;
pub const MIN_RELEASE_SEC: f32 = 0.010;
pub const MAX_RELEASE_SEC: f32 = 2.0;
pub const MIN_MAKEUP_DB: f32 = 0.0;
pub const MAX_MAKEUP_DB: f32 = 24.0;
pub const MIN_COMP_MIX: f32 = 0.001;
pub const MIN_SAMPLE_RATE: f32 = 1000.0;

/// Configuration parameters for stereo bus compressor.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct CompressorConfig {
    pub enabled: bool,
    pub threshold_db: f32,
    pub ratio: f32,
    pub attack_sec: f32,
    pub release_sec: f32,
    pub makeup_gain_db: f32,
    pub mix: f32,
}

impl Default for CompressorConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            threshold_db: DEFAULT_COMP_THRESHOLD_DB,
            ratio: DEFAULT_COMP_RATIO,
            attack_sec: DEFAULT_COMP_ATTACK_SEC,
            release_sec: DEFAULT_COMP_RELEASE_SEC,
            makeup_gain_db: DEFAULT_COMP_MAKEUP_DB,
            mix: 1.0,
        }
    }
}

/// Stereo-linked VCA studio bus compressor.
#[derive(Debug, Clone)]
pub struct BusCompressor {
    pub enabled: bool,
    pub threshold_db: f32,
    pub ratio: f32,
    pub attack_sec: f32,
    pub release_sec: f32,
    pub makeup_gain_db: f32,
    pub knee_db: f32,
    pub mix: f32,

    // Runtime state
    attack_coeff: f32,
    release_coeff: f32,
    makeup_linear: f32,
    envelope_db: f32,
    sample_rate: f32,
}

impl BusCompressor {
    pub fn new(sample_rate: f32) -> Self {
        let sr = if sample_rate > 0.0 {
            sample_rate
        } else {
            DEFAULT_SAMPLE_RATE
        };
        let mut comp = Self {
            enabled: false,
            threshold_db: DEFAULT_COMP_THRESHOLD_DB,
            ratio: DEFAULT_COMP_RATIO,
            attack_sec: DEFAULT_COMP_ATTACK_SEC,
            release_sec: DEFAULT_COMP_RELEASE_SEC,
            makeup_gain_db: DEFAULT_COMP_MAKEUP_DB,
            knee_db: DEFAULT_COMP_KNEE_DB,
            mix: 1.0,
            attack_coeff: 0.0,
            release_coeff: 0.0,
            makeup_linear: 1.0,
            envelope_db: 0.0,
            sample_rate: sr,
        };
        comp.update_coefficients();
        comp
    }

    /// Updates compressor parameters from config.
    pub fn set_config(&mut self, config: CompressorConfig) {
        self.enabled = config.enabled;
        self.threshold_db = config
            .threshold_db
            .clamp(MIN_THRESHOLD_DB, MAX_THRESHOLD_DB);
        self.ratio = config.ratio.clamp(MIN_RATIO, MAX_RATIO);
        self.attack_sec = config.attack_sec.clamp(MIN_ATTACK_SEC, MAX_ATTACK_SEC);
        self.release_sec = config.release_sec.clamp(MIN_RELEASE_SEC, MAX_RELEASE_SEC);
        self.makeup_gain_db = config.makeup_gain_db.clamp(MIN_MAKEUP_DB, MAX_MAKEUP_DB);
        self.mix = config.mix.clamp(0.0, 1.0);
        self.update_coefficients();
    }

    pub fn set_sample_rate(&mut self, sample_rate: f32) {
        if sample_rate > 0.0 && (sample_rate - self.sample_rate).abs() > 1.0 {
            self.sample_rate = sample_rate;
            self.update_coefficients();
        }
    }

    fn update_coefficients(&mut self) {
        let sr = self.sample_rate.max(MIN_SAMPLE_RATE);
        self.attack_coeff = (-1.0 / (self.attack_sec * sr)).exp();
        self.release_coeff = (-1.0 / (self.release_sec * sr)).exp();
        self.makeup_linear = db_to_gain(self.makeup_gain_db);
    }

    pub fn reset(&mut self) {
        self.envelope_db = 0.0;
    }

    /// Computes static soft-knee gain reduction in decibels (negative or zero).
    #[inline(always)]
    pub fn compute_gain_reduction_db(&self, input_db: f32) -> f32 {
        let half_knee = self.knee_db * 0.5;
        let diff = input_db - self.threshold_db;

        if diff <= -half_knee {
            // Below threshold and knee: no compression
            0.0
        } else if diff >= half_knee {
            // Above knee: standard linear compression ratio
            (1.0 / self.ratio - 1.0) * diff
        } else {
            // Inside soft-knee interval: smooth quadratic transition
            let knee_pos = diff + half_knee;
            let slope = 1.0 / self.ratio - 1.0;
            (slope * knee_pos * knee_pos) / (2.0 * self.knee_db)
        }
    }

    /// Processes a single stereo frame through the compressor.
    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if !self.enabled || self.mix <= MIN_COMP_MIX {
            return (in_l, in_r);
        }

        // Stereo-linked peak level detection (protects stereo field from tilting)
        let peak = in_l.abs().max(in_r.abs());
        let input_db = gain_to_db(peak);

        // Target gain reduction in dB (always <= 0.0)
        let target_gr_db = self.compute_gain_reduction_db(input_db);

        // Smooth envelope follower with decoupled attack and release
        if target_gr_db < self.envelope_db {
            // Compressing further: attack phase
            self.envelope_db =
                self.attack_coeff * self.envelope_db + (1.0 - self.attack_coeff) * target_gr_db;
        } else {
            // Recovering: release phase
            self.envelope_db =
                self.release_coeff * self.envelope_db + (1.0 - self.release_coeff) * target_gr_db;
        }

        let gain_linear = db_to_gain(self.envelope_db) * self.makeup_linear;

        let wet_l = in_l * gain_linear;
        let wet_r = in_r * gain_linear;

        (lerp(in_l, wet_l, self.mix), lerp(in_r, wet_r, self.mix))
    }
}

impl Default for BusCompressor {
    fn default() -> Self {
        Self::new(DEFAULT_SAMPLE_RATE)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_compressor_bypass_when_disabled() {
        let mut comp = BusCompressor::new(DEFAULT_SAMPLE_RATE);
        comp.set_config(CompressorConfig {
            enabled: false,
            threshold_db: -20.0,
            ratio: 8.0,
            attack_sec: 0.0005,
            release_sec: 0.050,
            makeup_gain_db: 6.0,
            mix: 1.0,
        });

        let (out_l, out_r) = comp.process(1.0, 1.0);
        assert_eq!(out_l, 1.0);
        assert_eq!(out_r, 1.0);
    }

    #[test]
    fn test_compressor_unity_gain_below_threshold() {
        let mut comp = BusCompressor::new(DEFAULT_SAMPLE_RATE);
        comp.set_config(CompressorConfig {
            enabled: true,
            threshold_db: -10.0,
            ratio: 4.0,
            attack_sec: 0.001,
            release_sec: 0.050,
            makeup_gain_db: 0.0,
            mix: 1.0,
        });

        // Quiet signal at -30 dB (peak ~0.0316)
        let (out_l, out_r) = comp.process(0.01, 0.01);
        assert!((out_l - 0.01).abs() < 1e-4);
        assert!((out_r - 0.01).abs() < 1e-4);
    }

    #[test]
    fn test_compressor_gain_reduction_above_threshold() {
        let mut comp = BusCompressor::new(DEFAULT_SAMPLE_RATE);
        comp.set_config(CompressorConfig {
            enabled: true,
            threshold_db: -10.0,
            ratio: 4.0,
            attack_sec: 0.0005,
            release_sec: 0.050,
            makeup_gain_db: 0.0,
            mix: 1.0,
        });

        // Loud signal at 0 dB (amplitude 1.0)
        let mut last_l = 0.0;
        for _ in 0..1000 {
            let (l, _) = comp.process(1.0, 1.0);
            last_l = l;
        }

        // At 0 dB input with -10 dB threshold and 4:1 ratio:
        // Expected GR = (1/4 - 1) * 10 dB = -7.5 dB = ~0.42 linear
        assert!(
            last_l < 0.60,
            "Compressor should attenuate loud signal, got {last_l}"
        );
        assert!(
            last_l > 0.35,
            "Compressor should maintain ratio reduction, got {last_l}"
        );
    }
}
