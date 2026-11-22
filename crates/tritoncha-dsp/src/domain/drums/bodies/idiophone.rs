//! Idiophone body synthesis: rigid struck hollow metal/wood cavity
//! (TR-808 dual-square cowbell, agogo bell, woodblock, claves).

use crate::core::math::wrap_phase;
use crate::domain::drums::bodies::{ResonatorBank, NUM_MODES};
use crate::domain::drums::patch::NoiseFilterSpec;

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
#[inline(always)]
pub fn render_idiophone(
    bank: &mut ResonatorBank,
    filter_spec: &NoiseFilterSpec,
    body_env: f32,
    sample_rate: f32,
) -> f32 {
    let sq0 = if bank.metallic_phases[0] < 0.5 {
        1.0
    } else {
        -1.0
    };
    let sq1 = if bank.metallic_phases[1] < 0.5 {
        1.0
    } else {
        -1.0
    };
    bank.metallic_phases[0] =
        wrap_phase(bank.metallic_phases[0] + bank.metallic_freqs[0] / sample_rate);
    bank.metallic_phases[1] =
        wrap_phase(bank.metallic_phases[1] + bank.metallic_freqs[1] / sample_rate);
    let raw_cavity = (sq0 + sq1) * 0.5;
    let filtered = filter_spec.apply(&mut bank.body_filter, raw_cavity, sample_rate);
    filtered * body_env * 0.85
}
