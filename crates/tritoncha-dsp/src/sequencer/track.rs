//! Sequencer track and step pattern representation.

pub const MAX_STEPS: usize = 128;
pub const MAX_TRACKS: usize = 16;
pub const DEFAULT_TRACK_LENGTH: usize = 16;
pub const DEFAULT_STEP_MULTIPLIER: usize = 4;
pub const DEFAULT_SYNTH_PATCH_ID: i16 = 4;
pub const REST_NOTE: i16 = -1;
pub const DEFAULT_STEP_VELOCITY: f32 = 0.9;
pub const DEFAULT_STEP_DURATION_S: f32 = 0.2;

/// Sequencer Track Pattern containing melodic and rhythmic event sequences.
#[derive(Clone)]
pub struct TrackPattern {
    pub active: bool,
    pub muted: bool,
    pub solo: bool,
    pub inst_kinds: [i16; MAX_STEPS],
    pub notes: [i16; MAX_STEPS],
    pub velocities: [f32; MAX_STEPS],
    pub durations: [f32; MAX_STEPS],
    pub length: usize,
    pub step_multiplier: usize,
}

impl TrackPattern {
    pub fn new() -> Self {
        Self {
            active: false,
            muted: false,
            solo: false,
            inst_kinds: [DEFAULT_SYNTH_PATCH_ID; MAX_STEPS],
            notes: [REST_NOTE; MAX_STEPS],
            velocities: [DEFAULT_STEP_VELOCITY; MAX_STEPS],
            durations: [DEFAULT_STEP_DURATION_S; MAX_STEPS],
            length: DEFAULT_TRACK_LENGTH,
            step_multiplier: DEFAULT_STEP_MULTIPLIER,
        }
    }

    /// Determines if this track should produce audio given global solo state.
    #[inline(always)]
    pub fn is_audible(&self, solo_active: bool) -> bool {
        self.active && !self.muted && (!solo_active || self.solo)
    }

    /// Sets a specific step's instrument, pitch, velocity, and duration.
    pub fn set_step(&mut self, step: usize, inst_kind: i16, note: i16, vel: f32, dur: f32) {
        if step < MAX_STEPS {
            self.inst_kinds[step] = inst_kind;
            self.notes[step] = note;
            self.velocities[step] = vel.clamp(0.0, 1.0);
            self.durations[step] = dur.max(0.005);
        }
    }

    /// Clears all note data and marks track as inactive.
    pub fn clear(&mut self) {
        self.active = false;
        self.length = 0;
        self.notes.fill(REST_NOTE);
        self.inst_kinds.fill(DEFAULT_SYNTH_PATCH_ID);
    }
}

impl Default for TrackPattern {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_track_initial_state() {
        let tr = TrackPattern::new();
        assert!(!tr.active);
        assert!(!tr.muted);
        assert!(!tr.solo);
        assert_eq!(tr.length, DEFAULT_TRACK_LENGTH);
        assert_eq!(tr.step_multiplier, DEFAULT_STEP_MULTIPLIER);
        assert_eq!(tr.notes[0], REST_NOTE);
    }

    #[test]
    fn test_track_audibility() {
        let mut tr = TrackPattern::new();
        assert!(!tr.is_audible(false));

        tr.active = true;
        assert!(tr.is_audible(false));

        tr.muted = true;
        assert!(!tr.is_audible(false));

        tr.muted = false;
        assert!(!tr.is_audible(true)); // Another track is soloed

        tr.solo = true;
        assert!(tr.is_audible(true)); // This track is soloed
    }

    #[test]
    fn test_set_step_and_clear() {
        let mut tr = TrackPattern::new();
        tr.active = true;
        tr.set_step(0, 5, 60, 0.8, 0.15);

        assert_eq!(tr.inst_kinds[0], 5);
        assert_eq!(tr.notes[0], 60);
        assert_eq!(tr.velocities[0], 0.8);
        assert_eq!(tr.durations[0], 0.15);

        tr.clear();
        assert!(!tr.active);
        assert_eq!(tr.notes[0], REST_NOTE);
    }
}
