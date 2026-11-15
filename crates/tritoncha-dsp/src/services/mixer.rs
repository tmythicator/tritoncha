//! Multi-bus audio mixing, routing, and effects send matrix.

use crate::core::math::db_to_gain;
use crate::domain::effects::{
    BusEffectChain, BusId, SidechainPump, BUS_ID_MASTER, NUM_ROUTABLE_BUSSES,
};
use crate::domain::Normalized;

pub use crate::domain::effects::{
    BusTarget, BUS_BASS, BUS_DIRECT, BUS_DRUMS, BUS_LEAD, BUS_SPACE, NUM_BUSSES,
};

pub const MAX_BUS_LINEAR_GAIN: f32 = 4.0;

/// Mixer bus state controlling volume, mutes, and effects sends.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct AudioBus {
    pub gain: f32,
    pub muted: bool,
    pub send_delay: Normalized,
    pub send_reverb: Normalized,
    pub bypass_master_fx: bool,
}

impl AudioBus {
    pub fn new(
        gain: f32,
        send_delay: impl Into<Normalized>,
        send_reverb: impl Into<Normalized>,
    ) -> Self {
        Self {
            gain,
            muted: false,
            send_delay: send_delay.into(),
            send_reverb: send_reverb.into(),
            bypass_master_fx: false,
        }
    }
}

/// Frame output from mixer bus summation separating master bus sum from direct bypass.
#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct MixerFrame {
    pub master_bus: f32,
    pub bypass_fx_bus: f32,
    pub delay_send: f32,
    pub reverb_send: f32,
    pub direct_bypass: f32,
}

/// Manages multi-bus routing, sidechain ducking, and effects send matrix.
pub struct Mixer {
    pub busses: [AudioBus; NUM_BUSSES],
    pub sidechain: SidechainPump,
}

impl Mixer {
    pub fn new() -> Self {
        Self {
            busses: [
                AudioBus::new(1.0, 0.02, 0.06), // BUS_DRUMS: neutral 0.0 dB
                AudioBus::new(1.0, 0.0, 0.0),   // BUS_BASS: neutral 0.0 dB
                AudioBus::new(1.0, 0.20, 0.35), // BUS_SPACE: neutral 0.0 dB
                AudioBus::new(1.0, 0.15, 0.10), // BUS_LEAD: neutral 0.0 dB
                AudioBus::new(1.0, 0.0, 0.0),   // BUS_DIRECT: metronome / click / cue bypass
            ],
            sidechain: SidechainPump::new(),
        }
    }

    #[inline(always)]
    pub fn bus_mut(&mut self, bus: impl Into<BusId>) -> Option<&mut AudioBus> {
        self.busses.get_mut(bus.into().as_usize())
    }

    #[inline(always)]
    pub fn bus(&self, bus: impl Into<BusId>) -> Option<&AudioBus> {
        self.busses.get(bus.into().as_usize())
    }

    pub fn set_bus_params(
        &mut self,
        bus: impl Into<BusId>,
        gain_db: f32,
        muted: bool,
        send_delay: impl Into<Normalized>,
        send_reverb: impl Into<Normalized>,
        bypass_master_fx: bool,
    ) {
        if let Some(b) = self.bus_mut(bus) {
            let linear_gain = db_to_gain(gain_db);
            b.gain = linear_gain.clamp(0.0, MAX_BUS_LINEAR_GAIN);
            b.muted = muted;
            b.send_delay = send_delay.into();
            b.send_reverb = send_reverb.into();
            b.bypass_master_fx = bypass_master_fx;
        }
    }

    #[inline(always)]
    pub fn trigger_sidechain_kick(&mut self) {
        self.sidechain.trigger_kick();
    }

    #[inline(always)]
    pub fn set_sidechain_amount(&mut self, amount: impl Into<Normalized>) {
        self.sidechain.amount = *amount.into();
    }

