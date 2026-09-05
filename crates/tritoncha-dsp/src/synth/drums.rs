use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::soft_clip;
use std::f32::consts::PI;

const RIDE_FREQS: [f32; 6] = [263.0, 400.0, 421.0, 474.0, 587.0, 845.0];
const RIDE_BELL_FREQS: [f32; 3] = [587.0, 845.0, 1280.0];
const COWBELL_FREQS: [f32; 2] = [587.0, 845.0];

pub const MIN_AUDIBLE_VELOCITY: f32 = 0.001;
const VELOCITY_MIN_CLAMP: f32 = 0.1;
const VELOCITY_MAX_CLAMP: f32 = 1.2;

const TOM_HIGH_START_HZ: f32 = 240.0;
const TOM_HIGH_MIN_HZ: f32 = 170.0;
const TOM_MID_START_HZ: f32 = 180.0;
const TOM_MID_MIN_HZ: f32 = 120.0;
const TOM_LOW_START_HZ: f32 = 120.0;
const TOM_LOW_MIN_HZ: f32 = 75.0;

const CRASH_16_CUTOFF_HZ: f32 = 4200.0;
const CRASH_17_CUTOFF_HZ: f32 = 3600.0;
const CRASH_18_CUTOFF_HZ: f32 = 2800.0;
const SPLASH_CUTOFF_HZ: f32 = 5800.0;
const CHINA_CUTOFF_HZ: f32 = 2600.0;
const COWBELL_BANDPASS_HZ: f32 = 820.0;

pub struct DrumMachine {
    kick_active: bool,
    kick_phase: f32,
    kick_freq: f32,
    kick_env: f32,
    kick_vel: f32,

    snare_active: bool,
    snare_phase: f32,
    snare_env_tone: f32,
    snare_env_noise: f32,
    snare_vel: f32,
    snare_filter: StateVariableFilter,

    hh_active: bool,
    hh_env: f32,
    hh_decay: f32,
    hh_vel: f32,
    hh_filter: StateVariableFilter,

    clap_active: bool,
    clap_env: f32,
    clap_vel: f32,
    clap_filter: StateVariableFilter,

    ride_active: bool,
    ride_env: f32,
    ride_vel: f32,
    ride_phases: [f32; 6],
    ride_filter: StateVariableFilter,

    ride_bell_active: bool,
    ride_bell_env: f32,
    ride_bell_vel: f32,
    ride_bell_phases: [f32; 3],
    ride_bell_filter: StateVariableFilter,

    tom_active: bool,
    tom_phase: f32,
    tom_freq: f32,
    tom_env: f32,
    tom_vel: f32,

    tom_high_active: bool,
    tom_high_phase: f32,
    tom_high_freq: f32,
    tom_high_env: f32,
    tom_high_vel: f32,

    tom_mid_active: bool,
    tom_mid_phase: f32,
    tom_mid_freq: f32,
    tom_mid_env: f32,
    tom_mid_vel: f32,

    tom_low_active: bool,
    tom_low_phase: f32,
    tom_low_freq: f32,
    tom_low_env: f32,
    tom_low_vel: f32,

    crash_16_active: bool,
    crash_16_env: f32,
    crash_16_vel: f32,
    crash_16_filter: StateVariableFilter,

    crash_17_active: bool,
    crash_17_env: f32,
    crash_17_vel: f32,
    crash_17_filter: StateVariableFilter,

    crash_18_active: bool,
    crash_18_env: f32,
    crash_18_vel: f32,
    crash_18_filter: StateVariableFilter,

    splash_active: bool,
    splash_env: f32,
    splash_vel: f32,
    splash_filter: StateVariableFilter,

    china_active: bool,
    china_env: f32,
    china_vel: f32,
    china_filter: StateVariableFilter,

    cowbell_active: bool,
    cowbell_env: f32,
    cowbell_vel: f32,
    cowbell_phases: [f32; 2],
    cowbell_filter: StateVariableFilter,

