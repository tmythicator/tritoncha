//! Modular physical and analog drum voice synthesis models (808, 909, and acoustic kit).

pub mod clap;
pub mod cymbals;
pub mod kick;
pub mod membrane;
pub mod snare;

pub use clap::ClapVoice;
pub use cymbals::*;
pub use kick::KickVoice;
pub use membrane::*;
pub use snare::SnareVoice;

pub const MIN_AUDIBLE_VELOCITY: f32 = 0.001;
pub const VELOCITY_MIN_CLAMP: f32 = 0.1;
pub const VELOCITY_MAX_CLAMP: f32 = 1.2;

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

/// Drum Machine Aggregate Root managing all drum voices.
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
            crash_16: MetallicVoice::new(
                CRASH_16_FREQS,
                CRASH_16_CUTOFF_HZ,
                0.20,
                0.99987,
                false,
                1.05,
                0.74,
            ),
            crash_17: MetallicVoice::new(
                CRASH_17_FREQS,
                CRASH_17_CUTOFF_HZ,
                0.20,
                0.99989,
                false,
                1.05,
                0.76,
            ),
            crash_18: MetallicVoice::new(
                CRASH_18_FREQS,
                CRASH_18_CUTOFF_HZ,
                0.20,
                0.99991,
                false,
                1.05,
                0.78,
            ),
            splash: MetallicVoice::new(
                SPLASH_FREQS,
                SPLASH_CUTOFF_HZ,
                0.25,
                0.99940,
                false,
                0.95,
                0.82,
            ),
            china: MetallicVoice::new(
                CHINA_FREQS,
                CHINA_CUTOFF_HZ,
                0.45,
                0.99984,
                false,
                1.35,
                0.80,
            ),
            cowbell: MetallicVoice::new(
                COWBELL_FREQS,
                COWBELL_BANDPASS_HZ,
                0.85,
                0.9985,
                true,
                1.4,
                0.0,
            ),
        }
    }

    pub fn trigger(&mut self, inst_id: i32, vel: f32, sample_rate: f32) {
        match inst_id {
            DRUM_KICK => self.kick.trigger(vel, sample_rate),
            DRUM_SNARE => self.snare.trigger_styled(vel, DRUM_SNARE),
            DRUM_SNARE_CRACK | DRUM_SNARE_WIRE | DRUM_SNARE_BODY | DRUM_SNARE_GHOST
            | DRUM_SNARE_RIM => self.snare.trigger_styled(vel, inst_id),
            DRUM_HH_CLOSED => self.hat.trigger(vel, false),
            DRUM_HH_OPEN => self.hat.trigger(vel, true),
            DRUM_CLAP => self.clap.trigger(vel),
            DRUM_TOM => self.tom.trigger(vel, sample_rate),
            DRUM_TOM_HIGH => self.tom_high.trigger(vel, sample_rate),
            DRUM_TOM_MID => self.tom_mid.trigger(vel, sample_rate),
            DRUM_TOM_LOW => self.tom_low.trigger(vel, sample_rate),
            DRUM_RIDE => self.ride.trigger(vel),
            DRUM_RIDE_BELL => self.ride_bell.trigger(vel),
            DRUM_CRASH_16 => self.crash_16.trigger(vel),
            DRUM_CRASH_17 => self.crash_17.trigger(vel),
            DRUM_CRASH_18 => self.crash_18.trigger(vel),
            DRUM_SPLASH => self.splash.trigger(vel),
            DRUM_CHINA => self.china.trigger(vel),
            DRUM_COWBELL => self.cowbell.trigger(vel),
            _ => {}
        }
    }

    pub fn set_drum_patch(&mut self, drum_id: i32, params: [f32; 7]) {
        match drum_id {
            DRUM_KICK => self.kick.set_params(
                params[0], params[1], params[2], params[3], params[4], params[5], params[6],
            ),
            DRUM_SNARE | DRUM_SNARE_CRACK | DRUM_SNARE_BODY | DRUM_SNARE_WIRE
            | DRUM_SNARE_GHOST => {
                self.snare.set_params(
                    params[0], params[1], params[2], params[3], params[4], params[6],
                );
            }
            DRUM_HH_CLOSED | DRUM_HH_OPEN => {
                self.hat
                    .set_params(params[0], params[1], params[2], params[6]);
            }
            DRUM_TOM => self.tom.set_params(
                params[0], params[1], params[2], params[3], params[4], params[6],
            ),
            DRUM_TOM_HIGH => self.tom_high.set_params(
                params[0], params[1], params[2], params[3], params[4], params[6],
            ),
            DRUM_TOM_MID => self.tom_mid.set_params(
                params[0], params[1], params[2], params[3], params[4], params[6],
            ),
            DRUM_TOM_LOW => self.tom_low.set_params(
                params[0], params[1], params[2], params[3], params[4], params[6],
            ),
            DRUM_RIDE => self
                .ride
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_RIDE_BELL => self
                .ride_bell
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_CRASH_16 => self
                .crash_16
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_CRASH_17 => self
                .crash_17
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_CRASH_18 => self
                .crash_18
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_SPLASH => self
                .splash
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_CHINA => self
                .china
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_COWBELL => self
                .cowbell
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            DRUM_CLAP => self
                .clap
                .set_params(params[0], params[1], params[2], params[3], params[6]),
            _ => {}
        }
    }

    pub fn set_mode_all(&mut self, mode: u8) {
        let m = mode.clamp(0, 3);
        self.kick.mode = m;
        self.snare.mode = m;
        self.hat.mode = m;
        self.clap.mode = m;
        self.tom.mode = m;
        self.tom_high.mode = m;
        self.tom_mid.mode = m;
        self.tom_low.mode = m;
        self.ride.mode = m;
        self.ride_bell.mode = m;
        self.crash_16.mode = m;
        self.crash_17.mode = m;
        self.crash_18.mode = m;
        self.splash.mode = m;
        self.china.mode = m;
        self.cowbell.mode = m;
    }

    #[inline(always)]
    pub fn trigger_kick(&mut self, vel: f32) {
        self.kick.trigger(vel, 48000.0);
    }

    #[inline(always)]
    pub fn trigger_snare(&mut self, vel: f32) {
        self.snare.trigger_styled(vel, DRUM_SNARE);
    }

    #[inline(always)]
    pub fn trigger_snare_crack(&mut self, vel: f32) {
        self.snare.trigger_styled(vel, DRUM_SNARE_CRACK);
    }

    #[inline(always)]
    pub fn trigger_snare_wire(&mut self, vel: f32) {
        self.snare.trigger_styled(vel, DRUM_SNARE_WIRE);
    }

    #[inline(always)]
    pub fn trigger_snare_body(&mut self, vel: f32) {
        self.snare.trigger_styled(vel, DRUM_SNARE_BODY);
    }

    #[inline(always)]
    pub fn trigger_snare_ghost(&mut self, vel: f32) {
        self.snare.trigger_styled(vel, DRUM_SNARE_GHOST);
    }

    #[inline(always)]
    pub fn trigger_hh(&mut self, vel: f32, open: bool) {
        self.hat.trigger(vel, open);
    }

    #[inline(always)]
    pub fn trigger_clap(&mut self, vel: f32) {
        self.clap.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_ride(&mut self, vel: f32) {
        self.ride.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_ride_bell(&mut self, vel: f32) {
        self.ride_bell.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_crash_16(&mut self, vel: f32) {
        self.crash_16.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_crash_17(&mut self, vel: f32) {
        self.crash_17.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_crash_18(&mut self, vel: f32) {
        self.crash_18.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_splash(&mut self, vel: f32) {
        self.splash.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_china(&mut self, vel: f32) {
        self.china.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_cowbell(&mut self, vel: f32) {
        self.cowbell.trigger(vel);
    }

    #[inline(always)]
    pub fn trigger_tom(&mut self, vel: f32, freq: f32) {
        self.tom.trigger_freq(vel, freq, 48000.0);
    }

    #[inline(always)]
    pub fn trigger_tom_high(&mut self, vel: f32) {
        self.tom_high.trigger(vel, 48000.0);
    }

    #[inline(always)]
    pub fn trigger_tom_mid(&mut self, vel: f32) {
        self.tom_mid.trigger(vel, 48000.0);
    }

    #[inline(always)]
    pub fn trigger_tom_low(&mut self, vel: f32) {
        self.tom_low.trigger(vel, 48000.0);
    }

    #[inline(always)]
    pub fn process_sample(&mut self, sample_rate: f32) -> f32 {
        self.process(sample_rate)
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        let mut out = 0.0;
        if self.kick.active {
            out += self.kick.process(sample_rate);
        }
        if self.snare.active {
            out += self.snare.process(sample_rate);
        }
        if self.hat.active {
            out += self.hat.process(sample_rate);
        }
        if self.clap.active {
            out += self.clap.process(sample_rate);
        }
        if self.tom.active {
            out += self.tom.process(sample_rate);
        }
        if self.tom_high.active {
            out += self.tom_high.process(sample_rate);
        }
        if self.tom_mid.active {
            out += self.tom_mid.process(sample_rate);
        }
        if self.tom_low.active {
            out += self.tom_low.process(sample_rate);
        }
        if self.ride.active {
            out += self.ride.process(sample_rate);
        }
        if self.ride_bell.active {
            out += self.ride_bell.process(sample_rate);
        }
        if self.crash_16.active {
            out += self.crash_16.process(sample_rate);
        }
        if self.crash_17.active {
            out += self.crash_17.process(sample_rate);
        }
        if self.crash_18.active {
            out += self.crash_18.process(sample_rate);
        }
        if self.splash.active {
            out += self.splash.process(sample_rate);
        }
        if self.china.active {
            out += self.china.process(sample_rate);
        }
        if self.cowbell.active {
            out += self.cowbell.process(sample_rate);
        }
        out
    }

    pub fn reset(&mut self) {
        self.kick.reset();
        self.snare.reset();
        self.hat.reset();
        self.clap.reset();
        self.tom.reset();
        self.tom_high.reset();
        self.tom_mid.reset();
        self.tom_low.reset();
        self.ride.reset();
        self.ride_bell.reset();
        self.crash_16.reset();
        self.crash_17.reset();
        self.crash_18.reset();
        self.splash.reset();
        self.china.reset();
        self.cowbell.reset();
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

    #[test]
    fn test_membrane_drum_sweep_and_decay() {
        let mut kick = MembraneVoice::new(140.0, 48.0, 0.05, 0.99, 1.0);
        assert!(!kick.active);

        kick.trigger(1.0, 48000.0);
        assert!(kick.active);

        let initial_sample = kick.process(48000.0);
        assert!(initial_sample.abs() < 2.0);

        for _ in 0..5000 {
            kick.process(48000.0);
        }
        assert!(!kick.active);
    }

    #[test]
    fn test_kick_voice_punch_and_decay() {
        let mut kick = KickVoice::new();
        assert!(!kick.active);
        kick.trigger(1.0, 48000.0);
        assert!(kick.active);
        let s0 = kick.process(48000.0);
        assert!(s0.abs() > 0.05, "Immediate punch presence on sample 0");
        for _ in 0..20000 {
            kick.process(48000.0);
        }
        assert!(!kick.active, "Kick must decay cleanly to silence");
    }

    #[test]
    fn test_snare_artistic_styles() {
        let mut snare = SnareVoice::new();
        snare.trigger_styled(1.0, DRUM_SNARE_CRACK);
        assert!(snare.active);
        let s = snare.process(48000.0);
        assert!(!s.is_nan());
    }

    #[test]
    fn test_drum_machine_full_dispatch() {
        let mut dm = DrumMachine::new();
        let drum_ids = [
            DRUM_KICK,
            DRUM_SNARE,
            DRUM_HH_CLOSED,
            DRUM_HH_OPEN,
            DRUM_CLAP,
            DRUM_TOM_HIGH,
            DRUM_TOM_MID,
            DRUM_TOM_LOW,
            DRUM_RIDE,
            DRUM_RIDE_BELL,
            DRUM_CRASH_16,
            DRUM_SPLASH,
            DRUM_CHINA,
            DRUM_COWBELL,
        ];

        for &id in &drum_ids {
            dm.trigger(id, 0.9, 48000.0);
            let sample = dm.process(48000.0);
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
            let s_ride = ride.process(48000.0);
            let s_bell = bell.process(48000.0);
            assert!(!s_ride.is_nan());
            assert!(!s_bell.is_nan());
            assert!(s_ride.abs() < 4.0);
            assert!(s_bell.abs() < 4.0);
        }
    }
}
