//! Studio 8-Channel Householder Feedback Delay Network (FDN) Reverb.
//!
//! Employs an energy-preserving unitary Householder reflection matrix across 8 mutually prime
//! delay lines, per-line one-pole high-frequency damping, and quadrature LFO read-head
//! modulation for modal density and smooth spatial decay without metallic comb flutter.

use crate::dsp::math::lerp;
use std::f32::consts::PI;

/// Number of parallel delay lines in the FDN topology.
pub const FDN_CHANNELS: usize = 8;

/// Baseline prime-tuned delay lengths in samples (calculated for 48kHz sample rate).
/// Mutually prime lengths avoid modal phase alignment and eliminate harmonic ringing.
pub const FDN_BASE_DELAYS: [usize; FDN_CHANNELS] = [1051, 1223, 1453, 1693, 1987, 2333, 2741, 3217];

/// Maximum buffer allocation capacity per delay line (accommodates 2x room scaling + modulation headroom).
pub const MAX_FDN_DELAY: usize = 8192;

/// Default decay time (feedback coefficient scale).
pub const DEFAULT_FDN_ROOM_SIZE: f32 = 0.75;

/// Default high-frequency damping factor.
pub const DEFAULT_FDN_DAMPING: f32 = 0.25;

/// Single delay line inside the Feedback Delay Network.
#[derive(Debug, Clone)]
pub struct FdnDelayLine {
    buffer: Vec<f32>,
    write_pos: usize,
    base_length: usize,
    damp_state: f32,
}

impl FdnDelayLine {
    pub fn new(base_length: usize) -> Self {
        Self {
            buffer: vec![0.0; MAX_FDN_DELAY],
            write_pos: 0,
            base_length: base_length.clamp(64, MAX_FDN_DELAY - 64),
            damp_state: 0.0,
        }
    }

    /// Writes a new sample into the circular buffer.
    #[inline(always)]
    pub fn write(&mut self, sample: f32) {
        self.buffer[self.write_pos] = sample;
        self.write_pos += 1;
        if self.write_pos >= MAX_FDN_DELAY {
            self.write_pos = 0;
        }
    }

    /// Reads with fractional linear interpolation and modulation offset.
    #[inline(always)]
    pub fn read_interpolated(&self, current_delay: f32) -> f32 {
        let d = current_delay.clamp(1.0, (MAX_FDN_DELAY - 2) as f32);
        let int_d = d.floor() as usize;
        let frac = d - (int_d as f32);

        let read_pos_0 = if self.write_pos >= int_d {
            self.write_pos - int_d
        } else {
            (self.write_pos + MAX_FDN_DELAY) - int_d
        };

        let read_pos_1 = if read_pos_0 > 0 {
            read_pos_0 - 1
        } else {
            MAX_FDN_DELAY - 1
        };

        let s0 = self.buffer[read_pos_0];
        let s1 = self.buffer[read_pos_1];

        lerp(s0, s1, frac)
    }

    /// Applies one-pole lowpass damping to simulate air absorption of high frequencies.
    #[inline(always)]
    pub fn apply_damping(&mut self, input: f32, damping: f32) -> f32 {
        self.damp_state = lerp(input, self.damp_state, damping);
        self.damp_state
    }

    pub fn reset(&mut self) {
        self.buffer.fill(0.0);
        self.write_pos = 0;
        self.damp_state = 0.0;
    }
}

/// 8-Channel Householder Feedback Delay Network Reverb.
#[derive(Debug, Clone)]
pub struct FdnReverb {
    lines: [FdnDelayLine; FDN_CHANNELS],
    room_size: f32,
    damping: f32,
    pub wet: f32,
    lfo_phase_1: f32,
    lfo_phase_2: f32,
    lfo_inc_1: f32,
    lfo_inc_2: f32,
}

impl Default for FdnReverb {
    fn default() -> Self {
        Self::new(48000.0)
    }
}

impl FdnReverb {
    /// Constructs a new 8-channel Householder FDN reverb processor.
    pub fn new(sample_rate: f32) -> Self {
        let sr = if sample_rate > 1000.0 {
            sample_rate
        } else {
            48000.0
        };
        let scale = sr / 48000.0;

        let lines = std::array::from_fn(|i| {
            let len = ((FDN_BASE_DELAYS[i] as f32) * scale).round() as usize;
            FdnDelayLine::new(len)
        });

        Self {
            lines,
            room_size: DEFAULT_FDN_ROOM_SIZE,
            damping: DEFAULT_FDN_DAMPING,
            wet: 0.35,
            lfo_phase_1: 0.0,
            lfo_phase_2: 0.25,                 // 90 degree quadrature offset
            lfo_inc_1: (2.0 * PI * 0.65) / sr, // 0.65 Hz slow drift
            lfo_inc_2: (2.0 * PI * 0.92) / sr, // 0.92 Hz uncorrelated drift
        }
    }

    /// Sets room size, scaling decay feedback between 0.0 and 0.98.
    pub fn set_room_size(&mut self, size: f32) {
        self.room_size = size.clamp(0.0, 0.98);
    }

    /// Sets high frequency absorption damping (0.0 to 0.8).
    pub fn set_damping(&mut self, damp: f32) {
        self.damping = damp.clamp(0.0, 0.85);
    }

