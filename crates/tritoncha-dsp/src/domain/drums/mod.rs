//! Layered parametric percussion engine (`PercPatch`, `PercVoice`, `PercVoicePool`)
//! organized by physical resonating body families (`Membrane`, `Shell`, `Cymbal`, `Idiophone`, `Burst`).

pub mod bodies;
pub mod patch;
pub mod pool;
pub mod voice;

pub use patch::{
    NoiseFilterMode, PercBodyType, PercPatch, MAX_KIT_SLOTS, MAX_PERC_VOICES,
    PERC_PATCH_PARAM_COUNT,
};
pub use pool::PercVoicePool;
pub use voice::PercVoice;

/// Minimum audible envelope threshold (-80 dB) below which a percussion voice deactivates.
pub const MIN_AUDIBLE_VELOCITY: f32 = 0.0001;

#[cfg(test)]
mod tests {
    use super::*;
    use crate::DEFAULT_SAMPLE_RATE;

    #[test]
    fn test_perc_voice_pool_initial_state_and_trigger() {
        let mut pool = PercVoicePool::new();
        assert_eq!(pool.active_count(), 0);
        assert_eq!(pool.process(DEFAULT_SAMPLE_RATE), 0.0);

        let patch = PercPatch::default();
        pool.trigger(&patch, 0.9, None, DEFAULT_SAMPLE_RATE);
        assert_eq!(pool.active_count(), 1);

        let mut peak = 0.0_f32;
        for _ in 0..256 {
            peak = peak.max(pool.process(DEFAULT_SAMPLE_RATE).abs());
        }
        assert!(peak > 0.05);

        pool.reset();
        assert_eq!(pool.active_count(), 0);
    }
}
