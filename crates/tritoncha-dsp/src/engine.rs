//! Core real-time DSP audio engine and mixer orchestration.

use crate::dsp::delay::StereoDelay;
use crate::dsp::effects::{
    BitcrusherDrive, DriveMode, SidechainPump, StereoChorus, MAX_SAMPLE_HOLD,
};
use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{db_to_gain, midi_to_freq, soft_clip};
use crate::dsp::reverb::{ReverbMode, StereoReverb};
use crate::sequencer::track::{
    TrackPattern, DEFAULT_STEP_DURATION_S, DEFAULT_STEP_VELOCITY, DEFAULT_SYNTH_PATCH_ID,
    MAX_STEPS, MAX_TRACKS,
};
use crate::synth::drums::DrumMachine;
use crate::synth::{ModularPatch, SynthVoice, MAX_PATCHES};

pub use crate::dsp::bus::*;
pub use crate::synth::drums::{
    is_drum_inst, INST_DRUM_CHINA, INST_DRUM_CLAP, INST_DRUM_COWBELL, INST_DRUM_CRASH_16,
    INST_DRUM_CRASH_17, INST_DRUM_CRASH_18, INST_DRUM_HH_CLOSED, INST_DRUM_HH_OPEN, INST_DRUM_KICK,
    INST_DRUM_RIDE, INST_DRUM_RIDE_BELL, INST_DRUM_SNARE, INST_DRUM_SNARE_BODY,
    INST_DRUM_SNARE_CRACK, INST_DRUM_SNARE_GHOST, INST_DRUM_SNARE_RIM, INST_DRUM_SNARE_WIRE,
    INST_DRUM_SPLASH, INST_DRUM_TOM, INST_DRUM_TOM_HIGH, INST_DRUM_TOM_LOW, INST_DRUM_TOM_MID,
};

pub const NUM_VOICES: usize = 32;

// Audio Clock and Timing Constants
pub const DEFAULT_SAMPLE_RATE: f32 = 48000.0;
pub const DEFAULT_BPM: f32 = 168.0;
pub const MIN_BPM: f32 = 30.0;
pub const MAX_BPM: f32 = 300.0;
pub const SECONDS_PER_MINUTE: f32 = 60.0;
pub const STEPS_PER_BEAT_64TH: f32 = 16.0;
pub const SEQUENCER_TICK_MODULO: usize = 65536;
pub const MIN_AUDIBLE_VELOCITY: f32 = 0.001;

// Master Filter Cutoff and Resonance Boundaries
pub const MIN_MASTER_CUTOFF_HZ: f32 = 40.0;
pub const MAX_MASTER_CUTOFF_HZ: f32 = 18000.0;
pub const DEFAULT_MASTER_CUTOFF_HZ: f32 = 18000.0;
pub const MASTER_FILTER_BYPASS_CUTOFF_HZ: f32 = 16000.0;
pub const MAX_MASTER_RESONANCE: f32 = 0.95;
pub const MIN_AUDIBLE_RESONANCE: f32 = 0.01;
pub const MIN_SWEEP_DURATION_S: f32 = 0.01;
pub const MASTER_HEADROOM_GAIN: f32 = 0.95;

pub const INST_CLICK: i32 = 11;
pub const DEFAULT_CLICK_FREQ_HZ: f32 = 2400.0;
pub const DEFAULT_NOTE_FREQ_HZ: f32 = 440.0;

/// Tritoncha Real-Time Audio Engine.
pub struct TritonchaEngine {
    pub sample_rate: f32,
    pub playing: bool,
    pub bpm: f32,
    samples_per_step: f32,
    step_timer: f32,
    current_step: usize,

    tracks: [TrackPattern; MAX_TRACKS],
    solo_active: bool,

    pub voices: [SynthVoice; NUM_VOICES],
    voice_bus_map: [usize; NUM_VOICES],
    patches: [ModularPatch; MAX_PATCHES],
    drums: DrumMachine,

    busses: [AudioBus; NUM_BUSSES],

    master_filter: StateVariableFilter,
    master_cutoff_hz: f32,
    master_resonance: f32,
    sweep_start_hz: f32,
    sweep_target_hz: f32,
    sweep_samples_total: usize,
    sweep_samples_current: usize,
    sweep_active: bool,

