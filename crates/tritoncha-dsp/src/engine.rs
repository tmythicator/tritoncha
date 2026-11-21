//! Core real-time DSP audio engine and mixer orchestration.

use crate::core::math::soft_clip;
use crate::domain::drums::{DrumMachine, DrumMode};
use crate::domain::effects::{
    BitcrusherDrive, BusCompressor, BusEffectChain, BusEffectNode, CompressorConfig, DriveMode,
    FrequencySweep, ReverbMode, StereoChorus, StereoDelay, StereoReverb, StereoSvfFilter,
    NUM_ROUTABLE_BUSSES,
};
use crate::domain::sequencer::{MasterSequencer, SequencerEvent, MAX_STEPS, MAX_TRACKS};
use crate::domain::synth::{ModularPatch, SynthVoice, MAX_PATCHES, VOICE_PATCH_PARAM_COUNT};
use crate::domain::{Bpm, BusId, CutoffHz, InstrumentId, Normalized, PatchId, Resonance, TrackId};
use crate::services::voice_allocator::{VoiceAllocation, VoiceAllocator};

pub use crate::domain::drums::{is_drum_inst, DrumId};
pub use crate::domain::sequencer::{
    DEFAULT_BPM, DEFAULT_CLICK_FREQ_HZ, DEFAULT_NOTE_FREQ_HZ, DEFAULT_SAMPLE_RATE,
    MIN_AUDIBLE_VELOCITY, SEQUENCER_TICK_MODULO,
};
pub use crate::services::mixer::*;

pub const NUM_VOICES: usize = 32;
pub const FFI_BLOCK_SIZE: usize = 128;
pub const ONE_SHOT_SLOTS: usize = 4;

// Master Filter Cutoff Boundaries and Headroom
pub const MIN_MASTER_CUTOFF_HZ: f32 = 40.0;
pub const MAX_MASTER_CUTOFF_HZ: f32 = 18000.0;
pub const DEFAULT_MASTER_CUTOFF_HZ: f32 = 18000.0;
pub const MASTER_HEADROOM_GAIN: f32 = 0.95;

/// Preallocated engine-owned linear memory buffers for zero-copy C-ABI FFI transport.
#[derive(Clone)]
pub struct FfiScratchBuffers {
    pub out_l: [f32; FFI_BLOCK_SIZE],
    pub out_r: [f32; FFI_BLOCK_SIZE],
    pub track_inst_ids: [i16; MAX_STEPS],
    pub track_notes: [i16; MAX_STEPS],
    pub track_vels: [f32; MAX_STEPS],
    pub track_durs: [f32; MAX_STEPS],
    pub patch_params: [f32; VOICE_PATCH_PARAM_COUNT],
}

impl Default for FfiScratchBuffers {
    fn default() -> Self {
        Self::new()
    }
}

impl FfiScratchBuffers {
    pub const fn new() -> Self {
        Self {
            out_l: [0.0; FFI_BLOCK_SIZE],
            out_r: [0.0; FFI_BLOCK_SIZE],
            track_inst_ids: [0; MAX_STEPS],
            track_notes: [-1; MAX_STEPS],
            track_vels: [0.0; MAX_STEPS],
            track_durs: [0.0; MAX_STEPS],
            patch_params: [0.0; VOICE_PATCH_PARAM_COUNT],
        }
    }
}

/// Tritoncha Real-Time Audio Engine.
pub struct TritonchaEngine {
    pub sample_rate: f32,
    pub sequencer: MasterSequencer,

    pub voices: [SynthVoice; NUM_VOICES],
    voice_bus_map: [BusTarget; NUM_VOICES],
    patches: [ModularPatch; MAX_PATCHES],
    next_one_shot: usize,
    drums: DrumMachine,

    pub mixer: Mixer,

    // Modular per-bus insert effect chains
    pub bus_chains: [BusEffectChain; NUM_ROUTABLE_BUSSES],

    master_cutoff_hz: f32,
    master_resonance: f32,
    pub sweep: FrequencySweep,
    pub master_gain: f32,
    pub ffi: FfiScratchBuffers,
}

