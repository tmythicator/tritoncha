//! Modular per-bus DSP insert effects chain for Tritoncha.

use crate::domain::effects::{
    BitcrusherDrive, BusCompressor, CompressorConfig, FrequencySweep, StateVariableFilter,
    StereoChorus, StereoDelay, StereoReverb,
};

pub const MAX_BUS_INSERTS: usize = 4;
pub const NUM_ROUTABLE_BUSSES: usize = 6;

pub const BUS_ID_DRUMS: usize = 0;
pub const BUS_ID_BASS: usize = 1;
pub const BUS_ID_SPACE: usize = 2;
pub const BUS_ID_LEAD: usize = 3;
pub const BUS_ID_DIRECT: usize = 4;
pub const BUS_ID_MASTER: usize = 5;

#[derive(Copy, Clone, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct BusId(pub usize);

impl BusId {
    pub const DRUMS: Self = Self(BUS_ID_DRUMS);
    pub const BASS: Self = Self(BUS_ID_BASS);
    pub const SPACE: Self = Self(BUS_ID_SPACE);
    pub const LEAD: Self = Self(BUS_ID_LEAD);
    pub const DIRECT: Self = Self(BUS_ID_DIRECT);
    pub const MASTER: Self = Self(BUS_ID_MASTER);

    pub const COUNT: usize = NUM_ROUTABLE_BUSSES;
    pub const VOICE_BUS_COUNT: usize = 5;

    #[inline(always)]
    pub const fn as_usize(self) -> usize {
        self.0
    }

    #[inline(always)]
    pub fn is_routable(self) -> bool {
        self.0 < NUM_ROUTABLE_BUSSES
    }
}

impl From<usize> for BusId {
    #[inline(always)]
    fn from(idx: usize) -> Self {
        Self(idx)
    }
}

pub trait AudioProcessor {
    fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32);
    fn reset(&mut self);
}

/// Stereo State Variable Filter (SVF) with exponential frequency sweep.
pub struct StereoSvfFilter {
    pub filter_l: StateVariableFilter,
    pub filter_r: StateVariableFilter,
    pub cutoff_hz: f32,
    pub resonance: f32,
    pub sweep: FrequencySweep,
}

impl StereoSvfFilter {
    pub fn new(cutoff_hz: f32, resonance: f32) -> Self {
        Self {
            filter_l: StateVariableFilter::new(),
            filter_r: StateVariableFilter::new(),
            cutoff_hz,
            resonance,
            sweep: FrequencySweep::new(cutoff_hz),
        }
    }

    pub fn set_params(&mut self, cutoff_hz: f32, resonance: f32) {
        self.cutoff_hz = cutoff_hz;
        self.resonance = resonance;
        self.sweep.cancel();
    }

    pub fn start_sweep(&mut self, from_hz: f32, to_hz: f32, dur_s: f32, sample_rate: f32) {
        self.cutoff_hz = from_hz;
        self.sweep.start(from_hz, to_hz, dur_s, sample_rate);
    }
}

impl AudioProcessor for StereoSvfFilter {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        if let Some(c) = self.sweep.advance(1, 20.0, 18000.0) {
            self.cutoff_hz = c;
        }
        let out_l = self
            .filter_l
            .process_lp(in_l, self.cutoff_hz, self.resonance, sample_rate);
        let out_r = self
            .filter_r
            .process_lp(in_r, self.cutoff_hz, self.resonance, sample_rate);
        (out_l, out_r)
    }

    fn reset(&mut self) {
        self.filter_l.reset();
        self.filter_r.reset();
        self.sweep.cancel();
    }
}

impl AudioProcessor for StereoDelay {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, _sample_rate: f32) -> (f32, f32) {
        self.process(in_l, in_r)
    }

    fn reset(&mut self) {
        self.reset();
    }
}

impl AudioProcessor for BitcrusherDrive {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, _sample_rate: f32) -> (f32, f32) {
        self.process(in_l, in_r)
    }

    fn reset(&mut self) {
        self.reset();
    }
}

impl AudioProcessor for StereoChorus {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        self.process(in_l, in_r, sample_rate)
    }

    fn reset(&mut self) {
        self.reset();
    }
}

impl AudioProcessor for StereoReverb {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, _sample_rate: f32) -> (f32, f32) {
        self.process(in_l, in_r)
    }

    fn reset(&mut self) {
        self.reset();
    }
}

impl AudioProcessor for BusCompressor {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, _sample_rate: f32) -> (f32, f32) {
        self.process(in_l, in_r)
    }

    fn reset(&mut self) {
        self.reset();
    }
}

