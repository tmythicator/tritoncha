//! Cymbals and metallic percussion synthesis modules (ride, bell, crashes, splash, china, cowbell, hats).

pub mod bell;
pub mod hats;
pub mod metallic;
pub mod ride;

pub use bell::RideBellVoice;
pub use hats::NoiseHatVoice;
pub use metallic::MetallicVoice;
pub use ride::RideVoice;

// Inharmonic metallic frequency banks for cymbals and percussion (B20 bronze modal models)
pub const RIDE_FREQS: [f32; 6] = [310.0, 437.0, 663.0, 915.0, 1380.0, 2110.0];
pub const RIDE_BELL_FREQS: [f32; 6] = [2080.0, 2095.0, 3280.0, 4650.0, 6820.0, 9450.0];
pub const CRASH_16_FREQS: [f32; 6] = [240.0, 360.0, 560.0, 890.0, 1420.0, 2260.0];
pub const CRASH_17_FREQS: [f32; 6] = [215.0, 325.0, 505.0, 810.0, 1290.0, 2050.0];
pub const CRASH_18_FREQS: [f32; 6] = [190.0, 290.0, 450.0, 720.0, 1150.0, 1840.0];
pub const SPLASH_FREQS: [f32; 6] = [480.0, 740.0, 1160.0, 1780.0, 2750.0, 4120.0];
pub const CHINA_FREQS: [f32; 6] = [230.0, 310.0, 440.0, 610.0, 890.0, 1340.0];
pub const COWBELL_FREQS: [f32; 2] = [587.0, 845.0];

// Cymbal filter cutoffs
pub const CRASH_16_CUTOFF_HZ: f32 = 1900.0;
pub const CRASH_17_CUTOFF_HZ: f32 = 1650.0;
pub const CRASH_18_CUTOFF_HZ: f32 = 1400.0;
pub const SPLASH_CUTOFF_HZ: f32 = 3200.0;
pub const CHINA_CUTOFF_HZ: f32 = 1800.0;
pub const COWBELL_BANDPASS_HZ: f32 = 820.0;
