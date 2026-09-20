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

pub enum BusEffectNode {
    Filter {
        filter_l: StateVariableFilter,
        filter_r: StateVariableFilter,
        cutoff_hz: f32,
        resonance: f32,
        sweep: FrequencySweep,
    },
    Delay(StereoDelay),
    Distort(BitcrusherDrive),
    Chorus(StereoChorus),
    Reverb(Box<StereoReverb>),
    Compressor(BusCompressor),
}

impl BusEffectNode {
    #[inline(always)]
    pub fn process(&mut self, in_l: f32, in_r: f32, sample_rate: f32) -> (f32, f32) {
        match self {
            BusEffectNode::Filter {
                filter_l,
                filter_r,
                cutoff_hz,
                resonance,
                sweep,
            } => {
                if let Some(c) = sweep.advance(1, 20.0, 18000.0) {
                    *cutoff_hz = c;
                }
                let out_l = filter_l.process_lp(in_l, *cutoff_hz, *resonance, sample_rate);
                let out_r = filter_r.process_lp(in_r, *cutoff_hz, *resonance, sample_rate);
                (out_l, out_r)
            }
            BusEffectNode::Delay(delay) => delay.process(in_l, in_r),
            BusEffectNode::Distort(distort) => distort.process(in_l, in_r),
            BusEffectNode::Chorus(chorus) => chorus.process(in_l, in_r, sample_rate),
            BusEffectNode::Reverb(reverb) => reverb.process(in_l, in_r),
            BusEffectNode::Compressor(comp) => comp.process(in_l, in_r),
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
            if let Some(BusEffectNode::Filter {
                cutoff_hz,
                resonance: res,
                sweep,
                ..
            }) = slot
            {
                *cutoff_hz = cutoff;
                *res = resonance;
                sweep.cancel();
            }
        }
    }

    pub fn sweep_filter(&mut self, from_hz: f32, to_hz: f32, dur_s: f32, sample_rate: f32) {
        for slot in &mut self.slots {
            if let Some(BusEffectNode::Filter {
                cutoff_hz, sweep, ..
            }) = slot
            {
                *cutoff_hz = from_hz;
                sweep.start(from_hz, to_hz, dur_s, sample_rate);
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
    pub fn process(&mut self, mut in_l: f32, mut in_r: f32, sample_rate: f32) -> (f32, f32) {
        for node in self.slots.iter_mut().flatten() {
            let (out_l, out_r) = node.process(in_l, in_r, sample_rate);
            in_l = out_l;
            in_r = out_r;
        }
        (in_l, in_r)
    }
}
