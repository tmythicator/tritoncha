//! First-Order Antiderivative Antialiased (ADAA-1) Soft-Saturation and Analog Overdrive.
//!
//! Antiderivative Antialiasing evaluates the continuous-time integral of a static
//! non-linear function to suppress high-frequency harmonic foldover (aliasing) without
//! the computational penalty or group-delay latency of heavy oversampling filters.

use std::f32;

/// Small epsilon threshold below which standard difference division risks numerical cancellation.
pub const ADAA_EPSILON: f32 = 1e-5;

/// Minimum drive amplification factor (linear gain 1.0 = clean passthrough).
pub const MIN_ADAA_DRIVE: f32 = 1.0;

/// Default analog drive gain factor.
pub const DEFAULT_ADAA_DRIVE: f32 = 1.0;

/// Maximum allowable drive amplification.
pub const MAX_ADAA_DRIVE: f32 = 12.0;

/// Threshold below which drive is considered clean and bypassed if no bias is applied.
pub const ADAA_PASS_DRIVE_THRESHOLD: f32 = 1.001;

/// Maximum asymmetric DC bias offset for even-harmonic saturation.
pub const MAX_ASYMMETRIC_BIAS: f32 = 0.5;

/// Threshold below which asymmetric bias is negligible.
pub const ADAA_PASS_BIAS_THRESHOLD: f32 = 0.001;

/// Output loudness normalization factor per unit of added drive gain.
pub const ADAA_LOUDNESS_NORM_FACTOR: f32 = 0.35;

/// L'Hopital midpoint interpolation weight when delta approaches zero.
pub const L_HOPITAL_MIDPOINT_WEIGHT: f32 = 0.5;

/// Antiderivative Antialiased Analog Saturation Processor.
#[derive(Debug, Clone)]
pub struct AdaaDrive {
    pub drive: f32,
    pub asymmetric_bias: f32,
    last_x_l: f32,
    last_x_r: f32,
    last_f1_l: f32,
    last_f1_r: f32,
}

impl Default for AdaaDrive {
    fn default() -> Self {
        Self::new()
    }
}

impl AdaaDrive {
    /// Creates a new ADAA saturation instance.
    pub fn new() -> Self {
        Self {
            drive: DEFAULT_ADAA_DRIVE,
            asymmetric_bias: 0.0,
            last_x_l: 0.0,
            last_x_r: 0.0,
            last_f1_l: Self::f1(0.0),
            last_f1_r: Self::f1(0.0),
        }
    }

    /// Primary non-linear transfer function: smooth algebraic soft-clipper.
    ///
    /// f(x) = x / sqrt(1 + x^2)
    #[inline(always)]
    pub fn f(x: f32) -> f32 {
        x / (1.0 + x * x).sqrt()
    }

    /// First-order continuous-time antiderivative F1(x) = integral(f(x) dx).
    ///
    /// F1(x) = sqrt(1 + x^2)
    #[inline(always)]
    pub fn f1(x: f32) -> f32 {
        (1.0 + x * x).sqrt()
    }

    /// Sets drive amount, scaling from 0.0 (clean passthrough) to 1.0 (heavy saturation).
    pub fn set_drive(&mut self, amount: f32) {
        let clamped = amount.clamp(0.0, 1.0);
        // Map 0.0..1.0 linearly to MIN_ADAA_DRIVE..MAX_ADAA_DRIVE
        self.drive = MIN_ADAA_DRIVE + clamped * (MAX_ADAA_DRIVE - MIN_ADAA_DRIVE);
    }

    /// Sets asymmetric bias for even-harmonic saturation (tube-like warmth).
    pub fn set_bias(&mut self, bias: f32) {
        self.asymmetric_bias = bias.clamp(-MAX_ASYMMETRIC_BIAS, MAX_ASYMMETRIC_BIAS);
    }

    /// Resets historical states.
    pub fn reset(&mut self) {
        self.last_x_l = 0.0;
        self.last_x_r = 0.0;
        self.last_f1_l = Self::f1(0.0);
        self.last_f1_r = Self::f1(0.0);
    }

