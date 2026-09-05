pub const MAX_PATCHES: usize = 64;

// Standard Modular Patch Identifiers
pub const PATCH_SAW_BASS: usize = 4;
pub const PATCH_ACID_BASS: usize = 5;
pub const PATCH_SUB_SINE: usize = 6;
pub const PATCH_DARK_PAD: usize = 7;
pub const PATCH_LEAD: usize = 8;
pub const PATCH_FM: usize = 9;
pub const PATCH_REESE: usize = 10;
pub const PATCH_CLICK: usize = 11;
pub const PATCH_SUPERSAW: usize = 12;
pub const PATCH_BLADE: usize = 13;
pub const PATCH_HOOVER: usize = 14;
pub const PATCH_KARPLUS: usize = 15;
pub const PATCH_ORGAN: usize = 16;
pub const PATCH_CHIPTUNE: usize = 17;
pub const PATCH_AMBIENT_GLASS: usize = 19;
pub const PATCH_ETHEREAL_PAD: usize = 27;
pub const PATCH_ICE_PAD: usize = 28;
pub const PATCH_WARM_STRINGS: usize = 29;
pub const PATCH_CHOIR_PAD: usize = 30;
pub const PATCH_SPACE_DRONE: usize = 31;
pub const PATCH_PLUCK_LEAD: usize = 32;
pub const PATCH_NEURO_BASS: usize = 33;
pub const PATCH_808_SUB: usize = 34;
pub const PATCH_SLAP_BASS: usize = 35;
pub const PATCH_ORGAN_BASS: usize = 36;
pub const PATCH_ACID_LEAD: usize = 37;
pub const PATCH_FM_BELL: usize = 38;
pub const PATCH_GLASS_KEYS: usize = 39;
pub const PATCH_SIREN: usize = 40;
pub const PATCH_LASER: usize = 41;

// Oscillator Types
pub const OSC_SAW: u8 = 0;
pub const OSC_PULSE: u8 = 1;
pub const OSC_TRIANGLE: u8 = 2;
pub const OSC_SINE: u8 = 3;
pub const OSC_SUPERSAW: u8 = 4;
pub const OSC_KARPLUS: u8 = 5;
pub const OSC_ORGAN: u8 = 6;
pub const OSC_CHIPTUNE: u8 = 7;
pub const OSC_FM: u8 = 8;
pub const OSC_REESE: u8 = 9;
pub const OSC_BLADE: u8 = 10;
pub const OSC_HOOVER: u8 = 11;
pub const OSC_CLICK: u8 = 12;

// Filter Types
pub const FILTER_LOWPASS: u8 = 0;
pub const FILTER_HIGHPASS: u8 = 1;
pub const FILTER_BANDPASS: u8 = 2;
pub const FILTER_NOTCH: u8 = 3;

// Bus IDs
pub const BUS_DRUMS: u8 = 0;
pub const BUS_BASS: u8 = 1;
pub const BUS_SPACE: u8 = 2;
pub const BUS_LEAD: u8 = 3;
pub const BUS_DIRECT: u8 = 4;

// Audio and Voice Timing Constants
pub const MIN_FREQ_HZ: f32 = 20.0;
pub const MAX_FREQ_HZ: f32 = 14000.0;
pub const MIN_HOLD_SEC: f32 = 0.005;
pub const MAX_HOLD_SEC: f32 = 10.0;
pub const DEFAULT_MONO_HOLD_SEC: f32 = 0.15;
pub const DEFAULT_POLY_HOLD_SEC: f32 = 0.40;
pub const MIN_ATTACK_SEC: f32 = 0.001;
pub const MIN_DECAY_SEC: f32 = 0.005;
pub const MIN_RELEASE_SEC: f32 = 0.005;

pub const MIN_PITCH_SNAP_SEMITONES: f32 = 0.0;
pub const MAX_PITCH_SNAP_SEMITONES: f32 = 48.0;
pub const MIN_PITCH_SNAP_DECAY_SEC: f32 = 0.002;
pub const MAX_PITCH_SNAP_DECAY_SEC: f32 = 0.150;
pub const DEFAULT_PITCH_SNAP_DECAY_SEC: f32 = 0.015;
pub const MAX_ANALOG_DRIFT_SEMITONES: f32 = 0.15; // ±15 cents max subtle VCO drift