impl TritonchaEngine {
    pub fn new(sample_rate: f32) -> Self {
        let sr = if sample_rate > 0.0 {
            sample_rate
        } else {
            DEFAULT_SAMPLE_RATE
        };

        let mut bus_chains = [
            BusEffectChain::new(), // BusId::DRUMS (0)
            BusEffectChain::new(), // BusId::BASS (1)
            BusEffectChain::new(), // BusId::SPACE (2)
            BusEffectChain::new(), // BusId::LEAD (3)
            BusEffectChain::new(), // BusId::DIRECT (4)
            BusEffectChain::new(), // BusId::MASTER (5)
        ];
        bus_chains[BusId::DIRECT.as_usize()].target_out = true;
        // Default graph: strictly compressor on master, all other busses dry thru
        bus_chains[BusId::MASTER.as_usize()]
            .push(BusEffectNode::Compressor(BusCompressor::new(sr)));

        Self {
            sample_rate: sr,
            sequencer: MasterSequencer::new(sr),
            voices: std::array::from_fn(|_| SynthVoice::new()),
            voice_bus_map: [BusTarget::Bass; NUM_VOICES],
            patches: std::array::from_fn(ModularPatch::default_for),
            next_one_shot: 0,
            drums: DrumMachine::new(),
            mixer: Mixer::new(),
            bus_chains,
            master_cutoff_hz: DEFAULT_MASTER_CUTOFF_HZ,
            master_resonance: 0.0,
            sweep: FrequencySweep::new(DEFAULT_MASTER_CUTOFF_HZ),
            master_gain: 1.0,
            ffi: FfiScratchBuffers::new(),
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
    pub fn set_bpm(&mut self, bpm: impl Into<Bpm>) {
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
        track_id: impl Into<TrackId>,
        inst_ids: &[i16],
        notes: &[i16],
        vels: &[f32],
        durs: &[f32],
        step_mult: usize,
    ) {
        self.sequencer
            .set_track(track_id, inst_ids, notes, vels, durs, step_mult);
    }

    pub fn deactivate_track(&mut self, track_id: impl Into<TrackId>) {
        self.sequencer.deactivate_track(track_id);
    }

    pub fn clear_tracks(&mut self) {
        self.sequencer.clear_tracks();
    }

    pub fn mute_track(&mut self, track_id: impl Into<TrackId>, muted: bool) {
        self.sequencer.mute_track(track_id, muted);
    }

    pub fn solo_track(&mut self, track_id: impl Into<TrackId>, solo: bool) {
        self.sequencer.solo_track(track_id, solo);
    }

    pub fn set_bus_params(
        &mut self,
        bus: impl Into<BusId>,
        gain_db: f32,
        muted: bool,
        send_delay: f32,
        send_reverb: f32,
        bypass_master_fx: bool,
    ) {
        self.mixer.set_bus_params(
            bus,
            gain_db,
            muted,
            send_delay,
            send_reverb,
            bypass_master_fx,
        );
    }

    #[inline(always)]
    pub fn patch_mut(&mut self, patch_id: impl Into<PatchId>) -> Option<&mut ModularPatch> {
        let pid = patch_id.into();
        if pid.is_valid() {
            Some(&mut self.patches[pid.as_usize()])
        } else {
            None
        }
    }

    pub fn set_voice_patch(&mut self, patch_id: impl Into<PatchId>, patch: ModularPatch) {
        if let Some(target) = self.patch_mut(patch_id) {
            *target = patch;
        }
    }

    /// Assigns the active modular synthesizer patch owned directly by a sequencer track (0..15).
    pub fn set_track_patch(&mut self, track_id: impl Into<TrackId>, patch: ModularPatch) {
        let tid = track_id.into();
        if tid.is_valid() {
            self.patches[tid.as_usize()] = patch;
        }
    }

    pub fn set_drum_patch(&mut self, drum_id: i32, params: [f32; 7]) {
        self.drums.set_drum_patch_by_id(drum_id, params);
    }

    pub fn set_drum_mode(&mut self, mode: impl Into<DrumMode>) {
        self.drums.set_mode_all(mode);
    }

    #[inline(always)]
    fn trigger_synth_slot(&mut self, pid: PatchId, freq: f32, vel: f32, dur_s: f32) {
        let pid = pid.clamped();
        let patch = self.patches[pid.as_usize()];

        match VoiceAllocator::allocate(&self.voices, pid, patch.polyphony, patch.glide) {
            VoiceAllocation::LegatoGlide { voice_idx } => {
                self.voices[voice_idx].glide_to(freq, vel, self.sample_rate, patch.glide, dur_s);
            }
            VoiceAllocation::Trigger { voice_idx } => {
                self.voice_bus_map[voice_idx] = patch.bus_id;
                self.voices[voice_idx].trigger(freq, vel, pid, &patch, self.sample_rate, dur_s);
            }
        }
    }

    /// Triggers a synthesizer voice using the patch owned by `track_idx` (0..15).
    #[inline(always)]
    pub fn trigger_track_synth(&mut self, track_idx: usize, freq: f32, vel: f32, dur_s: f32) {
        if track_idx < MAX_TRACKS {
            self.trigger_synth_slot(PatchId(track_idx), freq, vel, dur_s);
        }
    }

    /// Triggers a live one-shot synthesizer note using a rotating audition slot (16..19).
    #[inline(always)]
    pub fn trigger_one_shot_synth(&mut self, patch: ModularPatch, freq: f32, vel: f32, dur_s: f32) {
        let slot = MAX_TRACKS + (self.next_one_shot % ONE_SHOT_SLOTS);
        self.next_one_shot = self.next_one_shot.wrapping_add(1);
        self.patches[slot] = patch;
        self.trigger_synth_slot(PatchId(slot), freq, vel, dur_s);
    }

    pub fn trigger_note(&mut self, inst_id: i32, freq: f32, vel: f32, dur_s: f32) {
        match InstrumentId::from(inst_id) {
            InstrumentId::Drum(drum) => {
                if drum == DrumId::Kick {
                    self.mixer.trigger_sidechain_kick();
                }
                self.drums.trigger_drum(drum, vel, freq);
            }
            InstrumentId::Synth(patch_id) => {
                self.trigger_synth_slot(patch_id, freq, vel, dur_s);
            }
        }
    }

    pub fn master_cutoff_hz(&self) -> f32 {
        self.master_cutoff_hz
    }

    #[inline(always)]
    pub fn bus_chain_mut(&mut self, bus: impl Into<BusId>) -> Option<&mut BusEffectChain> {
        let b = bus.into();
        if b.is_routable() {
            self.bus_chains.get_mut(b.as_usize())
        } else {
            None
        }
    }

    #[inline(always)]
    fn for_bus_chains_mut<F>(&mut self, bus: Option<BusId>, mut f: F)
    where
        F: FnMut(&mut BusEffectChain),
    {
        match bus {
            Some(b) => {
                if let Some(chain) = self.bus_chain_mut(b) {
                    f(chain);
                }
            }
            None => {
                for chain in &mut self.bus_chains {
                    f(chain);
                }
            }
        }
    }

    pub fn push_bus_effect(&mut self, bus: impl Into<BusId>, node: BusEffectNode) {
        if let Some(chain) = self.bus_chain_mut(bus) {
            chain.push(node);
        }
    }

    pub fn clear_bus_chain(&mut self, bus: impl Into<BusId>) {
        if let Some(chain) = self.bus_chain_mut(bus) {
            chain.clear();
        }
    }

    pub fn set_bus_target_out(&mut self, bus: impl Into<BusId>, target_out: bool) {
        if let Some(chain) = self.bus_chain_mut(bus) {
            chain.target_out = target_out;
        }
    }

    pub fn add_bus_filter(
        &mut self,
        bus: impl Into<BusId>,
        cutoff_hz: impl Into<CutoffHz>,
        resonance: impl Into<Resonance>,
    ) {
        self.push_bus_effect(
            bus,
            BusEffectNode::Filter(StereoSvfFilter::new(cutoff_hz, resonance)),
        );
    }

    pub fn add_bus_delay(&mut self, bus: impl Into<BusId>, time_s: f32, feedback: f32, wet: f32) {
        let mut d = StereoDelay::new();
        d.set_params(time_s, feedback, wet, self.sample_rate);
        self.push_bus_effect(bus, BusEffectNode::Delay(d));
    }

    pub fn add_bus_distort(
        &mut self,
        bus: impl Into<BusId>,
        drive: f32,
        bits: f32,
        sample_hold: f32,
    ) {
        let mut dist = BitcrusherDrive::new();
        dist.set_params(drive, bits, sample_hold);
        self.push_bus_effect(bus, BusEffectNode::Distort(dist));
    }

    pub fn add_bus_chorus(&mut self, bus: impl Into<BusId>, rate_hz: f32, depth: f32, mix: f32) {
        let mut c = StereoChorus::new();
        c.set_params(rate_hz, depth, mix);
        self.push_bus_effect(bus, BusEffectNode::Chorus(c));
    }

    pub fn add_bus_reverb(&mut self, bus: impl Into<BusId>, room_size: f32, wet: f32) {
        let mut r = StereoReverb::new();
        r.set_params(room_size, wet);
        self.push_bus_effect(bus, BusEffectNode::Reverb(Box::new(r)));
    }

    pub fn add_bus_compressor(&mut self, bus: impl Into<BusId>, config: CompressorConfig) {
        let mut comp = BusCompressor::new(self.sample_rate);
        comp.set_config(config);
        self.push_bus_effect(bus, BusEffectNode::Compressor(comp));
    }

    pub fn update_bus_filter(
        &mut self,
        bus: Option<BusId>,
        cutoff_hz: impl Into<CutoffHz>,
        resonance: impl Into<Resonance>,
    ) {
        let f_hz = *cutoff_hz.into();
        let res = *resonance.into();
        self.for_bus_chains_mut(bus, |chain| chain.update_filter(f_hz, res));
    }

    pub fn sweep_bus_filter(
        &mut self,
        bus: Option<BusId>,
        from_hz: impl Into<CutoffHz>,
        to_hz: impl Into<CutoffHz>,
        dur_s: f32,
    ) {
        let f_hz = *from_hz.into();
        let t_hz = *to_hz.into();
        let sr = self.sample_rate;
        self.for_bus_chains_mut(bus, |chain| chain.sweep_filter(f_hz, t_hz, dur_s, sr));
    }

    pub fn update_bus_delay(
        &mut self,
        bus: Option<BusId>,
        time_s: f32,
        feedback: impl Into<Normalized>,
        wet: impl Into<Normalized>,
    ) {
        let fb = feedback.into();
        let w = wet.into();
        let sr = self.sample_rate;
        self.for_bus_chains_mut(bus, |chain| chain.update_delay(time_s, fb, w, sr));
    }

    pub fn update_bus_distort(
        &mut self,
        bus: Option<BusId>,
        drive: impl Into<Normalized>,
        bits: f32,
        sample_hold: f32,
    ) {
        let d = drive.into();
        self.for_bus_chains_mut(bus, |chain| chain.update_distort(d, bits, sample_hold));
    }

    pub fn update_bus_chorus(
        &mut self,
        bus: Option<BusId>,
        rate_hz: f32,
        depth: impl Into<Normalized>,
        mix: impl Into<Normalized>,
    ) {
        let dp = depth.into();
        let mx = mix.into();
        self.for_bus_chains_mut(bus, |chain| chain.update_chorus(rate_hz, dp, mx));
    }

    pub fn update_bus_reverb(
        &mut self,
        bus: Option<BusId>,
        room_size: impl Into<Normalized>,
        wet: impl Into<Normalized>,
    ) {
        let rs = room_size.into();
        let w = wet.into();
        self.for_bus_chains_mut(bus, |chain| chain.update_reverb(rs, w));
    }

    pub fn update_bus_compressor(&mut self, bus: Option<BusId>, config: CompressorConfig) {
        self.for_bus_chains_mut(bus, |chain| chain.update_compressor(config));
    }

    pub fn set_master_filter(
        &mut self,
        cutoff_hz: impl Into<CutoffHz>,
        resonance: impl Into<Resonance>,
    ) {
        self.sweep.cancel();
        let f_hz = *cutoff_hz.into();
        let res = *resonance.into();
        self.master_cutoff_hz = f_hz;
        self.master_resonance = res;
        self.update_bus_filter(None, f_hz, res);
    }

    pub fn sweep_master_filter(
        &mut self,
        from_hz: impl Into<CutoffHz>,
        to_hz: impl Into<CutoffHz>,
        duration_secs: f32,
    ) {
        let f_hz = *from_hz.into();
        let t_hz = *to_hz.into();
        self.sweep
            .start(f_hz, t_hz, duration_secs, self.sample_rate);
        self.master_cutoff_hz = f_hz;
        self.sweep_bus_filter(None, f_hz, t_hz, duration_secs);
    }

    pub fn set_drive_bitcrush(&mut self, drive: f32, bit_depth: f32, sample_hold: f32) {
        self.update_bus_distort(None, drive, bit_depth, sample_hold);
    }

    pub fn set_drive_mode(&mut self, mode: impl Into<DriveMode>) {
        let m = mode.into();
        self.for_bus_chains_mut(None, |chain| chain.set_drive_mode(m));
    }

    pub fn set_chorus(&mut self, rate_hz: f32, depth: f32, mix: f32) {
        self.update_bus_chorus(None, rate_hz, depth, mix);
    }

    pub fn set_sidechain(&mut self, amount: f32) {
        self.mixer.set_sidechain_amount(amount);
    }

    pub fn set_delay(&mut self, time_s: f32, feedback: f32, wet: f32) {
        self.update_bus_delay(None, time_s, feedback, wet);
    }

    pub fn set_reverb(&mut self, room_size: f32, wet: f32) {
        self.update_bus_reverb(None, room_size, wet);
    }

    pub fn set_reverb_mode(&mut self, mode: impl Into<ReverbMode>) {
        let m = mode.into();
        self.for_bus_chains_mut(None, |chain| chain.set_reverb_mode(m));
    }

    pub fn set_master_compressor(&mut self, config: CompressorConfig) {
        self.update_bus_compressor(Some(BusId::MASTER), config);
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

        let mut step_events = [SequencerEvent::EMPTY; MAX_TRACKS];

        for i in 0..len {
            let event_count = self.sequencer.advance_sample(&mut step_events);
            for ev in &step_events[..event_count] {
                if ev.inst_id < 0 {
                    self.trigger_track_synth(ev.track_idx, ev.freq, ev.vel, ev.dur_s);
                } else {
                    self.trigger_note(ev.inst_id, ev.freq, ev.vel, ev.dur_s);
                }
            }

            let mut bus_accum = [0.0_f32; NUM_BUSSES];
            bus_accum[BusTarget::Drums.as_usize()] += self.drums.process(self.sample_rate);

            for v_idx in 0..NUM_VOICES {
                let voice = &mut self.voices[v_idx];
                if voice.active {
                    let pid = voice.patch_id.as_usize();
                    let patch = &self.patches[pid];
                    let s = voice.process_sample(patch, self.sample_rate);
                    let target_bus = self.voice_bus_map[v_idx];
                    bus_accum[target_bus.as_usize()] += s;
                }
            }

            let (raw_l, raw_r) = self.mixer.mix_frame(
                &bus_accum,
                &mut self.bus_chains,
                self.master_gain,
                self.sample_rate,
            );

            out_l[i] = soft_clip(raw_l * MASTER_HEADROOM_GAIN);
            out_r[i] = soft_clip(raw_r * MASTER_HEADROOM_GAIN);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::core::math::db_to_gain;

    #[test]
    fn test_engine_initial_state() {
        let engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
        assert_eq!(engine.sequencer.bpm, DEFAULT_BPM);
        assert!(!engine.sequencer.playing);
        assert_eq!(engine.voices.len(), NUM_VOICES);
        assert_eq!(engine.mixer.busses.len(), NUM_BUSSES);
    }

    #[test]
    fn test_engine_bpm_and_timing() {
        let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
        engine.set_bpm(174.0);
        assert_eq!(engine.sequencer.bpm, 174.0);
    }

    #[test]
    fn test_engine_bus_parameters() {
        let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
        engine.set_bus_params(BusId::DRUMS, -6.0, true, 0.1, 0.2, false);
        assert!(engine.mixer.busses[BusTarget::Drums.as_usize()].muted);
        assert!(
            (engine.mixer.busses[BusTarget::Drums.as_usize()].gain - db_to_gain(-6.0)).abs() < 0.01
        );
    }

    #[test]
    fn test_engine_trigger_drum_and_synth() {
        let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
        engine.trigger_note(DrumId::Kick.as_i32(), 60.0, 0.9, 0.1);
        assert!(engine.drums.kick.active);

        engine.trigger_note(4, 55.0, 0.85, 0.2);
        assert!(engine.voices.iter().any(|v| v.active));
    }

    #[test]
    fn test_engine_process_block_silence_and_sound() {
        let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
        let mut out_l = [0.0; 128];
        let mut out_r = [0.0; 128];

        engine.process_block(&mut out_l, &mut out_r);
        assert!(out_l.iter().all(|&s| s == 0.0));

        engine.trigger_note(DrumId::Kick.as_i32(), 60.0, 1.0, 0.1);
        engine.process_block(&mut out_l, &mut out_r);

        let max_amp = out_l.iter().fold(0.0_f32, |m, &s| m.max(s.abs()));
        assert!(max_amp > 0.0);
        assert!(max_amp <= 1.0);
    }

    #[test]
    fn test_engine_master_filter_sweep() {
        let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
        engine.sweep_master_filter(400.0, 4000.0, 0.05);
        assert!(engine.sweep.is_active());

        let mut out_l = [0.0; 2400];
        let mut out_r = [0.0; 2400];
        engine.process_block(&mut out_l, &mut out_r);

        assert!(!engine.sweep.is_active());
        assert_eq!(engine.master_cutoff_hz, 4000.0);
    }
}
