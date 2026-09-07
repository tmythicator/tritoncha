//! Unconditionally stable Topology-Preserving Transform (TPT) State Variable Filter (SVF)
//! with non-linear feedback saturation modeling analog filter drive.

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

/// Filter response modes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum FilterMode {
    #[default]
    Lowpass = 0,
    Highpass = 1,
    Bandpass = 2,
    Notch = 3,
}

impl From<u8> for FilterMode {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => FilterMode::Highpass,
            2 => FilterMode::Bandpass,
            3 => FilterMode::Notch,
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
            FilterMode::Lowpass => v2,
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
}
