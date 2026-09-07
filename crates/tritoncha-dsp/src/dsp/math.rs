//! Pure mathematical primitives and DSP utilities.

pub const STANDARD_TUNING_A4_HZ: f32 = 440.0;
pub const MIDI_NOTE_A4: f32 = 69.0;
pub const SEMITONES_PER_OCTAVE: f32 = 12.0;

/// Linear interpolation between `a` and `b` by factor `t`.
#[inline(always)]
pub fn lerp(a: f32, b: f32, t: f32) -> f32 {
    a + t * (b - a)
}

/// Converts decibels to a linear gain factor.
#[inline(always)]
pub fn db_to_gain(db: f32) -> f32 {
    10.0_f32.powf(db / 20.0)
}

/// Converts a linear gain factor to decibels.
#[inline(always)]
pub fn gain_to_db(gain: f32) -> f32 {
    if gain <= 0.00001 {
        -100.0
    } else {
        20.0 * gain.log10()
    }
}

/// Calculates the normalized phase increment per sample [0.0, 1.0)
/// for a given frequency and sample rate.
#[inline(always)]
pub fn freq_to_phase_inc(freq_hz: f32, sample_rate: f32) -> f32 {
    if sample_rate <= 0.0 {
        0.0
    } else {
        (freq_hz / sample_rate).clamp(0.0, 0.499)
    }
}

/// Wraps an accumulated phase into the normalized interval [0.0, 1.0).
#[inline(always)]
pub fn wrap_phase(phase: f32) -> f32 {
    phase.fract().abs()
}

/// Fast Padé approximation of hyperbolic tangent (tanh)
/// for zero-latency analog overdrive and soft-clipping without libm transcendental overhead.
#[inline(always)]
pub fn tanh_approx(x: f32) -> f32 {
    if x < -3.0 {
        -1.0
    } else if x > 3.0 {
        1.0
    } else {
        let x2 = x * x;
        x * (27.0 + x2) / (27.0 + 9.0 * x2)
    }
}

/// Fast polynomial soft-clipping analog saturation curve.
#[inline(always)]
pub fn soft_clip(x: f32) -> f32 {
    tanh_approx(x)
}

/// PolyBLEP residual for bandlimited anti-aliasing step discontinuities.
#[inline(always)]
pub fn poly_blep(t: f32, dt: f32) -> f32 {
    if t < dt {
        let t_div = t / dt;
        t_div + t_div - t_div * t_div - 1.0
    } else if t > 1.0 - dt {
        let t_div = (t - 1.0) / dt;
        t_div * t_div + t_div + t_div + 1.0
    } else {
        0.0
    }
}

/// Converts standard MIDI pitch (0..127) to frequency in Hertz.
#[inline(always)]
pub fn midi_to_freq(midi_pitch: f32) -> f32 {
    let exponent = (midi_pitch - MIDI_NOTE_A4) / SEMITONES_PER_OCTAVE;
    STANDARD_TUNING_A4_HZ * 2.0_f32.powf(exponent)
}

/// Fast pseudo-random 32-bit xorshift generator producing a uniform float in [-1.0, 1.0].
#[inline(always)]
pub fn xorshift32_norm(seed: &mut u32) -> f32 {
    let mut x = *seed;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    *seed = x;
    (x as f32 / 2147483648.0) - 1.0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_lerp() {
        assert_eq!(lerp(10.0, 20.0, 0.0), 10.0);
        assert_eq!(lerp(10.0, 20.0, 1.0), 20.0);
        assert_eq!(lerp(10.0, 20.0, 0.5), 15.0);
    }

    #[test]
    fn test_db_gain_roundtrip() {
        assert!((db_to_gain(0.0) - 1.0).abs() < 0.001);
        assert!((db_to_gain(6.02) - 2.0).abs() < 0.01);
        assert!((db_to_gain(-6.02) - 0.5).abs() < 0.01);
        assert!((gain_to_db(1.0) - 0.0).abs() < 0.001);
        assert!((gain_to_db(2.0) - 6.02).abs() < 0.01);
    }

    #[test]
    fn test_freq_to_phase_inc() {
        let inc = freq_to_phase_inc(440.0, 44100.0);
        assert!((inc - (440.0 / 44100.0)).abs() < 0.00001);
    }

    #[test]
    fn test_midi_to_freq() {
        assert!((midi_to_freq(69.0) - 440.0).abs() < 0.001); // A4
        assert!((midi_to_freq(60.0) - 261.626).abs() < 0.01); // Middle C (C4)
        assert!((midi_to_freq(57.0) - 220.0).abs() < 0.001); // A3
    }

    #[test]
    fn test_tanh_approx_clamping() {
        assert_eq!(tanh_approx(5.0), 1.0);
        assert_eq!(tanh_approx(-5.0), -1.0);
        assert_eq!(tanh_approx(0.0), 0.0);
    }

    #[test]
    fn test_xorshift32_norm() {
        let mut seed = 0x12345678;
        for _ in 0..100 {
            let n = xorshift32_norm(&mut seed);
            assert!((-1.0..=1.0).contains(&n));
        }
    }
}
