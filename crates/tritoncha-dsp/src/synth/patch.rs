//! Declarative modular synthesizer voice patch specifications and sound design presets.

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

/// Strongly typed oscillator waveform selection.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum OscillatorType {
    #[default]
    Saw = 0,
    Pulse = 1,
    Triangle = 2,
    Sine = 3,
    Supersaw = 4,
    Karplus = 5,
    Organ = 6,
    Chiptune = 7,
    Fm = 8,
    Reese = 9,
    Blade = 10,
    Hoover = 11,
    Click = 12,
}

impl From<u8> for OscillatorType {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => OscillatorType::Pulse,
            2 => OscillatorType::Triangle,
            3 => OscillatorType::Sine,
            4 => OscillatorType::Supersaw,
            5 => OscillatorType::Karplus,
            6 => OscillatorType::Organ,
            7 => OscillatorType::Chiptune,
            8 => OscillatorType::Fm,
            9 => OscillatorType::Reese,
            10 => OscillatorType::Blade,
            11 => OscillatorType::Hoover,
            12 => OscillatorType::Click,
            _ => OscillatorType::Saw,
        }
    }
}

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
pub const MAX_ANALOG_DRIFT_SEMITONES: f32 = 0.15;

/// Modular sound design patch specification.
#[derive(Clone, Copy)]
pub struct ModularPatch {
    pub osc_type: u8,          // OSC_* constants / OscillatorType
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
    pub noise_level: f32,      // 0.0 .. 1.0 (analog noise injection)
    pub pitch_snap: f32,       // 0.0 .. 48.0 semitones (laser/punch initial transient)
    pub pitch_snap_decay: f32, // seconds (pitch snap exponential decay rate)
    pub analog_drift: f32,     // 0.0 .. 1.0 (subtle LFO pitch drift modeling analog VCOs)
}

impl ModularPatch {
    pub fn default_lead() -> Self {
        Self {
            osc_type: OSC_SAW,
            sub_level: 0.25,
            pulse_width: 0.5,
            filter_type: FILTER_LOWPASS,
            cutoff_base: 2800.0,
            cutoff_env_amt: 3500.0,
            cutoff_key_track: 1.0,
            resonance: 0.45,
            attack: 0.005,
            decay: 0.12,
            sustain: 0.6,
            release: 0.18,
            mod_attack: 0.005,
            mod_decay: 0.12,
            bus_id: BUS_LEAD,
            polyphony: 4,
            glide: 0.0,
            filter_drive: 0.2,
            noise_level: 0.0,
            pitch_snap: 0.0,
            pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
            analog_drift: 0.05,
        }
    }

    /// Clamps and sanitizes all patch parameters to ensure numerical stability.
    pub fn sanitized(&self) -> Self {
        Self {
            osc_type: self.osc_type.min(12),
            sub_level: self.sub_level.clamp(0.0, 1.0),
            pulse_width: self.pulse_width.clamp(0.05, 0.95),
            filter_type: self.filter_type.min(3),
            cutoff_base: self.cutoff_base.clamp(20.0, 20000.0),
            cutoff_env_amt: self.cutoff_env_amt.clamp(-20000.0, 20000.0),
            cutoff_key_track: self.cutoff_key_track.clamp(0.0, 4.0),
            resonance: self.resonance.clamp(0.0, 0.98),
            attack: self.attack.clamp(MIN_ATTACK_SEC, 10.0),
            decay: self.decay.clamp(MIN_DECAY_SEC, 10.0),
            sustain: self.sustain.clamp(0.0, 1.0),
            release: self.release.clamp(MIN_RELEASE_SEC, 10.0),
            mod_attack: self.mod_attack.clamp(0.001, 10.0),
            mod_decay: self.mod_decay.clamp(0.005, 10.0),
            bus_id: self.bus_id.min(4),
            polyphony: self.polyphony.clamp(1, 16),
            glide: self.glide.clamp(0.0, 2.0),
            filter_drive: self.filter_drive.clamp(0.0, 1.0),
            noise_level: self.noise_level.clamp(0.0, 1.0),
            pitch_snap: self
                .pitch_snap
                .clamp(MIN_PITCH_SNAP_SEMITONES, MAX_PITCH_SNAP_SEMITONES),
            pitch_snap_decay: self
                .pitch_snap_decay
                .clamp(MIN_PITCH_SNAP_DECAY_SEC, MAX_PITCH_SNAP_DECAY_SEC),
            analog_drift: self.analog_drift.clamp(0.0, 1.0),
        }
    }

