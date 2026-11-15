//! Modular physical and analog drum voice synthesis models (808, 909, and acoustic kit).

pub mod clap;
pub mod cymbals;
pub mod kick;
pub mod membrane;
pub mod snare;

pub use clap::ClapVoice;
pub use cymbals::*;
pub use kick::{KickParams, KickVoice};
pub use membrane::*;
pub use snare::{SnareParams, SnareVoice};

pub const MIN_AUDIBLE_VELOCITY: f32 = 0.001;
pub const VELOCITY_MIN_CLAMP: f32 = crate::domain::DrumVelocity::MIN.get();
pub const VELOCITY_MAX_CLAMP: f32 = crate::domain::DrumVelocity::MAX.get();

/// Drum acoustic character and synthesis modeling mode.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum DrumMode {
    #[default]
    Analog = 0,
    Natural = 1,
    Idm = 2,
    Industrial = 3,
}

impl From<u8> for DrumMode {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => DrumMode::Natural,
            2 => DrumMode::Idm,
            3 => DrumMode::Industrial,
            _ => DrumMode::Analog,
        }
    }
}

impl From<f32> for DrumMode {
    #[inline(always)]
    fn from(val: f32) -> Self {
        DrumMode::from(val.round() as u8)
    }
}

impl DrumMode {
    #[inline(always)]
    pub fn as_u8(self) -> u8 {
        self as u8
    }
}

// Instrument Identifiers
pub const DRUM_KICK: i32 = 0;
pub const DRUM_SNARE: i32 = 1;
pub const DRUM_HH_CLOSED: i32 = 2;
pub const DRUM_HH_OPEN: i32 = 3;
pub const DRUM_CLAP: i32 = 18;
pub const DRUM_RIDE: i32 = 20;
pub const DRUM_TOM: i32 = 21;
pub const DRUM_SNARE_CRACK: i32 = 22;
pub const DRUM_SNARE_WIRE: i32 = 23;
pub const DRUM_SNARE_BODY: i32 = 24;
pub const DRUM_SNARE_GHOST: i32 = 25;
pub const DRUM_SNARE_RIM: i32 = 26;
pub const DRUM_RIDE_BELL: i32 = 64;
pub const DRUM_TOM_HIGH: i32 = 65;
pub const DRUM_TOM_MID: i32 = 66;
pub const DRUM_TOM_LOW: i32 = 67;
pub const DRUM_CRASH_16: i32 = 68;
pub const DRUM_CRASH_17: i32 = 69;
pub const DRUM_CRASH_18: i32 = 70;
pub const DRUM_SPLASH: i32 = 71;
pub const DRUM_CHINA: i32 = 72;
pub const DRUM_COWBELL: i32 = 73;

/// Strongly-typed drum instrument identifier.
#[repr(i32)]
#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
pub enum DrumId {
    Kick = DRUM_KICK,
    Snare = DRUM_SNARE,
    HhClosed = DRUM_HH_CLOSED,
    HhOpen = DRUM_HH_OPEN,
    Clap = DRUM_CLAP,
    Ride = DRUM_RIDE,
    Tom = DRUM_TOM,
    SnareCrack = DRUM_SNARE_CRACK,
    SnareWire = DRUM_SNARE_WIRE,
    SnareBody = DRUM_SNARE_BODY,
    SnareGhost = DRUM_SNARE_GHOST,
    SnareRim = DRUM_SNARE_RIM,
    RideBell = DRUM_RIDE_BELL,
    TomHigh = DRUM_TOM_HIGH,
    TomMid = DRUM_TOM_MID,
    TomLow = DRUM_TOM_LOW,
    Crash16 = DRUM_CRASH_16,
    Crash17 = DRUM_CRASH_17,
    Crash18 = DRUM_CRASH_18,
    Splash = DRUM_SPLASH,
    China = DRUM_CHINA,
    Cowbell = DRUM_COWBELL,
}

impl DrumId {
    pub const ALL: [DrumId; 22] = [
        DrumId::Kick,
        DrumId::Snare,
        DrumId::HhClosed,
        DrumId::HhOpen,
        DrumId::Clap,
        DrumId::Ride,
        DrumId::Tom,
        DrumId::SnareCrack,
        DrumId::SnareWire,
        DrumId::SnareBody,
        DrumId::SnareGhost,
        DrumId::SnareRim,
        DrumId::RideBell,
        DrumId::TomHigh,
        DrumId::TomMid,
        DrumId::TomLow,
        DrumId::Crash16,
        DrumId::Crash17,
        DrumId::Crash18,
        DrumId::Splash,
        DrumId::China,
        DrumId::Cowbell,
    ];

    #[inline(always)]
    pub const fn as_i32(self) -> i32 {
        self as i32
    }
}

impl TryFrom<i32> for DrumId {
    type Error = ();

