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

pub use super::presets::init_default_patches;

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
