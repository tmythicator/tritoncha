//! Unconditionally stable Topology-Preserving Transform (TPT) State Variable Filter (SVF)
//! with non-linear feedback saturation modeling analog filter drive.

use crate::core::math::tanh_approx;
use std::f32::consts::PI;

pub const MIN_CUTOFF_HZ: f32 = 20.0;
pub const MAX_CUTOFF_RATIO: f32 = 0.45;
pub const MIN_Q: f32 = 0.707;
pub const MAX_RESONANCE: f32 = 0.98;
pub const MAX_Q_SCALE: f32 = 8.0;
pub const DRIVE_INPUT_SCALE: f32 = 2.0;
pub const DRIVE_SAT_SCALE: f32 = 0.8;
pub const DRIVE_NORM_SCALE: f32 = 0.8;
pub const DRIVE_THRESHOLD: f32 = 0.001;

pub const LADDER_MAX_RESONANCE: f32 = 0.99;
pub const LADDER_SELF_OSC_K: f32 = 3.96;
pub const LADDER_DRIVE_GAIN: f32 = 2.5;
pub const LADDER_BASS_COMP_SCALE: f32 = 0.5;

/// Filter response modes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum FilterMode {
    #[default]
    Lowpass = 0,
    Highpass = 1,
    Bandpass = 2,
    Notch = 3,
    Ladder24 = 4,
}

impl From<u8> for FilterMode {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => FilterMode::Highpass,
            2 => FilterMode::Bandpass,
            3 => FilterMode::Notch,
            4 => FilterMode::Ladder24,
            _ => FilterMode::Lowpass,
        }
    }
}

/// Unconditionally stable Topology-Preserving Transform (TPT) State Variable Filter (SVF)
/// with non-linear feedback saturation modeling analog filter drive.
#[derive(Clone, Copy)]
pub struct StateVariableFilter {
    ic1eq: f32,
    ic2eq: f32,
}

impl StateVariableFilter {
    pub fn new() -> Self {
        Self {
            ic1eq: 0.0,
            ic2eq: 0.0,
        }
    }

    pub fn reset(&mut self) {
        self.ic1eq = 0.0;
        self.ic2eq = 0.0;
    }

    #[inline(always)]
    pub fn process(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
        filter_type: u8,
    ) -> f32 {
        self.process_with_drive(input, cutoff_hz, resonance, sample_rate, filter_type, 0.0)
    }

    #[inline(always)]
    pub fn process_with_drive(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
        filter_type: u8,
        drive: f32,
    ) -> f32 {
        let mode = FilterMode::from(filter_type);
        self.process_mode(input, cutoff_hz, resonance, sample_rate, mode, drive)
    }

    #[inline(always)]
    pub fn process_mode(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
        mode: FilterMode,
        drive: f32,
    ) -> f32 {
        let clamped_cutoff = cutoff_hz.clamp(MIN_CUTOFF_HZ, sample_rate * MAX_CUTOFF_RATIO);
        let f = (clamped_cutoff * PI / sample_rate).tan();
        let q = MIN_Q + resonance.clamp(0.0, MAX_RESONANCE) * MAX_Q_SCALE;
        let k = 1.0 / q;

        let a1 = 1.0 / (1.0 + f * (f + k));
        let a2 = f * a1;
        let a3 = f * a2;

        let in_driven = if drive > DRIVE_THRESHOLD {
            input * (1.0 + drive * DRIVE_INPUT_SCALE)
        } else {
            input
        };

        let v3 = in_driven - self.ic2eq;
        let v1 = a1 * self.ic1eq + a2 * v3;

        // Non-linear soft saturation in the integrator loop when driven
        let v1_sat = if drive > DRIVE_THRESHOLD {
            v1 / (1.0 + (v1 * drive * DRIVE_SAT_SCALE).abs())
        } else {
            v1
        };

        let v2 = self.ic2eq + a2 * self.ic1eq + a3 * v3;

        self.ic1eq = 2.0 * v1_sat - self.ic1eq;
        self.ic2eq = 2.0 * v2 - self.ic2eq;

        let raw_out = match mode {
            FilterMode::Highpass => in_driven - k * v1_sat - v2,
            FilterMode::Bandpass => v1_sat,
            FilterMode::Notch => in_driven - k * v1_sat,
            FilterMode::Lowpass | FilterMode::Ladder24 => v2,
        };

        if drive > DRIVE_THRESHOLD {
            raw_out / (1.0 + drive * DRIVE_NORM_SCALE)
        } else {
            raw_out
        }
    }

