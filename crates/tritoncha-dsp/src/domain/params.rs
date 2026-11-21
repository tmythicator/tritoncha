//! Strongly-typed domain parameters and clamped value objects for synthesizer and percussion patches.

pub const DEFAULT_SAMPLE_RATE: f32 = 48000.0;

macro_rules! clamped_f32 {
    ($name:ident, $min:expr, $max:expr, $default:expr) => {
        #[derive(Copy, Clone, Debug, PartialEq, PartialOrd)]
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

        impl PartialEq<f32> for $name {
            #[inline(always)]
            fn eq(&self, other: &f32) -> bool {
                self.0 == *other
            }
        }

        impl PartialOrd<f32> for $name {
            #[inline(always)]
            fn partial_cmp(&self, other: &f32) -> Option<std::cmp::Ordering> {
                self.0.partial_cmp(other)
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

        impl From<$name> for f32 {
            #[inline(always)]
            fn from(val: $name) -> Self {
                val.0
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

macro_rules! clamped_u8 {
    ($name:ident, $min:expr, $max:expr, $default:expr) => {
        #[derive(Copy, Clone, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
        pub struct $name(pub(crate) u8);

        impl $name {
            pub const MIN_VAL: u8 = $min;
            pub const MAX_VAL: u8 = $max;

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

        impl std::ops::Deref for $name {
            type Target = u8;
            #[inline(always)]
            fn deref(&self) -> &u8 {
                &self.0
            }
        }

        impl PartialEq<u8> for $name {
            #[inline(always)]
            fn eq(&self, other: &u8) -> bool {
                self.0 == *other
            }
        }

        impl PartialOrd<u8> for $name {
            #[inline(always)]
            fn partial_cmp(&self, other: &u8) -> Option<std::cmp::Ordering> {
                self.0.partial_cmp(other)
            }
        }

        impl From<u8> for $name {
            #[inline(always)]
            fn from(val: u8) -> Self {
                Self::new(val)
            }
        }

        impl From<i32> for $name {
            #[inline(always)]
            fn from(val: i32) -> Self {
                Self::new(val.clamp(Self::MIN_VAL as i32, Self::MAX_VAL as i32) as u8)
            }
        }

        impl From<usize> for $name {
            #[inline(always)]
            fn from(val: usize) -> Self {
                Self::new(val.clamp(Self::MIN_VAL as usize, Self::MAX_VAL as usize) as u8)
            }
        }

        impl From<f32> for $name {
            #[inline(always)]
            fn from(val: f32) -> Self {
                Self::from(val.round() as i32)
            }
        }

        impl From<$name> for u8 {
            #[inline(always)]
            fn from(val: $name) -> Self {
                val.0
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

// Core & Synth Value Objects
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
clamped_f32!(Drive, 0.0, 10.0, 1.0);
clamped_f32!(DrumPitch, 20.0, 1000.0, 48.0);
clamped_f32!(PitchDrop, 0.0, 1000.0, 180.0);
clamped_f32!(DrumVelocity, 0.1, 1.2, 0.9);
clamped_f32!(Bpm, 30.0, 300.0, 168.0);

// Percussion Domain Value Objects
clamped_f32!(PercPitchHz, 20.0, 8000.0, 52.0);
clamped_f32!(PercPitchDrop, 0.0, 1200.0, 0.0);
clamped_f32!(PercPitchDecay, 0.002, 0.500, 0.030);
clamped_f32!(PercDecay, 0.005, 6.0, 0.25);
clamped_f32!(ClickFreqHz, 500.0, 12000.0, 2800.0);
clamped_f32!(BurstSpacing, 0.003, 0.030, 0.011);
clamped_f32!(FmDepth, 0.0, 2.0, 0.0);
clamped_f32!(FmRatio, 0.5, 8.0, 2.5);
clamped_f32!(NoiseDecay, 0.005, 4.0, 0.18);
clamped_f32!(NoiseCutoffHz, 200.0, 16000.0, 3200.0);
clamped_f32!(NoiseQ, 0.05, 0.95, 0.45);
clamped_f32!(ModalSpread, 0.5, 3.0, 1.0);
clamped_f32!(PercDrive, 0.5, 8.0, 1.3);

// Integer Domain Value Objects
clamped_u8!(Polyphony, 1, 16, 1);
clamped_u8!(BurstCount, 1, 6, 1);
clamped_u8!(ChokeGroup, 0, 15, 0);

impl Polyphony {
    pub const MONO: Self = Self(1);
}

impl ChokeGroup {
    pub const NONE: Self = Self(0);

    #[inline(always)]
    pub const fn is_active(self) -> bool {
        self.0 > 0
    }
}
