use std::f32::consts::PI;

/// Bitcrusher and analog drive / saturation effect
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
            bit_depth: 16.0,
            sample_hold: 1.0,
            hold_counter: 0.0,
            last_sample_l: 0.0,
            last_sample_r: 0.0,
        }
    }
}

impl Default for BitcrusherDrive {
    fn default() -> Self {
        Self::new()
    }
}

impl BitcrusherDrive {
    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.drive <= 0.001 && self.bit_depth >= 15.9 && self.sample_hold <= 1.05 {
            return (in_l, in_r);
        }

        // 1. Overdrive saturation
        let drive_gain = 1.0 + self.drive * 6.0;
        let mut x_l = (in_l * drive_gain).tanh();
        let mut x_r = (in_r * drive_gain).tanh();

        // 2. Sample rate reduction
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
        if self.bit_depth < 15.5 {
            let step = 2.0_f32.powf(self.bit_depth);
            x_l = (x_l * step).round() / step;
            x_r = (x_r * step).round() / step;
        }

        (x_l, x_r)
    }
}

/// Stereo LFO Chorus and Flanger
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
            buf_l: vec![0.0; 44100],
            buf_r: vec![0.0; 44100],
            pos: 0,
            lfo_phase: 0.0,
            rate_hz: 0.8,
            depth: 0.4,
            mix: 0.0,
        }
    }
}

impl Default for StereoChorus {
    fn default() -> Self {
        Self::new()
    }
}

impl StereoChorus {
    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        if self.mix <= 0.001 {
            return (in_l, in_r);
        }

        let lfo_inc = self.rate_hz / sample_rate;
        self.lfo_phase += lfo_inc;
        if self.lfo_phase >= 1.0 {
            self.lfo_phase -= 1.0;
        }

        let lfo_l = (self.lfo_phase * 2.0 * PI).sin();
        let lfo_r = ((self.lfo_phase + 0.25) * 2.0 * PI).sin();

        let base_delay = sample_rate * 0.015; // 15ms
        let mod_depth = sample_rate * 0.008 * self.depth; // +/- 8ms modulation

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
            in_l * (1.0 - self.mix * 0.5) + chorus_l * self.mix * 0.6,
            in_r * (1.0 - self.mix * 0.5) + chorus_r * self.mix * 0.6,
        )
    }
}

/// Dynamic Sidechain Pump (Ducking Envelope)
pub struct SidechainPump {
    pub amount: f32, // 0.0 to 1.0
    env: f32,
}

impl SidechainPump {
    pub fn new() -> Self {
        Self {
            amount: 0.0,
            env: 1.0,
        }
    }
}

impl Default for SidechainPump {
    fn default() -> Self {
        Self::new()
    }
}

impl SidechainPump {
    pub fn trigger_kick(&mut self) {
        if self.amount > 0.001 {
            self.env = 1.0 - self.amount * 0.85;
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample: f32) -> f32 {
        if self.amount <= 0.001 {
            return sample;
        }
        let out = sample * self.env;
        // Smooth exponential recovery to 1.0
        self.env += (1.0 - self.env) * 0.0012;
        out
    }
}
