//! Declarative 24-float percussion patch contract, physical body taxonomy, and filter spec.

use crate::domain::effects::StateVariableFilter;
pub use crate::domain::params::{
    BurstCount, BurstSpacing, ChokeGroup, ClickFreqHz, FmDepth, FmRatio, ModalSpread,
    NoiseCutoffHz, NoiseDecay, NoiseQ, Normalized, PercDecay, PercDrive, PercPitchDecay,
    PercPitchDrop, PercPitchHz,
};

pub const PERC_PATCH_PARAM_COUNT: usize = 24;
pub const MAX_PERC_VOICES: usize = 16;
pub const MAX_KIT_SLOTS: usize = 16;

/// Strongly-typed track-local percussion kit index (`0..15`).
#[derive(Copy, Clone, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct KitIndex(pub usize);

impl KitIndex {
    #[inline(always)]
    pub const fn as_usize(self) -> usize {
        self.0
    }

    #[inline(always)]
    pub const fn is_valid(self) -> bool {
        self.0 < MAX_KIT_SLOTS
    }
}

impl From<usize> for KitIndex {
    #[inline(always)]
    fn from(val: usize) -> Self {
        Self(val)
    }
}

/// Physical resonant body family for a percussion voice:
/// - `Membrane (0)`: Stretched circular drumhead + enclosed air column (kicks, toms, snare heads).
/// - `Shell (1)`: Rigid wooden drum cylinder / metal rim hoop (rimshots, cross-stick, shell body).
/// - `Cymbal (2)`: Spun/hammered bronze cymbal disc (bell ping <-> bow ride <-> clamped hat <-> edge crash).
/// - `Idiophone (3)`: Rigid struck hollow metal/wood cavity (808 cowbell, woodblock, agogo, claves).
/// - `Burst (4)`: Turbulent multi-tap air clap / snare wire rattle (hand claps, shakers, wire bursts).
#[repr(u8)]
#[derive(Copy, Clone, Debug, PartialEq, Eq, Default)]
pub enum PercBodyType {
    #[default]
    Membrane = 0,
    Shell = 1,
    Cymbal = 2,
    Idiophone = 3,
    Burst = 4,
}

impl From<u8> for PercBodyType {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => PercBodyType::Shell,
            2 => PercBodyType::Cymbal,
            3 => PercBodyType::Idiophone,
            4 => PercBodyType::Burst,
            _ => PercBodyType::Membrane,
        }
    }
}

/// Filter mode for the secondary noise layer and metallic body filter.
#[repr(u8)]
#[derive(Copy, Clone, Debug, PartialEq, Eq, Default)]
pub enum NoiseFilterMode {
    #[default]
    Highpass = 0,
    Bandpass = 1,
    Lowpass = 2,
}

impl From<u8> for NoiseFilterMode {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => NoiseFilterMode::Bandpass,
            2 => NoiseFilterMode::Lowpass,
            _ => NoiseFilterMode::Highpass,
        }
    }
}

/// Cohesive filter configuration shared by noise layers, metallic cymbals, and cavity idiophones.
#[derive(Copy, Clone, Debug, PartialEq)]
pub struct NoiseFilterSpec {
    pub cutoff_hz: f32,
    pub q: f32,
    pub mode: NoiseFilterMode,
}

impl Default for NoiseFilterSpec {
    fn default() -> Self {
        Self {
            cutoff_hz: 3200.0,
            q: 0.45,
            mode: NoiseFilterMode::Highpass,
        }
    }
}

impl NoiseFilterSpec {
    /// Applies the configured state-variable filter mode (HP, BP, or LP) to one audio sample.
    #[inline(always)]
    pub fn apply(&self, filter: &mut StateVariableFilter, input: f32, sample_rate: f32) -> f32 {
        match self.mode {
            NoiseFilterMode::Highpass => {
                filter.process_hp(input, self.cutoff_hz, self.q, sample_rate)
            }
            NoiseFilterMode::Bandpass => {
                filter.process_bp(input, self.cutoff_hz, self.q, sample_rate)
            }
            NoiseFilterMode::Lowpass => {
                filter.process_lp(input, self.cutoff_hz, self.q, sample_rate)
            }
        }
    }
}

#[macro_export]
macro_rules! perc_patch {
    (@val $f:ident) => { $f.into() };
    (@val $f:ident : $v:expr) => { ($v).into() };
    ($($field:ident $(: $val:expr)?),* $(,)?) => {
        $crate::domain::drums::PercPatch {
            $( $field: $crate::perc_patch!(@val $field $(: $val)?), )*
            ..$crate::domain::drums::PercPatch::default()
        }
    };
}