impl<T: AudioProcessor + ?Sized> AudioProcessor for Box<T> {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        (**self).process(in_l, in_r, sample_rate)
    }

    fn reset(&mut self) {
        (**self).reset();
    }
}

pub enum BusEffectNode {
    Filter(StereoSvfFilter),
    Delay(StereoDelay),
    Distort(BitcrusherDrive),
    Chorus(StereoChorus),
    Reverb(Box<StereoReverb>),
    Compressor(BusCompressor),
}

impl AudioProcessor for BusEffectNode {
    #[inline(always)]
    fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        match self {
            BusEffectNode::Filter(f) => AudioProcessor::process(f, in_l, in_r, sample_rate),
            BusEffectNode::Delay(d) => AudioProcessor::process(d, in_l, in_r, sample_rate),
            BusEffectNode::Distort(d) => AudioProcessor::process(d, in_l, in_r, sample_rate),
            BusEffectNode::Chorus(c) => AudioProcessor::process(c, in_l, in_r, sample_rate),
            BusEffectNode::Reverb(r) => AudioProcessor::process(r, in_l, in_r, sample_rate),
            BusEffectNode::Compressor(c) => AudioProcessor::process(c, in_l, in_r, sample_rate),
        }
    }

    fn reset(&mut self) {
        match self {
            BusEffectNode::Filter(f) => f.reset(),
            BusEffectNode::Delay(d) => d.reset(),
            BusEffectNode::Distort(d) => d.reset(),
            BusEffectNode::Chorus(c) => c.reset(),
            BusEffectNode::Reverb(r) => r.reset(),
            BusEffectNode::Compressor(c) => c.reset(),
        }
    }
}

pub struct BusEffectChain {
    pub target_out: bool,
    pub slots: [Option<BusEffectNode>; MAX_BUS_INSERTS],
}

impl Default for BusEffectChain {
    fn default() -> Self {
        Self::new()
    }
}

impl BusEffectChain {
    pub fn new() -> Self {
        Self {
            target_out: false,
            slots: [None, None, None, None],
        }
    }

    pub fn clear(&mut self) {
        self.target_out = false;
        self.slots = [None, None, None, None];
    }

    pub fn push(&mut self, node: BusEffectNode) {
        for slot in &mut self.slots {
            if slot.is_none() {
                *slot = Some(node);
                return;
            }
        }
    }

    pub fn update_filter(&mut self, cutoff: f32, resonance: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Filter(filter)) = slot {
                filter.set_params(cutoff, resonance);
            }
        }
    }

    pub fn sweep_filter(&mut self, from_hz: f32, to_hz: f32, dur_s: f32, sample_rate: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Filter(filter)) = slot {
                filter.start_sweep(from_hz, to_hz, dur_s, sample_rate);
            }
        }
    }

    pub fn update_delay(&mut self, time_s: f32, feedback: f32, wet: f32, sample_rate: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Delay(delay)) = slot {
                delay.set_params(time_s, feedback, wet, sample_rate);
            }
        }
    }

    pub fn update_distort(&mut self, drive: f32, bits: f32, sample_hold: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Distort(distort)) = slot {
                distort.set_drive(drive);
                distort.bit_depth = bits;
                distort.sample_hold = sample_hold;
            }
        }
    }

    pub fn update_chorus(&mut self, rate_hz: f32, depth: f32, mix: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Chorus(chorus)) = slot {
                chorus.rate_hz = rate_hz;
                chorus.depth = depth;
                chorus.mix = mix;
            }
        }
    }

    pub fn update_reverb(&mut self, room_size: f32, wet: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Reverb(reverb)) = slot {
                reverb.set_params(room_size, wet);
            }
        }
    }

    pub fn update_compressor(&mut self, config: CompressorConfig) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Compressor(comp)) = slot {
                comp.set_config(config);
            }
        }
    }

    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        AudioProcessor::process(self, in_l, in_r, sample_rate)
    }
}

impl AudioProcessor for BusEffectChain {
    #[inline(always)]
    fn process(&mut self, mut in_l: f32, mut in_r: f32, sample_rate: f32) -> (f32, f32) {
        for node in self.slots.iter_mut().flatten() {
            let (out_l, out_r) = node.process(in_l, in_r, sample_rate);
            in_l = out_l;
            in_r = out_r;
        }
        (in_l, in_r)
    }

