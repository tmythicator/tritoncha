use std::f32::consts::PI;

pub const MIN_CUTOFF_HZ: f32 = 20.0;
pub const MAX_CUTOFF_RATIO: f32 = 0.45;
pub const MIN_Q: f32 = 0.707;
pub const MAX_RESONANCE: f32 = 0.98;
pub const MAX_Q_SCALE: f32 = 8.0;
pub const DRIVE_INPUT_SCALE: f32 = 2.0;
pub const DRIVE_SAT_SCALE: f32 = 0.8;
pub const DRIVE_NORM_SCALE: f32 = 0.8;

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
}

impl Default for StateVariableFilter {
    fn default() -> Self {
        Self::new()
    }
}

impl StateVariableFilter {
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
        let f = (cutoff_hz.clamp(MIN_CUTOFF_HZ, sample_rate * MAX_CUTOFF_RATIO) * PI / sample_rate)
            .tan();
        let q = MIN_Q + resonance.clamp(0.0, MAX_RESONANCE) * MAX_Q_SCALE;
        let k = 1.0 / q;

        let a1 = 1.0 / (1.0 + f * (f + k));
        let a2 = f * a1;
        let a3 = f * a2;

        let in_driven = if drive > 0.001 {
            input * (1.0 + drive * DRIVE_INPUT_SCALE)
        } else {
            input
        };

        let v3 = in_driven - self.ic2eq;
        let v1 = a1 * self.ic1eq + a2 * v3;

        // Non-linear soft saturation in the integrator loop when driven
        let v1_sat = if drive > 0.001 {
            v1 / (1.0 + (v1 * drive * DRIVE_SAT_SCALE).abs())
        } else {
            v1
        };

        let v2 = self.ic2eq + a2 * self.ic1eq + a3 * v3;

        self.ic1eq = 2.0 * v1_sat - self.ic1eq;
        self.ic2eq = 2.0 * v2 - self.ic2eq;

        let raw_out = match filter_type {
            1 => in_driven - k * v1_sat - v2, // Highpass
            2 => v1_sat,                      // Bandpass
            3 => in_driven - k * v1_sat,      // Notch
            _ => v2,                          // Lowpass
        };

        if drive > 0.001 {
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
        self.process(input, cutoff_hz, resonance, sample_rate, 0)
    }

    #[inline(always)]
    pub fn process_hp(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
    ) -> f32 {
        self.process(input, cutoff_hz, resonance, sample_rate, 1)
    }

    #[inline(always)]
    pub fn process_bp(
        &mut self,
        input: f32,
        cutoff_hz: f32,
        resonance: f32,
        sample_rate: f32,
    ) -> f32 {
        self.process(input, cutoff_hz, resonance, sample_rate, 2)
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
        self.process_with_drive(input, cutoff_hz, resonance, sample_rate, 0, drive)
    }
}
