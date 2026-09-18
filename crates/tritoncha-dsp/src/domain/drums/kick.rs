//! Kick drum physical and analog modeling voice.

use super::{DrumMode, MIN_AUDIBLE_VELOCITY, VELOCITY_MAX_CLAMP, VELOCITY_MIN_CLAMP};
use crate::core::math::{sin_phase, soft_clip, t60_decay_coeff, wrap_phase};
use crate::engine::DEFAULT_SAMPLE_RATE;

// Default Tuning and Acoustic Physical Modeling Constants
pub const DEFAULT_KICK_PITCH_HZ: f32 = 48.0; // Deep sub fundamental
pub const DEFAULT_KICK_PITCH_DROP_HZ: f32 = 180.0; // Snap from 228 Hz down to 48 Hz
pub const DEFAULT_KICK_PITCH_DECAY_COEFF: f32 = 0.040; // Fast drop in ~3-4ms
pub const DEFAULT_KICK_PUNCH_DECAY: f32 = 0.9965; // Snappy punch envelope (~15ms)
pub const DEFAULT_KICK_BODY_DECAY: f32 = 0.99955; // Deep sub sustain (~280ms)
pub const DEFAULT_KICK_CLICK_DECAY: f32 = 0.988; // Transient snap (~2.5ms)
pub const DEFAULT_KICK_CLICK_LEVEL: f32 = 0.35; // Tactile beater knock
pub const DEFAULT_KICK_DRIVE: f32 = 1.6; // Warm 2nd harmonic analog tape drive
pub const KICK_INITIAL_PHASE_OFFSET: f32 = 0.15; // Instant pressure wave onset
pub const KICK_MASTER_OUTPUT_BOOST: f32 = 1.35;

// Mode 1: Natural (Acoustic wooden beater and dual-head resonance)
pub const KICK_NATURAL_HEAD2_FREQ_MULT: f32 = 1.34;
pub const KICK_NATURAL_HEAD2_GAIN: f32 = 0.40;
pub const KICK_NATURAL_CLICK_HZ: f32 = 1300.0;
pub const KICK_NATURAL_PITCH_DROP_MULT: f32 = 0.75;
pub const KICK_NATURAL_CLICK_LEVEL_MULT: f32 = 0.75;
pub const KICK_NATURAL_PUNCH_WEIGHT: f32 = 0.55;
pub const KICK_NATURAL_BODY_WEIGHT: f32 = 0.85;

// Mode 2: IDM (Micro-tuned laser pitch snap and FM chirped transient)
pub const KICK_IDM_FM_FREQ_MULT: f32 = 3.8;
pub const KICK_IDM_FM_DEPTH: f32 = 0.65;
pub const KICK_IDM_DRIVE_MULT: f32 = 1.35;
pub const KICK_IDM_CLICK_HZ: f32 = 4800.0;
pub const KICK_IDM_PITCH_DROP_MULT: f32 = 1.80;
pub const KICK_IDM_CLICK_LEVEL_MULT: f32 = 1.45;
pub const KICK_IDM_PUNCH_WEIGHT: f32 = 0.90;
pub const KICK_IDM_BODY_WEIGHT: f32 = 0.60;

// Mode 3: Industrial (Heavy wavefolded overdrive and distortion)
pub const KICK_IND_DRIVE_MULT: f32 = 2.2;
pub const KICK_IND_FOLD_GAIN: f32 = 1.4;
pub const KICK_IND_WARM_2ND_HARM: f32 = 0.3;
pub const KICK_IND_CLICK_HZ: f32 = 2400.0;
pub const KICK_IND_CLICK_LEVEL_MULT: f32 = 1.25;
pub const KICK_IND_PUNCH_WEIGHT: f32 = 0.85;
pub const KICK_IND_BODY_WEIGHT: f32 = 0.95;

// Mode 0: Analog (Standard 808/909 punch with 2nd harmonic saturation)
pub const KICK_ANALOG_WARM_2ND_HARM: f32 = 0.22;
pub const KICK_ANALOG_CLICK_HZ: f32 = 2400.0;
pub const KICK_ANALOG_PUNCH_WEIGHT: f32 = 0.60;
pub const KICK_ANALOG_BODY_WEIGHT: f32 = 0.80;

