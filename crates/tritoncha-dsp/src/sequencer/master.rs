//! Master sequencer managing timing, transport, and 16-track patterns.

use super::track::{
    TrackPattern, DEFAULT_STEP_DURATION_S, DEFAULT_STEP_VELOCITY, DEFAULT_SYNTH_PATCH_ID,
    MAX_STEPS, MAX_TRACKS,
};
use super::trigger_mask::TriggerMask;
use crate::dsp::math::midi_to_freq;
use crate::synth::drums::{is_drum_inst, INST_DRUM_TOM};

pub const DEFAULT_SAMPLE_RATE: f32 = 48000.0;
pub const DEFAULT_BPM: f32 = 168.0;
pub const MIN_BPM: f32 = 30.0;
pub const MAX_BPM: f32 = 300.0;
pub const SECONDS_PER_MINUTE: f32 = 60.0;
pub const STEPS_PER_BEAT_64TH: f32 = 16.0;
pub const SEQUENCER_TICK_MODULO: usize = 65536;
pub const MIN_AUDIBLE_VELOCITY: f32 = 0.001;

pub const INST_CLICK: i32 = 11;
pub const DEFAULT_CLICK_FREQ_HZ: f32 = 2400.0;
pub const DEFAULT_NOTE_FREQ_HZ: f32 = 440.0;

/// Event triggered by a track at a sequencer step: (inst_id, freq, vel, dur_s)
pub type SequencerEvent = (i32, f32, f32, f32);

/// Master sequencer coordinating real-time playback of up to 16 tracks.
pub struct MasterSequencer {
    pub sample_rate: f32,
    pub playing: bool,
    pub bpm: f32,
    samples_per_step: f32,
    step_timer: f32,
    pub current_step: usize,

    pub tracks: [TrackPattern; MAX_TRACKS],
    pub solo_active: bool,
    pub triggered_mask: TriggerMask,
}

impl MasterSequencer {
    pub fn new(sample_rate: f32) -> Self {
        let sr = if sample_rate > 0.0 {
            sample_rate
        } else {
            DEFAULT_SAMPLE_RATE
        };
        let bpm = DEFAULT_BPM;
        let samples_per_64th = (sr * SECONDS_PER_MINUTE) / (bpm * STEPS_PER_BEAT_64TH);

        Self {
            sample_rate: sr,
            playing: false,
            bpm,
            samples_per_step: samples_per_64th,
            step_timer: 0.0,
            current_step: 0,
            tracks: std::array::from_fn(|_| TrackPattern::new()),
            solo_active: false,
            triggered_mask: TriggerMask::new(),
        }
    }

    pub fn set_bpm(&mut self, bpm: f32) {
        if (MIN_BPM..=MAX_BPM).contains(&bpm) {
            self.bpm = bpm;
            self.samples_per_step =
                (self.sample_rate * SECONDS_PER_MINUTE) / (bpm * STEPS_PER_BEAT_64TH);
        }
    }

    pub fn set_playing(&mut self, playing: bool) {
        self.playing = playing;
        if !playing {
            self.current_step = 0;
            self.step_timer = 0.0;
        }
    }

    pub fn set_track(
        &mut self,
        track_idx: usize,
        inst_ids: &[i16],
        notes: &[i16],
        vels: &[f32],
        durs: &[f32],
        step_mult: usize,
    ) {
        if track_idx >= MAX_TRACKS {
            return;
        }

        let tr = &mut self.tracks[track_idx];
        let len = notes.len().min(MAX_STEPS);

        if len == 0 || notes.iter().all(|&n| n < 0) {
            tr.clear();
            return;
        }

        tr.active = true;
        tr.length = len;
        tr.step_multiplier = step_mult.max(1);

        for i in 0..len {
            let inst = if i < inst_ids.len() {
                inst_ids[i]
            } else {
                DEFAULT_SYNTH_PATCH_ID
            };
            let vel = if i < vels.len() {
                vels[i]
            } else {
                DEFAULT_STEP_VELOCITY
            };
            let dur = if i < durs.len() {
                durs[i]
            } else {
                DEFAULT_STEP_DURATION_S
            };
            tr.set_step(i, inst, notes[i], vel, dur);
        }
    }