    /// Processes a single channel sample using ADAA-1 difference formula.
    #[inline(always)]
    fn process_channel(x: f32, last_x: &mut f32, last_f1: &mut f32) -> f32 {
        let dx = x - *last_x;
        let current_f1 = Self::f1(x);

        let y = if dx.abs() < ADAA_EPSILON {
            // Ill-conditioned: apply L'Hopital limit at midpoint
            let mid = L_HOPITAL_MIDPOINT_WEIGHT * (x + *last_x);
            Self::f(mid)
        } else {
            // First-order discrete difference
            (current_f1 - *last_f1) / dx
        };

        *last_x = x;
        *last_f1 = current_f1;
        y
    }

    /// Processes stereo audio through the ADAA-1 non-linear saturation curve.
    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.drive <= ADAA_PASS_DRIVE_THRESHOLD
            && self.asymmetric_bias.abs() < ADAA_PASS_BIAS_THRESHOLD
        {
            return (in_l, in_r);
        }

        let pre_l = in_l * self.drive + self.asymmetric_bias;
        let pre_r = in_r * self.drive + self.asymmetric_bias;

        let sat_l = Self::process_channel(pre_l, &mut self.last_x_l, &mut self.last_f1_l);
        let sat_r = Self::process_channel(pre_r, &mut self.last_x_r, &mut self.last_f1_r);

        // Normalize output level by drive factor to maintain perceptual loudness headroom
        let norm_gain = 1.0 / (1.0 + ADAA_LOUDNESS_NORM_FACTOR * (self.drive - MIN_ADAA_DRIVE));
        (sat_l * norm_gain, sat_r * norm_gain)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_adaa_f_and_f1_mathematical_consistency() {
        // Derivative of F1(x) = sqrt(1 + x^2) is x / sqrt(1 + x^2) = f(x)
        for val in [-5.0_f32, -1.0, 0.0, 0.5, 2.0, 5.0] {
            let h = 1e-2_f32;
            let num_diff = (AdaaDrive::f1(val + h) - AdaaDrive::f1(val - h)) / (2.0 * h);
            let analytic = AdaaDrive::f(val);
            assert!(
                (num_diff - analytic).abs() < 5e-3,
                "Derivative of f1 at {} should match f(x): num={} vs analytic={}",
                val,
                num_diff,
                analytic
            );
        }
    }

    #[test]
    fn test_adaa_numerical_limit_near_zero_dx() {
        let mut adaa = AdaaDrive::new();
        adaa.set_drive(0.5);

        // Prime the historical state at 0.75
        adaa.process(0.75, 0.75);

        // Process with identical sample (dx = 0, triggers L'Hopital midpoint fallback)
        let (out_limit, _) = adaa.process(0.75, 0.75);

        // Process with tiny delta (dx = 5e-5, triggers discrete difference)
        let (out_diff, _) = adaa.process(0.75 + 5e-5, 0.75 + 5e-5);

        assert!(
            out_limit.is_finite(),
            "Output must be finite on identical sample"
        );
        assert!(out_diff.is_finite(), "Output must be finite on tiny delta");
        assert!(
            (out_limit - out_diff).abs() < 1e-2,
            "L'Hopital limit must match discrete difference near zero: {} vs {}",
            out_limit,
            out_diff
        );
    }

    #[test]
    fn test_adaa_bounded_saturation() {
        let mut adaa = AdaaDrive::new();
        adaa.set_drive(1.0); // Maximum drive

        // Large input signals must saturate cleanly without exceeding boundary
        for val in [-100.0, -10.0, 10.0, 100.0] {
            let (out_l, out_r) = adaa.process(val, val);
            assert!(
                out_l.abs() <= 1.5,
                "Saturation must clamp cleanly: {}",
                out_l
            );
            assert!(
                out_r.abs() <= 1.5,
                "Saturation must clamp cleanly: {}",
                out_r
            );
        }
    }
}
