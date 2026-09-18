//! Core real-time DSP audio engine and mixer orchestration.

use crate::core::math::soft_clip;
use crate::domain::drums::{DrumMachine, DrumMode};
use crate::domain::effects::{
    BitcrusherDrive, BusCompressor, CompressorConfig, DriveMode, FrequencySweep, ReverbMode,
    StateVariableFilter, StereoChorus, StereoDelay, StereoReverb, MAX_SAMPLE_HOLD,
};
use crate::domain::sequencer::{MasterSequencer, DEFAULT_SAMPLE_RATE, MAX_TRACKS};
use crate::domain::synth::{ModularPatch, SynthVoice, MAX_PATCHES};
use crate::services::voice_allocator::{VoiceAllocation, VoiceAllocator};

pub use crate::domain::drums::{
    is_drum_inst, INST_DRUM_CHINA, INST_DRUM_CLAP, INST_DRUM_COWBELL, INST_DRUM_CRASH_16,
    INST_DRUM_CRASH_17, INST_DRUM_CRASH_18, INST_DRUM_HH_CLOSED, INST_DRUM_HH_OPEN, INST_DRUM_KICK,
    INST_DRUM_RIDE, INST_DRUM_RIDE_BELL, INST_DRUM_SNARE, INST_DRUM_SNARE_BODY,
    INST_DRUM_SNARE_CRACK, INST_DRUM_SNARE_GHOST, INST_DRUM_SNARE_RIM, INST_DRUM_SNARE_WIRE,
    INST_DRUM_SPLASH, INST_DRUM_TOM, INST_DRUM_TOM_HIGH, INST_DRUM_TOM_LOW, INST_DRUM_TOM_MID,
};
pub use crate::domain::sequencer::{
    DEFAULT_BPM, DEFAULT_CLICK_FREQ_HZ, DEFAULT_NOTE_FREQ_HZ, INST_CLICK, MIN_AUDIBLE_VELOCITY,
    SEQUENCER_TICK_MODULO,
};
pub use crate::services::mixer::*;

pub const NUM_VOICES: usize = 32;

// Master Filter Cutoff and Resonance Boundaries
pub const MIN_MASTER_CUTOFF_HZ: f32 = 40.0;
pub const MAX_MASTER_CUTOFF_HZ: f32 = 18000.0;
pub const DEFAULT_MASTER_CUTOFF_HZ: f32 = 18000.0;
pub const MASTER_FILTER_BYPASS_CUTOFF_HZ: f32 = 16000.0;
pub const MAX_MASTER_RESONANCE: f32 = 0.95;
pub const MIN_AUDIBLE_RESONANCE: f32 = 0.01;
pub const MIN_SWEEP_DURATION_S: f32 = 0.01;
pub const MASTER_HEADROOM_GAIN: f32 = 0.95;
pub const DELAY_TO_REVERB_CROSSFEED: f32 = 0.20;

/// Tritoncha Real-Time Audio Engine.
pub struct TritonchaEngine {
    pub sample_rate: f32,
    pub sequencer: MasterSequencer,

    pub voices: [SynthVoice; NUM_VOICES],
    voice_bus_map: [usize; NUM_VOICES],
    patches: [ModularPatch; MAX_PATCHES],
    drums: DrumMachine,

    pub mixer: Mixer,

    master_filters: [StateVariableFilter; 2],
    master_cutoff_hz: f32,
    master_resonance: f32,
    pub sweep: FrequencySweep,

    pub bitcrush_drive: BitcrusherDrive,
    pub chorus: StereoChorus,
    pub compressor: BusCompressor,
    pub master_gain: f32,

    delay: StereoDelay,
    reverb: StereoReverb,
}

