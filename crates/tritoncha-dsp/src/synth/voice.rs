//! Modular synthesizer voice DSP implementation with bandlimited anti-aliased oscillators.

use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{poly_blep, soft_clip, wrap_phase, xorshift32_norm};
pub use crate::synth::patch::*;
use std::f32::consts::PI;

// Voice Tuning and Timing Constants
pub const MIN_PORTAMENTO_TIME_SEC: f32 = 0.001;
pub const MIN_GLIDE_RATE: f32 = 0.0001;
pub const MAX_GLIDE_RATE: f32 = 1.0;
pub const PORTAMENTO_EPSILON: f32 = 0.05;

pub const MIN_VELOCITY: f32 = 0.0;
pub const MAX_VELOCITY: f32 = 1.0;
pub const MIN_PULSE_WIDTH: f32 = 0.05;
pub const MAX_PULSE_WIDTH: f32 = 0.95;

pub const ENVELOPE_SILENCE_THRESHOLD: f32 = 0.0001;
pub const SUSTAIN_ACTIVE_THRESHOLD: f32 = 0.001;
pub const PITCH_MOD_ACTIVE_THRESHOLD: f32 = 0.001;

pub const VCO_BASE_DRIFT_HZ: f32 = 0.2;
pub const VCO_DRIFT_INCREMENT_HZ: f32 = 0.06;
pub const NUM_DRIFT_VOICE_SLOTS: usize = 7;

pub const KARPLUS_BUFFER_SIZE: usize = 1024;
pub const KARPLUS_MIN_LEN: f32 = 4.0;
pub const KARPLUS_MAX_LEN: f32 = 1020.0;
pub const KARPLUS_FEEDBACK_COEFF: f32 = 0.495;
pub const KARPLUS_OUTPUT_GAIN: f32 = 1.5;

pub const SUPERSAW_DETUNE_OFFSETS: [f32; 7] = [-0.012, -0.007, -0.003, 0.0, 0.003, 0.007, 0.012];
pub const SUPERSAW_NORMALIZATION: f32 = 0.25;

pub const ORGAN_NORMALIZATION: f32 = 0.6;
pub const CS80_BLADE_GAIN: f32 = 0.45;
pub const REESE_GAIN: f32 = 0.5;
pub const HOOVER_PULSE_GAIN: f32 = 0.7;
pub const HOOVER_SUB_GAIN: f32 = 0.3;

pub const DEFAULT_INITIAL_NOISE_SEED: u32 = 0x9e3779b9;
pub const NOISE_SEED_PRIME: u32 = 2654435761;

/// Strongly typed ADSR envelope stage.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
#[repr(u8)]
pub enum EnvelopeStage {
    #[default]
    Idle = 0,
    Attack = 1,
    Decay = 2,
    Sustain = 3,
    Release = 4,
}

impl From<u8> for EnvelopeStage {
    #[inline(always)]
    fn from(val: u8) -> Self {
        match val {
            1 => EnvelopeStage::Attack,
            2 => EnvelopeStage::Decay,
            3 => EnvelopeStage::Sustain,
            4 => EnvelopeStage::Release,
            _ => EnvelopeStage::Idle,
        }
    }
}

/// Polyphonic Synthesizer Voice Aggregate.
pub struct SynthVoice {
    pub active: bool,
    pub patch_id: usize,
    pub freq: f32,
    pub target_freq: f32,
    pub glide_rate: f32,
    pub velocity: f32,
    pub age: u32,
    phase: f32,
    phase_inc: f32,
    sub_phase: f32,
    mod_phase: f32,
    env_level: f32,
    env_stage: EnvelopeStage,
    sustain_level: f32,
    hold_counter: u32,
    hold_samples: u32,
    mod_env_level: f32,
    mod_env_stage: EnvelopeStage,
    attack_rate: f32,
    decay_rate: f32,
    release_rate: f32,
    mod_attack_rate: f32,
    mod_decay_rate: f32,
    filter: StateVariableFilter,

    // Karplus-Strong string synthesis buffer
    ks_buffer: [f32; KARPLUS_BUFFER_SIZE],
    ks_pos: usize,
    ks_len: usize,

    // Analog synthesis state modeling
    noise_seed: u32,
    pitch_snap_level: f32,
    pitch_snap_rate: f32,
    drift_phase: f32,
    drift_phase_inc: f32,
}