    #[inline(always)]
    fn try_from(val: i32) -> Result<Self, Self::Error> {
        match val {
            DRUM_KICK => Ok(Self::Kick),
            DRUM_SNARE => Ok(Self::Snare),
            DRUM_HH_CLOSED => Ok(Self::HhClosed),
            DRUM_HH_OPEN => Ok(Self::HhOpen),
            DRUM_CLAP => Ok(Self::Clap),
            DRUM_RIDE => Ok(Self::Ride),
            DRUM_TOM => Ok(Self::Tom),
            DRUM_SNARE_CRACK => Ok(Self::SnareCrack),
            DRUM_SNARE_WIRE => Ok(Self::SnareWire),
            DRUM_SNARE_BODY => Ok(Self::SnareBody),
            DRUM_SNARE_GHOST => Ok(Self::SnareGhost),
            DRUM_SNARE_RIM => Ok(Self::SnareRim),
            DRUM_RIDE_BELL => Ok(Self::RideBell),
            DRUM_TOM_HIGH => Ok(Self::TomHigh),
            DRUM_TOM_MID => Ok(Self::TomMid),
            DRUM_TOM_LOW => Ok(Self::TomLow),
            DRUM_CRASH_16 => Ok(Self::Crash16),
            DRUM_CRASH_17 => Ok(Self::Crash17),
            DRUM_CRASH_18 => Ok(Self::Crash18),
            DRUM_SPLASH => Ok(Self::Splash),
            DRUM_CHINA => Ok(Self::China),
            DRUM_COWBELL => Ok(Self::Cowbell),
            _ => Err(()),
        }
    }
}

#[inline(always)]
pub fn is_drum_inst(inst_id: i32) -> bool {
    DrumId::try_from(inst_id).is_ok()
}

/// Common trait for all modular drum voices.
pub trait DrumVoice {
    fn is_active(&self) -> bool;
    fn process(&mut self, sample_rate: f32) -> f32;
    fn set_mode(&mut self, mode: DrumMode);
    fn reset(&mut self);
}

macro_rules! impl_drum_voice {
    ($($t:ident $(<$n:ident>)?),* $(,)?) => {
        $(
            impl $(<const $n: usize>)? DrumVoice for $t $(<$n>)? {
                #[inline(always)]
                fn is_active(&self) -> bool {
                    self.active
                }
                #[inline(always)]
                fn process(&mut self, sample_rate: f32) -> f32 {
                    self.process(sample_rate)
                }
                #[inline(always)]
                fn set_mode(&mut self, mode: DrumMode) {
                    self.mode = mode;
                }
                #[inline(always)]
                fn reset(&mut self) {
                    self.reset();
                }
            }
        )*
    };
}

impl_drum_voice!(
    KickVoice,
    SnareVoice,
    NoiseHatVoice,
    ClapVoice,
    MembraneVoice,
    RideVoice,
    RideBellVoice,
    MetallicVoice<N>,
);

#[inline(always)]
fn render_voice<V: DrumVoice>(voice: &mut V, sample_rate: f32) -> f32 {
    if voice.is_active() {
        voice.process(sample_rate)
    } else {
        0.0
    }
}

macro_rules! for_all_drum_voices {
    (@each $self:ident, $v:ident, $body:expr; $($field:ident),+ $(,)?) => {
        $( { let $v = &mut $self.$field; $body; } )+
    };
    ($self:ident, |$v:ident| $body:expr) => {
        for_all_drum_voices!(
            @each $self, $v, $body;
            kick, snare, hat, clap, tom, tom_high, tom_mid, tom_low, ride, ride_bell, crash_16,
            crash_17, crash_18, splash, china, cowbell
        )
    };
}

/// Drum machine managing all drum voices.
pub struct DrumMachine {
    pub kick: KickVoice,
    pub snare: SnareVoice,
    pub hat: NoiseHatVoice,
    pub clap: ClapVoice,
    pub tom: MembraneVoice,
    pub tom_high: MembraneVoice,
    pub tom_mid: MembraneVoice,
    pub tom_low: MembraneVoice,
    pub ride: RideVoice,
    pub ride_bell: RideBellVoice,
    pub crash_16: MetallicVoice<6>,
    pub crash_17: MetallicVoice<6>,
    pub crash_18: MetallicVoice<6>,
    pub splash: MetallicVoice<6>,
    pub china: MetallicVoice<6>,
    pub cowbell: MetallicVoice<2>,
}

