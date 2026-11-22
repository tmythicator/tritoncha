//! Unified Bronze Cymbal synthesis:
//! - Clamped Hi-Hat (`choke_group > 0`): 6-oscillator pulse ring cluster + SVF filter.
//! - Open Bronze Disc (`choke_group == 0`): smooth crossfade (`noise_mix` 0.25 -> 0.75)
//!   between Chladni bronze plate modes (bell/bow) and a 4-line Householder FDN diffuser (crash).

use crate::core::math::{sin_phase, wrap_phase};
use crate::domain::drums::bodies::{ResonatorBank, NUM_MODES};
use crate::domain::drums::patch::{ChokeGroup, NoiseFilterSpec};
use crate::domain::drums::MIN_AUDIBLE_VELOCITY;

const BRONZE_PLATE_RATIOS: [f32; NUM_MODES] = [1.0, 1.503, 2.021, 2.756, 3.412, 4.187];
const BRONZE_PLATE_GAINS: [f32; NUM_MODES] = [0.42, 0.36, 0.32, 0.26, 0.20, 0.16];
const BRONZE_PLATE_DECAY_SCALES: [f32; NUM_MODES] = [1.0, 0.84, 0.68, 0.52, 0.38, 0.26];
const CYMBAL_SHIMMER_RATIOS: [f32; NUM_MODES] = [1.0, 1.447, 1.617, 1.926, 2.502, 2.917];

const CRASH_BLEND_START: f32 = 0.25;
const CRASH_BLEND_SPAN: f32 = 0.50;
const BLEND_EPSILON: f32 = 0.001;

const HAT_BASE_PW: f32 = 0.28;
const HAT_PW_STEP: f32 = 0.06;
const HAT_RING_MIX: f32 = 0.72;
const HAT_OUTPUT_GAIN: f32 = 0.42;

const PLATE_EXCITER_GAIN: f32 = 0.55;
const CRASH_FM_INDEX: f32 = 0.45;
const CRASH_NOISE_FM: f32 = 0.16;
const CRASH_FDN_MIX: f32 = 1.15;
const CRASH_RING_MIX: f32 = 0.25;
const OPEN_CYMBAL_HP_Q: f32 = 0.20;
const OPEN_CYMBAL_HP_MIN_HZ: f32 = 320.0;
const OPEN_CYMBAL_HP_MAX_HZ: f32 = 1400.0;

#[derive(Copy, Clone, Debug)]
pub struct CymbalStrikeSpec {
    pub base_pitch_hz: f32,
    pub noise_mix: f32,
    pub filter: NoiseFilterSpec,
    pub choke_group: ChokeGroup,
}

impl CymbalStrikeSpec {
    #[inline(always)]
    pub const fn is_clamped_hat(&self) -> bool {
        self.choke_group.is_active()
    }

    /// Smooth crossfade weight `0.0..1.0` from Bell/Bow ping (`0.0`) to Edge Crash (`1.0`).
    #[inline(always)]
    pub fn crash_blend(&self) -> f32 {
        ((self.noise_mix - CRASH_BLEND_START) / CRASH_BLEND_SPAN).clamp(0.0, 1.0)
    }
}

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
    let shimmer_scale = if clamped_hat {
        1.0
    } else {
        1.65 + 0.35 * (spread - 1.45).max(0.0)
    };
    let nyquist_limit = sample_rate * 0.45;

    for i in 0..NUM_MODES {
        let mode_freq = eff_pitch_hz * (1.0 + (BRONZE_PLATE_RATIOS[i] - 1.0) * plate_scale);
        let mode_decay = eff_decay_s * BRONZE_PLATE_DECAY_SCALES[i];
        bank.modes[i].configure(mode_freq, mode_decay, BRONZE_PLATE_GAINS[i], sample_rate);

        let metal_freq =
            eff_pitch_hz * shimmer_scale * (1.0 + (CYMBAL_SHIMMER_RATIOS[i] - 1.0) * spread);
        bank.metallic_freqs[i] = metal_freq.clamp(40.0, nyquist_limit);
    }

    if !clamped_hat {
        bank.plate_fdn
            .configure(eff_pitch_hz, eff_decay_s, spread, sample_rate);
    }
}

#[inline(always)]
fn render_clamped_hat(
    spec: &CymbalStrikeSpec,
    bank: &mut ResonatorBank,
    body_env: f32,
    noise_raw: f32,
    sample_rate: f32,
) -> f32 {
    let mut ring = 1.0_f32;
    let mut pulse_sum = 0.0_f32;
    for i in 0..NUM_MODES {
        let pw = HAT_BASE_PW + HAT_PW_STEP * (i as f32);
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
    let shimmer = ring * HAT_RING_MIX + (pulse_sum / NUM_MODES as f32) * (1.0 - HAT_RING_MIX);
    let raw_metal = shimmer * (HAT_RING_MIX + (1.0 - HAT_RING_MIX) * noise_raw);
    spec.filter
        .apply(&mut bank.body_filter, raw_metal, sample_rate)
        * body_env
        * HAT_OUTPUT_GAIN
}

#[inline(always)]
fn render_bronze_crash(
    bank: &mut ResonatorBank,
    body_env: f32,
    exc_level: f32,
    noise_raw: f32,
    sample_rate: f32,
) -> f32 {
    let mut s = [0.0_f32; NUM_MODES];
    for i in 0..NUM_MODES {
        let fm = if i > 0 {
            s[i - 1] * CRASH_FM_INDEX
        } else {
            0.0
        };
        s[i] = sin_phase(bank.metallic_phases[i] + fm + noise_raw * CRASH_NOISE_FM);
        bank.metallic_phases[i] =
            wrap_phase(bank.metallic_phases[i] + bank.metallic_freqs[i] / sample_rate);
    }
    let bronze_ring = (s[0] * s[1] + s[2] * s[3] + s[4] * s[5]) * 0.45
        + (s[0] + s[1] + s[2] + s[3] + s[4] + s[5]) * 0.08;
    let fdn_in =
        (bronze_ring * 0.70 + noise_raw * 0.55) * exc_level + bronze_ring * body_env * 0.08;
    let plate_wash = bank.plate_fdn.process(fdn_in);
    plate_wash * CRASH_FDN_MIX + bronze_ring * body_env * CRASH_RING_MIX
}

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
        return render_clamped_hat(spec, bank, body_env, noise_raw, sample_rate);
    }

    let crash_w = spec.crash_blend();
    let plate_w = 1.0 - crash_w;
    let exc_level = *exciter_env;
    let exc = if exc_level > MIN_AUDIBLE_VELOCITY {
        *exciter_env *= exciter_decay;
        noise_raw * exc_level
    } else {
        0.0
    };

    let plate_sig = if plate_w > BLEND_EPSILON {
        let plate_exc = exc * PLATE_EXCITER_GAIN;
        let sum: f32 = bank.modes.iter_mut().map(|m| m.process(plate_exc)).sum();
        sum * plate_w
    } else {
        0.0
    };

    let crash_sig = if crash_w > BLEND_EPSILON {
        render_bronze_crash(bank, body_env, exc_level, noise_raw, sample_rate) * crash_w
    } else {
        0.0
    };

    let hp_hz = (spec.base_pitch_hz * (0.85 + 0.55 * crash_w))
        .clamp(OPEN_CYMBAL_HP_MIN_HZ, OPEN_CYMBAL_HP_MAX_HZ);
    bank.body_filter
        .process_hp(plate_sig + crash_sig, hp_hz, OPEN_CYMBAL_HP_Q, sample_rate)
}
