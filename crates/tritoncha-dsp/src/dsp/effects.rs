//! Audio effect processors: BitcrusherDrive, StereoChorus, and SidechainPump.

use crate::dsp::math::{tanh_approx, wrap_phase};
use std::f32::consts::PI;

pub const DEFAULT_BIT_DEPTH: f32 = 16.0;
pub const MIN_BIT_DEPTH: f32 = 1.0;
pub const MAX_BIT_DEPTH: f32 = 16.0;
pub const BIT_CRUSH_ACTIVE_THRESHOLD: f32 = 15.5;

pub const DEFAULT_SAMPLE_HOLD: f32 = 1.0;
pub const MAX_SAMPLE_HOLD: f32 = 16.0;

pub const OVERDRIVE_GAIN_SCALE: f32 = 6.0;
pub const MIN_DRIVE_THRESHOLD: f32 = 0.001;

pub const CHORUS_BUFFER_SIZE: usize = 44100;
pub const DEFAULT_CHORUS_RATE_HZ: f32 = 0.8;
pub const DEFAULT_CHORUS_DEPTH: f32 = 0.4;
pub const CHORUS_BASE_DELAY_SEC: f32 = 0.015; // 15ms base delay
pub const CHORUS_MOD_DEPTH_SEC: f32 = 0.008; // 8ms modulation
pub const CHORUS_QUADRATURE_OFFSET: f32 = 0.25; // 90 degree stereo phase offset
pub const CHORUS_WET_GAIN: f32 = 0.6;
pub const CHORUS_DRY_ATTEN: f32 = 0.5;

pub const SIDECHAIN_DUCK_SCALE: f32 = 0.85;
pub const SIDECHAIN_RECOVERY_RATE: f32 = 0.0012; // exponential recovery coefficient

/// Bitcrusher and analog drive / saturation effect.
pub struct BitcrusherDrive {
    pub drive: f32,       // 0.0 to 1.0
    pub bit_depth: f32,   // 1.0 to 16.0
    pub sample_hold: f32, // 1.0 to 16.0 (downsampling factor)
    hold_counter: f32,
    last_sample_l: f32,
    last_sample_r: f32,
}

impl BitcrusherDrive {
    pub fn new() -> Self {
        Self {
            drive: 0.0,
            bit_depth: DEFAULT_BIT_DEPTH,
            sample_hold: DEFAULT_SAMPLE_HOLD,
            hold_counter: 0.0,
            last_sample_l: 0.0,
            last_sample_r: 0.0,
        }
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.drive <= MIN_DRIVE_THRESHOLD
            && self.bit_depth >= (MAX_BIT_DEPTH - 0.1)
            && self.sample_hold <= 1.05
        {
            return (in_l, in_r);
        }

        // 1. Overdrive saturation using fast analog tanh approximation
        let drive_gain = 1.0 + self.drive * OVERDRIVE_GAIN_SCALE;
        let mut x_l = tanh_approx(in_l * drive_gain);
        let mut x_r = tanh_approx(in_r * drive_gain);

        // 2. Sample rate reduction (sample & hold)
        self.hold_counter += 1.0;
        if self.hold_counter >= self.sample_hold {
            self.hold_counter = 0.0;
            self.last_sample_l = x_l;
            self.last_sample_r = x_r;
        } else {
            x_l = self.last_sample_l;
            x_r = self.last_sample_r;
        }

        // 3. Bit depth quantization
        if self.bit_depth < BIT_CRUSH_ACTIVE_THRESHOLD {
            let clamped_depth = self.bit_depth.clamp(MIN_BIT_DEPTH, MAX_BIT_DEPTH);
            let step = 2.0_f32.powf(clamped_depth);
            x_l = (x_l * step).round() / step;
            x_r = (x_r * step).round() / step;
        }

        (x_l, x_r)
    }

    pub fn reset(&mut self) {
        self.hold_counter = 0.0;
        self.last_sample_l = 0.0;
        self.last_sample_r = 0.0;
    }
}

impl Default for BitcrusherDrive {
    fn default() -> Self {
        Self::new()
    }
}

/// Stereo LFO Chorus and Flanger.
pub struct StereoChorus {
    buf_l: Vec<f32>,
    buf_r: Vec<f32>,
    pos: usize,
    lfo_phase: f32,
    pub rate_hz: f32,
    pub depth: f32,
    pub mix: f32,
}

