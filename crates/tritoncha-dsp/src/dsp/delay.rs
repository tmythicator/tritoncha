pub const MAX_DELAY_SAMPLES: usize = 96000;
pub const MIN_DELAY_SAMPLES: f32 = 500.0;
pub const MAX_FEEDBACK: f32 = 0.94;
pub const DEFAULT_DELAY_SAMPLES: usize = 16000;
pub const DEFAULT_FEEDBACK: f32 = 0.35;
pub const DEFAULT_WET: f32 = 0.25;
pub const DAMPING_COEFF: f32 = 0.35;
pub const INVERSE_DAMPING_COEFF: f32 = 0.65;

/// Stereo Ping-Pong Dub Delay with Lowpass Analog Damping
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
}

impl Default for StereoDelay {
    fn default() -> Self {
        Self::new()
    }
}

impl StereoDelay {
    pub fn set_params(&mut self, time_s: f32, feedback: f32, wet: f32, sample_rate: f32) {
        let len = (time_s * sample_rate).clamp(MIN_DELAY_SAMPLES, (MAX_DELAY_SAMPLES - 1) as f32)
            as usize;
        self.delay_len = len;
        self.feedback = feedback.clamp(0.0, MAX_FEEDBACK);
        self.wet = wet.clamp(0.0, 1.0);
    }

    #[inline(always)]
    pub fn process_wet(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet <= 0.001 {
            return (0.0, 0.0);
        }

        let read_l = (self.pos + MAX_DELAY_SAMPLES - self.delay_len) % MAX_DELAY_SAMPLES;
        // Stereo ping-pong offset for rhythmic width (dotted / 1.5x on right channel)
        let r_offset = (self.delay_len * 3) / 2;
        let read_r =
            (self.pos + MAX_DELAY_SAMPLES - (r_offset % MAX_DELAY_SAMPLES)) % MAX_DELAY_SAMPLES;

        let dl = self.buf_l[read_l];
        let dr = self.buf_r[read_r];

        // Analog lowpass damping
        self.prev_l = self.prev_l * DAMPING_COEFF + dl * INVERSE_DAMPING_COEFF;
        self.prev_r = self.prev_r * DAMPING_COEFF + dr * INVERSE_DAMPING_COEFF;

        // Ping-pong cross-feedback
        self.buf_l[self.pos] = (in_l + self.prev_r * self.feedback).tanh();
        self.buf_r[self.pos] = (in_r + self.prev_l * self.feedback).tanh();

        self.pos = (self.pos + 1) % MAX_DELAY_SAMPLES;

        (dl * self.wet, dr * self.wet)
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        let (wl, wr) = self.process_wet(in_l, in_r);
        (in_l + wl, in_r + wr)
    }
}
