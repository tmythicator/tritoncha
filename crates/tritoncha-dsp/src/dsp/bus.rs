//! Multi-bus audio mixing, routing, and effects send matrix.

pub const NUM_BUSSES: usize = 5;

// Audio Bus Indices
pub const BUS_DRUMS: usize = 0;
pub const BUS_BASS: usize = 1;
pub const BUS_SPACE: usize = 2;
pub const BUS_LEAD: usize = 3;
pub const BUS_DIRECT: usize = 4;

pub const MAX_BUS_LINEAR_GAIN: f32 = 4.0;

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
}
