//! Modular physical and analog drum voice synthesis models (808, 909, and acoustic kit).

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{soft_clip, wrap_phase, xorshift32_norm};
use std::f32::consts::PI;

pub const MIN_AUDIBLE_VELOCITY: f32 = 0.001;
pub const VELOCITY_MIN_CLAMP: f32 = 0.1;
pub const VELOCITY_MAX_CLAMP: f32 = 1.2;

// Inharmonic metallic frequency banks
pub const RIDE_FREQS: [f32; 6] = [263.0, 400.0, 421.0, 474.0, 587.0, 845.0];
pub const RIDE_BELL_FREQS: [f32; 3] = [587.0, 845.0, 1280.0];
pub const COWBELL_FREQS: [f32; 2] = [587.0, 845.0];

// Tuned frequency ranges for membrane toms
pub const TOM_HIGH_START_HZ: f32 = 240.0;
pub const TOM_HIGH_MIN_HZ: f32 = 170.0;
pub const TOM_MID_START_HZ: f32 = 180.0;
pub const TOM_MID_MIN_HZ: f32 = 120.0;
pub const TOM_LOW_START_HZ: f32 = 120.0;
pub const TOM_LOW_MIN_HZ: f32 = 75.0;

// Cymbal filter cutoffs
pub const CRASH_16_CUTOFF_HZ: f32 = 4200.0;
pub const CRASH_17_CUTOFF_HZ: f32 = 3600.0;
pub const CRASH_18_CUTOFF_HZ: f32 = 2800.0;
pub const SPLASH_CUTOFF_HZ: f32 = 5800.0;
pub const CHINA_CUTOFF_HZ: f32 = 2600.0;
pub const COWBELL_BANDPASS_HZ: f32 = 820.0;

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

/// Membrane Drum Voice modeling kicks, toms, and tuned sub-percussion.
#[derive(Clone)]
pub struct MembraneVoice {
    pub active: bool,
    phase: f32,
    freq: f32,
    env: f32,
    vel: f32,
    start_pitch_hz: f32,
    min_pitch_hz: f32,
    pitch_decay_coeff: f32,
    amp_decay_coeff: f32,
    drive_gain: f32,
}

impl MembraneVoice {
    pub fn new(
        start_pitch_hz: f32,
        min_pitch_hz: f32,
        pitch_decay_coeff: f32,
        amp_decay_coeff: f32,
        drive_gain: f32,
    ) -> Self {
        Self {
            active: false,
            phase: 0.0,
            freq: min_pitch_hz,
            env: 0.0,
            vel: 0.0,
            start_pitch_hz,
            min_pitch_hz,
            pitch_decay_coeff,
            amp_decay_coeff,
            drive_gain,
        }
    }

    pub fn trigger(&mut self, vel: f32, sample_rate: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.freq = self.start_pitch_hz;
        self.env = v;
        self.vel = v;
        // Suppress unused variable warning
        let _ = sample_rate;
    }

    pub fn trigger_freq(&mut self, vel: f32, freq: f32, sample_rate: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.freq = freq.max(self.min_pitch_hz);
        self.env = v;
        self.vel = v;
        let _ = sample_rate;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let sine_val = (self.phase * 2.0 * PI).sin();
        let sig = soft_clip(sine_val * self.drive_gain) * self.env * self.vel;

        self.phase = wrap_phase(self.phase + self.freq / sample_rate);

        // Exponential pitch descent
        self.freq += (self.min_pitch_hz - self.freq) * self.pitch_decay_coeff;
        // Exponential amplitude decay
        self.env *= self.amp_decay_coeff;

        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.env = 0.0;
    }
}

/// Snare Drum Voice combining body membrane resonance with filtered wire noise.
#[derive(Clone)]
pub struct SnareVoice {
    pub active: bool,
    phase: f32,
    env_tone: f32,
    env_noise: f32,
    vel: f32,
    filter: StateVariableFilter,
    noise_seed: u32,
    tone_decay: f32,
    noise_decay: f32,
    cutoff_hz: f32,
    resonance: f32,
    base_freq: f32,
    tone_gain: f32,
    noise_gain: f32,
}

