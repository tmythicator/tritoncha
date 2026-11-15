//! Membrane drum voices (toms, kicks, and tuned sub-percussion).

use super::{DrumMode, MIN_AUDIBLE_VELOCITY};
use crate::core::math::{sin_phase, soft_clip, t60_decay_coeff, wrap_phase};
use crate::domain::{
    AdsrTime, Drive, DrumPitch, DrumVelocity, PitchSnapDecay, DEFAULT_SAMPLE_RATE,
};

// Tuned frequency ranges for membrane toms
pub const TOM_HIGH_START_HZ: f32 = 240.0;
pub const TOM_HIGH_MIN_HZ: f32 = 170.0;
pub const TOM_MID_START_HZ: f32 = 180.0;
pub const TOM_MID_MIN_HZ: f32 = 120.0;
pub const TOM_LOW_START_HZ: f32 = 120.0;
pub const TOM_LOW_MIN_HZ: f32 = 80.0;
pub const TOM_DEFAULT_PITCH_DECAY: f32 = 0.015;
pub const TOM_DEFAULT_AMP_DECAY: f32 = 0.9992;
pub const TOM_DEFAULT_DRIVE: f32 = 1.0;

/// Membrane drum synthesis parameters.
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct MembraneParams {
    pub start_pitch: DrumPitch,
    pub min_pitch: DrumPitch,
    pub pitch_decay: PitchSnapDecay,
    pub decay: AdsrTime,
    pub drive: Drive,
    pub mode: DrumMode,
}

impl Default for MembraneParams {
    fn default() -> Self {
        Self {
            start_pitch: TOM_MID_START_HZ.into(),
            min_pitch: TOM_MID_MIN_HZ.into(),
            pitch_decay: TOM_DEFAULT_PITCH_DECAY.into(),
            decay: 0.25.into(),
            drive: TOM_DEFAULT_DRIVE.into(),
            mode: DrumMode::default(),
        }
    }
}

impl From<[f32; 7]> for MembraneParams {
    #[inline(always)]
    fn from(p: [f32; 7]) -> Self {
        Self {
            start_pitch: p[0].into(),
            min_pitch: p[1].into(),
            pitch_decay: p[2].into(),
            decay: p[3].into(),
            drive: p[4].into(),
            mode: p[6].into(),
        }
    }
}

/// Membrane Drum Voice modeling toms and tuned sub-percussion.
#[derive(Clone)]
pub struct MembraneVoice {
    pub active: bool,
    phase: f32,
    click_phase: f32,
    freq: f32,
    env: f32,
    click_env: f32,
    pitch_fast: f32,
    vel: f32,
    start_pitch_hz: f32,
    min_pitch_hz: f32,
    pitch_decay_coeff: f32,
    amp_decay_coeff: f32,
    drive_gain: f32,
    pub mode: DrumMode,
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
            click_phase: 0.0,
            freq: min_pitch_hz,
            env: 0.0,
            click_env: 0.0,
            pitch_fast: 0.0,
            vel: 0.0,
            start_pitch_hz,
            min_pitch_hz,
            pitch_decay_coeff,
            amp_decay_coeff,
            drive_gain,
            mode: DrumMode::default(),
        }
    }

    pub fn trigger(&mut self, vel: impl Into<DrumVelocity>) {
        self.active = true;
        self.phase = 0.05;
        self.click_phase = 0.0;
        self.freq = self.start_pitch_hz;
        self.pitch_fast = (self.start_pitch_hz - self.min_pitch_hz).max(30.0) * 0.85;
        self.env = 1.0;
        self.click_env = 1.0;
        self.vel = *vel.into();
    }

    pub fn trigger_freq(&mut self, vel: impl Into<DrumVelocity>, freq: f32) {
        self.active = true;
        self.phase = 0.05;
        self.click_phase = 0.0;
        let base_f = freq.max(self.min_pitch_hz);
        self.freq = base_f * 1.35;
        self.pitch_fast = base_f * 0.45;
        self.env = 1.0;
        self.click_env = 1.0;
        self.vel = *vel.into();
    }

    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        // Instantaneous frequency: shell fundamental + initial impact tension snap
        let cur_freq = (self.freq + self.pitch_fast).clamp(20.0, sample_rate * 0.45);

        // Stick tip impact click (~3200 Hz with fast ~3ms decay)
        let click_sig = if self.click_env > MIN_AUDIBLE_VELOCITY {
            let clk = sin_phase(self.click_phase) * self.click_env * 0.40;
            self.click_phase = wrap_phase(self.click_phase + 3200.0 / sample_rate);
            self.click_env *= 0.993;
            clk
        } else {
            0.0
        };

        // Multi-modal circular membrane vibration (Bessel ratios: 1.0, 1.587, 2.140)
        let membrane_val = match self.mode {
            DrumMode::Natural => {
                let m0 = sin_phase(self.phase);
                let m1 = sin_phase(self.phase * 1.587) * 0.32 * self.env;
                let m2 = sin_phase(self.phase * 2.140) * 0.14 * self.env * self.env;
                m0 + m1 + m2
            }
            DrumMode::Analog => {
                let m0 = sin_phase(self.phase);
                let m1 = sin_phase(self.phase * 1.587) * 0.20 * self.env;
                m0 + m1
            }
            DrumMode::Idm => {
                let chirp = sin_phase(self.phase * 3.2) * 0.6 * self.env;
                sin_phase(self.phase + chirp)
            }
            DrumMode::Industrial => {
                let raw = sin_phase(self.phase);
                (raw * 2.4).sin()
            }
        };

        let body_sig = soft_clip(membrane_val * self.drive_gain) * self.env;
        let sig = (body_sig + click_sig) * self.vel * 1.20;

        self.phase = wrap_phase(self.phase + cur_freq / sample_rate);

        // Exponential frequency relaxation to base shell pitch
        self.freq += (self.min_pitch_hz - self.freq) * (self.pitch_decay_coeff * 2.2);
        self.pitch_fast *= 0.985; // Fast decay for initial impact tension snap (~8-10ms)
        self.env *= self.amp_decay_coeff;

        if self.env < MIN_AUDIBLE_VELOCITY && self.click_env < MIN_AUDIBLE_VELOCITY {
            self.active = false;
        }

        soft_clip(sig)
    }

    pub fn set_params(&mut self, params: impl Into<MembraneParams>) {
        let p = params.into();
        self.start_pitch_hz = *p.start_pitch;
        self.min_pitch_hz = *p.min_pitch;
        self.pitch_decay_coeff = *p.pitch_decay;
        self.amp_decay_coeff = t60_decay_coeff(*p.decay, DEFAULT_SAMPLE_RATE);
        self.drive_gain = *p.drive;
        self.mode = p.mode;
    }

    pub fn reset(&mut self) {
        self.active = false;
        self.phase = 0.0;
        self.click_phase = 0.0;
        self.env = 0.0;
        self.click_env = 0.0;
        self.pitch_fast = 0.0;
    }
}
