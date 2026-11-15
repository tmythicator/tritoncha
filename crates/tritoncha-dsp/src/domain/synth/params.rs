//! Strongly-typed domain parameters and clamped value types for synthesizer patches.

macro_rules! clamped_f32 {
    ($name:ident, $min:expr, $max:expr, $default:expr) => {
        #[derive(Copy, Clone, Debug, PartialEq)]
        pub struct $name(pub(crate) f32);

        impl $name {
            pub const MIN: Self = Self($min);
            pub const MAX: Self = Self($max);

            #[inline(always)]
            pub const fn from_clamped(val: f32) -> Self {
                Self(val)
            }

            #[inline(always)]
            pub fn new(val: f32) -> Self {
                Self(val.clamp($min, $max))
            }

            #[inline(always)]
            pub const fn get(self) -> f32 {
                self.0
            }
        }

        impl std::ops::Deref for $name {
            type Target = f32;
            #[inline(always)]
            fn deref(&self) -> &f32 {
                &self.0
            }
        }

        impl From<f32> for $name {
            #[inline(always)]
            fn from(val: f32) -> Self {
                Self::new(val)
            }
        }

        impl From<f64> for $name {
            #[inline(always)]
            fn from(val: f64) -> Self {
                Self::new(val as f32)
            }
        }

        impl Default for $name {
            #[inline(always)]
            fn default() -> Self {
                Self($default)
            }
        }
    };
}

clamped_f32!(Normalized, 0.0, 1.0, 0.0);
clamped_f32!(PulseWidth, 0.05, 0.95, 0.5);
clamped_f32!(CutoffHz, 20.0, 20000.0, 1000.0);
clamped_f32!(CutoffEnvAmt, -20000.0, 20000.0, 0.0);
clamped_f32!(CutoffKeyTrack, 0.0, 4.0, 0.0);
clamped_f32!(Resonance, 0.0, 0.98, 0.0);
clamped_f32!(AdsrTime, 0.001, 10.0, 0.01);
clamped_f32!(GlideTime, 0.0, 2.0, 0.0);
clamped_f32!(PitchSnap, 0.0, 48.0, 0.0);
clamped_f32!(PitchSnapDecay, 0.002, 0.150, 0.015);

#[derive(Copy, Clone, Debug, PartialEq, Eq)]
pub struct Polyphony(pub(crate) u8);

impl Polyphony {
    pub const MIN_VAL: u8 = 1;
    pub const MAX_VAL: u8 = 16;
    pub const MONO: Self = Self(1);

    #[inline(always)]
    pub const fn from_clamped(val: u8) -> Self {
        Self(val)
    }

    #[inline(always)]
    pub fn new(val: u8) -> Self {
        Self(val.clamp(Self::MIN_VAL, Self::MAX_VAL))
    }

    #[inline(always)]
    pub const fn get(self) -> u8 {
        self.0
    }

    #[inline(always)]
    pub const fn as_u8(self) -> u8 {
        self.0
    }
}

impl std::ops::Deref for Polyphony {
    type Target = u8;
    #[inline(always)]
    fn deref(&self) -> &u8 {
        &self.0
    }
}

impl From<u8> for Polyphony {
    #[inline(always)]
    fn from(val: u8) -> Self {
        Self::new(val)
    }
}

impl From<i32> for Polyphony {
    #[inline(always)]
    fn from(val: i32) -> Self {
        Self::new(val.clamp(Self::MIN_VAL as i32, Self::MAX_VAL as i32) as u8)
    }
}

impl From<usize> for Polyphony {
    #[inline(always)]
    fn from(val: usize) -> Self {
        Self::new(val.clamp(Self::MIN_VAL as usize, Self::MAX_VAL as usize) as u8)
    }
}

impl Default for Polyphony {
    #[inline(always)]
    fn default() -> Self {
        Self(1)
    }
}