impl SynthVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            patch_id: PATCH_SAW_BASS,
            freq: 440.0,
            target_freq: 440.0,
            glide_rate: 1.0,
            velocity: 0.0,
            age: 0,
            phase: 0.0,
            phase_inc: 0.0,
            sub_phase: 0.0,
            mod_phase: 0.0,
            env_level: 0.0,
            env_stage: EnvelopeStage::Idle,
            sustain_level: 0.0,
            hold_counter: 0,
            hold_samples: 0,
            mod_env_level: 0.0,
            mod_env_stage: EnvelopeStage::Idle,
            attack_rate: 0.01,
            decay_rate: 0.001,
            release_rate: 0.001,
            mod_attack_rate: 0.01,
            mod_decay_rate: 0.001,
            filter: StateVariableFilter::new(),
            ks_buffer: [0.0; KARPLUS_BUFFER_SIZE],
            ks_pos: 0,
            ks_len: 200,
            noise_seed: DEFAULT_INITIAL_NOISE_SEED,
            pitch_snap_level: 0.0,
            pitch_snap_rate: 0.01,
            drift_phase: 0.0,
            drift_phase_inc: 0.0001,
        }
    }

    pub fn glide_to(
        &mut self,
        target_freq: f32,
        vel: f32,
        sample_rate: f32,
        glide_time: f32,
        dur_s: f32,
    ) {
        self.target_freq = target_freq.clamp(MIN_FREQ_HZ, MAX_FREQ_HZ);
        self.velocity = vel.clamp(MIN_VELOCITY, MAX_VELOCITY);
        let time_s = glide_time.max(MIN_PORTAMENTO_TIME_SEC);
        self.glide_rate = (1.0 / (time_s * sample_rate)).clamp(MIN_GLIDE_RATE, MAX_GLIDE_RATE);
        self.age = 0;
        self.hold_counter = 0;
        self.hold_samples = (dur_s * sample_rate).clamp(10.0, MAX_HOLD_SEC * sample_rate) as u32;

        // Retrigger envelopes so consecutive step notes articulate with punchy attack
        self.env_stage = EnvelopeStage::Attack;
        self.mod_env_stage = EnvelopeStage::Attack;
    }

    pub fn trigger(
        &mut self,
        freq: f32,
        vel: f32,
        patch_id: usize,
        patch: &ModularPatch,
        sample_rate: f32,
        dur_s: f32,
    ) {
        self.active = true;
        self.patch_id = patch_id;
        self.freq = freq.clamp(MIN_FREQ_HZ, MAX_FREQ_HZ);
        self.target_freq = self.freq;
        self.glide_rate = 1.0;
        self.velocity = vel.clamp(MIN_VELOCITY, MAX_VELOCITY);
        self.age = 0;
        self.phase = 0.0;
        self.sub_phase = 0.0;
        self.mod_phase = 0.0;
        self.phase_inc = self.freq / sample_rate;

        self.env_stage = EnvelopeStage::Attack;
        self.sustain_level = patch.sustain.clamp(0.0, 1.0);
        self.attack_rate = 1.0 / (patch.attack.max(MIN_ATTACK_SEC) * sample_rate);
        self.decay_rate = 1.0 / (patch.decay.max(MIN_DECAY_SEC) * sample_rate);
        self.release_rate = 1.0 / (patch.release.max(MIN_RELEASE_SEC) * sample_rate);

        let hold_time = if dur_s > MIN_HOLD_SEC {
            dur_s
        } else if patch.polyphony > 1 {
            DEFAULT_POLY_HOLD_SEC
        } else {
            DEFAULT_MONO_HOLD_SEC
        };
        self.hold_samples =
            (hold_time * sample_rate).clamp(10.0, MAX_HOLD_SEC * sample_rate) as u32;
        self.hold_counter = 0;

        self.mod_env_stage = EnvelopeStage::Attack;
        self.mod_attack_rate = 1.0 / (patch.mod_attack.max(0.001) * sample_rate);
        self.mod_decay_rate = 1.0 / (patch.mod_decay.max(0.005) * sample_rate);

        if patch.osc_type == OSC_KARPLUS {
            // Initialize Karplus-Strong string burst with white noise excitation
            self.ks_len =
                (sample_rate / self.freq).clamp(KARPLUS_MIN_LEN, KARPLUS_MAX_LEN) as usize;
            self.ks_pos = 0;
            let mut seed: u32 = DEFAULT_INITIAL_NOISE_SEED;
            for i in 0..self.ks_len {
                self.ks_buffer[i] = xorshift32_norm(&mut seed);
            }
        }

        // Initialize noise seed deterministically per voice trigger to eliminate inter-voice correlation
        self.noise_seed = DEFAULT_INITIAL_NOISE_SEED
            ^ (self.age.wrapping_mul(NOISE_SEED_PRIME)).wrapping_add((self.freq as u32) << 8);

        // Pitch attack transient snap
        if patch.pitch_snap > 0.01 {
            self.pitch_snap_level = 1.0;
            let decay_s = patch
                .pitch_snap_decay
                .clamp(MIN_PITCH_SNAP_DECAY_SEC, MAX_PITCH_SNAP_DECAY_SEC);
            self.pitch_snap_rate = 1.0 / (decay_s * sample_rate);
        } else {
            self.pitch_snap_level = 0.0;
            self.pitch_snap_rate = 0.0;
        }

        // Low-frequency VCO drift rate (0.2 .. 0.6 Hz)
        let drift_hz = VCO_BASE_DRIFT_HZ
            + ((patch_id % NUM_DRIFT_VOICE_SLOTS) as f32) * VCO_DRIFT_INCREMENT_HZ;
        self.drift_phase_inc = drift_hz / sample_rate;

        self.filter.reset();
    }

    #[inline(always)]
    pub fn process_sample(&mut self, patch: &ModularPatch, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        self.age = self.age.wrapping_add(1);

        // Portamento / Pitch Glide
        if (self.freq - self.target_freq).abs() > PORTAMENTO_EPSILON {
            self.freq += (self.target_freq - self.freq) * self.glide_rate;
        }

        // Pitch Envelope Decay (Transient Snap)
        if self.pitch_snap_level > 0.0 {
            self.pitch_snap_level -= self.pitch_snap_rate;
            if self.pitch_snap_level < 0.0 {
                self.pitch_snap_level = 0.0;
            }
        }

        // Calculate pitch modulation from transient snap and VCO analog drift
        let pitch_snap_st = patch.pitch_snap * self.pitch_snap_level;
        let drift_st = if patch.analog_drift > 0.001 {
            self.drift_phase = wrap_phase(self.drift_phase + self.drift_phase_inc);
            (self.drift_phase * 2.0 * PI).sin() * patch.analog_drift * MAX_ANALOG_DRIFT_SEMITONES
        } else {
            0.0
        };

        let total_mod_st = pitch_snap_st + drift_st;
        let eff_freq = if total_mod_st.abs() > PITCH_MOD_ACTIVE_THRESHOLD {
            self.freq * (2.0f32).powf(total_mod_st / 12.0)
        } else {
            self.freq
        };
        self.phase_inc = eff_freq / sample_rate;

        // 4-Stage ADSR Envelope State Machine
        match self.env_stage {
            EnvelopeStage::Attack => {
                self.env_level += self.attack_rate;
                if self.env_level >= 1.0 {
                    self.env_level = 1.0;
                    self.env_stage = EnvelopeStage::Decay;
                }
            }
            EnvelopeStage::Decay => {
                if self.env_level > self.sustain_level {
                    self.env_level -= self.decay_rate;
                    if self.env_level <= self.sustain_level {
                        self.env_level = self.sustain_level;
                        self.env_stage = if self.sustain_level > SUSTAIN_ACTIVE_THRESHOLD {
                            EnvelopeStage::Sustain
                        } else {
                            EnvelopeStage::Release
                        };
                    }
                } else {
                    self.env_level = self.sustain_level;
                    self.env_stage = if self.sustain_level > SUSTAIN_ACTIVE_THRESHOLD {
                        EnvelopeStage::Sustain
                    } else {
                        EnvelopeStage::Release
                    };
                }
            }
            EnvelopeStage::Sustain => {
                self.hold_counter = self.hold_counter.saturating_add(1);
                if self.hold_counter >= self.hold_samples {
                    self.env_stage = EnvelopeStage::Release;
                }
            }
            EnvelopeStage::Release => {
                self.env_level -= self.release_rate;
                if self.env_level <= ENVELOPE_SILENCE_THRESHOLD {
                    self.env_level = 0.0;
                    self.active = false;
                    return 0.0;
                }
            }
            EnvelopeStage::Idle => {
                self.active = false;
                return 0.0;
            }
        }

        // Modulation Envelope (for dynamic filter sweeps)
        if self.mod_env_stage == EnvelopeStage::Attack {
            self.mod_env_level += self.mod_attack_rate;
            if self.mod_env_level >= 1.0 {
                self.mod_env_level = 1.0;
                self.mod_env_stage = EnvelopeStage::Decay;
            }
        } else {
            self.mod_env_level -= self.mod_decay_rate;
            if self.mod_env_level < 0.0 {
                self.mod_env_level = 0.0;
            }
        }

        let dt = self.phase_inc;
        let osc_type = OscillatorType::from(patch.osc_type);

        let mut osc = match osc_type {
            OscillatorType::Saw => 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt),

            OscillatorType::Pulse => {
                let pw = patch.pulse_width.clamp(MIN_PULSE_WIDTH, MAX_PULSE_WIDTH);
                let raw = if self.phase < pw { 1.0 } else { -1.0 };
                raw - poly_blep(self.phase, dt) + poly_blep((self.phase + (1.0 - pw)) % 1.0, dt)
            }

            OscillatorType::Triangle => 2.0 * (2.0 * (self.phase - 0.5).abs() - 0.5),

            OscillatorType::Sine => (self.phase * 2.0 * PI).sin(),

            OscillatorType::Supersaw => {
                let mut sum = 0.0;
                for &d in &SUPERSAW_DETUNE_OFFSETS {
                    let p = (self.phase * (1.0 + d)).fract();
                    sum += 2.0 * p - 1.0 - poly_blep(p, dt * (1.0 + d));
                }
                sum * SUPERSAW_NORMALIZATION
            }

            OscillatorType::Karplus => {
                let r_pos = self.ks_pos;
                let next_pos = (self.ks_pos + 1) % self.ks_len;
                let out = self.ks_buffer[r_pos];
                let new_val =
                    (self.ks_buffer[r_pos] + self.ks_buffer[next_pos]) * KARPLUS_FEEDBACK_COEFF;
                self.ks_buffer[r_pos] = new_val;
                self.ks_pos = next_pos;
                out * KARPLUS_OUTPUT_GAIN
            }

            OscillatorType::Organ => {
                let h1 = (self.phase * 2.0 * PI).sin();
                let h2 = (self.phase * 4.0 * PI).sin() * 0.5;
                let h3 = (self.phase * 6.0 * PI).sin() * 0.25;
                let h4 = (self.phase * 8.0 * PI).sin() * 0.125;
                (h1 + h2 + h3 + h4) * ORGAN_NORMALIZATION
            }

            OscillatorType::Chiptune => {
                let pw = patch.pulse_width.clamp(0.1, 0.9);
                if self.phase < pw {
                    0.8
                } else {
                    -0.8
                }
            }

            OscillatorType::Fm => {
                let mod_freq = dt * 2.0;
                self.mod_phase = wrap_phase(self.mod_phase + mod_freq);
                let mod_val = (self.mod_phase * 2.0 * PI).sin() * 2.8 * self.env_level;
                (self.phase * 2.0 * PI + mod_val).sin()
            }

            OscillatorType::Reese => {
                let dt1 = dt * 0.992;
                let dt2 = dt * 1.008;
                let saw1 = 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt1);
                let p2 = (self.phase * 1.016) % 1.0;
                let saw2 = 2.0 * p2 - 1.0 - poly_blep(p2, dt2);
                saw1 * REESE_GAIN + saw2 * REESE_GAIN
            }

            OscillatorType::Blade => {
                let saw1 = 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt);
                let p2 = (self.phase * 1.004) % 1.0;
                let saw2 = 2.0 * p2 - 1.0 - poly_blep(p2, dt * 1.004);
                (saw1 + saw2) * CS80_BLADE_GAIN
            }

            OscillatorType::Hoover => {
                let pwm = 0.5 + 0.3 * (self.phase * 4.0 * PI).sin();
                let pulse = if self.phase < pwm { 0.7 } else { -0.7 };
                let sub = if self.sub_phase < 0.5 { 0.4 } else { -0.4 };
                pulse * HOOVER_PULSE_GAIN + sub * HOOVER_SUB_GAIN
            }

            OscillatorType::Click => {
                let click_pulse = if self.phase < 0.5 { 1.0 } else { -1.0 };
                let click_sine = (self.phase * 2.0 * PI).sin();
                (click_sine * 0.85 + click_pulse * 0.35) * (self.freq / 1600.0).clamp(0.6, 2.0)
            }
        };

        // Sub-oscillator synthesis
        if patch.sub_level > 0.0 {
            let sub = (self.sub_phase * 2.0 * PI).sin();
            osc = osc * (1.0 - patch.sub_level * 0.5) + sub * patch.sub_level;
        }

        // Per-voice white noise injection via xorshift32 PRNG
        if patch.noise_level > 0.001 {
            let white = xorshift32_norm(&mut self.noise_seed);
            osc += white * patch.noise_level.clamp(0.0, 1.0);
        }

        self.phase = wrap_phase(self.phase + dt);
        self.sub_phase = wrap_phase(self.sub_phase + dt * 0.5);

        osc *= self.velocity * self.env_level;

        // Dynamic filter cutoff with base + key tracking + envelope modulation
        let cutoff = (patch.cutoff_base
            + self.freq * patch.cutoff_key_track
            + patch.cutoff_env_amt * self.mod_env_level)
            .clamp(20.0, 20000.0);

        let filtered = self.filter.process_with_drive(
            osc,
            cutoff,
            patch.resonance,
            sample_rate,
            patch.filter_type,
            patch.filter_drive,
        );
        soft_clip(filtered)
    }
}