    pub fn deactivate_track(&mut self, track_idx: usize) {
        if track_idx < MAX_TRACKS {
            self.tracks[track_idx].clear();
        }
    }

    pub fn clear_tracks(&mut self) {
        for tr in &mut self.tracks {
            tr.clear();
            tr.muted = false;
            tr.solo = false;
        }
        self.solo_active = false;
    }

    pub fn mute_track(&mut self, track_idx: usize, muted: bool) {
        if track_idx < MAX_TRACKS {
            self.tracks[track_idx].muted = muted;
        }
    }

    pub fn solo_track(&mut self, track_idx: usize, solo: bool) {
        if track_idx < MAX_TRACKS {
            self.tracks[track_idx].solo = solo;
            self.solo_active = self.tracks.iter().any(|t| t.solo);
        }
    }

    /// Advances the sequencer timer by one audio sample.
    /// If a step boundary is reached, collects triggered track events into out_events buffer.
    /// Returns the number of events triggered at this sample (0 if not a step boundary).
    #[inline(always)]
    pub fn advance_sample(&mut self, out_events: &mut [SequencerEvent; MAX_TRACKS]) -> usize {
        if !self.playing {
            return 0;
        }

        self.step_timer += 1.0;
        if self.step_timer < self.samples_per_step {
            return 0;
        }

        self.step_timer -= self.samples_per_step;
        self.collect_step_events(out_events)
    }

    #[inline(always)]
    fn collect_step_events(&mut self, out_events: &mut [SequencerEvent; MAX_TRACKS]) -> usize {
        let step = self.current_step;
        let mut count = 0;

        for (tr_idx, tr) in self.tracks.iter().enumerate() {
            if !tr.is_audible(self.solo_active) {
                continue;
            }

            if !step.is_multiple_of(tr.step_multiplier) {
                continue;
            }

            let pat_idx = (step / tr.step_multiplier) % tr.length.max(1);
            let note = tr.notes[pat_idx];
            let vel = tr.velocities[pat_idx];
            let inst_id = tr.inst_kinds[pat_idx] as i32;
            let dur_s = tr.durations[pat_idx];

            if note >= 0 && vel > MIN_AUDIBLE_VELOCITY {
                let freq = if !is_drum_inst(inst_id) {
                    if note > 0 {
                        midi_to_freq(note as f32)
                    } else if inst_id == INST_CLICK {
                        DEFAULT_CLICK_FREQ_HZ
                    } else {
                        DEFAULT_NOTE_FREQ_HZ
                    }
                } else if inst_id == INST_DRUM_TOM {
                    midi_to_freq(note as f32)
                } else {
                    DEFAULT_NOTE_FREQ_HZ
                };
                out_events[count] = (inst_id, freq, vel, dur_s);
                count += 1;
                self.triggered_mask.set_track(tr_idx);
            }
        }

        self.current_step = (self.current_step + 1) % SEQUENCER_TICK_MODULO;
        count
    }
}

impl Default for MasterSequencer {
    fn default() -> Self {
        Self::new(DEFAULT_SAMPLE_RATE)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_master_sequencer_bpm_and_steps() {
        let mut seq = MasterSequencer::new(48000.0);
        seq.set_bpm(120.0);
        assert_eq!(seq.bpm, 120.0);
        assert_eq!(seq.samples_per_step, (48000.0 * 60.0) / (120.0 * 16.0));
    }

    #[test]
    fn test_master_sequencer_track_dispatch() {
        let mut seq = MasterSequencer::new(48000.0);
        seq.set_track(0, &[1], &[60], &[0.8], &[0.1], 1);
        seq.set_playing(true);

        let mut events = [(-1, 0.0, 0.0, 0.0); MAX_TRACKS];
        let count = seq.collect_step_events(&mut events);
        assert_eq!(count, 1);
        assert_eq!(events[0].0, 1);
        assert!(seq.triggered_mask.is_triggered(0));
    }
}
