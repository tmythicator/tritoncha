//! Stereo Ping-Pong Dub Delay with Lowpass Analog Damping and Feedback Limiting.

use crate::dsp::math::lerp;

pub const MAX_DELAY_SAMPLES: usize = 96000;
pub const MIN_DELAY_SAMPLES: f32 = 500.0;
pub const MAX_FEEDBACK: f32 = 0.94;
pub const DEFAULT_DELAY_SAMPLES: usize = 16000;
pub const DEFAULT_FEEDBACK: f32 = 0.35;
pub const DEFAULT_WET: f32 = 0.25;
pub const DAMPING_COEFF: f32 = 0.35;
pub const INVERSE_DAMPING_COEFF: f32 = 1.0 - DAMPING_COEFF;
pub const MIN_WET_THRESHOLD: f32 = 0.001;

/// Stereo Ping-Pong Dub Delay with Lowpass Analog Damping.
pub struct StereoDelay {
    buf_l: Vec<f32>,
    buf_r: Vec<f32>,
    pos: usize,
    delay_len: usize,
    feedback: f32,
    wet: f32,
    prev_l: f32,
    prev_r: f32,
}

impl StereoDelay {
    pub fn new() -> Self {
        Self {
            buf_l: vec![0.0; MAX_DELAY_SAMPLES],
            buf_r: vec![0.0; MAX_DELAY_SAMPLES],
            pos: 0,
            delay_len: DEFAULT_DELAY_SAMPLES,
            feedback: DEFAULT_FEEDBACK,
            wet: DEFAULT_WET,
            prev_l: 0.0,
            prev_r: 0.0,
        }
    }

    pub fn set_params(&mut self, time_s: f32, feedback: f32, wet: f32, sample_rate: f32) {
        let max_samples = (MAX_DELAY_SAMPLES - 1) as f32;
        let len = (time_s * sample_rate).clamp(MIN_DELAY_SAMPLES, max_samples) as usize;
        self.delay_len = len;
        self.feedback = feedback.clamp(0.0, MAX_FEEDBACK);
        self.wet = wet.clamp(0.0, 1.0);
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet <= MIN_WET_THRESHOLD {
            return (in_l, in_r);
        }

        let max_len = MAX_DELAY_SAMPLES;
        let read_idx = (self.pos + max_len - self.delay_len) % max_len;

        let delayed_l = self.buf_l[read_idx];
        let delayed_r = self.buf_r[read_idx];

        // One-pole lowpass filter for analog warmth on the feedback path
        self.prev_l = lerp(delayed_l, self.prev_l, DAMPING_COEFF);
        self.prev_r = lerp(delayed_r, self.prev_r, DAMPING_COEFF);

        // Ping-pong cross-feedback routing
        self.buf_l[self.pos] = in_l + self.prev_r * self.feedback;
        self.buf_r[self.pos] = in_r + self.prev_l * self.feedback;

        self.pos = (self.pos + 1) % max_len;

        (
            in_l * (1.0 - self.wet) + delayed_l * self.wet,
            in_r * (1.0 - self.wet) + delayed_r * self.wet,
        )
    }

    #[inline(always)]
    pub fn process_wet(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet <= MIN_WET_THRESHOLD {
            return (0.0, 0.0);
        }

        let max_len = MAX_DELAY_SAMPLES;
        let read_idx = (self.pos + max_len - self.delay_len) % max_len;

        let delayed_l = self.buf_l[read_idx];
        let delayed_r = self.buf_r[read_idx];

        // One-pole lowpass filter for analog warmth on the feedback path
        self.prev_l = lerp(delayed_l, self.prev_l, DAMPING_COEFF);
        self.prev_r = lerp(delayed_r, self.prev_r, DAMPING_COEFF);

        // Ping-pong cross-feedback routing
        self.buf_l[self.pos] = in_l + self.prev_r * self.feedback;
        self.buf_r[self.pos] = in_r + self.prev_l * self.feedback;

        self.pos = (self.pos + 1) % max_len;

        (delayed_l * self.wet, delayed_r * self.wet)
    }

    pub fn reset(&mut self) {
        self.buf_l.fill(0.0);
        self.buf_r.fill(0.0);
        self.pos = 0;
        self.prev_l = 0.0;
        self.prev_r = 0.0;
    }
}

impl Default for StereoDelay {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_delay_passthrough_when_dry() {
        let mut delay = StereoDelay::new();
        delay.set_params(0.2, 0.5, 0.0, 48000.0);
        let (out_l, out_r) = delay.process(0.75, -0.75);
        assert_eq!(out_l, 0.75);
        assert_eq!(out_r, -0.75);
    }

    #[test]
    fn test_delay_echoes_after_delay_length() {
        let mut delay = StereoDelay::new();
        let delay_s = 0.02; // 20ms = 960 samples @ 48kHz (> MIN_DELAY_SAMPLES of 500)
        let sample_rate = 48000.0;
        delay.set_params(delay_s, 0.0, 1.0, sample_rate); // 100% wet, no feedback

        // Send an impulse
        delay.process(1.0, 1.0);

        let delay_samples = (delay_s * sample_rate) as usize;
        // Process silence until echo point
        for _ in 1..delay_samples {
            let (l, r) = delay.process(0.0, 0.0);
            assert_eq!(l, 0.0);
            assert_eq!(r, 0.0);
        }

        // Echo arrives
        let (echo_l, echo_r) = delay.process(0.0, 0.0);
        assert!((echo_l - 1.0).abs() < 0.001);
        assert!((echo_r - 1.0).abs() < 0.001);
    }
}