/// High-Impact Analog and Club Kick Drum Voice.
/// Features dual-stage pitch sweep, beater click transient, asymmetric 2nd harmonic warmth,
/// and independent punch and sub-bass decay envelopes.
#[derive(Clone)]
pub struct KickVoice {
    pub active: bool,
    phase: f32,
    click_phase: f32,
    pitch_fast: f32,
    env_punch: f32,
    env_body: f32,
    env_click: f32,
    vel: f32,
    base_pitch_hz: f32,
    pitch_drop: f32,
    pitch_decay_coeff: f32,
    punch_decay: f32,
    body_decay: f32,
    click_decay: f32,
    click_level: f32,
    drive: f32,
    pub mode: DrumMode,
}

/// Parameter configuration for kick drum voice synthesis.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct KickParams {
    pub base_pitch_hz: f32,
    pub pitch_drop: f32,
    pub pitch_decay: f32,
    pub decay_s: f32,
    pub click_level: f32,
    pub drive: f32,
    pub mode: f32,
}

impl Default for KickParams {
    fn default() -> Self {
        Self {
            base_pitch_hz: DEFAULT_KICK_PITCH_HZ,
            pitch_drop: DEFAULT_KICK_PITCH_DROP_HZ,
            pitch_decay: DEFAULT_KICK_PITCH_DECAY_COEFF,
            decay_s: 0.25,
            click_level: DEFAULT_KICK_CLICK_LEVEL,
            drive: DEFAULT_KICK_DRIVE,
            mode: 0.0,
        }
    }
}

impl From<[f32; 7]> for KickParams {
    #[inline(always)]
    fn from(p: [f32; 7]) -> Self {
        Self {
            base_pitch_hz: p[0],
            pitch_drop: p[1],
            pitch_decay: p[2],
            decay_s: p[3],
            click_level: p[4],
            drive: p[5],
            mode: p[6],
        }
    }
}

