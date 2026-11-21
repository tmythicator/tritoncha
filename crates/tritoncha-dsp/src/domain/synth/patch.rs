//! Modular synthesizer voice patch definitions and presets.

pub const MAX_PATCHES: usize = 64;
pub const VOICE_PATCH_PARAM_COUNT: usize = 22;

#[derive(Copy, Clone, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct PatchId(pub usize);

impl PatchId {
    pub const CLICK: Self = Self(11);

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

    pub fn default_click() -> Self {
        modular_patch! {
            osc_type: OscillatorType::Click,
            sub_level: 0.0,
            pulse_width: 0.5,
            filter_type: FilterMode::Lowpass,
            cutoff_base: 8000.0,
            cutoff_env_amt: 0.0,
            cutoff_key_track: 1.0,
            resonance: 0.1,
            attack: 0.001,
            decay: 0.025,
            sustain: 0.0,
            release: 0.015,
            mod_attack: 0.001,
            mod_decay: 0.01,
            bus_id: BusTarget::Direct,
            polyphony: 2,
            glide: 0.0,
            filter_drive: 0.1,
            noise_level: 0.0,
            pitch_snap: 0.0,
            pitch_snap_decay: DEFAULT_PITCH_SNAP_DECAY_SEC,
            analog_drift: 0.0,
        }
    }

    /// Returns default patch for a given patch identifier or index.
    pub fn default_for(patch_id: impl Into<PatchId>) -> Self {
        let pid = patch_id.into();
        if pid == PatchId::CLICK {
            Self::default_click()
        } else {
            Self::default_lead()
        }
    }
}

impl Default for ModularPatch {
    fn default() -> Self {
        Self::default_lead()
    }
}

impl From<[f32; VOICE_PATCH_PARAM_COUNT]> for ModularPatch {
    #[inline(always)]
    fn from(p: [f32; VOICE_PATCH_PARAM_COUNT]) -> Self {
        modular_patch! {
            osc_type: (p[0].round() as u8),
            sub_level: p[1],
            pulse_width: p[2],
            filter_type: (p[3].round() as u8),
            cutoff_base: p[4],
            cutoff_env_amt: p[5],
            cutoff_key_track: p[6],
            resonance: p[7],
            attack: p[8],
            decay: p[9],
            sustain: p[10],
            release: p[11],
            mod_attack: p[12],
            mod_decay: p[13],
            bus_id: (p[14].round() as u8),
            polyphony: (p[15].round() as u8),
            glide: p[16],
            filter_drive: p[17],
            noise_level: p[18],
            pitch_snap: p[19],
            pitch_snap_decay: p[20],
            analog_drift: p[21],
        }
    }
}

pub fn init_default_patches() -> [ModularPatch; MAX_PATCHES] {
    std::array::from_fn(ModularPatch::default_for)
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
        assert_eq!(patches[4].osc_type, OscillatorType::Saw);
        assert_eq!(
            patches[PatchId::CLICK.as_usize()].osc_type,
            OscillatorType::Click
        );
        assert_eq!(patches[PatchId::CLICK.as_usize()].bus_id, BusTarget::Direct);
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
