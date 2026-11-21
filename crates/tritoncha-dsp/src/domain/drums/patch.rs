//! Declarative 24-float percussion patch contract and physical body taxonomy.

use crate::domain::drums::MIN_AUDIBLE_VELOCITY;

pub const PERC_PATCH_PARAM_COUNT: usize = 24;
pub const MAX_PERC_VOICES: usize = 16;
pub const MAX_KIT_SLOTS: usize = 16;

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

/// Declarative parameter specification for a layered percussion voice (24 floats across C-ABI FFI).
#[derive(Copy, Clone, Debug, PartialEq)]
pub struct PercPatch {
    pub body_type: PercBodyType,
    pub pitch_hz: f32,
    pub pitch_drop: f32,
    pub pitch_decay_s: f32,
    pub decay_s: f32,
    pub click_level: f32,
    pub click_freq_hz: f32,
    pub burst_count: u8,
    pub burst_spacing_s: f32,
    pub fm_depth: f32,
    pub fm_ratio: f32,
    pub noise_mix: f32,
    pub noise_decay_s: f32,
    pub noise_cutoff_hz: f32,
    pub noise_q: f32,
    pub noise_filter_mode: NoiseFilterMode,
    pub modal_spread: f32,
    pub drive: f32,
    pub fold: f32,
    pub crush: f32,
    pub vel_to_pitch: f32,
    pub vel_to_tone: f32,
    pub variation: f32,
    pub choke_group: u8,
}

impl Default for PercPatch {
    fn default() -> Self {
        Self {
            body_type: PercBodyType::Membrane,
            pitch_hz: 52.0,
            pitch_drop: 0.0,
            pitch_decay_s: 0.030,
            decay_s: 0.25,
            click_level: 0.20,
            click_freq_hz: 2800.0,
            burst_count: 1,
            burst_spacing_s: 0.011,
            fm_depth: 0.0,
            fm_ratio: 2.5,
            noise_mix: 0.0,
            noise_decay_s: 0.18,
            noise_cutoff_hz: 3200.0,
            noise_q: 0.45,
            noise_filter_mode: NoiseFilterMode::Highpass,
            modal_spread: 1.0,
            drive: 1.3,
            fold: 0.0,
            crush: 0.0,
            vel_to_pitch: 0.12,
            vel_to_tone: 0.35,
            variation: 0.08,
            choke_group: 0,
        }
    }
}

impl PercPatch {
    /// Returns true if this percussion patch represents a sub/kick drum that should trigger sidechain ducking.
    #[inline(always)]
    pub fn is_kick(&self) -> bool {
        self.body_type == PercBodyType::Membrane
            && self.pitch_hz <= 90.0
            && self.pitch_drop >= 40.0
            && self.noise_mix < 0.35
    }
}

impl From<[f32; PERC_PATCH_PARAM_COUNT]> for PercPatch {
    #[inline(always)]
    fn from(p: [f32; PERC_PATCH_PARAM_COUNT]) -> Self {
        let _ = MIN_AUDIBLE_VELOCITY;
        Self {
            body_type: PercBodyType::from(p[0].round().max(0.0) as u8),
            pitch_hz: p[1].clamp(20.0, 8000.0),
            pitch_drop: p[2].clamp(0.0, 1200.0),
            pitch_decay_s: p[3].clamp(0.002, 0.500),
            decay_s: p[4].clamp(0.005, 6.0),
            click_level: p[5].clamp(0.0, 1.0),
            click_freq_hz: p[6].clamp(500.0, 12000.0),
            burst_count: (p[7].round() as i32).clamp(1, 6) as u8,
            burst_spacing_s: p[8].clamp(0.003, 0.030),
            fm_depth: p[9].clamp(0.0, 2.0),
            fm_ratio: p[10].clamp(0.5, 8.0),
            noise_mix: p[11].clamp(0.0, 1.0),
            noise_decay_s: p[12].clamp(0.005, 4.0),
            noise_cutoff_hz: p[13].clamp(200.0, 16000.0),
            noise_q: p[14].clamp(0.05, 0.95),
            noise_filter_mode: NoiseFilterMode::from(p[15].round().max(0.0) as u8),
            modal_spread: p[16].clamp(0.5, 3.0),
            drive: p[17].clamp(0.5, 8.0),
            fold: p[18].clamp(0.0, 1.0),
            crush: p[19].clamp(0.0, 1.0),
            vel_to_pitch: p[20].clamp(0.0, 1.0),
            vel_to_tone: p[21].clamp(0.0, 1.0),
            variation: p[22].clamp(0.0, 1.0),
            choke_group: (p[23].round() as i32).clamp(0, 15) as u8,
        }
    }
}