    #[inline(always)]
    pub fn process_lp(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
    ) -> f32 {
        self.process_mode(
            input,
            cutoff_hz,
            resonance,
            sample_rate,
            FilterMode::Lowpass,
            0.0,
        )
    }

    #[inline(always)]
    pub fn process_hp(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
    ) -> f32 {
        self.process_mode(
            input,
            cutoff_hz,
            resonance,
            sample_rate,
            FilterMode::Highpass,
            0.0,
        )
    }

    #[inline(always)]
    pub fn process_bp(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
    ) -> f32 {
        self.process_mode(
            input,
            cutoff_hz,
            resonance,
            sample_rate,
            FilterMode::Bandpass,
            0.0,
        )
    }

    #[inline(always)]
    pub fn process_lp_with_drive(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
        drive: f32,
    ) -> f32 {
        self.process_mode(
            input,
            cutoff_hz,
            resonance,
            sample_rate,
            FilterMode::Lowpass,
            drive,
        )
    }
}

impl Default for StateVariableFilter {
    fn default() -> Self {
        Self::new()
    }
}

/// Virtual Analog 4-Pole (24 dB/oct) Moog Ladder Filter with non-linear feedback saturation.
#[derive(Clone, Copy, Debug, Default)]
pub struct LadderFilter {
    s1: f32,
    s2: f32,
    s3: f32,
    s4: f32,
}

impl LadderFilter {
    pub fn new() -> Self {
        Self {
            s1: 0.0,
            s2: 0.0,
            s3: 0.0,
            s4: 0.0,
        }
    }

    pub fn reset(&mut self) {
        self.s1 = 0.0;
        self.s2 = 0.0;
        self.s3 = 0.0;
        self.s4 = 0.0;
    }

    #[inline(always)]
    pub fn process(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
        drive: f32,
    ) -> f32 {
        let clamped_cutoff = cutoff_hz.clamp(MIN_CUTOFF_HZ, sample_rate * MAX_CUTOFF_RATIO);
        let g = (clamped_cutoff * PI / sample_rate).tan();
        let big_g = g / (1.0 + g);
        let big_g2 = big_g * big_g;
        let big_g3 = big_g2 * big_g;
        let big_g4 = big_g2 * big_g2;

        let res_clamped = resonance.clamp(0.0, LADDER_MAX_RESONANCE);
        let k = res_clamped * LADDER_SELF_OSC_K;

        let drive_mult = 1.0 + drive.max(0.0) * LADDER_DRIVE_GAIN;
        let driven_in = input * drive_mult;

        // Instantaneous feedback linear predictor
        let s_total = big_g3 * self.s1 + big_g2 * self.s2 + big_g * self.s3 + self.s4;
        let y4_predicted = (big_g4 * driven_in + s_total) / (1.0 + k * big_g4);

        // Saturate feedback loop modeling transistor differential pair
        let u = tanh_approx(driven_in - k * y4_predicted);

        // Stage 1
        let v1 = big_g * (u - self.s1);
        let y1 = v1 + self.s1;
        self.s1 += 2.0 * v1;

        // Stage 2
        let v2 = big_g * (y1 - self.s2);
        let y2 = v2 + self.s2;
        self.s2 += 2.0 * v2;

        // Stage 3
        let v3 = big_g * (y2 - self.s3);
        let y3 = v3 + self.s3;
        self.s3 += 2.0 * v3;

        // Stage 4
        let v4 = big_g * (y3 - self.s4);
        let y4 = v4 + self.s4;
        self.s4 += 2.0 * v4;

        // Bass resonance compensation to maintain punch
        let out = y4 * (1.0 + res_clamped * LADDER_BASS_COMP_SCALE);

        if drive > DRIVE_THRESHOLD {
            out / (1.0 + drive * DRIVE_NORM_SCALE)
        } else {
            out
        }
    }
}