    crack_active: bool,
    crack_phase: f32,
    crack_env_tone: f32,
    crack_env_noise: f32,
    crack_vel: f32,
    crack_filter: StateVariableFilter,

    seed: u32,
}

impl DrumMachine {
    pub fn new() -> Self {
        Self {
            kick_active: false,
            kick_phase: 0.0,
            kick_freq: 150.0,
            kick_env: 0.0,
            kick_vel: 0.0,

            snare_active: false,
            snare_phase: 0.0,
            snare_env_tone: 0.0,
            snare_env_noise: 0.0,
            snare_vel: 0.0,
            snare_filter: StateVariableFilter::new(),

            hh_active: false,
            hh_env: 0.0,
            hh_decay: 0.003,
            hh_vel: 0.0,
            hh_filter: StateVariableFilter::new(),

            clap_active: false,
            clap_env: 0.0,
            clap_vel: 0.0,
            clap_filter: StateVariableFilter::new(),

            ride_active: false,
            ride_env: 0.0,
            ride_vel: 0.0,
            ride_phases: [0.0; 6],
            ride_filter: StateVariableFilter::new(),

            ride_bell_active: false,
            ride_bell_env: 0.0,
            ride_bell_vel: 0.0,
            ride_bell_phases: [0.0; 3],
            ride_bell_filter: StateVariableFilter::new(),

            tom_active: false,
            tom_phase: 0.0,
            tom_freq: 140.0,
            tom_env: 0.0,
            tom_vel: 0.0,

            tom_high_active: false,
            tom_high_phase: 0.0,
            tom_high_freq: TOM_HIGH_START_HZ,
            tom_high_env: 0.0,
            tom_high_vel: 0.0,

            tom_mid_active: false,
            tom_mid_phase: 0.0,
            tom_mid_freq: TOM_MID_START_HZ,
            tom_mid_env: 0.0,
            tom_mid_vel: 0.0,

            tom_low_active: false,
            tom_low_phase: 0.0,
            tom_low_freq: TOM_LOW_START_HZ,
            tom_low_env: 0.0,
            tom_low_vel: 0.0,

            crash_16_active: false,
            crash_16_env: 0.0,
            crash_16_vel: 0.0,
            crash_16_filter: StateVariableFilter::new(),

            crash_17_active: false,
            crash_17_env: 0.0,
            crash_17_vel: 0.0,
            crash_17_filter: StateVariableFilter::new(),

            crash_18_active: false,
            crash_18_env: 0.0,
            crash_18_vel: 0.0,
            crash_18_filter: StateVariableFilter::new(),

            splash_active: false,
            splash_env: 0.0,
            splash_vel: 0.0,
            splash_filter: StateVariableFilter::new(),

            china_active: false,
            china_env: 0.0,
            china_vel: 0.0,
            china_filter: StateVariableFilter::new(),

            cowbell_active: false,
            cowbell_env: 0.0,
            cowbell_vel: 0.0,
            cowbell_phases: [0.0; 2],
            cowbell_filter: StateVariableFilter::new(),

            crack_active: false,
            crack_phase: 0.0,
            crack_env_tone: 0.0,
            crack_env_noise: 0.0,
            crack_vel: 0.0,
            crack_filter: StateVariableFilter::new(),

            seed: 0x13579bdf,
        }
    }
}

impl Default for DrumMachine {
    fn default() -> Self {
        Self::new()
    }
}

impl DrumMachine {
    #[inline(always)]
    fn white_noise(&mut self) -> f32 {
        self.seed = self.seed.wrapping_mul(1664525).wrapping_add(1013904223);
        ((self.seed >> 9) as f32 / 4194304.0) - 1.0
    }

    pub fn trigger_kick(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.kick_active = true;
        self.kick_phase = 0.0;
        self.kick_freq = 150.0;
        self.kick_env = 1.0;
        self.kick_vel = vel.clamp(0.1, 1.2);
    }

