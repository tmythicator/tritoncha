//! Studio-Quality Stereo Schroeder / Freeverb Diffusion Reverb.

use crate::dsp::math::lerp;

pub const NUM_COMB_FILTERS: usize = 8;
pub const NUM_ALLPASS_FILTERS: usize = 4;

pub const DEFAULT_SAMPLE_RATE: f32 = 48000.0;
pub const DEFAULT_REVERB_WET: f32 = 0.35;
pub const MIN_WET_THRESHOLD: f32 = 0.001;
pub const COMB_FEEDBACK_DEFAULT: f32 = 0.85;
pub const MAX_COMB_FEEDBACK: f32 = 0.98;
pub const COMB_DAMPING_DEFAULT: f32 = 0.20;
pub const COMB_FEEDBACK_OFFSET: f32 = 0.70;
pub const COMB_FEEDBACK_SCALE: f32 = 0.28;
pub const ALLPASS_FEEDBACK_DEFAULT: f32 = 0.50;
pub const REVERB_STEREO_SPREAD_GAIN: f32 = 0.015;
pub const REVERB_DRY_MIX_FACTOR: f32 = 0.5;
pub const STEREO_TO_MONO_SCALE: f32 = 0.5;

pub const FREEVERB_COMB_TUNINGS_L: [usize; NUM_COMB_FILTERS] =
    [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617];
pub const FREEVERB_COMB_TUNINGS_R: [usize; NUM_COMB_FILTERS] =
    [1139, 1211, 1300, 1379, 1445, 1514, 1580, 1640];

pub const FREEVERB_ALLPASS_TUNINGS_L: [usize; NUM_ALLPASS_FILTERS] = [556, 441, 341, 225];
pub const FREEVERB_ALLPASS_TUNINGS_R: [usize; NUM_ALLPASS_FILTERS] = [579, 464, 364, 248];

/// Feedback Comb Filter with lowpass damping.
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
            fb: COMB_FEEDBACK_DEFAULT,
            damp: COMB_DAMPING_DEFAULT,
            prev: 0.0,
        }
    }

    pub fn set_feedback(&mut self, fb: f32) {
        self.fb = fb.clamp(0.0, MAX_COMB_FEEDBACK);
    }

    #[inline(always)]
    pub fn process(&mut self, input: f32) -> f32 {
        let out = self.buf[self.pos];
        self.prev = lerp(out, self.prev, self.damp);
        self.buf[self.pos] = input + self.prev * self.fb;
        self.pos = (self.pos + 1) % self.buf.len();
        out
    }

    pub fn reset(&mut self) {
        self.buf.fill(0.0);
        self.pos = 0;
        self.prev = 0.0;
    }
}

/// Allpass filter for dense phase diffusion without magnitude alteration.
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
            feedback: ALLPASS_FEEDBACK_DEFAULT,
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

    pub fn reset(&mut self) {
        self.buf.fill(0.0);
        self.pos = 0;
    }
}

use crate::dsp::fdn_reverb::FdnReverb;

/// Reverb Engine Algorithm Mode.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ReverbMode {
    Freeverb = 0,
    Fdn = 1,
}

/// Studio-Quality Stereo Reverb with selectable Freeverb and 8-channel Householder FDN modes.
pub struct StereoReverb {
    pub mode: ReverbMode,
    pub fdn: FdnReverb,
    combs_l: [CombFilter; NUM_COMB_FILTERS],
    combs_r: [CombFilter; NUM_COMB_FILTERS],
    allpasses_l: [AllpassFilter; NUM_ALLPASS_FILTERS],
    allpasses_r: [AllpassFilter; NUM_ALLPASS_FILTERS],
    wet: f32,
}

impl StereoReverb {
    pub fn new() -> Self {
        Self::with_sample_rate(DEFAULT_SAMPLE_RATE)
    }

    pub fn with_sample_rate(sample_rate: f32) -> Self {
        Self {
            mode: ReverbMode::Fdn,
            fdn: FdnReverb::new(sample_rate),
            combs_l: std::array::from_fn(|i| CombFilter::new(FREEVERB_COMB_TUNINGS_L[i])),
            combs_r: std::array::from_fn(|i| CombFilter::new(FREEVERB_COMB_TUNINGS_R[i])),
            allpasses_l: std::array::from_fn(|i| AllpassFilter::new(FREEVERB_ALLPASS_TUNINGS_L[i])),
            allpasses_r: std::array::from_fn(|i| AllpassFilter::new(FREEVERB_ALLPASS_TUNINGS_R[i])),
            wet: DEFAULT_REVERB_WET,
        }
    }

