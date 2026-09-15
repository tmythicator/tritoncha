//! Anti-aliased bandlimited and physical oscillator waveform generators.

use crate::core::math::{poly_blep, sin_phase};

pub const MIN_PULSE_WIDTH: f32 = 0.05;
pub const MAX_PULSE_WIDTH: f32 = 0.95;

pub const SUPERSAW_DETUNE_OFFSETS: [f32; 7] = [-0.012, -0.007, -0.003, 0.0, 0.003, 0.007, 0.012];
pub const SUPERSAW_NORMALIZATION: f32 = 0.25;

pub const ORGAN_H1_GAIN: f32 = 1.0;
pub const ORGAN_H2_GAIN: f32 = 0.5;
pub const ORGAN_H3_GAIN: f32 = 0.25;
pub const ORGAN_H4_GAIN: f32 = 0.125;
pub const ORGAN_NORMALIZATION: f32 = 0.6;

pub const CHIPTUNE_MIN_PW: f32 = 0.1;
pub const CHIPTUNE_MAX_PW: f32 = 0.9;
pub const CHIPTUNE_AMPLITUDE: f32 = 0.8;

pub const FM_MOD_RATIO: f32 = 2.0;
pub const FM_INDEX: f32 = 2.8;

pub const REESE_DETUNE_DOWN: f32 = 0.992;
pub const REESE_DETUNE_UP: f32 = 1.008;
pub const REESE_PHASE_SPREAD: f32 = 1.016;
pub const REESE_GAIN: f32 = 0.5;

pub const BLADE_PHASE_SPREAD: f32 = 1.004;
pub const CS80_BLADE_GAIN: f32 = 0.45;

pub const HOOVER_PWM_CENTER: f32 = 0.5;
pub const HOOVER_PWM_DEPTH: f32 = 0.3;
pub const HOOVER_PWM_LFO_MULT: f32 = 2.0;
pub const HOOVER_PULSE_AMP: f32 = 0.7;
pub const HOOVER_SUB_AMP: f32 = 0.4;
pub const HOOVER_PULSE_GAIN: f32 = 0.7;
pub const HOOVER_SUB_GAIN: f32 = 0.3;

pub const CLICK_SINE_WEIGHT: f32 = 0.85;
pub const CLICK_PULSE_WEIGHT: f32 = 0.35;
pub const CLICK_REF_FREQ_HZ: f32 = 1600.0;
pub const CLICK_MIN_SCALE: f32 = 0.6;
pub const CLICK_MAX_SCALE: f32 = 2.0;

/// Generates a bandlimited PolyBLEP sawtooth waveform.
#[inline(always)]
pub fn render_saw(phase: f32, dt: f32) -> f32 {
    2.0 * phase - 1.0 - poly_blep(phase, dt)
}

/// Generates a bandlimited PolyBLEP pulse/square waveform with variable duty cycle.
#[inline(always)]
pub fn render_pulse(phase: f32, dt: f32, pulse_width: f32) -> f32 {
    let pw = pulse_width.clamp(MIN_PULSE_WIDTH, MAX_PULSE_WIDTH);
    let raw = if phase < pw { 1.0 } else { -1.0 };
    raw - poly_blep(phase, dt) + poly_blep((phase + (1.0 - pw)) % 1.0, dt)
}

/// Generates a pure triangle waveform.
#[inline(always)]
pub fn render_triangle(phase: f32) -> f32 {
    2.0 * (2.0 * (phase - 0.5).abs() - 0.5)
}

/// Generates a pure sine waveform.
#[inline(always)]
pub fn render_sine(phase: f32) -> f32 {
    sin_phase(phase)
}

/// Generates a 7-voice detuned supersaw waveform.
#[inline(always)]
pub fn render_supersaw(phase: f32, dt: f32) -> f32 {
    let mut sum = 0.0;
    for &d in &SUPERSAW_DETUNE_OFFSETS {
        let p = (phase * (1.0 + d)).fract();
        sum += 2.0 * p - 1.0 - poly_blep(p, dt * (1.0 + d));
    }
    sum * SUPERSAW_NORMALIZATION
}

/// Generates a Hammond-style additive drawbar organ tone.
#[inline(always)]
pub fn render_organ(phase: f32) -> f32 {
    let h1 = sin_phase(phase) * ORGAN_H1_GAIN;
    let h2 = sin_phase(phase * 2.0) * ORGAN_H2_GAIN;
    let h3 = sin_phase(phase * 3.0) * ORGAN_H3_GAIN;
    let h4 = sin_phase(phase * 4.0) * ORGAN_H4_GAIN;
    (h1 + h2 + h3 + h4) * ORGAN_NORMALIZATION
}