impl KickVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            phase: 0.0,
            click_phase: 0.0,
            pitch_fast: 0.0,
            env_punch: 0.0,
            env_body: 0.0,
            env_click: 0.0,
            vel: 0.0,
            base_pitch_hz: DEFAULT_KICK_PITCH_HZ,
            pitch_drop: DEFAULT_KICK_PITCH_DROP_HZ,
            pitch_decay_coeff: DEFAULT_KICK_PITCH_DECAY_COEFF,
            punch_decay: DEFAULT_KICK_PUNCH_DECAY,
            body_decay: DEFAULT_KICK_BODY_DECAY,
            click_decay: DEFAULT_KICK_CLICK_DECAY,
            click_level: DEFAULT_KICK_CLICK_LEVEL,
            drive: DEFAULT_KICK_DRIVE,
            mode: DrumMode::default(),
        }
    }

    pub fn set_params(&mut self, params: impl Into<KickParams>) {
        let p = params.into();
        if p.base_pitch_hz > 0.0 {
            self.base_pitch_hz = p.base_pitch_hz.clamp(30.0, 120.0);
        }
        if p.pitch_drop >= 0.0 {
            self.pitch_drop = p.pitch_drop.clamp(0.0, 500.0);
        }
        if p.pitch_decay > 0.0 {
            self.pitch_decay_coeff = p.pitch_decay.clamp(0.005, 0.20);
        }
        if p.decay_s > 0.0 {
            self.body_decay = t60_decay_coeff(p.decay_s.clamp(0.05, 1.5), DEFAULT_SAMPLE_RATE)
                .clamp(0.990, 0.99995);
        }
        if p.click_level >= 0.0 {
            self.click_level = p.click_level.clamp(0.0, 2.0);
        }
        if p.drive > 0.0 {
            self.drive = p.drive.clamp(0.5, 4.0);
        }
        if p.mode >= 0.0 {
            self.mode = DrumMode::from(p.mode);
        }
    }

    pub fn trigger(&mut self, vel: f32) {
        let v = vel.clamp(VELOCITY_MIN_CLAMP, VELOCITY_MAX_CLAMP);
        self.active = true;
        // Start oscillator slightly offset from zero for instant pressure wave
        self.phase = KICK_INITIAL_PHASE_OFFSET;
        self.click_phase = 0.0;
        self.pitch_fast = self.pitch_drop;
        self.env_punch = 1.0;
        self.env_body = 1.0;
        self.env_click = 1.0;
        self.vel = v;
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        // Instantaneous frequency: sharp transient drop + rock-solid sub fundamental
        let freq = self.base_pitch_hz + self.pitch_fast;

        let (body_sig, click_sig, click_hz, pitch_drop_mult) = match self.mode {
            DrumMode::Natural => {
                // Natural / Acoustic mode:
                // Organic dual-membrane beating: primary head + resonant head
                let head1 = sin_phase(self.phase);
                let head2 =
                    sin_phase(self.phase * KICK_NATURAL_HEAD2_FREQ_MULT) * KICK_NATURAL_HEAD2_GAIN;
                let warm = soft_clip((head1 + head2) * self.drive);
                let click_osc = sin_phase(self.click_phase);
                let c_sig =
                    click_osc * self.env_click * self.click_level * KICK_NATURAL_CLICK_LEVEL_MULT;
                let b_sig = warm
                    * (self.env_punch * KICK_NATURAL_PUNCH_WEIGHT
                        + self.env_body * KICK_NATURAL_BODY_WEIGHT);
                (
                    b_sig,
                    c_sig,
                    KICK_NATURAL_CLICK_HZ,
                    KICK_NATURAL_PITCH_DROP_MULT,
                )
            }
            DrumMode::Idm => {
                // IDM / Glitch mode:
                // Micro-tuned laser pitch snap + FM chirped transient
                let fm = sin_phase(self.click_phase * KICK_IDM_FM_FREQ_MULT)
                    * self.env_punch
                    * KICK_IDM_FM_DEPTH;
                let raw = sin_phase(self.phase + fm);
                let warm = soft_clip(raw * (self.drive * KICK_IDM_DRIVE_MULT));
                let click_osc = sin_phase(self.click_phase);
                let c_sig =
                    click_osc * self.env_click * self.click_level * KICK_IDM_CLICK_LEVEL_MULT;
                let b_sig = warm
                    * (self.env_punch * KICK_IDM_PUNCH_WEIGHT
                        + self.env_body * KICK_IDM_BODY_WEIGHT);
                (b_sig, c_sig, KICK_IDM_CLICK_HZ, KICK_IDM_PITCH_DROP_MULT)
            }
            DrumMode::Industrial => {
                // Industrial mode:
                // Heavy wavefolded overdrive and distortion
                let raw = sin_phase(self.phase);
                let folded = (raw * (self.drive * KICK_IND_DRIVE_MULT)).sin();
                let warm =
                    soft_clip(folded * KICK_IND_FOLD_GAIN + KICK_IND_WARM_2ND_HARM * raw * raw);
                let click_osc = if sin_phase(self.click_phase) > 0.0 {
                    1.0
                } else {
                    -1.0
                };
                let c_sig =
                    click_osc * self.env_click * self.click_level * KICK_IND_CLICK_LEVEL_MULT;
                let b_sig = warm
                    * (self.env_punch * KICK_IND_PUNCH_WEIGHT
                        + self.env_body * KICK_IND_BODY_WEIGHT);
                (b_sig, c_sig, KICK_IND_CLICK_HZ, 1.0)
            }
            DrumMode::Analog => {
                // Analog (default 808/909):
                let raw = sin_phase(self.phase);
                let warm = soft_clip(raw * self.drive + KICK_ANALOG_WARM_2ND_HARM * raw * raw);
                let click_osc = sin_phase(self.click_phase);
                let c_sig = click_osc * self.env_click * self.click_level;
                let b_sig = warm
                    * (self.env_punch * KICK_ANALOG_PUNCH_WEIGHT
                        + self.env_body * KICK_ANALOG_BODY_WEIGHT);
                (b_sig, c_sig, KICK_ANALOG_CLICK_HZ, 1.0)
            }
        };

        let sig = soft_clip(body_sig + click_sig) * self.vel * KICK_MASTER_OUTPUT_BOOST;

        self.phase = wrap_phase(self.phase + freq / sample_rate);
        self.click_phase = wrap_phase(self.click_phase + click_hz / sample_rate);
        self.pitch_fast *= 1.0 - (self.pitch_decay_coeff * pitch_drop_mult);
        self.env_punch *= self.punch_decay;
        self.env_body *= self.body_decay;
        self.env_click *= self.click_decay;

        if self.env_body < MIN_AUDIBLE_VELOCITY && self.env_punch < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        sig
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.click_phase = 0.0;
        self.pitch_fast = 0.0;
        self.env_punch = 0.0;
        self.env_body = 0.0;
        self.env_click = 0.0;
    }
}

impl Default for KickVoice {
    fn default() -> Self {
        Self::new()
    }
}
