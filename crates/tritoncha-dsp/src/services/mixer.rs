//! Multi-bus audio mixing, routing, and effects send matrix.

use crate::core::math::db_to_gain;
use crate::domain::effects::SidechainPump;

pub const NUM_BUSSES: usize = 5;

pub const BUS_DRUMS: usize = 0;
pub const BUS_BASS: usize = 1;
pub const BUS_SPACE: usize = 2;
pub const BUS_LEAD: usize = 3;
pub const BUS_DIRECT: usize = 4;

pub const MAX_BUS_LINEAR_GAIN: f32 = 4.0;

/// Strongly typed mixer bus routing targets.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum BusTarget {
    #[default]
    Drums = 0,
    Bass = 1,
    Space = 2,
    Lead = 3,
    Direct = 4,
}

impl BusTarget {
    #[inline(always)]
    pub fn as_usize(self) -> usize {
        self as usize
    }

    #[inline(always)]
    pub fn as_u8(self) -> u8 {
        self as u8
    }
}

impl From<u8> for BusTarget {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => BusTarget::Bass,
            2 => BusTarget::Space,
            3 => BusTarget::Lead,
            4 => BusTarget::Direct,
            _ => BusTarget::Drums,
        }
    }
}

impl From<usize> for BusTarget {
    #[inline(always)]
    fn from(val: usize) -> Self {
        match val {
            1 => BusTarget::Bass,
            2 => BusTarget::Space,
            3 => BusTarget::Lead,
            4 => BusTarget::Direct,
            _ => BusTarget::Drums,
        }
    }
}

impl From<i32> for BusTarget {
    #[inline(always)]
    fn from(val: i32) -> Self {
        match val {
            1 => BusTarget::Bass,
            2 => BusTarget::Space,
            3 => BusTarget::Lead,
            4 => BusTarget::Direct,
            _ => BusTarget::Drums,
        }
    }
}

/// Mixer bus state controlling volume, mutes, and effects sends.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct AudioBus {
    pub gain: f32,
    pub muted: bool,
    pub send_delay: f32,
    pub send_reverb: f32,
}

impl AudioBus {
    pub fn new(gain: f32, send_delay: f32, send_reverb: f32) -> Self {
        Self {
            gain,
            muted: false,
            send_delay,
            send_reverb,
        }
    }
}

/// Frame output from mixer bus summation containing direct dry audio and wet send levels.
#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct MixerFrame {
    pub direct: f32,
    pub delay_send: f32,
    pub reverb_send: f32,
}

/// Domain service managing multi-bus routing, sidechain ducking, and effects send matrix.
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
                AudioBus::new(1.0, 0.0, 0.0),   // BUS_DIRECT: metronome / click
            ],
            sidechain: SidechainPump::new(),
        }
    }

    pub fn set_bus_params(
        &mut self,
        bus_idx: usize,
        gain_db: f32,
        muted: bool,
        send_delay: f32,
        send_reverb: f32,
    ) {
        if bus_idx < NUM_BUSSES {
            let linear_gain = db_to_gain(gain_db);
            self.busses[bus_idx].gain = linear_gain.clamp(0.0, MAX_BUS_LINEAR_GAIN);
            self.busses[bus_idx].muted = muted;
            self.busses[bus_idx].send_delay = send_delay.clamp(0.0, 1.0);
            self.busses[bus_idx].send_reverb = send_reverb.clamp(0.0, 1.0);
        }
    }

    #[inline(always)]
    pub fn trigger_sidechain_kick(&mut self) {
        self.sidechain.trigger_kick();
    }

    #[inline(always)]
    pub fn set_sidechain_amount(&mut self, amount: f32) {
        self.sidechain.amount = amount.clamp(0.0, 1.0);
    }

    /// Sums accumulated bus signals, applies sidechain pump to musical tracks,
    /// and dispatches to master direct bus and auxiliary effects sends.
    #[inline(always)]
    pub fn process_frame(&mut self, bus_accum: &[f32; NUM_BUSSES]) -> MixerFrame {
        let mut direct = 0.0;
        let mut delay_send = 0.0;
        let mut reverb_send = 0.0;

        for (b, (bus, &accum)) in self.busses.iter().zip(bus_accum.iter()).enumerate() {
            if !bus.muted {
                let mut bus_val = accum * bus.gain;
                // Sidechain ducking is applied to bass and space/lead layers
                if matches!(b, BUS_BASS..=BUS_LEAD) {
                    bus_val = self.sidechain.process(bus_val);
                }
                direct += bus_val;
                delay_send += bus_val * bus.send_delay;
                reverb_send += bus_val * bus.send_reverb;
            }
        }

        MixerFrame {
            direct,
            delay_send,
            reverb_send,
        }
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
        assert_eq!(bus.send_delay, 0.2);
        assert_eq!(bus.send_reverb, 0.3);
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

        let frame = mixer.process_frame(&accum);
        assert!(frame.direct > 0.0);
        assert!(frame.delay_send >= 0.0);
        assert!(frame.reverb_send >= 0.0);
    }
}
