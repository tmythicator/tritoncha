//! Sound synthesis, percussion, audio effects, and sequencing.

pub mod drums;
pub mod effects;
pub mod params;
pub mod sequencer;
pub mod synth;

pub use drums::{NoiseFilterMode, PercBodyType, PercPatch, PercVoice, PercVoicePool};
pub use effects::{BusId, BusTarget};
pub use params::*;
pub use sequencer::TrackId;
pub use synth::{ModularPatch, PatchId};

#[cfg(test)]
mod tests {
    use super::*;

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
        let p = PatchId(4);
        assert_eq!(p.as_usize(), 4);

        let over = PatchId(100);
        assert_eq!(over.clamped().as_usize(), 63);

        let from_usize: PatchId = 8.into();
        assert_eq!(from_usize, PatchId(8));

        assert_eq!(*Bpm::new(10.0), 30.0);
        assert_eq!(*Bpm::new(500.0), 300.0);
        assert_eq!(*DrumVelocity::new(0.01), 0.1);
        assert_eq!(*DrumVelocity::new(2.0), 1.2);
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
}