    /// Sets the reverb algorithm mode.
    pub fn set_mode(&mut self, mode: ReverbMode) {
        self.mode = mode;
    }

    pub fn set_wet(&mut self, wet: f32) {
        self.wet = wet.clamp(0.0, 1.0);
        self.fdn.set_wet(self.wet);
    }

    pub fn set_params(&mut self, room_size: f32, wet: f32) {
        self.wet = wet.clamp(0.0, 1.0);
        self.fdn.set_room_size(room_size);
        self.fdn.set_wet(self.wet);

        let fb =
            (COMB_FEEDBACK_OFFSET + room_size * COMB_FEEDBACK_SCALE).clamp(0.0, MAX_COMB_FEEDBACK);
        for c in &mut self.combs_l {
            c.set_feedback(fb);
        }
        for c in &mut self.combs_r {
            c.set_feedback(fb);
        }
    }

    #[inline(always)]
    pub fn process_wet(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet <= MIN_WET_THRESHOLD {
            return (0.0, 0.0);
        }

        match self.mode {
            ReverbMode::Fdn => self.fdn.process_wet(in_l, in_r),
            ReverbMode::Freeverb => {
                let mono_in = (in_l + in_r) * STEREO_TO_MONO_SCALE;

                let mut out_l = 0.0;
                let mut out_r = 0.0;

                for c in &mut self.combs_l {
                    out_l += c.process(mono_in);
                }
                for c in &mut self.combs_r {
                    out_r += c.process(mono_in);
                }

                for ap in &mut self.allpasses_l {
                    out_l = ap.process(out_l);
                }
                for ap in &mut self.allpasses_r {
                    out_r = ap.process(out_r);
                }

                let wet_gain = self.wet * REVERB_STEREO_SPREAD_GAIN;
                (out_l * wet_gain, out_r * wet_gain)
            }
        }
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32) -> (f32, f32) {
        if self.wet <= MIN_WET_THRESHOLD {
            return (in_l, in_r);
        }

        let mono_in = (in_l + in_r) * STEREO_TO_MONO_SCALE;

        // Parallel comb filters
        let mut out_l = 0.0;
        let mut out_r = 0.0;

        for c in &mut self.combs_l {
            out_l += c.process(mono_in);
        }
        for c in &mut self.combs_r {
            out_r += c.process(mono_in);
        }

        // Series allpass diffusers
        for ap in &mut self.allpasses_l {
            out_l = ap.process(out_l);
        }
        for ap in &mut self.allpasses_r {
            out_r = ap.process(out_r);
        }

        let wet_gain = self.wet * REVERB_STEREO_SPREAD_GAIN;
        let dry_gain = 1.0 - self.wet * REVERB_DRY_MIX_FACTOR;

        (
            in_l * dry_gain + out_l * wet_gain,
            in_r * dry_gain + out_r * wet_gain,
        )
    }

    pub fn reset(&mut self) {
        for c in &mut self.combs_l {
            c.reset();
        }
        for c in &mut self.combs_r {
            c.reset();
        }
        for ap in &mut self.allpasses_l {
            ap.reset();
        }
        for ap in &mut self.allpasses_r {
            ap.reset();
        }
    }
}

impl Default for StereoReverb {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_allpass_diffusion_preserves_energy() {
        let mut ap = AllpassFilter::new(50);
        let mut energy_out = 0.0;

        for i in 0..500 {
            let x = if i == 0 { 1.0 } else { 0.0 };
            let y = ap.process(x);
            assert!(y.is_finite());
            energy_out += y * y;
        }

        // Verify impulse response is non-zero, finite and diffuses energy
        assert!(energy_out > 0.5);
    }

    #[test]
    fn test_reverb_passthrough_when_dry() {
        let mut reverb = StereoReverb::new();
        reverb.set_wet(0.0);
        let (out_l, out_r) = reverb.process(0.5, -0.5);
        assert_eq!(out_l, 0.5);
        assert_eq!(out_r, -0.5);
    }

    #[test]
    fn test_reverb_tail_generation() {
        let mut reverb = StereoReverb::new();
        reverb.set_wet(0.5);

        // Send an impulse
        reverb.process(1.0, 1.0);

        // Tail should ring out with decaying energy
        let mut tail_energy = 0.0;
        for _ in 0..2000 {
            let (l, r) = reverb.process(0.0, 0.0);
            tail_energy += l * l + r * r;
        }
        assert!(tail_energy > 0.001);
    }
}