/// Strongly-typed parameter specification for a layered percussion voice (24 floats across C-ABI FFI).
#[derive(Copy, Clone, Debug, PartialEq)]
pub struct PercPatch {
    pub body_type: PercBodyType,
    pub pitch_hz: PercPitchHz,
    pub pitch_drop: PercPitchDrop,
    pub pitch_decay_s: PercPitchDecay,
    pub decay_s: PercDecay,
    pub click_level: Normalized,
    pub click_freq_hz: ClickFreqHz,
    pub burst_count: BurstCount,
    pub burst_spacing_s: BurstSpacing,
    pub fm_depth: FmDepth,
    pub fm_ratio: FmRatio,
    pub noise_mix: Normalized,
    pub noise_decay_s: NoiseDecay,
    pub noise_cutoff_hz: NoiseCutoffHz,
    pub noise_q: NoiseQ,
    pub noise_filter_mode: NoiseFilterMode,
    pub modal_spread: ModalSpread,
    pub drive: PercDrive,
    pub fold: Normalized,
    pub crush: Normalized,
    pub vel_to_pitch: Normalized,
    pub vel_to_tone: Normalized,
    pub variation: Normalized,
    pub choke_group: ChokeGroup,
}

impl Default for PercPatch {
    fn default() -> Self {
        Self {
            body_type: PercBodyType::Membrane,
            pitch_hz: PercPitchHz::from_clamped(52.0),
            pitch_drop: PercPitchDrop::from_clamped(0.0),
            pitch_decay_s: PercPitchDecay::from_clamped(0.030),
            decay_s: PercDecay::from_clamped(0.25),
            click_level: Normalized::from_clamped(0.20),
            click_freq_hz: ClickFreqHz::from_clamped(2800.0),
            burst_count: BurstCount::from_clamped(1),
            burst_spacing_s: BurstSpacing::from_clamped(0.011),
            fm_depth: FmDepth::from_clamped(0.0),
            fm_ratio: FmRatio::from_clamped(2.5),
            noise_mix: Normalized::from_clamped(0.0),
            noise_decay_s: NoiseDecay::from_clamped(0.18),
            noise_cutoff_hz: NoiseCutoffHz::from_clamped(3200.0),
            noise_q: NoiseQ::from_clamped(0.45),
            noise_filter_mode: NoiseFilterMode::Highpass,
            modal_spread: ModalSpread::from_clamped(1.0),
            drive: PercDrive::from_clamped(1.3),
            fold: Normalized::from_clamped(0.0),
            crush: Normalized::from_clamped(0.0),
            vel_to_pitch: Normalized::from_clamped(0.12),
            vel_to_tone: Normalized::from_clamped(0.35),
            variation: Normalized::from_clamped(0.08),
            choke_group: ChokeGroup::NONE,
        }
    }
}

impl PercPatch {
    /// Returns true if this percussion patch represents a sub/kick drum that should trigger sidechain ducking.
    #[inline(always)]
    pub fn is_kick(&self) -> bool {
        self.body_type == PercBodyType::Membrane
            && *self.pitch_hz <= 90.0
            && *self.pitch_drop >= 40.0
            && *self.noise_mix < 0.35
    }
}

impl From<[f32; PERC_PATCH_PARAM_COUNT]> for PercPatch {
    #[inline(always)]
    fn from(p: [f32; PERC_PATCH_PARAM_COUNT]) -> Self {
        Self {
            body_type: PercBodyType::from(p[0].round().max(0.0) as u8),
            pitch_hz: p[1].into(),
            pitch_drop: p[2].into(),
            pitch_decay_s: p[3].into(),
            decay_s: p[4].into(),
            click_level: p[5].into(),
            click_freq_hz: p[6].into(),
            burst_count: p[7].into(),
            burst_spacing_s: p[8].into(),
            fm_depth: p[9].into(),
            fm_ratio: p[10].into(),
            noise_mix: p[11].into(),
            noise_decay_s: p[12].into(),
            noise_cutoff_hz: p[13].into(),
            noise_q: p[14].into(),
            noise_filter_mode: NoiseFilterMode::from(p[15].round().max(0.0) as u8),
            modal_spread: p[16].into(),
            drive: p[17].into(),
            fold: p[18].into(),
            crush: p[19].into(),
            vel_to_pitch: p[20].into(),
            vel_to_tone: p[21].into(),
            variation: p[22].into(),
            choke_group: p[23].into(),
        }
    }
}