impl DrumMachine {
    pub fn new() -> Self {
        Self {
            kick: KickVoice::new(),
            snare: SnareVoice::new(),
            hat: NoiseHatVoice::new(),
            clap: ClapVoice::new(),
            tom: MembraneVoice::new(180.0, 105.0, 0.015, 0.9988, 1.1),
            tom_high: MembraneVoice::new(TOM_HIGH_START_HZ, TOM_HIGH_MIN_HZ, 0.018, 0.9985, 1.15),
            tom_mid: MembraneVoice::new(TOM_MID_START_HZ, TOM_MID_MIN_HZ, 0.016, 0.9987, 1.15),
            tom_low: MembraneVoice::new(TOM_LOW_START_HZ, TOM_LOW_MIN_HZ, 0.014, 0.9990, 1.15),
            ride: RideVoice::new(),
            ride_bell: RideBellVoice::new(),
            crash_16: new_crash_16(),
            crash_17: new_crash_17(),
            crash_18: new_crash_18(),
            splash: new_splash(),
            china: new_china(),
            cowbell: new_cowbell(),
        }
    }

    pub fn set_drum_patch(&mut self, drum: DrumId, params: [f32; 7]) {
        match drum {
            DrumId::Kick => self.kick.set_params(params),
            DrumId::Snare
            | DrumId::SnareCrack
            | DrumId::SnareBody
            | DrumId::SnareWire
            | DrumId::SnareGhost
            | DrumId::SnareRim => self.snare.set_params(params),
            DrumId::HhClosed | DrumId::HhOpen => self.hat.set_params(params),
            DrumId::Tom => self.tom.set_params(params),
            DrumId::TomHigh => self.tom_high.set_params(params),
            DrumId::TomMid => self.tom_mid.set_params(params),
            DrumId::TomLow => self.tom_low.set_params(params),
            DrumId::Ride => self.ride.set_params(params),
            DrumId::RideBell => self.ride_bell.set_params(params),
            DrumId::Crash16 => self.crash_16.set_params(params),
            DrumId::Crash17 => self.crash_17.set_params(params),
            DrumId::Crash18 => self.crash_18.set_params(params),
            DrumId::Splash => self.splash.set_params(params),
            DrumId::China => self.china.set_params(params),
            DrumId::Cowbell => self.cowbell.set_params(params),
            DrumId::Clap => self.clap.set_params(params),
        }
    }

    pub fn set_drum_patch_by_id(&mut self, inst_id: i32, params: [f32; 7]) -> bool {
        if let Ok(drum) = DrumId::try_from(inst_id) {
            self.set_drum_patch(drum, params);
            true
        } else {
            false
        }
    }

    pub fn set_mode_all(&mut self, mode: impl Into<DrumMode>) {
        let m = mode.into();
        for_all_drum_voices!(self, |v| v.set_mode(m));
    }

    /// Dispatches a trigger to the corresponding drum voice by strongly-typed DrumId.
    #[inline(always)]
    pub fn trigger_drum(&mut self, drum: DrumId, vel: f32, freq: f32) {
        match drum {
            DrumId::Kick => self.kick.trigger(vel),
            DrumId::Snare => self.snare.trigger_styled(vel, DRUM_SNARE),
            DrumId::HhClosed => self.hat.trigger(vel, false),
            DrumId::HhOpen => self.hat.trigger(vel, true),
            DrumId::Clap => self.clap.trigger(vel),
            DrumId::Ride => self.ride.trigger(vel),
            DrumId::Tom => self.tom.trigger_freq(vel, freq),
            DrumId::SnareCrack => self.snare.trigger_styled(vel, DRUM_SNARE_CRACK),
            DrumId::SnareWire => self.snare.trigger_styled(vel, DRUM_SNARE_WIRE),
            DrumId::SnareBody => self.snare.trigger_styled(vel, DRUM_SNARE_BODY),
            DrumId::SnareGhost => self.snare.trigger_styled(vel, DRUM_SNARE_GHOST),
            DrumId::SnareRim => self.snare.trigger_styled(vel, DRUM_SNARE_RIM),
            DrumId::RideBell => self.ride_bell.trigger(vel),
            DrumId::TomHigh => self.tom_high.trigger(vel),
            DrumId::TomMid => self.tom_mid.trigger(vel),
            DrumId::TomLow => self.tom_low.trigger(vel),
            DrumId::Crash16 => self.crash_16.trigger(vel),
            DrumId::Crash17 => self.crash_17.trigger(vel),
            DrumId::Crash18 => self.crash_18.trigger(vel),
            DrumId::Splash => self.splash.trigger(vel),
            DrumId::China => self.china.trigger(vel),
            DrumId::Cowbell => self.cowbell.trigger(vel),
        }
    }

    /// Dispatches a trigger to the corresponding drum voice by instrument ID.
    /// Returns true if the ID matched a drum voice, false otherwise.
    #[inline(always)]
    pub fn trigger_by_id(&mut self, inst_id: i32, vel: f32, freq: f32) -> bool {
        if let Ok(drum) = DrumId::try_from(inst_id) {
            self.trigger_drum(drum, vel, freq);
            true
        } else {
            false
        }
    }