impl TritonchaEngine {
    pub fn new(sample_rate: f32) -> Self {
        let sr = if sample_rate > 0.0 {
            sample_rate
        } else {
            DEFAULT_SAMPLE_RATE
        };

        Self {
            sample_rate: sr,
            sequencer: MasterSequencer::new(sr),
            voices: std::array::from_fn(|_| SynthVoice::new()),
            voice_bus_map: [BUS_BASS; NUM_VOICES],
            patches: std::array::from_fn(ModularPatch::default_for),
            drums: DrumMachine::new(),
            mixer: Mixer::new(),
            master_filters: [StateVariableFilter::new(), StateVariableFilter::new()],
            master_cutoff_hz: DEFAULT_MASTER_CUTOFF_HZ,
            master_resonance: 0.0,
            sweep: FrequencySweep::new(DEFAULT_MASTER_CUTOFF_HZ),
            bitcrush_drive: BitcrusherDrive::new(),
            chorus: StereoChorus::new(),
            compressor: BusCompressor::new(sr),
            master_gain: 1.0,
            delay: StereoDelay::new(),
            reverb: StereoReverb::with_sample_rate(sr),
        }
    }

    #[inline]
    pub fn take_triggered_tracks_mask(&mut self) -> u32 {
        self.sequencer.triggered_mask.take()
    }
}

impl Default for TritonchaEngine {
    fn default() -> Self {
        Self::new(DEFAULT_SAMPLE_RATE)
    }
}

impl TritonchaEngine {
    pub fn set_bpm(&mut self, bpm: f32) {
        self.sequencer.set_bpm(bpm);
    }

