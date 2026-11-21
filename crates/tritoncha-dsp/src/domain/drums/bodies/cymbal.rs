//! Unified Bronze Cymbal synthesis:
//! Models a spun/hammered bronze cymbal disc across its three physical strike zones:
//! 1. Bell / Bow strike (`noise_mix < 0.65` and `choke_group == 0`, e.g. `:ride-bell`, `:ride`):
//!    Chladni bronze plate modes excited by a zero-DC metallic ping and high-passed above 320 Hz.
//! 2. Clamped Hi-Hat pair (`choke_group > 0`, e.g. `:hat-closed`, `:hat-open`):
//!    6-oscillator inharmonic metallic cluster + noise-diffused shimmer routed through the SVF filter.
//! 3. Thin Edge Crash / China / Splash (`noise_mix >= 0.65` and `choke_group == 0`):
//!    Flat non-resonant high-pass bronze splash (`Q = 0.08`) + high-index noise-FM shimmer.

use crate::core::math::{sin_phase, soft_clip, wrap_phase};
use crate::domain::drums::bodies::{CachedModalFilter, NUM_MODES};
use crate::domain::drums::patch::NoiseFilterMode;
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;
use crate::domain::effects::StateVariableFilter;

/// Inharmonic Chladni bronze plate / bell partial ratios ($f_n \propto (m + 2n)^2$).
const BRONZE_PLATE_RATIOS: [f32; NUM_MODES] = [1.0, 1.503, 2.021, 2.756, 3.412, 4.187];
const BRONZE_PLATE_GAINS: [f32; NUM_MODES] = [0.42, 0.36, 0.32, 0.26, 0.20, 0.16];
const BRONZE_PLATE_DECAY_SCALES: [f32; NUM_MODES] = [1.0, 0.84, 0.68, 0.52, 0.38, 0.26];

/// Inharmonic ratios for 6-oscillator metallic shimmer matrix (808/909 hats & crashes).
const CYMBAL_SHIMMER_RATIOS: [f32; NUM_MODES] = [1.0, 1.447, 1.617, 1.926, 2.502, 2.917];

#[inline(always)]
pub fn is_edge_crash(noise_mix: f32, choke_group: u8) -> bool {
    choke_group == 0 && noise_mix >= 0.65
}

#[inline(always)]
pub fn is_bell_or_bow_ride(noise_mix: f32, choke_group: u8) -> bool {
    choke_group == 0 && noise_mix < 0.65
}

/// Precomputes both the Chladni bronze plate resonators (for bell/bow strikes)
/// and the 6 inharmonic shimmer frequencies (for hats and edge crashes).
#[inline(always)]
pub fn configure_cymbal(
    modes: &mut [CachedModalFilter; NUM_MODES],
    metallic_freqs: &mut [f32; NUM_MODES],
    eff_pitch_hz: f32,
    eff_decay_s: f32,
    spread: f32,
    edge_crash: bool,
    sample_rate: f32,
) {
    let plate_scale = (spread - 0.4).clamp(0.7, 2.2);
    for i in 0..NUM_MODES {
        let plate_ratio = 1.0 + (BRONZE_PLATE_RATIOS[i] - 1.0) * plate_scale;
        let mode_freq = eff_pitch_hz * plate_ratio;
        let mode_decay = eff_decay_s * BRONZE_PLATE_DECAY_SCALES[i];
        modes[i].configure(mode_freq, mode_decay, BRONZE_PLATE_GAINS[i], sample_rate);

        let metal_freq = if edge_crash {
            // High-register inharmonic bronze shimmer partials (1.2kHz - 8kHz)
            let shimmer_scale = 2.6 + 0.35 * (spread - 1.45).max(0.0);
            eff_pitch_hz * shimmer_scale * (1.0 + (CYMBAL_SHIMMER_RATIOS[i] - 1.0) * spread)
        } else {
            eff_pitch_hz * (1.0 + (CYMBAL_SHIMMER_RATIOS[i] - 1.0) * spread)
        };
        metallic_freqs[i] = metal_freq.clamp(40.0, sample_rate * 0.45);
    }
}