    pub fn trigger_snare(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.snare_active = true;
        self.snare_phase = 0.0;
        self.snare_env_tone = 1.0;
        self.snare_env_noise = 1.0;
        self.snare_vel = vel.clamp(0.1, 1.2);
        self.snare_filter.reset();
    }

    pub fn trigger_snare_wire(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.snare_active = true;
        self.snare_phase = 0.0;
        self.snare_env_tone = 0.0;
        self.snare_env_noise = 1.0;
        self.snare_vel = vel.clamp(0.1, 1.2);
        self.snare_filter.reset();
    }

    pub fn trigger_snare_body(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.snare_active = true;
        self.snare_phase = 0.0;
        self.snare_env_tone = 1.0;
        self.snare_env_noise = 0.15;
        self.snare_vel = vel.clamp(0.1, 1.2);
        self.snare_filter.reset();
    }

    pub fn trigger_snare_ghost(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.snare_active = true;
        self.snare_phase = 0.0;
        self.snare_env_tone = 0.35;
        self.snare_env_noise = 0.45;
        self.snare_vel = (vel * 0.55).clamp(0.05, 0.9);
        self.snare_filter.reset();
    }

    pub fn trigger_hh(&mut self, vel: f32, open: bool) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.hh_active = true;
        self.hh_env = 1.0;
        self.hh_decay = if open { 0.00035 } else { 0.0025 };
        self.hh_vel = vel.clamp(0.1, 1.2);
        self.hh_filter.reset();
    }

    pub fn trigger_clap(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.clap_active = true;
        self.clap_env = 1.0;
        self.clap_vel = vel.clamp(0.1, 1.2);
        self.clap_filter.reset();
    }

    pub fn trigger_ride(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.ride_active = true;
        self.ride_env = 1.0;
        self.ride_vel = vel.clamp(0.1, 1.2);
        self.ride_filter.reset();
    }

    pub fn trigger_tom(&mut self, vel: f32, pitch_hz: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.tom_active = true;
        self.tom_phase = 0.0;
        self.tom_freq = pitch_hz.clamp(60.0, 300.0);
        self.tom_env = 1.0;
        self.tom_vel = vel.clamp(0.1, 1.2);
    }

    pub fn trigger_snare_crack(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.crack_active = true;
        self.crack_phase = 0.0;
        self.crack_env_tone = 1.0;
        self.crack_env_noise = 1.0;
        self.crack_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.crack_filter.reset();
    }

    pub fn trigger_ride_bell(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.ride_bell_active = true;
        self.ride_bell_env = 1.0;
        self.ride_bell_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.ride_bell_filter.reset();
    }

    pub fn trigger_tom_high(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.tom_high_active = true;
        self.tom_high_phase = 0.0;
        self.tom_high_freq = TOM_HIGH_START_HZ;
        self.tom_high_env = 1.0;
        self.tom_high_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
    }

    pub fn trigger_tom_mid(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.tom_mid_active = true;
        self.tom_mid_phase = 0.0;
        self.tom_mid_freq = TOM_MID_START_HZ;
        self.tom_mid_env = 1.0;
        self.tom_mid_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
    }

    pub fn trigger_tom_low(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.tom_low_active = true;
        self.tom_low_phase = 0.0;
        self.tom_low_freq = TOM_LOW_START_HZ;
        self.tom_low_env = 1.0;
        self.tom_low_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
    }

    pub fn trigger_crash_16(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.crash_16_active = true;
        self.crash_16_env = 1.0;
        self.crash_16_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.crash_16_filter.reset();
    }

    pub fn trigger_crash_17(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.crash_17_active = true;
        self.crash_17_env = 1.0;
        self.crash_17_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.crash_17_filter.reset();
    }

    pub fn trigger_crash_18(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.crash_18_active = true;
        self.crash_18_env = 1.0;
        self.crash_18_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.crash_18_filter.reset();
    }

    pub fn trigger_splash(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.splash_active = true;
        self.splash_env = 1.0;
        self.splash_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.splash_filter.reset();
    }

    pub fn trigger_china(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.china_active = true;
        self.china_env = 1.0;
        self.china_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.china_filter.reset();
    }

    pub fn trigger_cowbell(&mut self, vel: f32) {
        if vel <= MIN_AUDIBLE_VELOCITY {
            return;
        }
        self.cowbell_active = true;
        self.cowbell_env = 1.0;
        self.cowbell_vel = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.cowbell_filter.reset();
    }

    #[inline(always)]
    pub fn process_sample(&mut self, sample_rate: f32) -> f32 {
        let mut out = 0.0;

        // 1. Kick: Punchy exponential pitch sweep + click transient
        if self.kick_active {
            self.kick_phase += self.kick_freq / sample_rate;
            if self.kick_phase >= 1.0 {
                self.kick_phase -= 1.0;
            }
            let sine = (self.kick_phase * 2.0 * PI).sin();
            let click = if self.kick_env > 0.94 { 0.55 } else { 0.0 };
            let kick_raw = (sine * 1.15 + click) * self.kick_env * self.kick_vel;
            out += soft_clip(kick_raw * 1.4);

            self.kick_freq = (self.kick_freq * 0.9991).max(48.0);
            self.kick_env *= 0.99965;
            if self.kick_env < 0.001 {
                self.kick_active = false;
            }
        }

        // 2. Snare: 185 Hz body tone + filtered noise
        if self.snare_active {
            self.snare_phase += 185.0 / sample_rate;
            if self.snare_phase >= 1.0 {
                self.snare_phase -= 1.0;
            }
            let tone = (self.snare_phase * 2.0 * PI).sin() * self.snare_env_tone * 0.95;
            let raw_noise = self.white_noise();
            let filtered_noise = self
                .snare_filter
                .process_lp(raw_noise, 3800.0, 0.4, sample_rate);
            let noise = filtered_noise * self.snare_env_noise * 0.85;

            out += (tone + noise) * self.snare_vel * 1.1;

            self.snare_env_tone *= 0.9982;
            self.snare_env_noise *= 0.9992;
            if self.snare_env_noise < 0.001 && self.snare_env_tone < 0.001 {
                self.snare_active = false;
            }
        }

        // 3. Hi-Hat: Crisp highpassed metallic noise
        if self.hh_active {
            let noise = self.white_noise();
            let hp = noise - self.hh_filter.process_lp(noise, 6500.0, 0.6, sample_rate);
            out += hp * self.hh_env * self.hh_vel * 0.7;
            self.hh_env -= self.hh_decay;
            if self.hh_env <= 0.0 {
                self.hh_active = false;
            }
        }

        // 4. Hand Clap (909 style multi-burst bandpassed noise)
        if self.clap_active {
            let noise = self.white_noise();
            let band = self.clap_filter.process_lp(noise, 2200.0, 0.6, sample_rate);
            out += band * self.clap_env * self.clap_vel * 0.85;
            self.clap_env *= 0.9988;
            if self.clap_env < 0.001 {
                self.clap_active = false;
            }
        }

        // 5. 6-Oscillator Metallic Ride Cymbal (Body)
        if self.ride_active {
            let mut metal = 0.0;
            for (phase, &freq) in self.ride_phases.iter_mut().zip(RIDE_FREQS.iter()) {
                *phase += freq / sample_rate;
                if *phase >= 1.0 {
                    *phase -= 1.0;
                }
                metal += if *phase < 0.5 { 0.2 } else { -0.2 };
            }
            let noise = self.white_noise() * 0.3;
            let filtered = metal + noise
                - self
                    .ride_filter
                    .process_lp(metal + noise, 7000.0, 0.5, sample_rate);
            out += filtered * self.ride_env * self.ride_vel * 0.6;
            self.ride_env *= 0.99982;
            if self.ride_env < 0.001 {
                self.ride_active = false;
            }
        }

        // 6. Resonant Jungle Tom (Pitch-dropping membrane)
        if self.tom_active {
            self.tom_phase += self.tom_freq / sample_rate;
            if self.tom_phase >= 1.0 {
                self.tom_phase -= 1.0;
            }
            let tone = (self.tom_phase * 2.0 * PI).sin() * self.tom_env * self.tom_vel;
            out += soft_clip(tone * 1.2);
            self.tom_freq = (self.tom_freq * 0.9993).max(50.0);
            self.tom_env *= 0.9995;
            if self.tom_env < 0.001 {
                self.tom_active = false;
            }
        }

        // 7. Breakbeat Snare Crack (310 Hz punch + tight crack noise)
        if self.crack_active {
            self.crack_phase += 310.0 / sample_rate;
            if self.crack_phase >= 1.0 {
                self.crack_phase -= 1.0;
            }
            let tone = (self.crack_phase * 2.0 * PI).sin() * self.crack_env_tone * 0.65;
            let raw_noise = self.white_noise();
            let filtered_noise = self
                .crack_filter
                .process_lp(raw_noise, 4800.0, 0.5, sample_rate);
            let noise = filtered_noise * self.crack_env_noise * 0.85;

            out += (tone + noise) * self.crack_vel * 1.25;

            self.crack_env_tone *= 0.9975;
            self.crack_env_noise *= 0.9985;
            if self.crack_env_noise < 0.001 {
                self.crack_active = false;
            }
        }

        // 8. Ride Bell (High metallic ping and singing bell cluster)
        if self.ride_bell_active {
            let mut metal = 0.0;
            for (phase, &freq) in self.ride_bell_phases.iter_mut().zip(RIDE_BELL_FREQS.iter()) {
                *phase += freq / sample_rate;
                if *phase >= 1.0 {
                    *phase -= 1.0;
                }
                metal += (*phase * 2.0 * PI).sin() * 0.35;
            }
            let noise = self.white_noise() * 0.15;
            let filtered =
                self.ride_bell_filter
                    .process_bp(metal + noise, 1040.0, 0.75, sample_rate);
            out += filtered * self.ride_bell_env * self.ride_bell_vel * 0.85;
            self.ride_bell_env *= 0.99965;
            if self.ride_bell_env < 0.001 {
                self.ride_bell_active = false;
            }
        }

        // 9. High Tom
        if self.tom_high_active {
            self.tom_high_phase += self.tom_high_freq / sample_rate;
            if self.tom_high_phase >= 1.0 {
                self.tom_high_phase -= 1.0;
            }
            let tone =
                (self.tom_high_phase * 2.0 * PI).sin() * self.tom_high_env * self.tom_high_vel;
            out += soft_clip(tone * 1.2);
            self.tom_high_freq = (self.tom_high_freq * 0.9993).max(TOM_HIGH_MIN_HZ);
            self.tom_high_env *= 0.9994;
            if self.tom_high_env < 0.001 {
                self.tom_high_active = false;
            }
        }

        // 10. Mid Tom
        if self.tom_mid_active {
            self.tom_mid_phase += self.tom_mid_freq / sample_rate;
            if self.tom_mid_phase >= 1.0 {
                self.tom_mid_phase -= 1.0;
            }
            let tone = (self.tom_mid_phase * 2.0 * PI).sin() * self.tom_mid_env * self.tom_mid_vel;
            out += soft_clip(tone * 1.2);
            self.tom_mid_freq = (self.tom_mid_freq * 0.9993).max(TOM_MID_MIN_HZ);
            self.tom_mid_env *= 0.9995;
            if self.tom_mid_env < 0.001 {
                self.tom_mid_active = false;
            }
        }

        // 11. Low Tom
        if self.tom_low_active {
            self.tom_low_phase += self.tom_low_freq / sample_rate;
            if self.tom_low_phase >= 1.0 {
                self.tom_low_phase -= 1.0;
            }
            let tone = (self.tom_low_phase * 2.0 * PI).sin() * self.tom_low_env * self.tom_low_vel;
            out += soft_clip(tone * 1.25);
            self.tom_low_freq = (self.tom_low_freq * 0.9993).max(TOM_LOW_MIN_HZ);
            self.tom_low_env *= 0.9996;
            if self.tom_low_env < 0.001 {
                self.tom_low_active = false;
            }
        }

        // 12. 16\" Crash Cymbal (Bright, fast explosion)
        if self.crash_16_active {
            let noise = self.white_noise();
            let hp = noise
                - self
                    .crash_16_filter
                    .process_lp(noise, CRASH_16_CUTOFF_HZ, 0.45, sample_rate);
            out += hp * self.crash_16_env * self.crash_16_vel * 0.65;
            self.crash_16_env *= 0.99975;
            if self.crash_16_env < 0.001 {
                self.crash_16_active = false;
            }
        }

        // 13. 17\" Crash Cymbal (Balanced full-body acoustic crash)
        if self.crash_17_active {
            let noise = self.white_noise();
            let hp = noise
                - self
                    .crash_17_filter
                    .process_lp(noise, CRASH_17_CUTOFF_HZ, 0.48, sample_rate);
            out += hp * self.crash_17_env * self.crash_17_vel * 0.7;
            self.crash_17_env *= 0.99982;
            if self.crash_17_env < 0.001 {
                self.crash_17_active = false;
            }
        }

        // 14. 18\" Crash Cymbal (Deep, dark power crash)
        if self.crash_18_active {
            let noise = self.white_noise();
            let hp = noise
                - self
                    .crash_18_filter
                    .process_lp(noise, CRASH_18_CUTOFF_HZ, 0.52, sample_rate);
            out += hp * self.crash_18_env * self.crash_18_vel * 0.75;
            self.crash_18_env *= 0.99988;
            if self.crash_18_env < 0.001 {
                self.crash_18_active = false;
            }
        }

        // 15. Splash Cymbal (Fast high-frequency cut)
        if self.splash_active {
            let noise = self.white_noise();
            let hp = noise
                - self
                    .splash_filter
                    .process_lp(noise, SPLASH_CUTOFF_HZ, 0.6, sample_rate);
            out += hp * self.splash_env * self.splash_vel * 0.6;
            self.splash_env *= 0.9991;
            if self.splash_env < 0.001 {
                self.splash_active = false;
            }
        }

        // 16. China Cymbal (Trashy, aggressive inharmonic roar)
        if self.china_active {
            let noise = self.white_noise();
            let bp = self
                .china_filter
                .process_bp(noise, CHINA_CUTOFF_HZ, 0.7, sample_rate);
            let trash = soft_clip(bp * 1.8);
            out += trash * self.china_env * self.china_vel * 0.7;
            self.china_env *= 0.99978;
            if self.china_env < 0.001 {
                self.china_active = false;
            }
        }

        // 17. Cowbell (808-style dual-oscillator resonant metallic clank)
        if self.cowbell_active {
            let mut sq = 0.0;
            for (phase, &freq) in self.cowbell_phases.iter_mut().zip(COWBELL_FREQS.iter()) {
                *phase += freq / sample_rate;
                if *phase >= 1.0 {
                    *phase -= 1.0;
                }
                sq += if *phase < 0.5 { 0.4 } else { -0.4 };
            }
            let filtered =
                self.cowbell_filter
                    .process_bp(sq, COWBELL_BANDPASS_HZ, 0.72, sample_rate);
            out += soft_clip(filtered * 1.3) * self.cowbell_env * self.cowbell_vel * 0.8;
            self.cowbell_env *= 0.9982;
            if self.cowbell_env < 0.001 {
                self.cowbell_active = false;
            }
        }

        out
    }
}