/// Exponential frequency sweep interpolator for smooth filter automation.
#[derive(Clone, Copy, Debug)]
pub struct FrequencySweep {
    start_hz: f32,
    target_hz: f32,
    total_samples: usize,
    current_samples: usize,
    active: bool,
}

impl FrequencySweep {
    pub fn new(initial_hz: f32) -> Self {
        Self {
            start_hz: initial_hz,
            target_hz: initial_hz,
            total_samples: 1,
            current_samples: 0,
            active: false,
        }
    }

    pub fn start(&mut self, from_hz: f32, to_hz: f32, duration_secs: f32, sample_rate: f32) {
        let dur = duration_secs.max(0.01);
        let total = (dur * sample_rate) as usize;
        self.start_hz = from_hz;
        self.target_hz = to_hz;
        self.total_samples = total.max(1);
        self.current_samples = 0;
        self.active = true;
    }

    #[inline(always)]
    pub fn advance(&mut self, num_samples: usize, min_hz: f32, max_hz: f32) -> Option<f32> {
        if !self.active {
            return None;
        }
        self.current_samples = (self.current_samples + num_samples).min(self.total_samples);
        let progress = self.current_samples as f32 / self.total_samples as f32;
        let ratio = self.target_hz / self.start_hz.max(1.0);
        let cutoff = (self.start_hz * ratio.powf(progress)).clamp(min_hz, max_hz);
        if self.current_samples >= self.total_samples {
            self.active = false;
            Some(self.target_hz)
        } else {
            Some(cutoff)
        }
    }

    #[inline(always)]
    pub fn is_active(&self) -> bool {
        self.active
    }

    #[inline(always)]
    pub fn cancel(&mut self) {
        self.active = false;
    }
}

impl Default for FrequencySweep {
    fn default() -> Self {
        Self::new(18000.0)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_filter_mode_conversion() {
        assert_eq!(FilterMode::from(0), FilterMode::Lowpass);
        assert_eq!(FilterMode::from(1), FilterMode::Highpass);
        assert_eq!(FilterMode::from(2), FilterMode::Bandpass);
        assert_eq!(FilterMode::from(3), FilterMode::Notch);
        assert_eq!(FilterMode::from(99), FilterMode::Lowpass);
    }

    #[test]
    fn test_svf_lowpass_attenuation() {
        let mut filter = StateVariableFilter::new();
        let sample_rate = 48000.0;
        let cutoff = 1000.0;

        // Process a DC step to establish steady state
        for _ in 0..500 {
            filter.process_lp(1.0, cutoff, 0.0, sample_rate);
        }
        let out = filter.process_lp(1.0, cutoff, 0.0, sample_rate);
        assert!((out - 1.0).abs() < 0.05);
    }

    #[test]
    fn test_svf_stability_under_extreme_drive() {
        let mut filter = StateVariableFilter::new();
        let sample_rate = 48000.0;

        for _ in 0..1000 {
            let out = filter.process_lp_with_drive(10.0, 100.0, MAX_RESONANCE, sample_rate, 1.0);
            assert!(!out.is_nan() && !out.is_infinite());
            assert!(out.abs() < 50.0);
        }
    }

    #[test]
    fn test_ladder_filter_stability_and_resonance() {
        let mut ladder = LadderFilter::new();
        let sample_rate = 48000.0;

        for cutoff in [40.0, 200.0, 1000.0, 5000.0, 15000.0] {
            ladder.reset();
            for _ in 0..500 {
                let out = ladder.process(1.0, cutoff, 0.95, sample_rate, 0.5);
                assert!(
                    !out.is_nan(),
                    "Ladder output should not be NaN at {cutoff} Hz"
                );
                assert!(
                    !out.is_infinite(),
                    "Ladder output should not blow up at {cutoff} Hz"
                );
                assert!(
                    out.abs() < 10.0,
                    "Ladder output should be bounded by saturation"
                );
            }
        }
    }
}