    /// Retrieves the curated factory default patch for a given patch index.
    pub fn default_for(patch_id: usize) -> Self {
        let all = init_default_patches();
        if patch_id < MAX_PATCHES {
            all[patch_id]
        } else {
            ModularPatch::default()
        }
    }
}

impl Default for ModularPatch {
    fn default() -> Self {
        Self::default_lead()
    }
}

pub fn init_default_patches() -> [ModularPatch; MAX_PATCHES] {
    let mut patches = [ModularPatch::default(); MAX_PATCHES];

    // Patch 4: Saw Bass (Classic Phrygian Roller Bass)
    patches[PATCH_SAW_BASS] = ModularPatch {
        osc_type: OSC_SAW,
        sub_level: 0.55,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 320.0,
        cutoff_env_amt: 1600.0,
        cutoff_key_track: 0.6,
        resonance: 0.35,
        attack: 0.005,
        decay: 0.14,
        sustain: 0.45,
        release: 0.12,
        mod_attack: 0.005,
        mod_decay: 0.10,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.035,
        filter_drive: 0.4,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.05,
    };

    // Patch 5: Acid Bass (Resonant 303 Screamer)
    patches[PATCH_ACID_BASS] = ModularPatch {
        osc_type: OSC_SAW,
        sub_level: 0.2,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 450.0,
        cutoff_env_amt: 4800.0,
        cutoff_key_track: 1.2,
        resonance: 0.88,
        attack: 0.005,
        decay: 0.18,
        sustain: 0.2,
        release: 0.1,
        mod_attack: 0.005,
        mod_decay: 0.14,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.05,
        filter_drive: 0.65,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.1,
    };

    // Patch 6: Sub Sine (Deep seismic 808 sub)
    patches[PATCH_SUB_SINE] = ModularPatch {
        osc_type: OSC_SINE,
        sub_level: 0.8,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 180.0,
        cutoff_env_amt: 150.0,
        cutoff_key_track: 0.2,
        resonance: 0.1,
        attack: 0.01,
        decay: 0.25,
        sustain: 0.8,
        release: 0.2,
        mod_attack: 0.01,
        mod_decay: 0.2,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.06,
        filter_drive: 0.15,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.0,
    };

    // Patch 7: Dark Pad (Atmospheric minor chord pad)
    patches[PATCH_DARK_PAD] = ModularPatch {
        osc_type: OSC_SUPERSAW,
        sub_level: 0.3,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 850.0,
        cutoff_env_amt: 1400.0,
        cutoff_key_track: 0.8,
        resonance: 0.3,
        attack: 0.25,
        decay: 0.6,
        sustain: 0.7,
        release: 1.2,
        mod_attack: 0.3,
        mod_decay: 0.8,
        bus_id: BUS_SPACE,
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.1,
        noise_level: 0.02,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.12,
    };

    // Patch 8: Lead (Cutting sync lead)
    patches[PATCH_LEAD] = ModularPatch::default_lead();

    // Patch 9: FM Chime / Metallic Perc
    patches[PATCH_FM] = ModularPatch {
        osc_type: OSC_FM,
        sub_level: 0.0,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 3800.0,
        cutoff_env_amt: 4000.0,
        cutoff_key_track: 1.0,
        resonance: 0.3,
        attack: 0.005,
        decay: 0.3,
        sustain: 0.1,
        release: 0.3,
        mod_attack: 0.005,
        mod_decay: 0.25,
        bus_id: BUS_SPACE,
        polyphony: 6,
        glide: 0.0,
        filter_drive: 0.15,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.02,
    };

    // Patch 10: Reese Bass (Dark detuned jungle reese)
    patches[PATCH_REESE] = ModularPatch {
        osc_type: OSC_REESE,
        sub_level: 0.45,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 420.0,
        cutoff_env_amt: 1200.0,
        cutoff_key_track: 0.5,
        resonance: 0.4,
        attack: 0.01,
        decay: 0.3,
        sustain: 0.8,
        release: 0.2,
        mod_attack: 0.01,
        mod_decay: 0.2,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.04,
        filter_drive: 0.55,
        noise_level: 0.01,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.15,
    };

    // Patch 11: Click (Metronome click downbeat/beat)
    patches[PATCH_CLICK] = ModularPatch {
        osc_type: OSC_CLICK,
        sub_level: 0.0,
        pulse_width: 0.5,
        filter_type: FILTER_HIGHPASS,
        cutoff_base: 1500.0,
        cutoff_env_amt: 2000.0,
        cutoff_key_track: 0.0,
        resonance: 0.1,
        attack: 0.001,
        decay: 0.015,
        sustain: 0.0,
        release: 0.01,
        mod_attack: 0.001,
        mod_decay: 0.01,
        bus_id: BUS_DIRECT,
        polyphony: 2,
        glide: 0.0,
        filter_drive: 0.0,
        noise_level: 0.0,
        pitch_snap: 12.0,
        pitch_snap_decay: 0.005,
        analog_drift: 0.0,
    };

    // Patch 12: Supersaw (Trance/Hardcore detuned stack)
    patches[PATCH_SUPERSAW] = ModularPatch {
        osc_type: OSC_SUPERSAW,
        sub_level: 0.35,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 2200.0,
        cutoff_env_amt: 4500.0,
        cutoff_key_track: 1.0,
        resonance: 0.35,
        attack: 0.01,
        decay: 0.2,
        sustain: 0.6,
        release: 0.25,
        mod_attack: 0.01,
        mod_decay: 0.2,
        bus_id: BUS_LEAD,
        polyphony: 4,
        glide: 0.02,
        filter_drive: 0.3,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.08,
    };

    // Patch 13: Blade Runner Brass Lead
    patches[PATCH_BLADE] = ModularPatch {
        osc_type: OSC_BLADE,
        sub_level: 0.2,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 950.0,
        cutoff_env_amt: 3200.0,
        cutoff_key_track: 1.1,
        resonance: 0.5,
        attack: 0.08,
        decay: 0.4,
        sustain: 0.7,
        release: 0.8,
        mod_attack: 0.1,
        mod_decay: 0.5,
        bus_id: BUS_SPACE,
        polyphony: 4,
        glide: 0.08,
        filter_drive: 0.25,
        noise_level: 0.02,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.15,
    };

    // Patch 14: Hoover Synth (Alpha Juno Mentasm)
    patches[PATCH_HOOVER] = ModularPatch {
        osc_type: OSC_HOOVER,
        sub_level: 0.4,
        pulse_width: 0.4,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 1800.0,
        cutoff_env_amt: 3600.0,
        cutoff_key_track: 0.8,
        resonance: 0.55,
        attack: 0.01,
        decay: 0.25,
        sustain: 0.6,
        release: 0.2,
        mod_attack: 0.01,
        mod_decay: 0.2,
        bus_id: BUS_LEAD,
        polyphony: 2,
        glide: 0.06,
        filter_drive: 0.45,
        noise_level: 0.0,
        pitch_snap: 24.0,
        pitch_snap_decay: 0.045,
        analog_drift: 0.12,
    };

    // Patch 15: Karplus-Strong Acoustic Pluck
    patches[PATCH_KARPLUS] = ModularPatch {
        osc_type: OSC_KARPLUS,
        sub_level: 0.0,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 6000.0,
        cutoff_env_amt: 0.0,
        cutoff_key_track: 1.0,
        resonance: 0.1,
        attack: 0.002,
        decay: 0.4,
        sustain: 0.0,
        release: 0.3,
        mod_attack: 0.002,
        mod_decay: 0.3,
        bus_id: BUS_SPACE,
        polyphony: 6,
        glide: 0.0,
        filter_drive: 0.0,
        noise_level: 0.8,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.0,
    };

    // Patch 16: Tonewheel Organ (Gospel / House Organ)
    patches[PATCH_ORGAN] = ModularPatch {
        osc_type: OSC_ORGAN,
        sub_level: 0.5,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 4500.0,
        cutoff_env_amt: 1000.0,
        cutoff_key_track: 1.0,
        resonance: 0.2,
        attack: 0.003,
        decay: 0.05,
        sustain: 0.9,
        release: 0.05,
        mod_attack: 0.003,
        mod_decay: 0.05,
        bus_id: BUS_LEAD,
        polyphony: 6,
        glide: 0.0,
        filter_drive: 0.35,
        noise_level: 0.0,
        pitch_snap: 12.0,
        pitch_snap_decay: 0.008,
        analog_drift: 0.02,
    };

    // Patch 17: Chiptune NES Square (8-bit Arpeggiator Lead)
    patches[PATCH_CHIPTUNE] = ModularPatch {
        osc_type: OSC_CHIPTUNE,
        sub_level: 0.0,
        pulse_width: 0.25,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 8000.0,
        cutoff_env_amt: 0.0,
        cutoff_key_track: 1.0,
        resonance: 0.0,
        attack: 0.001,
        decay: 0.08,
        sustain: 0.7,
        release: 0.04,
        mod_attack: 0.001,
        mod_decay: 0.08,
        bus_id: BUS_DIRECT,
        polyphony: 2,
        glide: 0.0,
        filter_drive: 0.1,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.0,
    };

    // Patch 19: Ambient Glass Drift (Shimmering crystal pad)
    patches[PATCH_AMBIENT_GLASS] = ModularPatch {
        osc_type: OSC_SINE,
        sub_level: 0.2,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 1400.0,
        cutoff_env_amt: 2200.0,
        cutoff_key_track: 1.0,
        resonance: 0.25,
        attack: 0.4,
        decay: 0.8,
        sustain: 0.7,
        release: 1.6,
        mod_attack: 0.5,
        mod_decay: 1.0,
        bus_id: BUS_SPACE,
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.05,
        noise_level: 0.01,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.18,
    };

    // Patch 27: Ethereal Pad
    patches[PATCH_ETHEREAL_PAD] = ModularPatch {
        osc_type: OSC_SUPERSAW,
        sub_level: 0.3,
        pulse_width: 0.5,
        filter_type: FILTER_BANDPASS,
        cutoff_base: 1100.0,
        cutoff_env_amt: 1800.0,
        cutoff_key_track: 0.8,
        resonance: 0.45,
        attack: 0.35,
        decay: 0.7,
        sustain: 0.65,
        release: 1.4,
        mod_attack: 0.4,
        mod_decay: 0.9,
        bus_id: BUS_SPACE,
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.1,
        noise_level: 0.02,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.14,
    };

    // Patch 28: Ice Pad (Cold, sharp digital pad)
    patches[PATCH_ICE_PAD] = ModularPatch {
        osc_type: OSC_PULSE,
        sub_level: 0.1,
        pulse_width: 0.2,
        filter_type: FILTER_HIGHPASS,
        cutoff_base: 650.0,
        cutoff_env_amt: 1200.0,
        cutoff_key_track: 0.9,
        resonance: 0.3,
        attack: 0.2,
        decay: 0.5,
        sustain: 0.7,
        release: 1.0,
        mod_attack: 0.25,
        mod_decay: 0.6,
        bus_id: BUS_SPACE,
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.05,
        noise_level: 0.03,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.08,
    };

    // Patch 29: Warm Strings (Analog polyphonic string ensemble)
    patches[PATCH_WARM_STRINGS] = ModularPatch {
        osc_type: OSC_SAW,
        sub_level: 0.25,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 1250.0,
        cutoff_env_amt: 2100.0,
        cutoff_key_track: 0.9,
        resonance: 0.25,
        attack: 0.15,
        decay: 0.4,
        sustain: 0.75,
        release: 0.8,
        mod_attack: 0.2,
        mod_decay: 0.5,
        bus_id: BUS_SPACE,
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.2,
        noise_level: 0.01,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.16,
    };

    // Patch 30: Choir Pad (Vocal formant simulation)
    patches[PATCH_CHOIR_PAD] = ModularPatch {
        osc_type: OSC_PULSE,
        sub_level: 0.3,
        pulse_width: 0.35,
        filter_type: FILTER_BANDPASS,
        cutoff_base: 800.0,
        cutoff_env_amt: 950.0,
        cutoff_key_track: 0.6,
        resonance: 0.65,
        attack: 0.3,
        decay: 0.6,
        sustain: 0.7,
        release: 1.2,
        mod_attack: 0.35,
        mod_decay: 0.7,
        bus_id: BUS_SPACE,
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.15,
        noise_level: 0.02,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.12,
    };

    // Patch 31: Space Drone (Continuous atmospheric evolving drone)
    patches[PATCH_SPACE_DRONE] = ModularPatch {
        osc_type: OSC_SUPERSAW,
        sub_level: 0.6,
        pulse_width: 0.5,
        filter_type: FILTER_NOTCH,
        cutoff_base: 550.0,
        cutoff_env_amt: 800.0,
        cutoff_key_track: 0.3,
        resonance: 0.75,
        attack: 0.8,
        decay: 1.5,
        sustain: 0.9,
        release: 2.5,
        mod_attack: 1.0,
        mod_decay: 2.0,
        bus_id: BUS_SPACE,
        polyphony: 4,
        glide: 0.15,
        filter_drive: 0.3,
        noise_level: 0.05,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.25,
    };

    // Patch 32: Pluck Lead (Bright fast EDM/Trance pluck)
    patches[PATCH_PLUCK_LEAD] = ModularPatch {
        osc_type: OSC_PULSE,
        sub_level: 0.2,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 1200.0,
        cutoff_env_amt: 5200.0,
        cutoff_key_track: 1.2,
        resonance: 0.5,
        attack: 0.002,
        decay: 0.12,
        sustain: 0.0,
        release: 0.1,
        mod_attack: 0.002,
        mod_decay: 0.09,
        bus_id: BUS_LEAD,
        polyphony: 4,
        glide: 0.0,
        filter_drive: 0.25,
        noise_level: 0.0,
        pitch_snap: 12.0,
        pitch_snap_decay: 0.01,
        analog_drift: 0.04,
    };

    // Patch 33: Neuro Bass (Heavy distorted modulated neurofunk bass)
    patches[PATCH_NEURO_BASS] = ModularPatch {
        osc_type: OSC_REESE,
        sub_level: 0.5,
        pulse_width: 0.5,
        filter_type: FILTER_BANDPASS,
        cutoff_base: 380.0,
        cutoff_env_amt: 2200.0,
        cutoff_key_track: 0.8,
        resonance: 0.72,
        attack: 0.008,
        decay: 0.2,
        sustain: 0.7,
        release: 0.18,
        mod_attack: 0.01,
        mod_decay: 0.16,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.045,
        filter_drive: 0.85,
        noise_level: 0.02,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.15,
    };

    // Patch 34: 808 Sub (Pure sub-bass with punchy pitch envelope)
    patches[PATCH_808_SUB] = ModularPatch {
        osc_type: OSC_SINE,
        sub_level: 0.9,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 120.0,
        cutoff_env_amt: 80.0,
        cutoff_key_track: 0.1,
        resonance: 0.0,
        attack: 0.005,
        decay: 0.45,
        sustain: 0.6,
        release: 0.35,
        mod_attack: 0.005,
        mod_decay: 0.3,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.05,
        filter_drive: 0.3,
        noise_level: 0.0,
        pitch_snap: 36.0,
        pitch_snap_decay: 0.025,
        analog_drift: 0.0,
    };

    // Patch 35: Slap Bass (Percussive funk slap bass)
    patches[PATCH_SLAP_BASS] = ModularPatch {
        osc_type: OSC_PULSE,
        sub_level: 0.3,
        pulse_width: 0.4,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 650.0,
        cutoff_env_amt: 3800.0,
        cutoff_key_track: 1.0,
        resonance: 0.55,
        attack: 0.003,
        decay: 0.14,
        sustain: 0.3,
        release: 0.08,
        mod_attack: 0.003,
        mod_decay: 0.1,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.02,
        filter_drive: 0.4,
        noise_level: 0.05,
        pitch_snap: 24.0,
        pitch_snap_decay: 0.015,
        analog_drift: 0.05,
    };

    // Patch 36: Organ Bass (Deep house M1-style organ bass)
    patches[PATCH_ORGAN_BASS] = ModularPatch {
        osc_type: OSC_ORGAN,
        sub_level: 0.4,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 850.0,
        cutoff_env_amt: 1800.0,
        cutoff_key_track: 0.8,
        resonance: 0.3,
        attack: 0.003,
        decay: 0.16,
        sustain: 0.5,
        release: 0.1,
        mod_attack: 0.003,
        mod_decay: 0.12,
        bus_id: BUS_BASS,
        polyphony: 1,
        glide: 0.03,
        filter_drive: 0.3,
        noise_level: 0.0,
        pitch_snap: 12.0,
        pitch_snap_decay: 0.012,
        analog_drift: 0.03,
    };

    // Patch 37: Acid Lead (High resonant squealing 303 lead)
    patches[PATCH_ACID_LEAD] = ModularPatch {
        osc_type: OSC_SAW,
        sub_level: 0.1,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 950.0,
        cutoff_env_amt: 6200.0,
        cutoff_key_track: 1.3,
        resonance: 0.92,
        attack: 0.004,
        decay: 0.15,
        sustain: 0.25,
        release: 0.12,
        mod_attack: 0.004,
        mod_decay: 0.12,
        bus_id: BUS_LEAD,
        polyphony: 2,
        glide: 0.06,
        filter_drive: 0.75,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.12,
    };

    // Patch 38: FM Bell (Bright crystalline FM bell)
    patches[PATCH_FM_BELL] = ModularPatch {
        osc_type: OSC_FM,
        sub_level: 0.0,
        pulse_width: 0.5,
        filter_type: FILTER_BANDPASS,
        cutoff_base: 2400.0,
        cutoff_env_amt: 3500.0,
        cutoff_key_track: 1.0,
        resonance: 0.6,
        attack: 0.002,
        decay: 0.65,
        sustain: 0.0,
        release: 0.5,
        mod_attack: 0.002,
        mod_decay: 0.45,
        bus_id: BUS_SPACE,
        polyphony: 6,
        glide: 0.0,
        filter_drive: 0.1,
        noise_level: 0.0,
        pitch_snap: 0.0,
        pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
        analog_drift: 0.02,
    };

    // Patch 39: Glass Keys (Delicate ambient electric piano keys)
    patches[PATCH_GLASS_KEYS] = ModularPatch {
        osc_type: OSC_SINE,
        sub_level: 0.25,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 2800.0,
        cutoff_env_amt: 2400.0,
        cutoff_key_track: 1.0,
        resonance: 0.2,
        attack: 0.004,
        decay: 0.35,
        sustain: 0.3,
        release: 0.4,
        mod_attack: 0.004,
        mod_decay: 0.3,
        bus_id: BUS_SPACE,
        polyphony: 6,
        glide: 0.0,
        filter_drive: 0.1,
        noise_level: 0.0,
        pitch_snap: 12.0,
        pitch_snap_decay: 0.006,
        analog_drift: 0.04,
    };

    // Patch 40: Siren (Dub sound system laser siren)
    patches[PATCH_SIREN] = ModularPatch {
        osc_type: OSC_SAW,
        sub_level: 0.3,
        pulse_width: 0.5,
        filter_type: FILTER_BANDPASS,
        cutoff_base: 1800.0,
        cutoff_env_amt: 3200.0,
        cutoff_key_track: 0.0,
        resonance: 0.8,
        attack: 0.01,
        decay: 0.4,
        sustain: 0.6,
        release: 0.4,
        mod_attack: 0.01,
        mod_decay: 0.35,
        bus_id: BUS_SPACE,
        polyphony: 1,
        glide: 0.12,
        filter_drive: 0.5,
        noise_level: 0.0,
        pitch_snap: 48.0,
        pitch_snap_decay: 0.12,
        analog_drift: 0.2,
    };

    // Patch 41: Laser (High sci-fi arcade zap)
    patches[PATCH_LASER] = ModularPatch {
        osc_type: OSC_PULSE,
        sub_level: 0.0,
        pulse_width: 0.5,
        filter_type: FILTER_LOWPASS,
        cutoff_base: 5000.0,
        cutoff_env_amt: 6000.0,
        cutoff_key_track: 0.0,
        resonance: 0.65,
        attack: 0.001,
        decay: 0.08,
        sustain: 0.0,
        release: 0.06,
        mod_attack: 0.001,
        mod_decay: 0.06,
        bus_id: BUS_DIRECT,
        polyphony: 2,
        glide: 0.0,
        filter_drive: 0.4,
        noise_level: 0.0,
        pitch_snap: 48.0,
        pitch_snap_decay: 0.035,
        analog_drift: 0.0,
    };

    patches
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_oscillator_type_from_u8() {
        assert_eq!(OscillatorType::from(0), OscillatorType::Saw);
        assert_eq!(OscillatorType::from(4), OscillatorType::Supersaw);
        assert_eq!(OscillatorType::from(8), OscillatorType::Fm);
        assert_eq!(OscillatorType::from(12), OscillatorType::Click);
        assert_eq!(OscillatorType::from(99), OscillatorType::Saw);
    }

    #[test]
    fn test_bus_target_from_u8() {
        assert_eq!(BusTarget::from(0), BusTarget::Drums);
        assert_eq!(BusTarget::from(1), BusTarget::Bass);
        assert_eq!(BusTarget::from(2), BusTarget::Space);
        assert_eq!(BusTarget::from(3), BusTarget::Lead);
        assert_eq!(BusTarget::from(4), BusTarget::Direct);
        assert_eq!(BusTarget::from(99), BusTarget::Drums);
    }

    #[test]
    fn test_init_default_patches() {
        let patches = init_default_patches();
        assert_eq!(patches[PATCH_SAW_BASS].osc_type, OSC_SAW);
        assert_eq!(patches[PATCH_ACID_BASS].polyphony, 1);
        assert_eq!(patches[PATCH_DARK_PAD].polyphony, 8);
        assert!(patches[PATCH_808_SUB].pitch_snap > 0.0);
    }
}
