//! Unified Bronze Cymbal synthesis:
//! Models a spun/hammered bronze cymbal disc smoothly across its strike zones:
//! 1. Clamped Hi-Hat pair (`choke_group > 0`, e.g. `:hat-closed`, `:hat-open`):
//!    6-oscillator inharmonic metallic cluster + noise-diffused shimmer routed through the SVF filter.
//! 2. Open Bronze Disc (`choke_group == 0`, e.g. `:ride-bell`, `:ride`, `:crash-16..18`, `:splash`, `:china`):
//!    Smoothly crossfades (`noise_mix` 0.25 -> 0.75) between Chladni bronze plate modes (bell/bow ping)
//!    and a 4-line Householder Bronze Plate FDN Diffuser excited by a cascaded FM metallic strike burst.

use crate::core::math::{sin_phase, wrap_phase};
use crate::domain::drums::bodies::{ResonatorBank, NUM_MODES};
use crate::domain::drums::patch::{ChokeGroup, NoiseFilterSpec};
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;

/// Inharmonic Chladni bronze plate / bell partial ratios ($f_n \propto (m + 2n)^2$).
const BRONZE_PLATE_RATIOS: [f32; NUM_MODES] = [1.0, 1.503, 2.021, 2.756, 3.412, 4.187];
const BRONZE_PLATE_GAINS: [f32; NUM_MODES] = [0.42, 0.36, 0.32, 0.26, 0.20, 0.16];
const BRONZE_PLATE_DECAY_SCALES: [f32; NUM_MODES] = [1.0, 0.84, 0.68, 0.52, 0.38, 0.26];

/// Inharmonic ratios for 6-oscillator metallic shimmer matrix (808/909 hats & crashes).
const CYMBAL_SHIMMER_RATIOS: [f32; NUM_MODES] = [1.0, 1.447, 1.617, 1.926, 2.502, 2.917];

/// Strike-zone configuration for a bronze cymbal hit.
#[derive(Copy, Clone, Debug)]
pub struct CymbalStrikeSpec {
    pub base_pitch_hz: f32,
    pub noise_mix: f32,
    pub filter: NoiseFilterSpec,
    pub choke_group: ChokeGroup,
}

impl CymbalStrikeSpec {
    /// Returns true if this cymbal is a clamped hi-hat pair (`choke_group > 0`).
    #[inline(always)]
    pub const fn is_clamped_hat(&self) -> bool {
        self.choke_group.is_active()
    }

    /// Smooth crossfade weight `0.0..1.0` from Bell/Bow ping (`0.0`) to Edge Crash shimmer (`1.0`).
    /// Crossfades smoothly across `noise_mix` `0.25..0.75` with zero abrupt mode-switch cliff.
    #[inline(always)]
    pub fn crash_blend(&self) -> f32 {
        ((self.noise_mix - 0.25) / 0.50).clamp(0.0, 1.0)
    }
}

/// Precomputes the Chladni bronze plate resonators, 6 inharmonic shimmer frequencies,
/// and 4-line Householder bronze plate FDN for open cymbals.
#[inline(always)]
pub fn configure_cymbal(
    bank: &mut ResonatorBank,
    eff_pitch_hz: f32,
    eff_decay_s: f32,
    spread: f32,
    clamped_hat: bool,
    sample_rate: f32,
) {
    let plate_scale = (spread - 0.4).clamp(0.7, 2.2);
    for i in 0..NUM_MODES {
        let plate_ratio = 1.0 + (BRONZE_PLATE_RATIOS[i] - 1.0) * plate_scale;
        let mode_freq = eff_pitch_hz * plate_ratio;
        let mode_decay = eff_decay_s * BRONZE_PLATE_DECAY_SCALES[i];
        bank.modes[i].configure(mode_freq, mode_decay, BRONZE_PLATE_GAINS[i], sample_rate);

        let metal_freq = if clamped_hat {
            eff_pitch_hz * (1.0 + (CYMBAL_SHIMMER_RATIOS[i] - 1.0) * spread)
        } else {
            let shimmer_scale = 1.65 + 0.35 * (spread - 1.45).max(0.0);
            eff_pitch_hz * shimmer_scale * (1.0 + (CYMBAL_SHIMMER_RATIOS[i] - 1.0) * spread)
        };
        bank.metallic_freqs[i] = metal_freq.clamp(40.0, sample_rate * 0.45);
    }

    if !clamped_hat {
        bank.plate_fdn
            .configure(eff_pitch_hz, eff_decay_s, spread, sample_rate);
    }
}

