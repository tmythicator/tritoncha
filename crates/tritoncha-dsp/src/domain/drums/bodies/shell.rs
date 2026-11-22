//! Shell body synthesis: rigid wooden drum cylinder / metal rim hoop 6-mode resonator bank
//! (snare rimshots, cross-sticks, acoustic shell bodies).

use crate::domain::drums::bodies::{CachedModalFilter, NUM_MODES};
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;

/// Circular shell / stiff membrane Bessel partial ratios (scaled by `modal_spread`).
const SHELL_MODAL_RATIOS: [f32; NUM_MODES] = [1.0, 1.587, 2.140, 2.653, 3.156, 3.891];
const SHELL_MODAL_GAINS: [f32; NUM_MODES] = [0.55, 0.38, 0.28, 0.20, 0.14, 0.10];
const SHELL_MODAL_DECAY_SCALES: [f32; NUM_MODES] = [1.0, 0.82, 0.65, 0.48, 0.35, 0.24];

/// Precomputes the 6 modal resonator frequencies and decays on voice trigger.
#[inline(always)]
pub fn configure_shell_modes(
    modes: &mut [CachedModalFilter; NUM_MODES],
    eff_pitch_hz: f32,
    eff_decay_s: f32,
    spread: f32,
    sample_rate: f32,
) {
    for (i, mode) in modes.iter_mut().enumerate() {
        let ratio = 1.0 + (SHELL_MODAL_RATIOS[i] - 1.0) * spread;
        let mode_freq = eff_pitch_hz * ratio;
        let mode_decay = eff_decay_s * SHELL_MODAL_DECAY_SCALES[i];
        mode.configure(mode_freq, mode_decay, SHELL_MODAL_GAINS[i], sample_rate);
    }
}

/// Renders one sample of the 6-mode rigid shell resonator bank.
#[inline(always)]
pub fn render_shell(
    modes: &mut [CachedModalFilter; NUM_MODES],
    exciter_env: &mut f32,
    exciter_decay: f32,
    noise_raw: f32,
) -> f32 {
    let exciter = if *exciter_env > MIN_AUDIBLE_VELOCITY {
        let e = (*exciter_env + 0.4 * noise_raw * *exciter_env) * 0.42;
        *exciter_env *= exciter_decay;
        e
    } else {
        0.0
    };
    let mut sum = 0.0;
    for m in modes.iter_mut() {
        sum += m.process(exciter);
    }
    sum
}
