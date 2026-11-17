//! Voice allocation and polyphony voice-stealing strategies.

use crate::domain::synth::SynthVoice;
use crate::domain::{GlideTime, PatchId, Polyphony};

pub const MIN_LEGATO_GLIDE_SEC: f32 = 0.001;

/// Strategy returned by the voice allocator for dispatching a note event.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VoiceAllocation {
    /// Monophonic voice should glide to new frequency with portamento without phase reset.
    LegatoGlide { voice_idx: usize },
    /// Voice should be triggered (either fresh, retriggered, or stolen via LRU).
    Trigger { voice_idx: usize },
}

/// Zero-allocation voice allocator implementing monophonic legato, per-patch polyphony limits,
/// and deterministic Least-Recently-Used (LRU) global voice stealing.
pub struct VoiceAllocator;

impl VoiceAllocator {
    /// Determines the voice index and allocation strategy for a new note event.
    pub fn allocate<const N: usize>(
        voices: &[SynthVoice; N],
        patch_id: impl Into<PatchId>,
        polyphony: impl Into<Polyphony>,
        glide_time: impl Into<GlideTime>,
    ) -> VoiceAllocation {
        let pid = patch_id.into();
        let poly = *polyphony.into();
        let glide = *glide_time.into();
        if poly <= 1 {
            // 1. Monophonic mode: find existing active voice for this patch
            if let Some(i) = voices.iter().position(|v| v.active && v.patch_id == pid) {
                return if glide > MIN_LEGATO_GLIDE_SEC {
                    VoiceAllocation::LegatoGlide { voice_idx: i }
                } else {
                    VoiceAllocation::Trigger { voice_idx: i }
                };
            }
        } else {
            // 2. Polyphonic mode: enforce max polyphony for this specific patch
            let mut patch_count = 0;
            let mut oldest: Option<(usize, u32)> = None;
            for (i, v) in voices
                .iter()
                .enumerate()
                .filter(|(_, v)| v.active && v.patch_id == pid)
            {
                patch_count += 1;
                if oldest.is_none_or(|(_, max_age)| v.age > max_age) {
                    oldest = Some((i, v.age));
                }
            }
            if patch_count >= poly as usize {
                if let Some((oldest_idx, _)) = oldest {
                    return VoiceAllocation::Trigger {
                        voice_idx: oldest_idx,
                    };
                }
            }
        }

        // 3. Find first inactive voice in pool, or 4. Global LRU voice stealing
        let voice_idx = voices.iter().position(|v| !v.active).unwrap_or_else(|| {
            voices
                .iter()
                .enumerate()
                .max_by_key(|(_, v)| v.age)
                .map_or(0, |(i, _)| i)
        });
        VoiceAllocation::Trigger { voice_idx }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_allocate_inactive_voice_first() {
        let voices: [SynthVoice; 4] = std::array::from_fn(|_| SynthVoice::new());
        let alloc = VoiceAllocator::allocate(&voices, 0, 4, 0.0);
        assert_eq!(alloc, VoiceAllocation::Trigger { voice_idx: 0 });
    }

    #[test]
    fn test_monophonic_legato_glide_vs_retrigger() {
        let mut voices: [SynthVoice; 4] = std::array::from_fn(|_| SynthVoice::new());
        voices[1].active = true;
        voices[1].patch_id = PatchId(2);

        // With glide > 0.001 -> Legato
        let alloc_glide = VoiceAllocator::allocate(&voices, 2, 1, 0.05);
        assert_eq!(alloc_glide, VoiceAllocation::LegatoGlide { voice_idx: 1 });

        // With glide == 0.0 -> Retrigger same voice
        let alloc_retrigger = VoiceAllocator::allocate(&voices, 2, 1, 0.0);
        assert_eq!(alloc_retrigger, VoiceAllocation::Trigger { voice_idx: 1 });
    }

    #[test]
    fn test_per_patch_polyphony_stealing() {
        let mut voices: [SynthVoice; 4] = std::array::from_fn(|_| SynthVoice::new());
        voices[0].active = true;
        voices[0].patch_id = PatchId(1);
        voices[0].age = 100;

        voices[1].active = true;
        voices[1].patch_id = PatchId(1);
        voices[1].age = 500; // Oldest for patch 1

        // Polyphony limit is 2 for patch 1 -> should steal voice 1 (age 500)
        let alloc = VoiceAllocator::allocate(&voices, 1, 2, 0.0);
        assert_eq!(alloc, VoiceAllocation::Trigger { voice_idx: 1 });
    }

    #[test]
    fn test_global_lru_stealing_when_all_active() {
        let mut voices: [SynthVoice; 4] = std::array::from_fn(|_| SynthVoice::new());
        for (i, v) in voices.iter_mut().enumerate() {
            v.active = true;
            v.patch_id = PatchId(i);
            v.age = (i as u32 + 1) * 100;
        }
        voices[2].age = 1000; // Globally oldest

        let alloc = VoiceAllocator::allocate(&voices, 5, 8, 0.0);
        assert_eq!(alloc, VoiceAllocation::Trigger { voice_idx: 2 });
    }
}