/// Generates an 8-bit chiptune square waveform.
#[inline(always)]
pub fn render_chiptune(phase: f32, pulse_width: f32) -> f32 {
    let pw = pulse_width.clamp(CHIPTUNE_MIN_PW, CHIPTUNE_MAX_PW);
    if phase < pw {
        CHIPTUNE_AMPLITUDE
    } else {
        -CHIPTUNE_AMPLITUDE
    }
}

/// Generates a 2-operator FM synth waveform.
#[inline(always)]
pub fn render_fm(phase: f32, mod_phase: f32, env_level: f32) -> f32 {
    let mod_val = sin_phase(mod_phase) * FM_INDEX * env_level;
    sin_phase(phase + (mod_val / std::f32::consts::TAU))
}

/// Generates a detuned Reese bass dual-sawtooth waveform.
#[inline(always)]
pub fn render_reese(phase: f32, dt: f32) -> f32 {
    let dt1 = dt * REESE_DETUNE_DOWN;
    let dt2 = dt * REESE_DETUNE_UP;
    let saw1 = 2.0 * phase - 1.0 - poly_blep(phase, dt1);
    let p2 = (phase * REESE_PHASE_SPREAD) % 1.0;
    let saw2 = 2.0 * p2 - 1.0 - poly_blep(p2, dt2);
    saw1 * REESE_GAIN + saw2 * REESE_GAIN
}

/// Generates a Yamaha CS-80 Blade Runner brass sawtooth pair.
#[inline(always)]
pub fn render_blade(phase: f32, dt: f32) -> f32 {
    let saw1 = 2.0 * phase - 1.0 - poly_blep(phase, dt);
    let p2 = (phase * BLADE_PHASE_SPREAD) % 1.0;
    let saw2 = 2.0 * p2 - 1.0 - poly_blep(p2, dt * BLADE_PHASE_SPREAD);
    (saw1 + saw2) * CS80_BLADE_GAIN
}

/// Generates a Roland Alpha Juno Hoover rave lead with PWM and sub oscillator.
#[inline(always)]
pub fn render_hoover(phase: f32, sub_phase: f32) -> f32 {
    let pwm = HOOVER_PWM_CENTER + HOOVER_PWM_DEPTH * sin_phase(phase * HOOVER_PWM_LFO_MULT);
    let pulse = if phase < pwm {
        HOOVER_PULSE_AMP
    } else {
        -HOOVER_PULSE_AMP
    };
    let sub = if sub_phase < 0.5 {
        HOOVER_SUB_AMP
    } else {
        -HOOVER_SUB_AMP
    };
    pulse * HOOVER_PULSE_GAIN + sub * HOOVER_SUB_GAIN
}

/// Generates a metronome click impulse combining high-frequency sine with square burst.
#[inline(always)]
pub fn render_click(phase: f32, freq: f32) -> f32 {
    let click_pulse = if phase < 0.5 { 1.0 } else { -1.0 };
    let click_sine = sin_phase(phase);
    let freq_scale = (freq / CLICK_REF_FREQ_HZ).clamp(CLICK_MIN_SCALE, CLICK_MAX_SCALE);
    (click_sine * CLICK_SINE_WEIGHT + click_pulse * CLICK_PULSE_WEIGHT) * freq_scale
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_render_waveforms_bounded() {
        let dt = 440.0 / 48000.0;
        let phase = 0.25;

        assert!((-1.2..=1.2).contains(&render_saw(phase, dt)));
        assert!((-1.2..=1.2).contains(&render_pulse(phase, dt, 0.5)));
        assert!((-1.1..=1.1).contains(&render_triangle(phase)));
        assert!((-1.1..=1.1).contains(&render_sine(phase)));
        assert!((-1.2..=1.2).contains(&render_supersaw(phase, dt)));
        assert!((-1.2..=1.2).contains(&render_organ(phase)));
        assert!((-1.2..=1.2).contains(&render_chiptune(phase, 0.5)));
        assert!((-1.2..=1.2).contains(&render_fm(phase, 0.1, 0.9)));
        assert!((-1.2..=1.2).contains(&render_reese(phase, dt)));
        assert!((-1.2..=1.2).contains(&render_blade(phase, dt)));
        assert!((-1.2..=1.2).contains(&render_hoover(phase, 0.1)));
        assert!((-2.5..=2.5).contains(&render_click(phase, 1600.0)));
    }
}