impl SnareVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            phase: 0.0,
            env_tone: 0.0,
            env_noise: 0.0,
            vel: 0.0,
            filter: StateVariableFilter::new(),
            noise_seed: 0x19a3b5c7,
            tone_decay: 0.9985,
            noise_decay: 0.9991,
            cutoff_hz: 2400.0,
            resonance: 0.45,
            base_freq: 185.0,
            tone_gain: 0.65,
            noise_gain: 0.85,
        }
    }

    pub fn trigger_styled(&mut self, vel: f32, style: i32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.phase = 0.0;
        self.vel = v;

        match style {
            DRUM_SNARE_CRACK => {
                self.env_tone = v * 1.2;
                self.env_noise = v * 1.35;
                self.cutoff_hz = 3800.0;
                self.resonance = 0.65;
                self.base_freq = 220.0;
                self.noise_decay = 0.9988;
            }
            DRUM_SNARE_WIRE => {
                self.env_tone = v * 0.35;
                self.env_noise = v * 1.15;
                self.cutoff_hz = 5200.0;
                self.resonance = 0.35;
                self.base_freq = 175.0;
                self.noise_decay = 0.9993;
            }
            DRUM_SNARE_BODY => {
                self.env_tone = v * 1.3;
                self.env_noise = v * 0.4;
                self.cutoff_hz = 1600.0;
                self.resonance = 0.55;
                self.base_freq = 160.0;
                self.noise_decay = 0.9985;
            }
            DRUM_SNARE_GHOST => {
                self.env_tone = v * 0.45;
                self.env_noise = v * 0.55;
                self.cutoff_hz = 2800.0;
                self.resonance = 0.3;
                self.base_freq = 190.0;
                self.noise_decay = 0.9980;
            }
            DRUM_SNARE_RIM => {
                self.env_tone = v * 1.4;
                self.env_noise = v * 0.2;
                self.cutoff_hz = 4200.0;
                self.resonance = 0.85;
                self.base_freq = 420.0;
                self.noise_decay = 0.9975;
            }
            _ => {
                self.env_tone = v;
                self.env_noise = v;
                self.cutoff_hz = 2400.0;
                self.resonance = 0.45;
                self.base_freq = 185.0;
                self.noise_decay = 0.9991;
            }
        }
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let tone = (self.phase * 2.0 * PI).sin() * self.env_tone * self.tone_gain;
        let noise_raw = xorshift32_norm(&mut self.noise_seed);
        let noise_filtered =
            self.filter
                .process_bp(noise_raw, self.cutoff_hz, self.resonance, sample_rate);
        let noise = noise_filtered * self.env_noise * self.noise_gain;

        let sig = soft_clip(tone + noise) * self.vel;

        self.phase = wrap_phase(self.phase + self.base_freq / sample_rate);
        self.env_tone *= self.tone_decay;
        self.env_noise *= self.noise_decay;

        if self.env_tone < MIN_AUDIBLE_VELOCITY && self.env_noise < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.env_tone = 0.0;
        self.env_noise = 0.0;
        self.filter.reset();
    }
}

impl Default for SnareVoice {
    fn default() -> Self {
        Self::new()
    }
}

/// Metallic Inharmonic Cymbal and Percussion Voice.
#[derive(Clone)]
pub struct MetallicVoice<const N: usize> {
    pub active: bool,
    env: f32,
    vel: f32,
    phases: [f32; N],
    freqs: [f32; N],
    decay_coeff: f32,
    filter: StateVariableFilter,
    cutoff_hz: f32,
    resonance: f32,
    is_bandpass: bool,
    drive: f32,
}

impl<const N: usize> MetallicVoice<N> {
    pub fn new(
        freqs: [f32; N],
        cutoff_hz: f32,
        resonance: f32,
        decay_coeff: f32,
        is_bandpass: bool,
        drive: f32,
    ) -> Self {
        Self {
            active: false,
            env: 0.0,
            vel: 0.0,
            phases: [0.0; N],
            freqs,
            decay_coeff,
            filter: StateVariableFilter::new(),
            cutoff_hz,
            resonance,
            is_bandpass,
            drive,
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.env = v;
        self.vel = v;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let mut sum = 0.0;
        for i in 0..N {
            let square_val = if self.phases[i] < 0.5 { 1.0 } else { -1.0 };
            sum += square_val;
            self.phases[i] = wrap_phase(self.phases[i] + self.freqs[i] / sample_rate);
        }
        sum /= N as f32;

        let filtered = if self.is_bandpass {
            self.filter
                .process_bp(sum, self.cutoff_hz, self.resonance, sample_rate)
        } else {
            self.filter
                .process_hp(sum, self.cutoff_hz, self.resonance, sample_rate)
        };

        let sig = soft_clip(filtered * self.drive) * self.env * self.vel;
        self.env *= self.decay_coeff;

        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.env = 0.0;
        self.phases = [0.0; N];
        self.filter.reset();
    }
}

/// Noise Hi-Hat Voice (Closed and Open).
#[derive(Clone)]
pub struct NoiseHatVoice {
    pub active: bool,
    env: f32,
    decay: f32,
    vel: f32,
    filter: StateVariableFilter,
    noise_seed: u32,
}

impl NoiseHatVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            env: 0.0,
            decay: 0.994,
            vel: 0.0,
            filter: StateVariableFilter::new(),
            noise_seed: 0x5a827999,
        }
    }

    pub fn trigger(&mut self, vel: f32, open: bool) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.env = v;
        self.vel = v;
        self.decay = if open { 0.9992 } else { 0.9940 };
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise = xorshift32_norm(&mut self.noise_seed);
        let filtered = self.filter.process_hp(noise, 7200.0, 0.45, sample_rate);
        let sig = filtered * self.env * self.vel * 0.7;

        self.env *= self.decay;
        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.env = 0.0;
        self.filter.reset();
    }
}