    pub bitcrush_drive: BitcrusherDrive,
    pub chorus: StereoChorus,
    pub sidechain: SidechainPump,

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
            voices: std::array::from_fn(|_| SynthVoice::new()),
            voice_bus_map: [BUS_BASS; NUM_VOICES],
            patches: std::array::from_fn(ModularPatch::default_for),
            drums: DrumMachine::new(),
            busses: [
                AudioBus::new(1.0, 0.02, 0.06), // BUS_DRUMS: neutral 0.0 dB
                AudioBus::new(1.0, 0.0, 0.0),   // BUS_BASS: neutral 0.0 dB
                AudioBus::new(1.0, 0.20, 0.35), // BUS_SPACE: neutral 0.0 dB
                AudioBus::new(1.0, 0.15, 0.10), // BUS_LEAD: neutral 0.0 dB
                AudioBus::new(1.0, 0.0, 0.0),   // BUS_DIRECT: metronome / click (0.0 dB)
            ],
            master_filter: StateVariableFilter::new(),
            master_cutoff_hz: DEFAULT_MASTER_CUTOFF_HZ,
            master_resonance: 0.0,
            sweep_start_hz: DEFAULT_MASTER_CUTOFF_HZ,
            sweep_target_hz: DEFAULT_MASTER_CUTOFF_HZ,
            sweep_samples_total: 1,
            sweep_samples_current: 0,
            sweep_active: false,
            bitcrush_drive: BitcrusherDrive::new(),
            chorus: StereoChorus::new(),
            sidechain: SidechainPump::new(),
            delay: StereoDelay::new(),
            reverb: StereoReverb::with_sample_rate(sr),
        }
    }
}

impl Default for TritonchaEngine {
    fn default() -> Self {
        Self::new(DEFAULT_SAMPLE_RATE)
    }
}

impl TritonchaEngine {
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
        if track_idx >= MAX_TRACKS {
            return;
        }

        let tr = &mut self.tracks[track_idx];
        let len = notes.len().min(MAX_STEPS);

        // If track has no notes or all notes are rests, deactivate it
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

    pub fn set_bus_params(
        &mut self,
        bus_idx: usize,
        gain_db: f32,
        muted: bool,
        send_delay: f32,
        send_reverb: f32,
    ) {
        if bus_idx < NUM_BUSSES {
            let linear_gain = db_to_gain(gain_db);
            self.busses[bus_idx].gain = linear_gain.clamp(0.0, MAX_BUS_LINEAR_GAIN);
            self.busses[bus_idx].muted = muted;
            self.busses[bus_idx].send_delay = send_delay.clamp(0.0, 1.0);
            self.busses[bus_idx].send_reverb = send_reverb.clamp(0.0, 1.0);
        }
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
        self.drums.set_mode_all(mode);
    }

    pub fn trigger_note(&mut self, inst_id: i32, freq: f32, vel: f32, dur_s: f32) {
        if inst_id == INST_DRUM_KICK {
            self.sidechain.trigger_kick();
        }
        if self.drums.trigger_by_id(inst_id, vel, freq) {
            return;
        }

        let pid = (inst_id as usize).min(MAX_PATCHES - 1);
        let patch = self.patches[pid];
        let target_bus = patch.bus_id as usize;

        if patch.polyphony <= 1 {
            // 1. Monophonic mode: find existing active voice for this patch
            let mut found_mono: Option<usize> = None;
            for (i, v) in self.voices.iter().enumerate() {
                if v.active && v.patch_id == pid {
                    found_mono = Some(i);
                    break;
                }
            }

            if let Some(v_idx) = found_mono {
                if patch.glide > 0.001 {
                    // Legato portamento glide without phase discontinuity
                    self.voices[v_idx].glide_to(freq, vel, self.sample_rate, patch.glide, dur_s);
                    return;
                } else {
                    // Retrigger same voice (prevents muddy bass buildup)
                    self.voice_bus_map[v_idx] = target_bus;
                    self.voices[v_idx].trigger(freq, vel, pid, &patch, self.sample_rate, dur_s);
                    return;
                }
            }
        } else {
            // 2. Polyphonic mode: enforce max polyphony for this specific patch
            let mut patch_voice_indices: [usize; NUM_VOICES] = [0; NUM_VOICES];
            let mut patch_count = 0;
            for (i, v) in self.voices.iter().enumerate() {
                if v.active && v.patch_id == pid {
                    patch_voice_indices[patch_count] = i;
                    patch_count += 1;
                }
            }

            if patch_count >= patch.polyphony as usize {
                // Steal the oldest voice playing this patch
                let mut oldest_idx = patch_voice_indices[0];
                let mut max_age = self.voices[oldest_idx].age;
                for &idx in &patch_voice_indices[1..patch_count] {
                    if self.voices[idx].age > max_age {
                        max_age = self.voices[idx].age;
                        oldest_idx = idx;
                    }
                }
                self.voice_bus_map[oldest_idx] = target_bus;
                self.voices[oldest_idx].trigger(freq, vel, pid, &patch, self.sample_rate, dur_s);
                return;
            }
        }

        // 3. Find first inactive voice in pool
        let mut target_idx = None;
        for (i, v) in self.voices.iter().enumerate() {
            if !v.active {
                target_idx = Some(i);
                break;
            }
        }

        // 4. If all 32 voices are active, steal the globally oldest voice (LRU)
        let final_idx = target_idx.unwrap_or_else(|| {
            let mut oldest = 0;
            let mut max_age = self.voices[0].age;
            for (i, v) in self.voices.iter().enumerate().skip(1) {
                if v.age > max_age {
                    max_age = v.age;
                    oldest = i;
                }
            }
            oldest
        });

        self.voice_bus_map[final_idx] = target_bus;
        self.voices[final_idx].trigger(freq, vel, pid, &patch, self.sample_rate, dur_s);
    }