    fn reset(&mut self) {
        for node in self.slots.iter_mut().flatten() {
            node.reset();
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_audio_processor_trait_contract_and_object_safety() {
        struct MockPassthrough;
        impl AudioProcessor for MockPassthrough {
            fn process(&mut self, in_l: f32, in_r: f32, _sample_rate: f32) -> (f32, f32) {
                (in_l * 0.5, in_r * 0.5)
            }
            fn reset(&mut self) {}
        }

        let mut mock = MockPassthrough;
        let dyn_proc: &mut dyn AudioProcessor = &mut mock;
        let (out_l, out_r) = dyn_proc.process(1.0, -1.0, 48000.0);
        assert!((out_l - 0.5).abs() < 1e-6);
        assert!((out_r - (-0.5)).abs() < 1e-6);
        dyn_proc.reset();
    }

    #[test]
    fn test_stereo_svf_filter_implements_audio_processor() {
        let mut filter = StereoSvfFilter::new(500.0, 0.3);
        let sample_rate = 48000.0;

        // Process a block of high-frequency pulses (alternating +1.0, -1.0 at 24kHz)
        let mut last_l = 0.0;
        let mut last_r = 0.0;
        for i in 0..128 {
            let s = if i % 2 == 0 { 1.0 } else { -1.0 };
            let (out_l, out_r) = filter.process(s, s, sample_rate);
            last_l = out_l;
            last_r = out_r;
        }

        // High frequencies should be significantly attenuated by 500Hz LP filter
        assert!(
            last_l.abs() < 0.2,
            "Expected LP attenuation, got {}",
            last_l
        );
        assert!(
            last_r.abs() < 0.2,
            "Expected LP attenuation, got {}",
            last_r
        );

        // Reset should clear filter state
        filter.reset();
        let (zero_l, zero_r) = filter.process(0.0, 0.0, sample_rate);
        assert_eq!(zero_l, 0.0);
        assert_eq!(zero_r, 0.0);
    }

    #[test]
    fn test_individual_dsp_effects_implement_audio_processor() {
        let sample_rate = 48000.0;

        // 1. StereoDelay
        let mut delay = StereoDelay::new();
        delay.set_params(0.01, 0.5, 1.0, sample_rate);
        let _ = AudioProcessor::process(&mut delay, 1.0, 1.0, sample_rate);
        delay.reset();
        let (silence_l, silence_r) = AudioProcessor::process(&mut delay, 0.0, 0.0, sample_rate);
        assert_eq!(silence_l, 0.0);
        assert_eq!(silence_r, 0.0);

        // 2. BitcrusherDrive
        let mut dist = BitcrusherDrive::new();
        dist.set_drive(0.5);
        let (d_l, d_r) = AudioProcessor::process(&mut dist, 0.8, -0.8, sample_rate);
        assert!(d_l.is_finite() && d_r.is_finite());
        dist.reset();

        // 3. StereoChorus
        let mut chorus = StereoChorus::new();
        let (c_l, c_r) = AudioProcessor::process(&mut chorus, 0.5, 0.5, sample_rate);
        assert!(c_l.is_finite() && c_r.is_finite());
        chorus.reset();

        // 4. StereoReverb
        let mut reverb = StereoReverb::new();
        let (rev_l, rev_r) = AudioProcessor::process(&mut reverb, 1.0, 1.0, sample_rate);
        assert!(rev_l.is_finite() && rev_r.is_finite());
        reverb.reset();

        // 5. BusCompressor
        let mut comp = BusCompressor::new(sample_rate);
        let (comp_l, comp_r) = AudioProcessor::process(&mut comp, 2.0, 2.0, sample_rate);
        assert!(comp_l.is_finite() && comp_l <= 2.0);
        assert!(comp_r.is_finite() && comp_r <= 2.0);
        comp.reset();
    }

    #[test]
    fn test_bus_effect_node_and_chain_composite_audio_processor() {
        let sample_rate = 48000.0;
        let mut chain = BusEffectChain::new();

        // Push Filter and Delay
        chain.push(BusEffectNode::Filter(StereoSvfFilter::new(2000.0, 0.1)));
        let mut delay = StereoDelay::new();
        delay.set_params(0.01, 0.3, 0.5, sample_rate);
        chain.push(BusEffectNode::Delay(delay));

        // Process through chain as AudioProcessor
        let (out_l, out_r) = chain.process(0.5, 0.5, sample_rate);
        assert!(out_l.is_finite());
        assert!(out_r.is_finite());

        // Chain reset clears all slots
        chain.reset();
        let (zero_l, zero_r) = chain.process(0.0, 0.0, sample_rate);
        assert!(zero_l.abs() < 1e-4);
        assert!(zero_r.abs() < 1e-4);
    }
}