    #[inline(always)]
    pub fn process_sample(&mut self, sample_rate: f32) -> f32 {
        self.process(sample_rate)
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        let mut sum = 0.0;
        for_all_drum_voices!(self, |v| sum += render_voice(v, sample_rate));
        sum
    }

    pub fn reset(&mut self) {
        for_all_drum_voices!(self, |v| v.reset());
    }
}

impl Default for DrumMachine {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::domain::DEFAULT_SAMPLE_RATE;

    #[test]
    fn test_membrane_drum_sweep_and_decay() {
        let mut kick = MembraneVoice::new(140.0, 48.0, 0.05, 0.99, 1.0);
        assert!(!kick.active);

        kick.trigger(1.0);
        assert!(kick.active);

        let initial_sample = kick.process(DEFAULT_SAMPLE_RATE);
        assert!(initial_sample.abs() < 2.0);

        for _ in 0..5000 {
            kick.process(DEFAULT_SAMPLE_RATE);
        }
        assert!(!kick.active);
    }

    #[test]
    fn test_kick_voice_punch_and_decay() {
        let mut kick = KickVoice::new();
        assert!(!kick.active);
        kick.trigger(1.0);
        assert!(kick.active);
        let s0 = kick.process(DEFAULT_SAMPLE_RATE);
        assert!(s0.abs() > 0.05, "Immediate punch presence on sample 0");
        for _ in 0..20000 {
            kick.process(DEFAULT_SAMPLE_RATE);
        }
        assert!(!kick.active, "Kick must decay cleanly to silence");
    }

    #[test]
    fn test_snare_artistic_styles() {
        let mut snare = SnareVoice::new();
        snare.trigger_styled(1.0, DRUM_SNARE_CRACK);
        assert!(snare.active);
        let s = snare.process(DEFAULT_SAMPLE_RATE);
        assert!(!s.is_nan());
    }

    #[test]
    fn test_drum_machine_full_dispatch() {
        let mut dm = DrumMachine::new();
        for &drum in &DrumId::ALL {
            dm.trigger_drum(drum, 0.9, 440.0);
            let sample = dm.process(DEFAULT_SAMPLE_RATE);
            assert!(!sample.is_nan());
        }
    }

    #[test]
    fn test_physical_ride_and_bell_modal_synthesis() {
        let mut ride = RideVoice::new();
        let mut bell = RideBellVoice::new();

        assert!(!ride.active);
        assert!(!bell.active);

        ride.trigger(0.9);
        bell.trigger(0.85);

        assert!(ride.active);
        assert!(bell.active);

        for _ in 0..1000 {
            let s_ride = ride.process(DEFAULT_SAMPLE_RATE);
            let s_bell = bell.process(DEFAULT_SAMPLE_RATE);
            assert!(!s_ride.is_nan());
            assert!(!s_bell.is_nan());
            assert!(s_ride.abs() < 4.0);
            assert!(s_bell.abs() < 4.0);
        }
    }

    #[test]
    fn test_drum_patch_clamping_and_dispatch() {
        let mut dm = DrumMachine::new();

        let raw_kick = [5000.0, 9999.0, 5.0, 10.0, 20.0, 50.0, 2.0];
        assert_eq!(
            KickParams::from(raw_kick),
            KickParams {
                base_pitch: 1000.0.into(),
                pitch_drop: 1000.0.into(),
                pitch_decay: 0.150.into(),
                decay: 10.0.into(),
                click_level: 10.0.into(),
                drive: 10.0.into(),
                mode: DrumMode::Idm,
            }
        );

        let raw_snare = [10.0, 5.0, 5.0, 50000.0, 20.0, 0.0, 3.0];
        assert_eq!(
            SnareParams::from(raw_snare),
            SnareParams {
                base_freq: 20.0.into(),
                tone_decay: 1.0.into(),
                noise_decay: 1.0.into(),
                cutoff: 20000.0.into(),
                snappy: 10.0.into(),
                mode: DrumMode::Industrial,
            }
        );

        let raw_perc = [50000.0, 2.0, 50.0, 50.0, 0.0, 0.0, 0.0];
        assert_eq!(
            PercussionParams::from(raw_perc),
            PercussionParams {
                cutoff: 20000.0.into(),
                resonance: 0.98.into(),
                decay: 10.0.into(),
                drive: 10.0.into(),
                mode: DrumMode::Analog,
            }
        );

        for &drum in &DrumId::ALL {
            dm.reset();
            dm.set_drum_patch(drum, [50000.0, 9999.0, 50.0, 50.0, 50.0, 50.0, 3.0]);
            dm.trigger_drum(drum, 1.0, 440.0);
            let s = dm.process(DEFAULT_SAMPLE_RATE);
            assert!(!s.is_nan() && s.abs() < 5.0);
        }
    }
}