    pub fn master_cutoff_hz(&self) -> f32 {
        self.master_cutoff_hz
    }

    pub fn set_master_filter(&mut self, cutoff_hz: f32, resonance: f32) {
        self.sweep_active = false;
        self.master_cutoff_hz = cutoff_hz.clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
        self.master_resonance = resonance.clamp(0.0, MAX_MASTER_RESONANCE);
    }

    pub fn sweep_master_filter(&mut self, from_hz: f32, to_hz: f32, duration_secs: f32) {
        let f_hz = from_hz.clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
        let t_hz = to_hz.clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
        let dur = duration_secs.max(MIN_SWEEP_DURATION_S);
        let total = (dur * self.sample_rate) as usize;
        self.sweep_start_hz = f_hz;
        self.sweep_target_hz = t_hz;
        self.sweep_samples_total = total.max(1);
        self.sweep_samples_current = 0;
        self.sweep_active = true;
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
        self.sidechain.amount = amount.clamp(0.0, 1.0);
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

    #[inline(always)]
    fn tick_sequencer_step(&mut self) {
        let step = self.current_step;
        let mut events: [(i32, f32, f32, f32); MAX_TRACKS] = [(-1, 0.0, 0.0, 0.0); MAX_TRACKS];
        let mut count = 0;

        for tr in &self.tracks {
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
                events[count] = (inst_id, freq, vel, dur_s);
                count += 1;
            }
        }

        for &(inst_id, freq, vel, dur_s) in &events[..count] {
            self.trigger_note(inst_id, freq, vel, dur_s);
        }

        self.current_step = (self.current_step + 1) % SEQUENCER_TICK_MODULO;
    }