#[derive(Clone, Copy)]
pub struct ModularPatch {
    pub osc_type: u8,          // OSC_* constants
    pub sub_level: f32,        // 0.0 .. 1.0
    pub pulse_width: f32,      // 0.05 .. 0.95
    pub filter_type: u8,       // FILTER_* constants
    pub cutoff_base: f32,      // 20.0 .. 20000.0 Hz
    pub cutoff_env_amt: f32,   // envelope to cutoff modulation in Hz
    pub cutoff_key_track: f32, // key tracking multiplier
    pub resonance: f32,        // 0.0 .. 0.98
    pub attack: f32,           // seconds
    pub decay: f32,            // seconds
    pub sustain: f32,          // level 0.0 .. 1.0
    pub release: f32,          // seconds
    pub mod_attack: f32,       // seconds
    pub mod_decay: f32,        // seconds
    pub bus_id: u8,            // BUS_* constants
    pub polyphony: u8,         // 1=Mono, >1=Max polyphony voices (e.g. 8)
    pub glide: f32,            // Portamento / pitch slide time in seconds (e.g. 0.04)
    pub filter_drive: f32,     // 0.0 .. 1.0 (analog saturation in SVF integrator feedback)
    pub noise_level: f32,      // 0.0 .. 1.0 (per-voice white noise injection level)
    pub pitch_env_amt: f32,    // 0.0 .. 48.0 (pitch attack transient snap in semitones)
    pub pitch_env_decay: f32,  // 0.005 .. 0.100 (seconds: pitch snap decay time)
    pub analog_drift: f32,     // 0.0 .. 1.0 (organic per-voice VCO drift/slop)
}

impl Default for ModularPatch {
    fn default() -> Self {
        Self {
            osc_type: OSC_SAW,
            sub_level: 0.0,
            pulse_width: 0.5,
            filter_type: FILTER_LOWPASS,
            cutoff_base: 2000.0,
            cutoff_env_amt: 1000.0,
            cutoff_key_track: 1.0,
            resonance: 0.2,
            attack: 0.01,
            decay: 0.2,
            sustain: 0.5,
            release: 0.2,
            mod_attack: 0.01,
            mod_decay: 0.2,
            bus_id: BUS_LEAD,
            polyphony: 8,
            glide: 0.0,
            filter_drive: 0.0,
            noise_level: 0.0,
            pitch_env_amt: 0.0,
            pitch_env_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
            analog_drift: 0.0,
        }
    }
}

impl ModularPatch {
    #[must_use]
    pub fn sanitized(mut self) -> Self {
        self.sub_level = self.sub_level.clamp(0.0, 1.0);
        self.pulse_width = self.pulse_width.clamp(0.05, 0.95);
        self.cutoff_base = self.cutoff_base.clamp(MIN_FREQ_HZ, MAX_FREQ_HZ);
        self.cutoff_env_amt = self.cutoff_env_amt.clamp(-12000.0, 12000.0);
        self.cutoff_key_track = self.cutoff_key_track.clamp(0.0, 10.0);
        self.resonance = self.resonance.clamp(0.0, 0.98);
        self.attack = self.attack.max(MIN_ATTACK_SEC);
        self.decay = self.decay.max(MIN_DECAY_SEC);
        self.sustain = self.sustain.clamp(0.0, 1.0);
        self.release = self.release.max(MIN_RELEASE_SEC);
        self.mod_attack = self.mod_attack.max(0.001);
        self.mod_decay = self.mod_decay.max(0.005);
        self.bus_id = self.bus_id.min(4);
        self.polyphony = self.polyphony.max(1);
        self.glide = self.glide.clamp(0.0, 2.0);
        self.filter_drive = self.filter_drive.clamp(0.0, 1.0);
        self.noise_level = self.noise_level.clamp(0.0, 1.0);
        self.pitch_env_amt = self
            .pitch_env_amt
            .clamp(MIN_PITCH_SNAP_SEMITONES, MAX_PITCH_SNAP_SEMITONES);
        self.pitch_env_decay = self
            .pitch_env_decay
            .clamp(MIN_PITCH_SNAP_DECAY_SEC, MAX_PITCH_SNAP_DECAY_SEC);
        self.analog_drift = self.analog_drift.clamp(0.0, 1.0);
        self
    }