    /// Sets wet mix gain.
    pub fn set_wet(&mut self, wet: f32) {
        self.wet = wet.clamp(0.0, 1.0);
    }

    /// Resets all delay buffers and filters to silence.
    pub fn reset(&mut self) {
        for line in &mut self.lines {
            line.reset();
        }
        self.lfo_phase_1 = 0.0;
        self.lfo_phase_2 = 0.25;
    }

    /// Processes an incoming stereo sample and returns only the wet reverberated stereo signal.
    #[inline(always)]
    pub fn process_wet(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet < 0.001 {
            return (0.0, 0.0);
        }

        // Advance dual quadrature LFOs
        self.lfo_phase_1 += self.lfo_inc_1;
        if self.lfo_phase_1 >= 2.0 * PI {
            self.lfo_phase_1 -= 2.0 * PI;
        }

        self.lfo_phase_2 += self.lfo_inc_2;
        if self.lfo_phase_2 >= 2.0 * PI {
            self.lfo_phase_2 -= 2.0 * PI;
        }

        let mod_1 = self.lfo_phase_1.sin() * 5.5;
        let mod_2 = self.lfo_phase_2.sin() * 5.5;

        // 1. Read delay line outputs with LFO modulation
        let mut delay_outs = [0.0_f32; FDN_CHANNELS];
        for (i, (out, line)) in delay_outs.iter_mut().zip(self.lines.iter()).enumerate() {
            let m = if i % 2 == 0 { mod_1 } else { mod_2 };
            let delay_len = (line.base_length as f32) + m;
            *out = line.read_interpolated(delay_len);
        }

        // 2. Apply lowpass damping absorption
        for (out, line) in delay_outs.iter_mut().zip(self.lines.iter_mut()) {
            *out = line.apply_damping(*out, self.damping);
        }

        // 3. Fast Householder Feedback Matrix: A = I - (2 / N) * 1 * 1^T
        // For N = 8: (A * x)_i = x_i - 0.25 * sum(x)
        let sum: f32 = delay_outs.iter().sum();
        let householder_term = 0.25 * sum;

        // 4. Feedback attenuation scaled by room size (decay time)
        let fb_gain = self.room_size;

        // 5. Input injection: stereo inputs are injected across all 8 delay lines with alternating polarity
        // Lines 0..3 take Left, Lines 4..7 take Right
        let input_scale = 0.35;
        for (i, (&delayed, line)) in delay_outs.iter().zip(self.lines.iter_mut()).enumerate() {
            let reflected = delayed - householder_term;
            let inj = if i < 4 {
                if i % 2 == 0 {
                    in_l
                } else {
                    -in_l
                }
            } else {
                if i % 2 == 0 {
                    in_r
                } else {
                    -in_r
                }
            };
            let next_in = inj * input_scale + reflected * fb_gain;
            line.write(next_in);
        }

        // 6. Stereo decorrelated output matrix
        // Left: lines 0, 2, 4, 6
        let out_l =
            (delay_outs[0] - delay_outs[2] + delay_outs[4] - delay_outs[6]) * 0.5 * self.wet;
        // Right: lines 1, 3, 5, 7
        let out_r =
            (delay_outs[1] + delay_outs[3] - delay_outs[5] - delay_outs[7]) * 0.5 * self.wet;

        (out_l, out_r)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_fdn_householder_matrix_energy_preservation() {
        // Householder matrix A = I - 0.25 * 1 * 1^T for N = 8
        // Test unitary property: sum(A*x)^2 == sum(x)^2 for orthogonal input vector
        let x = [1.0, 0.5, -0.2, 0.8, -0.6, 0.3, -0.1, 0.4];
        let sum: f32 = x.iter().sum();
        let term = 0.25 * sum;

        let mut y = [0.0_f32; 8];
        for i in 0..8 {
            y[i] = x[i] - term;
        }

        let energy_x: f32 = x.iter().map(|&v| v * v).sum();
        let energy_y: f32 = y.iter().map(|&v| v * v).sum();

        assert!(
            (energy_x - energy_y).abs() < 1e-4,
            "Householder matrix must strictly preserve signal energy: {} vs {}",
            energy_x,
            energy_y
        );
    }

    #[test]
    fn test_fdn_impulse_response_stability() {
        let mut fdn = FdnReverb::new(48000.0);
        fdn.set_room_size(0.85);
        fdn.set_damping(0.2);
        fdn.set_wet(1.0);

        // Inject 1.0 impulse
        let (first_l, first_r) = fdn.process_wet(1.0, 1.0);
        assert!(first_l.is_finite());
        assert!(first_r.is_finite());

        // Process 10,000 samples of tail decay
        for _ in 0..10_000 {
            let (l, r) = fdn.process_wet(0.0, 0.0);
            assert!(l.is_finite(), "Output must never produce NaN or Inf");
            assert!(r.is_finite(), "Output must never produce NaN or Inf");
            assert!(l.abs() <= 2.0, "Reverb tail must remain bounded: {}", l);
            assert!(r.abs() <= 2.0, "Reverb tail must remain bounded: {}", r);
        }
    }
}
