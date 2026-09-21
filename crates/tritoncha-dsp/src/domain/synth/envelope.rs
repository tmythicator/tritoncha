//! ADSR and multi-stage envelope generators for modular synthesis voices.

use crate::core::math::calc_rate;

pub const ENVELOPE_SILENCE_THRESHOLD: f32 = 0.0001;
pub const SUSTAIN_ACTIVE_THRESHOLD: f32 = 0.001;
pub const MIN_ENVELOPE_TIME_SEC: f32 = 0.00005;

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

/// 4-Stage ADSR Envelope Generator with hold duration support.
#[derive(Debug, Clone)]
pub struct AdsrEnvelope {
    pub stage: EnvelopeStage,
    pub level: f32,
    pub sustain_level: f32,
    pub attack_rate: f32,
    pub decay_rate: f32,
    pub release_rate: f32,
    pub hold_counter: u32,
    pub hold_samples: u32,
}

impl AdsrEnvelope {
    pub fn new() -> Self {
        Self {
            stage: EnvelopeStage::Idle,
            level: 0.0,
            sustain_level: 0.0,
            attack_rate: 0.01,
            decay_rate: 0.001,
            release_rate: 0.001,
            hold_counter: 0,
            hold_samples: 0,
        }
    }

    /// Triggers an ADSR envelope with given parameters.
    pub fn trigger(
        &mut self,
        attack_s: f32,
        decay_s: f32,
        sustain: f32,
        release_s: f32,
        hold_s: f32,
        sample_rate: f32,
    ) {
        self.stage = EnvelopeStage::Attack;
        self.sustain_level = sustain.clamp(0.0, 1.0);
        self.attack_rate = calc_rate(attack_s, sample_rate, MIN_ENVELOPE_TIME_SEC);
        self.decay_rate = calc_rate(decay_s, sample_rate, MIN_ENVELOPE_TIME_SEC);
        self.release_rate = calc_rate(release_s, sample_rate, MIN_ENVELOPE_TIME_SEC);
        self.hold_samples = (hold_s.max(0.0) * sample_rate).max(1.0) as u32;
        self.hold_counter = 0;
    }

    /// Triggers a simpler 2-stage Attack-Decay (AD) modulation envelope.
    pub fn trigger_ad(&mut self, attack_s: f32, decay_s: f32, sample_rate: f32) {
        self.stage = EnvelopeStage::Attack;
        self.sustain_level = 0.0;
        self.attack_rate = calc_rate(attack_s, sample_rate, MIN_ENVELOPE_TIME_SEC);
        self.decay_rate = calc_rate(decay_s, sample_rate, MIN_ENVELOPE_TIME_SEC);
        self.release_rate = self.decay_rate;
        self.hold_samples = 0;
        self.hold_counter = 0;
    }

    /// Retriggers the attack stage (e.g. for legato note re-articulation) without resetting current level.
    #[inline(always)]
    pub fn retrigger_attack(&mut self) {
        self.stage = EnvelopeStage::Attack;
        self.hold_counter = 0;
    }

    /// Resets the envelope to complete silence and idle state.
    #[inline(always)]
    pub fn reset(&mut self) {
        self.stage = EnvelopeStage::Idle;
        self.level = 0.0;
        self.hold_counter = 0;
    }

    /// Returns true if envelope is producing non-silent output.
    #[inline(always)]
    pub fn is_active(&self) -> bool {
        self.stage != EnvelopeStage::Idle || self.level > ENVELOPE_SILENCE_THRESHOLD
    }

    /// Advances envelope state by one sample and returns the current amplitude level [0.0, 1.0].
    #[inline(always)]
    pub fn process_sample(&mut self) -> f32 {
        match self.stage {
            EnvelopeStage::Attack => {
                self.level += self.attack_rate;
                if self.level >= 1.0 {
                    self.level = 1.0;
                    self.stage = EnvelopeStage::Decay;
                }
            }
            EnvelopeStage::Decay => {
                if self.level > self.sustain_level {
                    self.level -= self.decay_rate;
                    if self.level <= self.sustain_level {
                        self.level = self.sustain_level;
                        self.stage = if self.sustain_level > SUSTAIN_ACTIVE_THRESHOLD {
                            EnvelopeStage::Sustain
                        } else {
                            EnvelopeStage::Release
                        };
                    }
                } else {
                    self.level = self.sustain_level;
                    self.stage = if self.sustain_level > SUSTAIN_ACTIVE_THRESHOLD {
                        EnvelopeStage::Sustain
                    } else {
                        EnvelopeStage::Release
                    };
                }
            }
            EnvelopeStage::Sustain => {
                self.hold_counter = self.hold_counter.saturating_add(1);
                if self.hold_counter >= self.hold_samples {
                    self.stage = EnvelopeStage::Release;
                }
            }
            EnvelopeStage::Release => {
                self.level -= self.release_rate;
                if self.level <= ENVELOPE_SILENCE_THRESHOLD {
                    self.level = 0.0;
                    self.stage = EnvelopeStage::Idle;
                }
            }
            EnvelopeStage::Idle => {
                self.level = 0.0;
            }
        }
        self.level
    }
}

impl Default for AdsrEnvelope {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_adsr_envelope_lifecycle() {
        let mut env = AdsrEnvelope::new();
        assert_eq!(env.stage, EnvelopeStage::Idle);
        assert_eq!(env.level, 0.0);

        let sr = crate::engine::DEFAULT_SAMPLE_RATE;
        let attack_s = 10.0 / sr;
        let decay_s = 10.0 / sr;
        let sustain = 0.5;
        let release_s = 10.0 / sr;
        let hold_s = 50.0 / sr;

        env.trigger(attack_s, decay_s, sustain, release_s, hold_s, sr);
        assert_eq!(env.stage, EnvelopeStage::Attack);

        for _ in 0..10 {
            env.process_sample();
        }
        assert!(env.level >= 0.99);

        for _ in 0..10 {
            env.process_sample();
        }
        assert_eq!(env.stage, EnvelopeStage::Sustain);
        assert!((env.level - 0.5).abs() < 0.05);

        while env.stage == EnvelopeStage::Sustain {
            env.process_sample();
        }
        assert_eq!(env.hold_counter, 50);
        assert_eq!(env.stage, EnvelopeStage::Release);

        while env.stage == EnvelopeStage::Release {
            env.process_sample();
        }
        assert_eq!(env.stage, EnvelopeStage::Idle);
        assert_eq!(env.level, 0.0);
    }
}