    #[must_use]
    pub fn default_for(patch_id: usize) -> Self {
        match patch_id {
            // SawBass (Mono)
            PATCH_SAW_BASS => Self {
                osc_type: OSC_SAW,
                sub_level: 0.45,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 800.0,
                cutoff_env_amt: 3500.0,
                cutoff_key_track: 2.0,
                resonance: 0.45,
                attack: 0.005,
                decay: 0.18,
                sustain: 0.2,
                release: 0.15,
                mod_attack: 0.005,
                mod_decay: 0.18,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.0,
                pitch_env_amt: 7.0,
                pitch_env_decay: 0.012,
                filter_drive: 0.25,
                ..Default::default()
            },
            // AcidBass (Mono + 303 Portamento Glide)
            PATCH_ACID_BASS => Self {
                osc_type: OSC_SAW,
                sub_level: 0.3,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 400.0,
                cutoff_env_amt: 5500.0,
                cutoff_key_track: 1.5,
                resonance: 0.78,
                attack: 0.003,
                decay: 0.15,
                sustain: 0.0,
                release: 0.1,
                mod_attack: 0.003,
                mod_decay: 0.15,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.04,
                filter_drive: 0.35,
                ..Default::default()
            },
            // SubSine (Mono)
            PATCH_SUB_SINE => Self {
                osc_type: OSC_SINE,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 400.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 2.0,
                resonance: 0.0,
                attack: 0.006,
                decay: 0.24,
                sustain: 0.6,
                release: 0.2,
                mod_attack: 0.006,
                mod_decay: 0.24,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.0,
                ..Default::default()
            },
            // Dark Pad (8-Voice Polyphonic - dark warm saw pad)
            PATCH_DARK_PAD => Self {
                osc_type: OSC_SAW,
                sub_level: 0.35,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 1200.0,
                cutoff_env_amt: 600.0,
                cutoff_key_track: 0.8,
                resonance: 0.25,
                attack: 0.08,
                decay: 0.5,
                sustain: 0.70,
                release: 0.60,
                mod_attack: 0.08,
                mod_decay: 0.5,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                noise_level: 0.03,
                analog_drift: 0.30,
                ..Default::default()
            },
            // Lead (16-Voice Polyphonic)
            8 => Self {
                osc_type: 1,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 1200.0,
                cutoff_env_amt: 4500.0,
                cutoff_key_track: 3.0,
                resonance: 0.5,
                attack: 0.003,
                decay: 0.18,
                sustain: 0.1,
                release: 0.15,
                mod_attack: 0.003,
                mod_decay: 0.18,
                bus_id: 3,
                polyphony: 16,
                glide: 0.0,
                pitch_env_amt: 12.0,
                pitch_env_decay: 0.010,
                filter_drive: 0.18,
                ..Default::default()
            },
            // FmSynth (8-Voice Polyphonic)
            9 => Self {
                osc_type: 8,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 1500.0,
                cutoff_env_amt: 3000.0,
                cutoff_key_track: 2.5,
                resonance: 0.3,
                attack: 0.003,
                decay: 0.16,
                sustain: 0.2,
                release: 0.15,
                mod_attack: 0.003,
                mod_decay: 0.16,
                bus_id: 3,
                polyphony: 8,
                glide: 0.0,
                pitch_env_amt: 12.0,
                pitch_env_decay: 0.015,
                filter_drive: 0.32,
                ..Default::default()
            },
            // Reese (Mono Bass)
            10 => Self {
                osc_type: 9,
                sub_level: 0.4,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 900.0,
                cutoff_env_amt: 2800.0,
                cutoff_key_track: 2.0,
                resonance: 0.5,
                attack: 0.008,
                decay: 0.35,
                sustain: 0.5,
                release: 0.3,
                mod_attack: 0.008,
                mod_decay: 0.35,
                bus_id: 1,
                polyphony: 1,
                glide: 0.0,
                filter_drive: 0.22,
                analog_drift: 0.25,
                ..Default::default()
            },
            // Click (Direct 2-voice)
            11 => Self {
                osc_type: 12,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 2800.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 0.0,
                resonance: 0.4,
                attack: 0.001,
                decay: 0.015,
                sustain: 0.0,
                release: 0.01,
                mod_attack: 0.001,
                mod_decay: 0.015,
                bus_id: 4,
                polyphony: 2,
                glide: 0.0,
                ..Default::default()
            },
            // SuperSaw (8-Voice Polyphonic)
            12 => Self {
                osc_type: 4,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 2000.0,
                cutoff_env_amt: 5000.0,
                cutoff_key_track: 3.0,
                resonance: 0.4,
                attack: 0.004,
                decay: 0.22,
                sustain: 0.4,
                release: 0.3,
                mod_attack: 0.004,
                mod_decay: 0.22,
                bus_id: 3,
                polyphony: 8,
                glide: 0.0,
                filter_drive: 0.25,
                analog_drift: 0.22,
                ..Default::default()
            },
            // Blade (8-Voice Polyphonic)
            13 => Self {
                osc_type: 10,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 1800.0,
                cutoff_env_amt: 1800.0,
                cutoff_key_track: 1.0,
                resonance: 0.25,
                attack: 0.08,
                decay: 0.6,
                sustain: 0.7,
                release: 1.4,
                mod_attack: 0.08,
                mod_decay: 0.6,
                bus_id: 2,
                polyphony: 8,
                glide: 0.0,
                filter_drive: 0.20,
                noise_level: 0.02,
                analog_drift: 0.32,
                ..Default::default()
            },
            // Hoover (4-Voice Polyphonic)
            14 => Self {
                osc_type: 11,
                sub_level: 0.3,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 1200.0,
                cutoff_env_amt: 4000.0,
                cutoff_key_track: 2.0,
                resonance: 0.65,
                attack: 0.004,
                decay: 0.25,
                sustain: 0.5,
                release: 0.3,
                mod_attack: 0.004,
                mod_decay: 0.25,
                bus_id: 3,
                polyphony: 4,
                glide: 0.0,
                filter_drive: 0.35,
                analog_drift: 0.20,
                ..Default::default()
            },
            // Karplus (8-Voice Polyphonic)
            15 => Self {
                osc_type: 5,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 4000.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 8.0,
                resonance: 0.1,
                attack: 0.001,
                decay: 0.8,
                sustain: 0.0,
                release: 0.4,
                mod_attack: 0.001,
                mod_decay: 0.8,
                bus_id: 3,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
            // Organ (8-Voice Polyphonic)
            16 => Self {
                osc_type: 6,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 14000.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 0.0,
                resonance: 0.0,
                attack: 0.002,
                decay: 0.20,
                sustain: 0.8,
                release: 0.2,
                mod_attack: 0.002,
                mod_decay: 0.20,
                bus_id: 3,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
            // Chiptune (4-Voice Polyphonic)
            17 => Self {
                osc_type: 7,
                sub_level: 0.0,
                pulse_width: 0.25,
                filter_type: 0,
                cutoff_base: 4000.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 4.0,
                resonance: 0.2,
                attack: 0.002,
                decay: 0.18,
                sustain: 0.3,
                release: 0.15,
                mod_attack: 0.002,
                mod_decay: 0.18,
                bus_id: 3,
                polyphony: 4,
                glide: 0.0,
                ..Default::default()
            },
            // AmbientGlass (Crystalline sine chime with fast clean decay)
            19 => Self {
                osc_type: 3,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 3200.0,
                cutoff_env_amt: 1200.0,
                cutoff_key_track: 1.0,
                resonance: 0.15,
                attack: 0.005,
                decay: 0.18,
                sustain: 0.0,
                release: 0.15,
                mod_attack: 0.005,
                mod_decay: 0.18,
                bus_id: 2,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
            // Ethereal Pad (Bright SuperSaw pad)
            PATCH_ETHEREAL_PAD => Self {
                osc_type: OSC_SUPERSAW,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 2400.0,
                cutoff_env_amt: 1200.0,
                cutoff_key_track: 1.0,
                resonance: 0.20,
                attack: 0.05,
                decay: 0.4,
                sustain: 0.70,
                release: 0.50,
                mod_attack: 0.05,
                mod_decay: 0.4,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                analog_drift: 0.20,
                ..Default::default()
            },
            // Ice Pad (Shimmering bandpass triangle pad)
            PATCH_ICE_PAD => Self {
                osc_type: OSC_TRIANGLE,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_BANDPASS,
                cutoff_base: 1800.0,
                cutoff_env_amt: 800.0,
                cutoff_key_track: 1.2,
                resonance: 0.45,
                attack: 0.04,
                decay: 0.35,
                sustain: 0.70,
                release: 0.45,
                mod_attack: 0.04,
                mod_decay: 0.35,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
            // Warm Strings (Lush smooth supersaw strings)
            PATCH_WARM_STRINGS => Self {
                osc_type: OSC_SUPERSAW,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 2100.0,
                cutoff_env_amt: 900.0,
                cutoff_key_track: 0.9,
                resonance: 0.14,
                attack: 0.07,
                decay: 0.45,
                sustain: 0.75,
                release: 0.60,
                mod_attack: 0.07,
                mod_decay: 0.45,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                analog_drift: 0.25,
                ..Default::default()
            },
            // Choir Pad (Vocal formant bandpass pulse)
            PATCH_CHOIR_PAD => Self {
                osc_type: OSC_PULSE,
                sub_level: 0.0,
                pulse_width: 0.35,
                filter_type: FILTER_BANDPASS,
                cutoff_base: 1950.0,
                cutoff_env_amt: 950.0,
                cutoff_key_track: 1.0,
                resonance: 0.48,
                attack: 0.05,
                decay: 0.4,
                sustain: 0.70,
                release: 0.45,
                mod_attack: 0.05,
                mod_decay: 0.4,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                noise_level: 0.04,
                analog_drift: 0.35,
                ..Default::default()
            },
            // Space Drone (Deep rumbling sub-saw drone)
            PATCH_SPACE_DRONE => Self {
                osc_type: OSC_SAW,
                sub_level: 0.45,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 800.0,
                cutoff_env_amt: 500.0,
                cutoff_key_track: 0.8,
                resonance: 0.35,
                attack: 0.10,
                decay: 0.6,
                sustain: 0.80,
                release: 0.80,
                mod_attack: 0.10,
                mod_decay: 0.6,
                bus_id: BUS_SPACE,
                polyphony: 4,
                glide: 0.0,
                filter_drive: 0.25,
                noise_level: 0.03,
                analog_drift: 0.32,
                ..Default::default()
            },
            // Pluck Lead (Fast percussive lead)
            PATCH_PLUCK_LEAD => Self {
                osc_type: OSC_PULSE,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 1200.0,
                cutoff_env_amt: 4500.0,
                cutoff_key_track: 3.0,
                resonance: 0.5,
                attack: 0.003,
                decay: 0.18,
                sustain: 0.1,
                release: 0.15,
                mod_attack: 0.003,
                mod_decay: 0.18,
                bus_id: BUS_LEAD,
                polyphony: 16,
                glide: 0.0,
                pitch_env_amt: 12.0,
                pitch_env_decay: 0.015,
                ..Default::default()
            },
            // Neuro Bass (Resonant notch Reese)
            PATCH_NEURO_BASS => Self {
                osc_type: OSC_REESE,
                sub_level: 0.45,
                pulse_width: 0.5,
                filter_type: FILTER_NOTCH,
                cutoff_base: 1000.0,
                cutoff_env_amt: 2500.0,
                cutoff_key_track: 2.0,
                resonance: 0.58,
                attack: 0.006,
                decay: 0.28,
                sustain: 0.45,
                release: 0.22,
                mod_attack: 0.006,
                mod_decay: 0.28,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.0,
                filter_drive: 0.40,
                analog_drift: 0.15,
                ..Default::default()
            },
            // 808 Sub (Pure deep sub)
            PATCH_808_SUB => Self {
                osc_type: OSC_SINE,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 350.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 1.5,
                resonance: 0.0,
                attack: 0.004,
                decay: 0.45,
                sustain: 0.4,
                release: 0.35,
                mod_attack: 0.004,
                mod_decay: 0.45,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.0,
                filter_drive: 0.15,
                pitch_env_amt: 24.0,
                pitch_env_decay: 0.018,
                ..Default::default()
            },
            // Slap Bass (Punchy funky triangle)
            PATCH_SLAP_BASS => Self {
                osc_type: OSC_TRIANGLE,
                sub_level: 0.4,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 1600.0,
                cutoff_env_amt: 3200.0,
                cutoff_key_track: 2.0,
                resonance: 0.45,
                attack: 0.002,
                decay: 0.15,
                sustain: 0.15,
                release: 0.10,
                mod_attack: 0.002,
                mod_decay: 0.15,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.0,
                filter_drive: 0.22,
                pitch_env_amt: 12.0,
                pitch_env_decay: 0.010,
                ..Default::default()
            },
            // Organ Bass
            PATCH_ORGAN_BASS => Self {
                osc_type: OSC_ORGAN,
                sub_level: 0.3,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 950.0,
                cutoff_env_amt: 0.0,
                cutoff_key_track: 1.0,
                resonance: 0.1,
                attack: 0.003,
                decay: 0.20,
                sustain: 0.6,
                release: 0.15,
                mod_attack: 0.003,
                mod_decay: 0.20,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.0,
                ..Default::default()
            },
            // Acid Lead
            PATCH_ACID_LEAD => Self {
                osc_type: OSC_SAW,
                sub_level: 0.25,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 600.0,
                cutoff_env_amt: 5000.0,
                cutoff_key_track: 1.5,
                resonance: 0.82,
                attack: 0.005,
                decay: 0.16,
                sustain: 0.1,
                release: 0.08,
                mod_attack: 0.005,
                mod_decay: 0.14,
                bus_id: BUS_BASS,
                polyphony: 1,
                glide: 0.03,
                filter_drive: 0.35,
                ..Default::default()
            },
            // FM Bell
            PATCH_FM_BELL => Self {
                osc_type: OSC_FM,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 4000.0,
                cutoff_env_amt: 3000.0,
                cutoff_key_track: 2.0,
                resonance: 0.25,
                attack: 0.002,
                decay: 0.7,
                sustain: 0.05,
                release: 0.6,
                mod_attack: 0.002,
                mod_decay: 0.7,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
            // Glass Keys
            PATCH_GLASS_KEYS => Self {
                osc_type: OSC_SINE,
                sub_level: 0.1,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 2600.0,
                cutoff_env_amt: 1200.0,
                cutoff_key_track: 1.5,
                resonance: 0.15,
                attack: 0.008,
                decay: 0.35,
                sustain: 0.2,
                release: 0.4,
                mod_attack: 0.008,
                mod_decay: 0.35,
                bus_id: BUS_SPACE,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
            // Siren (Dub laser siren)
            PATCH_SIREN => Self {
                osc_type: OSC_SAW,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 3200.0,
                cutoff_env_amt: 4000.0,
                cutoff_key_track: 1.0,
                resonance: 0.55,
                attack: 0.02,
                decay: 0.35,
                sustain: 0.4,
                release: 0.6,
                mod_attack: 0.02,
                mod_decay: 0.35,
                bus_id: BUS_SPACE,
                polyphony: 4,
                glide: 0.0,
                ..Default::default()
            },
            // Laser (Sci-Fi resonant zap)
            PATCH_LASER => Self {
                osc_type: OSC_PULSE,
                sub_level: 0.0,
                pulse_width: 0.5,
                filter_type: FILTER_LOWPASS,
                cutoff_base: 8000.0,
                cutoff_env_amt: -6000.0,
                cutoff_key_track: 1.0,
                resonance: 0.70,
                attack: 0.002,
                decay: 0.12,
                sustain: 0.0,
                release: 0.08,
                mod_attack: 0.002,
                mod_decay: 0.12,
                bus_id: BUS_LEAD,
                polyphony: 8,
                glide: 0.0,
                pitch_env_amt: 36.0,
                pitch_env_decay: 0.025,
                filter_drive: 0.40,
                ..Default::default()
            },
            // Default user fallback
            _ => Self {
                osc_type: 0,
                sub_level: 0.3,
                pulse_width: 0.5,
                filter_type: 0,
                cutoff_base: 1200.0,
                cutoff_env_amt: 3000.0,
                cutoff_key_track: 2.0,
                resonance: 0.4,
                attack: 0.005,
                decay: 0.2,
                sustain: 0.3,
                release: 0.2,
                mod_attack: 0.005,
                mod_decay: 0.2,
                bus_id: 3,
                polyphony: 8,
                glide: 0.0,
                ..Default::default()
            },
        }
    }
}