    pub fn set_playing(&mut self, playing: bool) {
        self.sequencer.set_playing(playing);
        if !playing {
            for v in &mut self.voices {
                v.active = false;
            }
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
        self.sequencer
            .set_track(track_idx, inst_ids, notes, vels, durs, step_mult);
    }

    pub fn deactivate_track(&mut self, track_idx: usize) {
        self.sequencer.deactivate_track(track_idx);
    }

    pub fn clear_tracks(&mut self) {
        self.sequencer.clear_tracks();
    }

    pub fn mute_track(&mut self, track_idx: usize, muted: bool) {
        self.sequencer.mute_track(track_idx, muted);
    }

    pub fn solo_track(&mut self, track_idx: usize, solo: bool) {
        self.sequencer.solo_track(track_idx, solo);
    }

    pub fn set_bus_params(
        &mut self,
        bus_idx: usize,
        gain_db: f32,
        muted: bool,
        send_delay: f32,
        send_reverb: f32,
    ) {
        self.mixer
            .set_bus_params(bus_idx, gain_db, muted, send_delay, send_reverb);
    }

    pub fn set_voice_patch(&mut self, patch_id: usize, patch: ModularPatch) {
        if patch_id < MAX_PATCHES {
            self.patches[patch_id] = patch.sanitized();
        }
    }

    pub fn set_drum_patch(&mut self, drum_id: i32, params: [f32; 7]) {
        self.drums.set_drum_patch(drum_id, params);
    }

    pub fn set_drum_mode(&mut self, mode: u8) {
        self.drums.set_mode_all(DrumMode::from(mode));
    }

    pub fn trigger_note(&mut self, inst_id: i32, freq: f32, vel: f32, dur_s: f32) {
        if inst_id == INST_DRUM_KICK {
            self.mixer.trigger_sidechain_kick();
        }
        if self.drums.trigger_by_id(inst_id, vel, freq) {
            return;
        }

        let pid = (inst_id as usize).min(MAX_PATCHES - 1);
        let patch = self.patches[pid];
        let target_bus = patch.bus_id as usize;

        match VoiceAllocator::allocate(&self.voices, pid, patch.polyphony, patch.glide) {
            VoiceAllocation::LegatoGlide { voice_idx } => {
                self.voices[voice_idx].glide_to(freq, vel, self.sample_rate, patch.glide, dur_s);
            }
            VoiceAllocation::Trigger { voice_idx } => {
                self.voice_bus_map[voice_idx] = target_bus;
                self.voices[voice_idx].trigger(freq, vel, pid, &patch, self.sample_rate, dur_s);
            }
        }
    }

    pub fn master_cutoff_hz(&self) -> f32 {
        self.master_cutoff_hz
    }

    pub fn set_master_filter(&mut self, cutoff_hz: f32, resonance: f32) {
        self.sweep.cancel();
        self.master_cutoff_hz = cutoff_hz.clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
        self.master_resonance = resonance.clamp(0.0, MAX_MASTER_RESONANCE);
    }

    pub fn sweep_master_filter(&mut self, from_hz: f32, to_hz: f32, duration_secs: f32) {
        let f_hz = from_hz.clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
        let t_hz = to_hz.clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
        self.sweep
            .start(f_hz, t_hz, duration_secs, self.sample_rate);
        self.master_cutoff_hz = f_hz;
    }

    pub fn set_drive_bitcrush(&mut self, drive: f32, bit_depth: f32, sample_hold: f32) {
        self.bitcrush_drive.set_drive(drive);
        self.bitcrush_drive.bit_depth = bit_depth.clamp(1.0, 16.0);
        self.bitcrush_drive.sample_hold = sample_hold.clamp(1.0, MAX_SAMPLE_HOLD);
    }

    pub fn set_drive_mode(&mut self, mode: u8) {
        let m = if mode == 0 {
            DriveMode::Classic
        } else {
            DriveMode::Adaa
        };
        self.bitcrush_drive.set_mode(m);
    }

    pub fn set_chorus(&mut self, rate_hz: f32, depth: f32, mix: f32) {
        self.chorus.rate_hz = rate_hz.clamp(0.1, 10.0);
        self.chorus.depth = depth.clamp(0.0, 1.0);
        self.chorus.mix = mix.clamp(0.0, 1.0);
    }

    pub fn set_sidechain(&mut self, amount: f32) {
        self.mixer.set_sidechain_amount(amount);
    }

    pub fn set_delay(&mut self, time_s: f32, feedback: f32, wet: f32) {
        self.delay
            .set_params(time_s, feedback, wet, self.sample_rate);
    }

    pub fn set_reverb(&mut self, room_size: f32, wet: f32) {
        self.reverb.set_params(room_size, wet);
    }

    pub fn set_reverb_mode(&mut self, mode: u8) {
        let m = if mode == 0 {
            ReverbMode::Freeverb
        } else {
            ReverbMode::Fdn
        };
        self.reverb.set_mode(m);
    }

    pub fn set_master_compressor(&mut self, config: CompressorConfig) {
        self.compressor.set_config(config);
    }

    pub fn set_master_volume(&mut self, gain_db: f32) {
        self.master_gain = crate::core::math::db_to_gain(gain_db.clamp(-60.0, 6.0));
    }

    #[inline(always)]
    pub fn process_block(&mut self, out_l: &mut [f32], out_r: &mut [f32]) {
        let len = out_l.len().min(out_r.len());

        if let Some(cutoff) = self
            .sweep
            .advance(len, MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ)
        {
            self.master_cutoff_hz = cutoff;
        }

        let mut step_events = [(-1, 0.0, 0.0, 0.0); MAX_TRACKS];

        for i in 0..len {
            let event_count = self.sequencer.advance_sample(&mut step_events);
            for &(inst_id, freq, vel, dur_s) in &step_events[..event_count] {
                self.trigger_note(inst_id, freq, vel, dur_s);
            }

            let mut bus_accum = [0.0_f32; NUM_BUSSES];
            bus_accum[BUS_DRUMS] += self.drums.process(self.sample_rate);

            for v_idx in 0..NUM_VOICES {
                let voice = &mut self.voices[v_idx];
                if voice.active {
                    let pid = voice.patch_id;
                    let patch = &self.patches[pid];
                    let s = voice.process_sample(patch, self.sample_rate);
                    let target_bus = self.voice_bus_map[v_idx];
                    bus_accum[target_bus] += s;
                }
            }

            let frame = self.mixer.process_frame(&bus_accum);

            let (wet_dl, wet_dr) = self.delay.process_wet(frame.delay_send, frame.delay_send);
            let (wet_rl, wet_rr) = self.reverb.process_wet(
                frame.reverb_send + wet_dl * DELAY_TO_REVERB_CROSSFEED,
                frame.reverb_send + wet_dr * DELAY_TO_REVERB_CROSSFEED,
            );

            let mut master_l = frame.master_bus + wet_dl + wet_rl;
            let mut master_r = frame.master_bus + wet_dr + wet_rr;

            let (chorus_l, chorus_r) = self.chorus.process(master_l, master_r, self.sample_rate);
            master_l = chorus_l;
            master_r = chorus_r;

            let (drive_l, drive_r) = self.bitcrush_drive.process(master_l, master_r);
            master_l = drive_l;
            master_r = drive_r;

            let (filtered_l, filtered_r) = if self.master_cutoff_hz < MASTER_FILTER_BYPASS_CUTOFF_HZ
                || self.master_resonance > MIN_AUDIBLE_RESONANCE
            {
                (
                    self.master_filters[0].process_lp(
                        master_l,
                        self.master_cutoff_hz,
                        self.master_resonance,
                        self.sample_rate,
                    ),
                    self.master_filters[1].process_lp(
                        master_r,
                        self.master_cutoff_hz,
                        self.master_resonance,
                        self.sample_rate,
                    ),
                )
            } else {
                (master_l, master_r)
            };

            let (comp_l, comp_r) = self.compressor.process(filtered_l, filtered_r);
            let master_out_l = comp_l * self.master_gain;
            let master_out_r = comp_r * self.master_gain;

            let final_l = (master_out_l + frame.direct_bypass) * MASTER_HEADROOM_GAIN;
            let final_r = (master_out_r + frame.direct_bypass) * MASTER_HEADROOM_GAIN;

            out_l[i] = soft_clip(final_l);
            out_r[i] = soft_clip(final_r);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::core::math::db_to_gain;
    use crate::domain::synth::patch::PATCH_SAW_BASS;

    #[test]
    fn test_engine_initial_state() {
        let engine = TritonchaEngine::new(48000.0);
        assert_eq!(engine.sequencer.bpm, DEFAULT_BPM);
        assert!(!engine.sequencer.playing);
        assert_eq!(engine.voices.len(), NUM_VOICES);
        assert_eq!(engine.mixer.busses.len(), NUM_BUSSES);
    }

    #[test]
    fn test_engine_bpm_and_timing() {
        let mut engine = TritonchaEngine::new(48000.0);
        engine.set_bpm(174.0);
        assert_eq!(engine.sequencer.bpm, 174.0);

        engine.set_bpm(10.0);
        assert_eq!(engine.sequencer.bpm, 174.0);
    }

    #[test]
    fn test_engine_bus_parameters() {
        let mut engine = TritonchaEngine::new(48000.0);
        engine.set_bus_params(BUS_DRUMS, -6.0, true, 0.1, 0.2);
        assert!(engine.mixer.busses[BUS_DRUMS].muted);
        assert!((engine.mixer.busses[BUS_DRUMS].gain - db_to_gain(-6.0)).abs() < 0.01);
    }

    #[test]
    fn test_engine_trigger_drum_and_synth() {
        let mut engine = TritonchaEngine::new(48000.0);
        engine.trigger_note(INST_DRUM_KICK, 60.0, 0.9, 0.1);
        assert!(engine.drums.kick.active);

        engine.trigger_note(PATCH_SAW_BASS as i32, 55.0, 0.85, 0.2);
        assert!(engine.voices.iter().any(|v| v.active));
    }

    #[test]
    fn test_engine_process_block_silence_and_sound() {
        let mut engine = TritonchaEngine::new(48000.0);
        let mut out_l = [0.0; 128];
        let mut out_r = [0.0; 128];

        engine.process_block(&mut out_l, &mut out_r);
        assert!(out_l.iter().all(|&s| s == 0.0));

        engine.trigger_note(INST_DRUM_KICK, 60.0, 1.0, 0.1);
        engine.process_block(&mut out_l, &mut out_r);

        let max_amp = out_l.iter().fold(0.0_f32, |m, &s| m.max(s.abs()));
        assert!(max_amp > 0.0);
        assert!(max_amp <= 1.0);
    }

    #[test]
    fn test_engine_master_filter_sweep() {
        let mut engine = TritonchaEngine::new(48000.0);
        engine.sweep_master_filter(400.0, 4000.0, 0.05);
        assert!(engine.sweep.is_active());

        let mut out_l = [0.0; 2400];
        let mut out_r = [0.0; 2400];
        engine.process_block(&mut out_l, &mut out_r);

        assert!(!engine.sweep.is_active());
        assert_eq!(engine.master_cutoff_hz, 4000.0);
    }
}