    /// Sums accumulated bus signals, applies sidechain pump to musical tracks,
    /// and dispatches to master mix bus, auxiliary effects sends, and direct output bypass.
    #[inline(always)]
    pub fn process_frame(&mut self, bus_accum: &[f32; NUM_BUSSES]) -> MixerFrame {
        let mut master_bus = 0.0;
        let mut bypass_fx_bus = 0.0;
        let mut delay_send = 0.0;
        let mut reverb_send = 0.0;
        let mut direct_bypass = 0.0;

        for (b, (bus, &accum)) in self.busses.iter().zip(bus_accum.iter()).enumerate() {
            if !bus.muted {
                let mut bus_val = accum * bus.gain;
                if b == BUS_DIRECT {
                    direct_bypass += bus_val;
                } else if bus.bypass_master_fx {
                    bypass_fx_bus += bus_val;
                    delay_send += bus_val * *bus.send_delay;
                    reverb_send += bus_val * *bus.send_reverb;
                } else {
                    // Sidechain ducking is applied to bass and space/lead layers
                    if matches!(b, BUS_BASS..=BUS_LEAD) {
                        bus_val = self.sidechain.process(bus_val);
                    }
                    master_bus += bus_val;
                    delay_send += bus_val * *bus.send_delay;
                    reverb_send += bus_val * *bus.send_reverb;
                }
            }
        }

        MixerFrame {
            master_bus,
            bypass_fx_bus,
            delay_send,
            reverb_send,
            direct_bypass,
        }
    }

    /// Processes all mixer busses through their modular insert chains, applies sidechain
    /// ducking, routes through the master bus chain, and returns stereo `(left, right)`
    /// prior to master headroom/clipping.
    #[inline(always)]
    pub fn mix_frame(
        &mut self,
        bus_accum: &[f32; NUM_BUSSES],
        bus_chains: &mut [BusEffectChain; NUM_ROUTABLE_BUSSES],
        master_gain: f32,
        sample_rate: f32,
    ) -> (f32, f32) {
        let mut master_in_l = 0.0_f32;
        let mut master_in_r = 0.0_f32;
        let mut direct_out_l = 0.0_f32;
        let mut direct_out_r = 0.0_f32;
        let mut cue_click_l = 0.0_f32;
        let mut cue_click_r = 0.0_f32;

        for (b, (bus, &accum)) in self.busses.iter().zip(bus_accum.iter()).enumerate() {
            if !bus.muted {
                let mut bus_val = accum * bus.gain;
                if matches!(b, BUS_BASS..=BUS_LEAD) {
                    bus_val = self.sidechain.process(bus_val);
                }

                let (proc_l, proc_r) = bus_chains[b].process(bus_val, bus_val, sample_rate);

                if b == BUS_DIRECT {
                    cue_click_l += proc_l;
                    cue_click_r += proc_r;
                } else if bus_chains[b].target_out || bus.bypass_master_fx {
                    direct_out_l += proc_l;
                    direct_out_r += proc_r;
                } else {
                    master_in_l += proc_l;
                    master_in_r += proc_r;
                }
            }
        }

        let (master_l, master_r) =
            bus_chains[BUS_ID_MASTER].process(master_in_l, master_in_r, sample_rate);

        let master_out_l = (master_l + direct_out_l) * master_gain;
        let master_out_r = (master_r + direct_out_r) * master_gain;

        (master_out_l + cue_click_l, master_out_r + cue_click_r)
    }
}

impl Default for Mixer {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_audio_bus_initial_state() {
        let bus = AudioBus::new(1.0, 0.2, 0.3);
        assert_eq!(bus.gain, 1.0);
        assert!(!bus.muted);
        assert_eq!(*bus.send_delay, 0.2);
        assert_eq!(*bus.send_reverb, 0.3);
    }

    #[test]
    fn test_mixer_bus_target_conversions() {
        assert_eq!(BusTarget::from(0u8), BusTarget::Drums);
        assert_eq!(BusTarget::from(1usize), BusTarget::Bass);
        assert_eq!(BusTarget::from(2u8), BusTarget::Space);
        assert_eq!(BusTarget::from(3usize), BusTarget::Lead);
        assert_eq!(BusTarget::from(4u8), BusTarget::Direct);
        assert_eq!(BusTarget::Drums.as_usize(), 0);
        assert_eq!(BusTarget::Bass.as_u8(), 1);
    }

    #[test]
    fn test_mixer_frame_summation_and_sends() {
        let mut mixer = Mixer::new();
        let mut accum = [0.0; NUM_BUSSES];
        accum[BUS_DRUMS] = 0.5;
        accum[BUS_BASS] = 0.3;
        accum[BUS_DIRECT] = 0.2;

        let frame = mixer.process_frame(&accum);
        assert!(frame.master_bus > 0.0);
        assert!(frame.direct_bypass > 0.0);
        assert!(frame.delay_send >= 0.0);
        assert!(frame.reverb_send >= 0.0);
    }
}