/// Renders one sample of the unified bronze cymbal body across Clamped Hat and smooth Bell <-> Crash strikes.
#[inline(always)]
pub fn render_cymbal(
    spec: &CymbalStrikeSpec,
    bank: &mut ResonatorBank,
    body_env: f32,
    exciter_env: &mut f32,
    exciter_decay: f32,
    noise_raw: f32,
    sample_rate: f32,
) -> f32 {
    if spec.is_clamped_hat() {
        let mut ring = 1.0_f32;
        let mut pulse_sum = 0.0_f32;
        for i in 0..NUM_MODES {
            let pw = 0.28 + 0.06 * (i as f32);
            let pulse = if bank.metallic_phases[i] < pw {
                1.0
            } else {
                -1.0
            };
            ring *= pulse;
            pulse_sum += pulse;
            bank.metallic_phases[i] =
                wrap_phase(bank.metallic_phases[i] + bank.metallic_freqs[i] / sample_rate);
        }
        let shimmer = ring * 0.72 + (pulse_sum / NUM_MODES as f32) * 0.28;
        let raw_metal = shimmer * (0.72 + 0.28 * noise_raw);
        let filtered = spec
            .filter
            .apply(&mut bank.body_filter, raw_metal, sample_rate);
        return filtered * body_env * 0.42;
    }

    let crash_w = spec.crash_blend();
    let plate_w = 1.0 - crash_w;

    // Brief stick impact exciter
    let exc_level = *exciter_env;
    let exc = if exc_level > MIN_AUDIBLE_VELOCITY {
        let e = noise_raw * exc_level;
        *exciter_env *= exciter_decay;
        e
    } else {
        0.0
    };

    // 1. Bell/Bow Chladni bronze plate modes (active when plate_w > 0)
    let plate_sig = if plate_w > 0.001 {
        let mut sum = 0.0_f32;
        let plate_exc = exc * 0.55;
        for m in bank.modes.iter_mut() {
            sum += m.process(plate_exc);
        }
        sum * plate_w
    } else {
        0.0
    };

    // 2. Bronze Plate FDN Diffuser excited by a short cascaded FM metallic strike burst
    let crash_sig = if crash_w > 0.001 {
        let mut s = [0.0_f32; NUM_MODES];
        for i in 0..NUM_MODES {
            let fm = if i > 0 { s[i - 1] * 0.45 } else { 0.0 };
            s[i] = sin_phase(bank.metallic_phases[i] + fm + noise_raw * 0.16);
            bank.metallic_phases[i] =
                wrap_phase(bank.metallic_phases[i] + bank.metallic_freqs[i] / sample_rate);
        }
        let bronze_ring = (s[0] * s[1] + s[2] * s[3] + s[4] * s[5]) * 0.45
            + (s[0] + s[1] + s[2] + s[3] + s[4] + s[5]) * 0.08;

        // Feed a brief metallic+noise strike burst (plus subtle continuous shimmer) into the 4-line
        // Householder bronze plate FDN so thousands of inharmonic partials bloom and decay naturally
        let fdn_in =
            (bronze_ring * 0.70 + noise_raw * 0.55) * exc_level + bronze_ring * body_env * 0.08;
        let plate_wash = bank.plate_fdn.process(fdn_in);

        // Blend the liquid FDN bronze wash with a touch of direct metallic ring
        (plate_wash * 1.15 + bronze_ring * body_env * 0.25) * crash_w
    } else {
        0.0
    };

    let hp_hz = (spec.base_pitch_hz * (0.85 + 0.55 * crash_w)).clamp(320.0, 1400.0);
    bank.body_filter
        .process_hp(plate_sig + crash_sig, hp_hz, 0.20, sample_rate)
}
