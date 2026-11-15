//! Modular synthesizer voice patch definitions and presets.

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
pub const PATCH_SUB_MOOG: usize = 42;
pub const PATCH_BASS_MOOG: usize = 43;
pub const PATCH_LEAD_NBELL: usize = 44;
pub const PATCH_ZAP: usize = 45;

#[derive(Copy, Clone, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct PatchId(pub usize);

impl PatchId {
    pub const SAW_BASS: Self = Self(PATCH_SAW_BASS);
    pub const ACID_BASS: Self = Self(PATCH_ACID_BASS);
    pub const SUB_SINE: Self = Self(PATCH_SUB_SINE);
    pub const DARK_PAD: Self = Self(PATCH_DARK_PAD);
    pub const LEAD: Self = Self(PATCH_LEAD);
    pub const FM: Self = Self(PATCH_FM);
    pub const REESE: Self = Self(PATCH_REESE);
    pub const CLICK: Self = Self(PATCH_CLICK);
    pub const SUPERSAW: Self = Self(PATCH_SUPERSAW);
    pub const BLADE: Self = Self(PATCH_BLADE);
    pub const HOOVER: Self = Self(PATCH_HOOVER);
    pub const KARPLUS: Self = Self(PATCH_KARPLUS);
    pub const ORGAN: Self = Self(PATCH_ORGAN);
    pub const CHIPTUNE: Self = Self(PATCH_CHIPTUNE);
    pub const AMBIENT_GLASS: Self = Self(PATCH_AMBIENT_GLASS);

    #[inline(always)]
    pub const fn as_usize(self) -> usize {
        self.0
    }

    #[inline(always)]
    pub fn is_valid(self) -> bool {
        self.0 < MAX_PATCHES
    }

    #[inline(always)]
    pub fn clamped(self) -> Self {
        Self(self.0.min(MAX_PATCHES - 1))
    }
}

impl From<usize> for PatchId {
    #[inline(always)]
    fn from(val: usize) -> Self {
        Self(val)
    }
}

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

// Oscillator Types
pub const OSC_SAW: OscillatorType = OscillatorType::Saw;
pub const OSC_PULSE: OscillatorType = OscillatorType::Pulse;
pub const OSC_TRIANGLE: OscillatorType = OscillatorType::Triangle;
pub const OSC_SINE: OscillatorType = OscillatorType::Sine;
pub const OSC_SUPERSAW: OscillatorType = OscillatorType::Supersaw;
pub const OSC_KARPLUS: OscillatorType = OscillatorType::Karplus;
pub const OSC_ORGAN: OscillatorType = OscillatorType::Organ;
pub const OSC_CHIPTUNE: OscillatorType = OscillatorType::Chiptune;
pub const OSC_FM: OscillatorType = OscillatorType::Fm;
pub const OSC_REESE: OscillatorType = OscillatorType::Reese;
pub const OSC_BLADE: OscillatorType = OscillatorType::Blade;
pub const OSC_HOOVER: OscillatorType = OscillatorType::Hoover;
pub const OSC_CLICK: OscillatorType = OscillatorType::Click;

pub use crate::domain::effects::FilterMode;

// Filter Types
pub const FILTER_LOWPASS: FilterMode = FilterMode::Lowpass;
pub const FILTER_HIGHPASS: FilterMode = FilterMode::Highpass;
pub const FILTER_BANDPASS: FilterMode = FilterMode::Bandpass;
pub const FILTER_NOTCH: FilterMode = FilterMode::Notch;
pub const FILTER_LADDER_24DB: FilterMode = FilterMode::Ladder24;

pub use crate::domain::BusTarget;

// Bus IDs
pub const BUS_DRUMS: BusTarget = BusTarget::Drums;
pub const BUS_BASS: BusTarget = BusTarget::Bass;
pub const BUS_SPACE: BusTarget = BusTarget::Space;
pub const BUS_LEAD: BusTarget = BusTarget::Lead;
pub const BUS_DIRECT: BusTarget = BusTarget::Direct;

// Audio and Voice Timing Constants
pub const MIN_FREQ_HZ: f32 = 20.0;
pub const MAX_FREQ_HZ: f32 = 14000.0;
pub const MIN_HOLD_SEC: f32 = 0.005;
pub const MAX_HOLD_SEC: f32 = 10.0;
pub const DEFAULT_MONO_HOLD_SEC: f32 = 0.15;
pub const DEFAULT_POLY_HOLD_SEC: f32 = 0.40;
pub const MIN_DECAY_SEC: f32 = 0.005;
pub const MIN_PITCH_SNAP_DECAY_SEC: f32 = 0.002;
pub const DEFAULT_PITCH_SNAP_DECAY_SEC: f32 = 0.015;
pub const MAX_ANALOG_DRIFT_SEMITONES: f32 = 0.15;

use crate::domain::params::*;

#[macro_export]
macro_rules! modular_patch {
    (@val $f:ident) => { $f.into() };
    (@val $f:ident : $v:expr) => { ($v).into() };
    ($($field:ident $(: $val:expr)?),* $(,)?) => {
        $crate::domain::synth::ModularPatch {
            $( $field: $crate::modular_patch!(@val $field $(: $val)?), )*
        }
    };
}

/// Modular sound design patch parameters.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct ModularPatch {
    pub osc_type: OscillatorType,
    pub sub_level: Normalized,
    pub pulse_width: PulseWidth,
    pub filter_type: FilterMode,
    pub cutoff_base: CutoffHz,
    pub cutoff_env_amt: CutoffEnvAmt,
    pub cutoff_key_track: CutoffKeyTrack,
    pub resonance: Resonance,
    pub attack: AdsrTime,
    pub decay: AdsrTime,
    pub sustain: Normalized,
    pub release: AdsrTime,
    pub mod_attack: AdsrTime,
    pub mod_decay: AdsrTime,
    pub bus_id: BusTarget,
    pub polyphony: Polyphony,
    pub glide: GlideTime,
    pub filter_drive: Normalized,
    pub noise_level: Normalized,
    pub pitch_snap: PitchSnap,
    pub pitch_snap_decay: PitchSnapDecay,
    pub analog_drift: Normalized,
}

impl ModularPatch {
    pub fn default_lead() -> Self {
        modular_patch! {
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

    /// Returns default patch for a given patch index.
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
        assert_eq!(*patches[PATCH_ACID_BASS].polyphony, 1);
        assert_eq!(*patches[PATCH_DARK_PAD].polyphony, 8);
        assert!(*patches[PATCH_808_SUB].pitch_snap > 0.0);
    }

    #[test]
    fn test_modular_patch_types_clamp_bounds() {
        let patch = ModularPatch {
            cutoff_base: 50000.0.into(), // Above 20000.0
            resonance: 5.0.into(),       // Above 0.98
            sub_level: (-2.0).into(),    // Below 0.0
            pulse_width: 0.01.into(),    // Below 0.05
            polyphony: 99.into(),        // Above 16
            glide: (-1.0).into(),        // Below 0.0
            ..ModularPatch::default()
        };

        assert_eq!(*patch.cutoff_base, 20000.0);
        assert_eq!(*patch.resonance, 0.98);
        assert_eq!(*patch.sub_level, 0.0);
        assert_eq!(*patch.pulse_width, 0.05);
        assert_eq!(*patch.polyphony, 16);
        assert_eq!(*patch.glide, 0.0);
    }
}
