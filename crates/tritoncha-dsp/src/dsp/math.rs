/// PolyBLEP residual for bandlimited anti-aliasing step discontinuities
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

/// Fast polynomial soft-clipping analog saturation
#[inline(always)]
pub fn soft_clip(x: f32) -> f32 {
    if x < -3.0 {
        -1.0
    } else if x > 3.0 {
        1.0
    } else {
        x * (27.0 + x * x) / (27.0 + 9.0 * x * x)
    }
}

/// Converts standard MIDI pitch (0..127) to frequency in Hertz
#[inline(always)]
pub fn midi_to_freq(m: f32) -> f32 {
    440.0 * 2.0_f32.powf((m - 69.0) / 12.0)
}
