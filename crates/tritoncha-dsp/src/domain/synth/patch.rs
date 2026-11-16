//! Modular synthesizer voice patch definitions and presets.

pub const MAX_PATCHES: usize = 64;

#[derive(Copy, Clone, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct PatchId(pub usize);

impl PatchId {
    pub const SAW_BASS: Self = Self(4);
    pub const ACID_BASS: Self = Self(5);
    pub const SUB_SINE: Self = Self(6);
    pub const DARK_PAD: Self = Self(7);
    pub const LEAD: Self = Self(8);
    pub const FM: Self = Self(9);
    pub const REESE: Self = Self(10);
    pub const CLICK: Self = Self(11);
    pub const SUPERSAW: Self = Self(12);
    pub const BLADE: Self = Self(13);
    pub const HOOVER: Self = Self(14);
    pub const KARPLUS: Self = Self(15);
    pub const ORGAN: Self = Self(16);
    pub const CHIPTUNE: Self = Self(17);
    pub const AMBIENT_GLASS: Self = Self(19);
    pub const ETHEREAL_PAD: Self = Self(27);
    pub const ICE_PAD: Self = Self(28);
    pub const WARM_STRINGS: Self = Self(29);
    pub const CHOIR_PAD: Self = Self(30);
    pub const SPACE_DRONE: Self = Self(31);
    pub const PLUCK_LEAD: Self = Self(32);
    pub const NEURO_BASS: Self = Self(33);
    pub const SUB_808: Self = Self(34);
    pub const SLAP_BASS: Self = Self(35);
    pub const ORGAN_BASS: Self = Self(36);
    pub const ACID_LEAD: Self = Self(37);
    pub const FM_BELL: Self = Self(38);
    pub const GLASS_KEYS: Self = Self(39);
    pub const SIREN: Self = Self(40);
    pub const LASER: Self = Self(41);
    pub const LEAD_NBELL: Self = Self(44);
    pub const ZAP: Self = Self(45);

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

pub use crate::domain::effects::FilterMode;
pub use crate::domain::BusTarget;

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
            osc_type: OscillatorType::Saw,
            sub_level: 0.25,
            pulse_width: 0.5,
            filter_type: FilterMode::Lowpass,
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
            bus_id: BusTarget::Lead,
            polyphony: 4,
            glide: 0.0,
            filter_drive: 0.2,
            noise_level: 0.0,
            pitch_snap: 0.0,
            pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
            analog_drift: 0.05,
        }
    }

    /// Returns default patch for a given patch identifier or index.
    pub fn default_for(patch_id: impl Into<PatchId>) -> Self {
        let pid = patch_id.into();
        let all = init_default_patches();
        if pid.is_valid() {
            all[pid.as_usize()]
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
        assert_eq!(
            patches[PatchId::SAW_BASS.as_usize()].osc_type,
            OscillatorType::Saw
        );
        assert_eq!(*patches[PatchId::ACID_BASS.as_usize()].polyphony, 1);
        assert_eq!(*patches[PatchId::DARK_PAD.as_usize()].polyphony, 8);
        assert!(*patches[PatchId::SUB_808.as_usize()].pitch_snap > 0.0);
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
