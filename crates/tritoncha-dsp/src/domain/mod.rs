//! Sound synthesis, drums, audio effects, and sequencing.

pub mod drums;
pub mod effects;
pub mod params;
pub mod sequencer;
pub mod synth;

pub use effects::{BusId, BusTarget};
pub use params::*;
pub use sequencer::TrackId;
pub use synth::{ModularPatch, PatchId};

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
pub enum InstrumentId {
    Drum(drums::DrumId),
    Synth(PatchId),
}

impl InstrumentId {
    #[inline(always)]
    pub fn is_drum(self) -> bool {
        matches!(self, InstrumentId::Drum(_))
    }

    #[inline(always)]
    pub fn is_synth(self) -> bool {
        matches!(self, InstrumentId::Synth(_))
    }
}

impl From<i32> for InstrumentId {
    #[inline(always)]
    fn from(id: i32) -> Self {
        if let Ok(drum) = drums::DrumId::try_from(id) {
            InstrumentId::Drum(drum)
        } else {
            InstrumentId::Synth(PatchId(id.max(0) as usize))
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::domain::drums::{DrumId, DRUM_KICK, DRUM_SNARE};
    use crate::domain::synth::PATCH_SAW_BASS;

    #[test]
    fn test_bus_id_type_safety_and_routing_bounds() {
        assert_eq!(BusId::DRUMS.as_usize(), 0);
        assert_eq!(BusId::BASS.as_usize(), 1);
        assert_eq!(BusId::SPACE.as_usize(), 2);
        assert_eq!(BusId::LEAD.as_usize(), 3);
        assert_eq!(BusId::DIRECT.as_usize(), 4);
        assert_eq!(BusId::MASTER.as_usize(), 5);

        assert!(BusId::DRUMS.is_routable());
        assert!(BusId::MASTER.is_routable());
        assert!(!BusId(6).is_routable());
        assert!(!BusId(99).is_routable());

        let bus: BusId = 2.into();
        assert_eq!(bus, BusId::SPACE);
    }

    #[test]
    fn test_patch_id_bounds_and_clamping() {
        let p = PatchId::SAW_BASS;
        assert_eq!(p.as_usize(), 4);

        let over = PatchId(100);
        assert_eq!(over.clamped().as_usize(), 63);

        let from_usize: PatchId = 8.into();
        assert_eq!(from_usize, PatchId::LEAD);
    }

    #[test]
    fn test_track_id_validity_bounds() {
        let t0 = TrackId(0);
        assert!(t0.is_valid());

        let t15 = TrackId(15);
        assert!(t15.is_valid());

        let t16 = TrackId(16);
        assert!(!t16.is_valid());

        let from_u: TrackId = 3.into();
        assert_eq!(from_u.as_usize(), 3);
    }

    #[test]
    fn test_instrument_id_resolves_drums_vs_synths() {
        // Drums
        assert_eq!(
            InstrumentId::from(DRUM_KICK),
            InstrumentId::Drum(DrumId::Kick)
        );
        assert_eq!(
            InstrumentId::from(DRUM_SNARE),
            InstrumentId::Drum(DrumId::Snare)
        );

        // Synth Patches
        assert_eq!(
            InstrumentId::from(PATCH_SAW_BASS as i32),
            InstrumentId::Synth(PatchId::SAW_BASS)
        );
        assert_eq!(InstrumentId::from(8), InstrumentId::Synth(PatchId::LEAD));

        // Introspection helpers
        assert!(InstrumentId::from(DRUM_KICK).is_drum());
        assert!(!InstrumentId::from(DRUM_KICK).is_synth());

        assert!(InstrumentId::from(4).is_synth());
        assert!(!InstrumentId::from(4).is_drum());
    }
}