impl Default for SynthVoice {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_voice_initial_state() {
        let voice = SynthVoice::new();
        assert!(!voice.active);
        assert_eq!(voice.age, 0);
        assert_eq!(voice.env_stage, EnvelopeStage::Idle);
    }

    #[test]
    fn test_voice_trigger_and_envelope_progression() {
        let mut voice = SynthVoice::new();
        let patch = ModularPatch::default_lead();
        let sr = 48000.0;

        voice.trigger(440.0, 0.9, PATCH_LEAD, &patch, sr, 0.1);
        assert!(voice.active);
        assert_eq!(voice.env_stage, EnvelopeStage::Attack);
        assert_eq!(voice.freq, 440.0);

        // Process samples and verify output is generated
        let mut max_sample = 0.0_f32;
        for _ in 0..100 {
            let s = voice.process_sample(&patch, sr);
            max_sample = max_sample.max(s.abs());
        }
        assert!(max_sample > 0.0);
    }

    #[test]
    fn test_voice_portamento_glide() {
        let mut voice = SynthVoice::new();
        let patch = ModularPatch::default_lead();
        let sr = 48000.0;

        voice.trigger(220.0, 0.8, PATCH_LEAD, &patch, sr, 0.5);
        voice.glide_to(440.0, 0.8, sr, 0.05, 0.5);
        assert_eq!(voice.target_freq, 440.0);
        assert!(voice.freq < 440.0);

        // Advance voice and ensure pitch converges toward target
        for _ in 0..15000 {
            voice.process_sample(&patch, sr);
        }
        assert!((voice.freq - 440.0).abs() < 1.0);
    }

    #[test]
    fn test_all_oscillator_types_bounded_output() {
        let sr = 48000.0;
        for osc_id in 0..=12 {
            let mut voice = SynthVoice::new();
            let mut patch = ModularPatch::default_lead();
            patch.osc_type = osc_id;
            patch.attack = 0.001;
            patch.sustain = 1.0;

            voice.trigger(300.0, 1.0, 0, &patch, sr, 0.2);

            for _ in 0..500 {
                let s = voice.process_sample(&patch, sr);
                assert!(
                    s.is_finite(),
                    "Oscillator {} produced non-finite output",
                    osc_id
                );
                assert!(
                    s.abs() <= 2.0,
                    "Oscillator {} exceeded safe ceiling: {}",
                    osc_id,
                    s
                );
            }
        }
    }

    #[test]
    fn test_karplus_strong_synthesis() {
        let mut voice = SynthVoice::new();
        let mut patch = ModularPatch::default_lead();
        patch.osc_type = OSC_KARPLUS;
        let sr = 48000.0;

        voice.trigger(220.0, 0.9, PATCH_KARPLUS, &patch, sr, 0.1);
        assert!(voice.ks_len > 0);

        let s1 = voice.process_sample(&patch, sr);
        assert!(s1.is_finite());
    }
}