impl Default for NoiseHatVoice {
    fn default() -> Self {
        Self::new()
    }
}

/// Hand Clap Voice with multi-burst envelope.
#[derive(Clone)]
pub struct ClapVoice {
    pub active: bool,
    env: f32,
    vel: f32,
    filter: StateVariableFilter,
    noise_seed: u32,
}

impl ClapVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            env: 0.0,
            vel: 0.0,
            filter: StateVariableFilter::new(),
            noise_seed: 0x33b91a7f,
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        self.env = v;
        self.vel = v;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        let noise = xorshift32_norm(&mut self.noise_seed);
        let filtered = self.filter.process_bp(noise, 1200.0, 0.7, sample_rate);
        let sig = filtered * self.env * self.vel * 0.8;

        self.env *= 0.9982;
        if self.env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.env = 0.0;
        self.filter.reset();
    }
}

impl Default for ClapVoice {
    fn default() -> Self {
        Self::new()
    }
}

/// Drum Machine Aggregate Root managing all drum voices.
pub struct DrumMachine {
    pub kick: MembraneVoice,
    pub snare: SnareVoice,
    pub hat: NoiseHatVoice,
    pub clap: ClapVoice,
    pub tom: MembraneVoice,
    pub tom_high: MembraneVoice,
    pub tom_mid: MembraneVoice,
    pub tom_low: MembraneVoice,
    pub ride: MetallicVoice<6>,
    pub ride_bell: MetallicVoice<3>,
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
            kick: MembraneVoice::new(140.0, 48.0, 0.025, 0.9992, 1.4),
            snare: SnareVoice::new(),
            hat: NoiseHatVoice::new(),
            clap: ClapVoice::new(),
            tom: MembraneVoice::new(180.0, 105.0, 0.015, 0.9988, 1.1),
            tom_high: MembraneVoice::new(TOM_HIGH_START_HZ, TOM_HIGH_MIN_HZ, 0.018, 0.9985, 1.15),
            tom_mid: MembraneVoice::new(TOM_MID_START_HZ, TOM_MID_MIN_HZ, 0.016, 0.9987, 1.15),
            tom_low: MembraneVoice::new(TOM_LOW_START_HZ, TOM_LOW_MIN_HZ, 0.014, 0.9990, 1.15),
            ride: MetallicVoice::new(RIDE_FREQS, 3500.0, 0.35, 0.99982, false, 0.75),
            ride_bell: MetallicVoice::new(RIDE_BELL_FREQS, 4800.0, 0.75, 0.99988, true, 1.35),
            crash_16: MetallicVoice::new(
                RIDE_FREQS,
                CRASH_16_CUTOFF_HZ,
                0.25,
                0.99986,
                false,
                0.95,
            ),
            crash_17: MetallicVoice::new(
                RIDE_FREQS,
                CRASH_17_CUTOFF_HZ,
                0.25,
                0.99988,
                false,
                0.95,
            ),
            crash_18: MetallicVoice::new(
                RIDE_FREQS,
                CRASH_18_CUTOFF_HZ,
                0.25,
                0.99990,
                false,
                0.95,
            ),
            splash: MetallicVoice::new(RIDE_FREQS, SPLASH_CUTOFF_HZ, 0.3, 0.99975, false, 0.85),
            china: MetallicVoice::new(RIDE_FREQS, CHINA_CUTOFF_HZ, 0.45, 0.99982, false, 1.1),
            cowbell: MetallicVoice::new(
                COWBELL_FREQS,
                COWBELL_BANDPASS_HZ,
                0.85,
                0.9985,
                true,
                1.4,
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
}
