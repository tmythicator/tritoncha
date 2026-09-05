pub const MAX_STEPS: usize = 128;
pub const MAX_TRACKS: usize = 16;
pub const DEFAULT_TRACK_LENGTH: usize = 16;
pub const DEFAULT_STEP_MULTIPLIER: usize = 4;
pub const DEFAULT_SYNTH_PATCH_ID: i16 = 4;
pub const REST_NOTE: i16 = -1;
pub const DEFAULT_STEP_VELOCITY: f32 = 0.9;
pub const DEFAULT_STEP_DURATION_S: f32 = 0.2;

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
}

impl Default for TrackPattern {
    fn default() -> Self {
        Self::new()
    }
}