/// Renders one sample of the unified bronze cymbal body across Bell/Bow, Hi-Hat, or Edge Crash strikes.
#[allow(clippy::too_many_arguments)]
#[inline(always)]
pub fn render_cymbal(
    modes: &mut [CachedModalFilter; NUM_MODES],
    metallic_phases: &mut [f32; NUM_MODES],
    metallic_freqs: &[f32; NUM_MODES],
    body_filter: &mut StateVariableFilter,
    base_pitch_hz: f32,
    noise_mix: f32,
    noise_cutoff_hz: f32,
    noise_q: f32,
    noise_filter_mode: NoiseFilterMode,
    choke_group: u8,
    body_env: f32,
    exciter_env: &mut f32,
    exciter_decay: f32,
    noise_raw: f32,
    sample_rate: f32,
) -> f32 {
    if is_bell_or_bow_ride(noise_mix, choke_group) {
        // Bell / Bow strike (:ride, :ride-bell): Chladni bronze plate resonators with zero-DC ping
        let exciter = if *exciter_env > MIN_AUDIBLE_VELOCITY {
            let e = noise_raw * *exciter_env * 0.55;
            *exciter_env *= exciter_decay;
            e
        } else {
            0.0
        };
        let mut sum = 0.0;
        for m in modes.iter_mut() {
            sum += m.process(exciter);
        }
        let hp_hz = (base_pitch_hz * 0.82).clamp(320.0, sample_rate * 0.40);
        body_filter.process_hp(sum, hp_hz, 0.25, sample_rate)
    } else if is_edge_crash(noise_mix, choke_group) {
        // Edge Crash / China / Splash: flat non-resonant HP splash + high-index noise-FM shimmer
        let mut shimmer = 0.0_f32;
        let fm_noise = noise_raw * 0.85;
        for (i, phase) in metallic_phases.iter_mut().enumerate() {
            shimmer += sin_phase(*phase + fm_noise * (i as f32 + 1.0));
            *phase = wrap_phase(*phase + metallic_freqs[i] / sample_rate);
        }
        shimmer *= 1.0 / (NUM_MODES as f32);

        let splash_hp_hz = (noise_cutoff_hz * 0.50).clamp(1100.0, 3200.0);
        let splash_src = soft_clip((noise_raw * 0.85 + shimmer * 0.45) * 1.5);
        let splash_body = body_filter.process_hp(splash_src, splash_hp_hz, 0.08, sample_rate);
        let impact_env = *exciter_env * 0.65 + body_env * 0.35;
        *exciter_env *= exciter_decay;

        splash_body * impact_env * 0.95
    } else {
        // Clamped Hi-Hat pair (choke_group > 0): 6-oscillator inharmonic metallic cluster
        let mut ring = 1.0_f32;
        let mut pulse_sum = 0.0_f32;
        for i in 0..NUM_MODES {
            let pw = 0.28 + 0.06 * (i as f32);
            let pulse = if metallic_phases[i] < pw { 1.0 } else { -1.0 };
            ring *= pulse;
            pulse_sum += pulse;
            metallic_phases[i] = wrap_phase(metallic_phases[i] + metallic_freqs[i] / sample_rate);
        }
        let shimmer = ring * 0.72 + (pulse_sum / NUM_MODES as f32) * 0.28;
        let raw_metal = shimmer * (0.72 + 0.28 * noise_raw);
        let filtered = match noise_filter_mode {
            NoiseFilterMode::Highpass => {
                body_filter.process_hp(raw_metal, noise_cutoff_hz, noise_q, sample_rate)
            }
            NoiseFilterMode::Bandpass => {
                body_filter.process_bp(raw_metal, noise_cutoff_hz, noise_q, sample_rate)
            }
            NoiseFilterMode::Lowpass => {
                body_filter.process_lp(raw_metal, noise_cutoff_hz, noise_q, sample_rate)
            }
        };
        filtered * body_env * 0.85
    }
}