impl StereoChorus {
    pub fn new() -> Self {
        Self {
            buf_l: vec![0.0; CHORUS_BUFFER_SIZE],
            buf_r: vec![0.0; CHORUS_BUFFER_SIZE],
            pos: 0,
            lfo_phase: 0.0,
            rate_hz: DEFAULT_CHORUS_RATE_HZ,
            depth: DEFAULT_CHORUS_DEPTH,
            mix: 0.0,
        }
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        if self.mix <= MIN_DRIVE_THRESHOLD {
            return (in_l, in_r);
        }

        let lfo_inc = self.rate_hz / sample_rate;
        self.lfo_phase = wrap_phase(self.lfo_phase + lfo_inc);

        let lfo_l = (self.lfo_phase * 2.0 * PI).sin();
        let lfo_r = ((self.lfo_phase + CHORUS_QUADRATURE_OFFSET) * 2.0 * PI).sin();

        let base_delay = sample_rate * CHORUS_BASE_DELAY_SEC;
        let mod_depth = sample_rate * CHORUS_MOD_DEPTH_SEC * self.depth;

        let delay_l = (base_delay + lfo_l * mod_depth) as usize;
        let delay_r = (base_delay + lfo_r * mod_depth) as usize;

        let max_len = self.buf_l.len();
        let read_l = (self.pos + max_len - delay_l) % max_len;
        let read_r = (self.pos + max_len - delay_r) % max_len;

        self.buf_l[self.pos] = in_l;
        self.buf_r[self.pos] = in_r;
        self.pos = (self.pos + 1) % max_len;

        let chorus_l = self.buf_l[read_l];
        let chorus_r = self.buf_r[read_r];

        (
            in_l * (1.0 - self.mix * CHORUS_DRY_ATTEN) + chorus_l * self.mix * CHORUS_WET_GAIN,
            in_r * (1.0 - self.mix * CHORUS_DRY_ATTEN) + chorus_r * self.mix * CHORUS_WET_GAIN,
        )
    }

    pub fn reset(&mut self) {
        self.buf_l.fill(0.0);
        self.buf_r.fill(0.0);
        self.pos = 0;
        self.lfo_phase = 0.0;
    }
}

impl Default for StereoChorus {
    fn default() -> Self {
        Self::new()
    }
}

/// Dynamic Sidechain Pump (Ducking Envelope).
pub struct SidechainPump {
    pub amount: f32, // 0.0 to 1.0
    env: f32,
}

pub const DEFAULT_SIDECHAIN_AMOUNT: f32 = 0.50;

impl SidechainPump {
    pub fn new() -> Self {
        Self {
            amount: DEFAULT_SIDECHAIN_AMOUNT,
            env: 1.0,
        }
    }

    pub fn trigger_kick(&mut self) {
        if self.amount > MIN_DRIVE_THRESHOLD {
            self.env = 1.0 - self.amount * SIDECHAIN_DUCK_SCALE;
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample: f32) -> f32 {
        if self.amount <= MIN_DRIVE_THRESHOLD {
            return sample;
        }
        let out = sample * self.env;
        // Smooth exponential recovery to 1.0
        self.env += (1.0 - self.env) * SIDECHAIN_RECOVERY_RATE;
        out
    }

    pub fn reset(&mut self) {
        self.env = 1.0;
    }
}

impl Default for SidechainPump {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bitcrusher_passthrough_when_off() {
        let mut bc = BitcrusherDrive::new();
        let (out_l, out_r) = bc.process(0.5, -0.5);
        assert_eq!(out_l, 0.5);
        assert_eq!(out_r, -0.5);
    }

    #[test]
    fn test_bitcrusher_quantizes_step() {
        let mut bc = BitcrusherDrive::new();
        bc.bit_depth = 2.0; // 4 levels
        let (out_l, _) = bc.process(0.48, 0.0);
        assert_eq!(out_l, 0.5);
    }

    #[test]
    fn test_chorus_mix_passthrough_when_zero() {
        let mut chorus = StereoChorus::new();
        chorus.mix = 0.0;
        let (l, r) = chorus.process(0.8, -0.8, 48000.0);
        assert_eq!(l, 0.8);
        assert_eq!(r, -0.8);
    }

    #[test]
    fn test_sidechain_ducking_and_recovery() {
        let mut sc = SidechainPump::new();
        sc.amount = 1.0;

        assert_eq!(sc.process(1.0), 1.0);

        sc.trigger_kick();
        let ducked = sc.process(1.0);
        assert!(ducked < 0.2); // Heavily ducked

        // Process recovery samples
        for _ in 0..5000 {
            sc.process(1.0);
        }
        let recovered = sc.process(1.0);
        assert!(recovered > 0.95);
    }
}