    pub fn process_block(&mut self, out_l: &mut [f32], out_r: &mut [f32]) {
        let len = out_l.len().min(out_r.len());

        // Advance master filter frequency sweep if active
        if self.sweep_active {
            self.sweep_samples_current =
                (self.sweep_samples_current + len).min(self.sweep_samples_total);
            let progress = self.sweep_samples_current as f32 / self.sweep_samples_total as f32;
            let ratio = self.sweep_target_hz / self.sweep_start_hz.max(1.0);
            self.master_cutoff_hz = (self.sweep_start_hz * ratio.powf(progress))
                .clamp(MIN_MASTER_CUTOFF_HZ, MAX_MASTER_CUTOFF_HZ);
            if self.sweep_samples_current >= self.sweep_samples_total {
                self.master_cutoff_hz = self.sweep_target_hz;
                self.sweep_active = false;
            }
        }

        for i in 0..len {
            // 1. Advance sequencer clock
            if self.playing {
                self.step_timer += 1.0;
                if self.step_timer >= self.samples_per_step {
                    self.step_timer -= self.samples_per_step;
                    self.tick_sequencer_step();
                }
            }

            // 2. Clear bus accumulation buffers
            let mut bus_accum = [0.0_f32; NUM_BUSSES];

            // Accumulate drums into bus 0
            let drum_sample = self.drums.process(self.sample_rate);
            bus_accum[BUS_DRUMS] += drum_sample;

            // Accumulate synth voices into their respective target bus
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

            // 3. Multi-Bus Routing + FX Send Matrix
            let mut direct_mix = 0.0;
            let mut delay_send = 0.0;
            let mut reverb_send = 0.0;

            for (b, (bus, &accum)) in self.busses.iter().zip(bus_accum.iter()).enumerate() {
                if !bus.muted {
                    let mut bus_val = accum * bus.gain;
                    // Apply sidechain ducking pump to bass (bus 1) and space/leads (busses 2, 3)
                    if matches!(b, BUS_BASS..=BUS_LEAD) {
                        bus_val = self.sidechain.process(bus_val);
                    }
                    direct_mix += bus_val;
                    delay_send += bus_val * bus.send_delay;
                    reverb_send += bus_val * bus.send_reverb;
                }
            }

            // 4. Run Delay and Reverb effects on their send lines (WET ONLY)
            let (wet_dl, wet_dr) = self.delay.process_wet(delay_send, delay_send);
            let (wet_rl, wet_rr) = self.reverb.process_wet(reverb_send, reverb_send);

            // 5. Combine direct dry mix with wet effects
            let mut combined_l = direct_mix + wet_dl + wet_rl;
            let mut combined_r = direct_mix + wet_dr + wet_rr;

            // 6. Master Chorus / Flanger
            let (chorus_l, chorus_r) =
                self.chorus
                    .process(combined_l, combined_r, self.sample_rate);
            combined_l = chorus_l;
            combined_r = chorus_r;

            // 7. Master Bitcrusher + Overdrive Saturation
            let (drive_l, drive_r) = self.bitcrush_drive.process(combined_l, combined_r);
            combined_l = drive_l;
            combined_r = drive_r;

            // 8. Master Lowpass Filter (bypass when wide open >= 16 kHz)
            let (filtered_l, filtered_r) = if self.master_cutoff_hz < MASTER_FILTER_BYPASS_CUTOFF_HZ
                || self.master_resonance > MIN_AUDIBLE_RESONANCE
            {
                (
                    self.master_filter.process_lp(
                        combined_l,
                        self.master_cutoff_hz,
                        self.master_resonance,
                        self.sample_rate,
                    ),
                    self.master_filter.process_lp(
                        combined_r,
                        self.master_cutoff_hz,
                        self.master_resonance,
                        self.sample_rate,
                    ),
                )
            } else {
                (combined_l, combined_r)
            };

            // 9. Master soft-clipping analog limiter
            out_l[i] = soft_clip(filtered_l * MASTER_HEADROOM_GAIN);
            out_r[i] = soft_clip(filtered_r * MASTER_HEADROOM_GAIN);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::synth::patch::PATCH_SAW_BASS;

    #[test]
    fn test_engine_initial_state() {
        let engine = TritonchaEngine::new(48000.0);
        assert_eq!(engine.bpm, DEFAULT_BPM);
        assert!(!engine.playing);
        assert_eq!(engine.voices.len(), NUM_VOICES);
        assert_eq!(engine.busses.len(), NUM_BUSSES);
    }

    #[test]
    fn test_engine_bpm_and_timing() {
        let mut engine = TritonchaEngine::new(48000.0);
        engine.set_bpm(174.0);
        assert_eq!(engine.bpm, 174.0);

        // Clamping check
        engine.set_bpm(10.0);
        assert_eq!(engine.bpm, 174.0); // Out of bounds, ignored
    }

    #[test]
    fn test_engine_bus_parameters() {
        let mut engine = TritonchaEngine::new(48000.0);
        engine.set_bus_params(BUS_DRUMS, -6.0, true, 0.1, 0.2);
        assert!(engine.busses[BUS_DRUMS].muted);
        assert!((engine.busses[BUS_DRUMS].gain - db_to_gain(-6.0)).abs() < 0.01);
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

        // Process silence
        engine.process_block(&mut out_l, &mut out_r);
        assert!(out_l.iter().all(|&s| s == 0.0));

        // Trigger kick and process block
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
        assert!(engine.sweep_active);

        let mut out_l = [0.0; 2400];
        let mut out_r = [0.0; 2400];
        engine.process_block(&mut out_l, &mut out_r);

        assert!(!engine.sweep_active);
        assert_eq!(engine.master_cutoff_hz, 4000.0);
    }
}
