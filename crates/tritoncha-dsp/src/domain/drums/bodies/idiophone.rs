//! Idiophone body synthesis: rigid struck hollow metal/wood cavity
//! (TR-808 dual-square cowbell, agogo bell, woodblock, claves).

use crate::core::math::wrap_phase;
use crate::domain::drums::bodies::NUM_MODES;
use crate::domain::drums::patch::NoiseFilterMode;
use crate::domain::effects::StateVariableFilter;

/// Classic TR-808 dual-square cowbell frequency ratio (800 Hz / 540 Hz).
const COWBELL_RATIO: f32 = 1.481_481_4;

/// Precomputes the dual-mode cavity resonator frequencies on trigger.
#[inline(always)]
pub fn configure_idiophone_freqs(
    metallic_freqs: &mut [f32; NUM_MODES],
    eff_pitch_hz: f32,
    sample_rate: f32,
) {
    metallic_freqs[0] = eff_pitch_hz.clamp(40.0, sample_rate * 0.45);
    metallic_freqs[1] = (eff_pitch_hz * COWBELL_RATIO).clamp(40.0, sample_rate * 0.45);
}

/// Renders one sample of a dual-mode hollow cavity idiophone filtered through the cavity formant SVF.
#[allow(clippy::too_many_arguments)]
#[inline(always)]
pub fn render_idiophone(
    metallic_phases: &mut [f32; NUM_MODES],
    metallic_freqs: &[f32; NUM_MODES],
    body_filter: &mut StateVariableFilter,
    noise_cutoff_hz: f32,
    noise_q: f32,
    noise_filter_mode: NoiseFilterMode,
    body_env: f32,
    sample_rate: f32,
) -> f32 {
    let sq0 = if metallic_phases[0] < 0.5 { 1.0 } else { -1.0 };
    let sq1 = if metallic_phases[1] < 0.5 { 1.0 } else { -1.0 };
    metallic_phases[0] = wrap_phase(metallic_phases[0] + metallic_freqs[0] / sample_rate);
    metallic_phases[1] = wrap_phase(metallic_phases[1] + metallic_freqs[1] / sample_rate);
    let raw_cavity = (sq0 + sq1) * 0.5;

    let filtered = match noise_filter_mode {
        NoiseFilterMode::Bandpass => {
            body_filter.process_bp(raw_cavity, noise_cutoff_hz, noise_q, sample_rate)
        }
        NoiseFilterMode::Highpass => {
            body_filter.process_hp(raw_cavity, noise_cutoff_hz, noise_q, sample_rate)
        }
        NoiseFilterMode::Lowpass => {
            body_filter.process_lp(raw_cavity, noise_cutoff_hz, noise_q, sample_rate)
        }
    };
    filtered * body_env * 0.85
}
