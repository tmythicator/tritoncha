pub struct CombFilter {
    buf: Vec<f32>,
    pos: usize,
    fb: f32,
    damp: f32,
    prev: f32,
}

impl CombFilter {
    pub fn new(size: usize) -> Self {
        Self {
            buf: vec![0.0; size],
            pos: 0,
            fb: 0.85,
            damp: 0.2,
            prev: 0.0,
        }
    }

    #[inline(always)]
    pub fn process(&mut self, input: f32) -> f32 {
        let out = self.buf[self.pos];
        self.prev = out * (1.0 - self.damp) + self.prev * self.damp;
        self.buf[self.pos] = input + self.prev * self.fb;
        self.pos = (self.pos + 1) % self.buf.len();
        out
    }
}

pub struct AllpassFilter {
    buf: Vec<f32>,
    pos: usize,
    feedback: f32,
}

impl AllpassFilter {
    pub fn new(size: usize) -> Self {
        Self {
            buf: vec![0.0; size],
            pos: 0,
            feedback: 0.5,
        }
    }

    #[inline(always)]
    pub fn process(&mut self, input: f32) -> f32 {
        let buf_out = self.buf[self.pos];
        let out = -input + buf_out;
        self.buf[self.pos] = input + buf_out * self.feedback;
        self.pos = (self.pos + 1) % self.buf.len();
        out
    }
}

/// Studio-Quality Stereo Schroeder / Freeverb Diffusion Reverb
pub struct StereoReverb {
    combs_l: [CombFilter; 8],
    combs_r: [CombFilter; 8],
    allpasses_l: [AllpassFilter; 4],
    allpasses_r: [AllpassFilter; 4],
    wet: f32,
}

impl StereoReverb {
    pub fn new() -> Self {
        Self {
            combs_l: [
                CombFilter::new(1116),
                CombFilter::new(1188),
                CombFilter::new(1277),
                CombFilter::new(1356),
                CombFilter::new(1422),
                CombFilter::new(1491),
                CombFilter::new(1557),
                CombFilter::new(1617),
            ],
            combs_r: [
                CombFilter::new(1139),
                CombFilter::new(1211),
                CombFilter::new(1300),
                CombFilter::new(1379),
                CombFilter::new(1445),
                CombFilter::new(1514),
                CombFilter::new(1580),
                CombFilter::new(1640),
            ],
            allpasses_l: [
                AllpassFilter::new(556),
                AllpassFilter::new(441),
                AllpassFilter::new(341),
                AllpassFilter::new(225),
            ],
            allpasses_r: [
                AllpassFilter::new(579),
                AllpassFilter::new(464),
                AllpassFilter::new(364),
                AllpassFilter::new(248),
            ],
            wet: 0.35,
        }
    }
}

impl Default for StereoReverb {
    fn default() -> Self {
        Self::new()
    }
}

impl StereoReverb {
    pub fn set_params(&mut self, room_size: f32, wet: f32) {
        self.wet = wet.clamp(0.0, 1.0);
        let fb = (0.70 + room_size * 0.20).clamp(0.65, 0.96);
        for c in &mut self.combs_l {
            c.fb = fb;
        }
        for c in &mut self.combs_r {
            c.fb = fb;
        }
    }

    #[inline(always)]
    pub fn process_wet(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet <= 0.001 {
            return (0.0, 0.0);
        }

        let mono = (in_l + in_r) * 0.35;
        let mut out_l = 0.0;
        let mut out_r = 0.0;

        // 8 Parallel tuned comb filters
        for c in &mut self.combs_l {
            out_l += c.process(mono);
        }
        for c in &mut self.combs_r {
            out_r += c.process(mono);
        }

        // 4 Series allpass diffusion stages for lush spatial cloud
        for a in &mut self.allpasses_l {
            out_l = a.process(out_l);
        }
        for a in &mut self.allpasses_r {
            out_r = a.process(out_r);
        }

        (out_l * self.wet * 0.75, out_r * self.wet * 0.75)
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        let (wl, wr) = self.process_wet(in_l, in_r);
        (in_l + wl, in_r + wr)
    }
}
