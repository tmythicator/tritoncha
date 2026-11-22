//! Fixed-capacity 16-voice polyphonic percussion pool with choke groups and LRU voice stealing.

use crate::domain::drums::patch::{PercPatch, MAX_PERC_VOICES};
use crate::domain::drums::voice::PercVoice;

/// Fixed-capacity polyphonic percussion voice pool with choke groups and LRU voice stealing.
#[derive(Clone)]
pub struct PercVoicePool {
    voices: [PercVoice; MAX_PERC_VOICES],
    age_counter: u64,
}

impl Default for PercVoicePool {
    fn default() -> Self {
        Self::new()
    }
}

impl PercVoicePool {
    pub fn new() -> Self {
        let mut voices: [PercVoice; MAX_PERC_VOICES] = std::array::from_fn(|i| {
            let seed = 0x5a82_7999_u32 ^ ((i as u32).wrapping_add(1)).wrapping_mul(0x9e37_79b9);
            PercVoice::with_seed(seed)
        });
        for (i, v) in voices.iter_mut().enumerate() {
            v.age = i as u64;
        }
        Self {
            voices,
            age_counter: MAX_PERC_VOICES as u64,
        }
    }

    /// Triggers a percussion patch in the pool.
    /// If `patch.choke_group > 0`, any currently active voices in the same choke group are smoothly choked.
    pub fn trigger(
        &mut self,
        patch: &PercPatch,
        vel: f32,
        note_freq_hz: Option<f32>,
        sample_rate: f32,
    ) {
        if patch.choke_group.is_active() {
            for v in &mut self.voices {
                if v.active && v.choke_group == patch.choke_group {
                    v.choke(sample_rate);
                }
            }
        }

        // Allocate first inactive voice, or steal oldest (LRU) voice
        let target_idx = self
            .voices
            .iter()
            .position(|v| !v.active)
            .unwrap_or_else(|| {
                self.voices
                    .iter()
                    .enumerate()
                    .min_by_key(|(_, v)| v.age)
                    .map(|(idx, _)| idx)
                    .unwrap_or(0)
            });

        self.age_counter += 1;
        let voice = &mut self.voices[target_idx];
        voice.age = self.age_counter;
        voice.trigger(patch, vel, note_freq_hz, sample_rate);
    }

    /// Renders and sums all active percussion voices for one audio frame.
    #[inline(always)]
    pub fn process(&mut self, sample_rate: f32) -> f32 {
        let mut sum = 0.0_f32;
        for v in &mut self.voices {
            if v.active {
                sum += v.process(sample_rate);
            }
        }
        sum
    }

    /// Returns the number of currently active voices in the pool.
    #[inline(always)]
    pub fn active_count(&self) -> usize {
        self.voices.iter().filter(|v| v.active).count()
    }

    pub fn reset(&mut self) {
        for v in &mut self.voices {
            v.reset();
        }
    }
}
