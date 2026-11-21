//! Membrane body synthesis: stretched circular drumhead coupled to an enclosed air column
//! (kick drums, toms, snare batter/resonant heads).

use crate::core::math::{sin_phase, wrap_phase};

/// Renders one sample of a tensioned circular drumhead + air column oscillator
/// with instantaneous pitch sweep and Bessel $J_{11}$ FM overtone modulation.
#[inline(always)]
pub fn render_membrane(
    phase: &mut f32,
    fm_phase: &mut f32,
    cur_freq_hz: f32,
    fm_depth: f32,
    fm_ratio: f32,
    body_env: f32,
    sample_rate: f32,
) -> f32 {
    let fm = if fm_depth > 0.0 {
        let f = sin_phase(*fm_phase) * fm_depth * body_env;
        *fm_phase = wrap_phase(*fm_phase + (cur_freq_hz * fm_ratio) / sample_rate);
        f
    } else {
        0.0
    };
    let osc = sin_phase(*phase + fm);
    *phase = wrap_phase(*phase + cur_freq_hz / sample_rate);
    osc * body_env
}
