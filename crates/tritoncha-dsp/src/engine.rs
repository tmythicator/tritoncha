use crate::dsp::delay::StereoDelay;
use crate::dsp::effects::{BitcrusherDrive, SidechainPump, StereoChorus};
use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{midi_to_freq, soft_clip};
use crate::dsp::reverb::StereoReverb;
use crate::sequencer::track::{
    TrackPattern, DEFAULT_STEP_DURATION_S, DEFAULT_STEP_VELOCITY, DEFAULT_SYNTH_PATCH_ID,
    MAX_STEPS, MAX_TRACKS, REST_NOTE,
};
use crate::synth::drums::DrumMachine;
use crate::synth::{ModularPatch, SynthVoice, MAX_PATCHES};

pub const NUM_VOICES: usize = 32;
pub const NUM_BUSSES: usize = 5;

// Audio Bus Indices
pub const BUS_DRUMS: usize = 0;
pub const BUS_BASS: usize = 1;
pub const BUS_SPACE: usize = 2;
pub const BUS_LEAD: usize = 3;
pub const BUS_DIRECT: usize = 4;

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
pub const MAX_BUS_LINEAR_GAIN: f32 = 4.0;

// Drum Voice Identifiers
pub const INST_DRUM_KICK: i32 = 0;
pub const INST_DRUM_SNARE: i32 = 1;
pub const INST_DRUM_HH_CLOSED: i32 = 2;
pub const INST_DRUM_HH_OPEN: i32 = 3;
pub const INST_CLICK: i32 = 11;
pub const INST_DRUM_CLAP: i32 = 18;
pub const INST_DRUM_RIDE: i32 = 20;
pub const INST_DRUM_TOM: i32 = 21;
pub const INST_DRUM_SNARE_CRACK: i32 = 22;
pub const INST_DRUM_SNARE_WIRE: i32 = 23;
pub const INST_DRUM_SNARE_BODY: i32 = 24;
pub const INST_DRUM_SNARE_GHOST: i32 = 25;
pub const INST_DRUM_SNARE_RIM: i32 = 26;
pub const INST_DRUM_RIDE_BELL: i32 = 64;
pub const INST_DRUM_TOM_HIGH: i32 = 65;
pub const INST_DRUM_TOM_MID: i32 = 66;
pub const INST_DRUM_TOM_LOW: i32 = 67;
pub const INST_DRUM_CRASH_16: i32 = 68;
pub const INST_DRUM_CRASH_17: i32 = 69;
pub const INST_DRUM_CRASH_18: i32 = 70;
pub const INST_DRUM_SPLASH: i32 = 71;
pub const INST_DRUM_CHINA: i32 = 72;
pub const INST_DRUM_COWBELL: i32 = 73;

pub const DEFAULT_TOM_FREQ_HZ: f32 = 130.0;
pub const DEFAULT_CLICK_FREQ_HZ: f32 = 2400.0;
pub const DEFAULT_NOTE_FREQ_HZ: f32 = 440.0;

#[inline(always)]
pub fn is_drum_inst(inst_id: i32) -> bool {
    matches!(
        inst_id,
        INST_DRUM_KICK..=INST_DRUM_HH_OPEN
            | INST_DRUM_CLAP
            | INST_DRUM_RIDE..=INST_DRUM_SNARE_RIM
            | INST_DRUM_RIDE_BELL..=INST_DRUM_COWBELL
    )
}

#[derive(Clone, Copy)]
pub struct AudioBus {
    pub gain: f32,
    pub muted: bool,
    pub send_delay: f32,
    pub send_reverb: f32,
}

impl AudioBus {
    pub fn new(gain: f32, send_delay: f32, send_reverb: f32) -> Self {
        Self {
            gain,
            muted: false,
            send_delay,
            send_reverb,
        }
    }
}

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
                AudioBus::new(1.0, 0.02, 0.06),  // BUS_DRUMS: tight, clean punch
                AudioBus::new(1.0, 0.0, 0.0),    // BUS_BASS: pure dry punch
                AudioBus::new(0.85, 0.20, 0.35), // BUS_SPACE: transparent space
                AudioBus::new(0.85, 0.15, 0.10), // BUS_LEAD: subtle presence
                AudioBus::new(1.0, 0.0, 0.0),    // BUS_DIRECT: metronome / click
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
            reverb: StereoReverb::new(),
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
            tr.active = false;
            tr.length = 0;
            tr.notes = [REST_NOTE; MAX_STEPS];
            tr.inst_kinds = [DEFAULT_SYNTH_PATCH_ID; MAX_STEPS];
            return;
        }

        tr.active = true;
        tr.length = len;
        tr.step_multiplier = step_mult.max(1);

        for i in 0..len {
            tr.inst_kinds[i] = if i < inst_ids.len() {
                inst_ids[i]
            } else {
                DEFAULT_SYNTH_PATCH_ID
            };
            tr.notes[i] = notes[i];
            tr.velocities[i] = if i < vels.len() {
                vels[i]
            } else {
                DEFAULT_STEP_VELOCITY
            };
            tr.durations[i] = if i < durs.len() {
                durs[i]
            } else {
                DEFAULT_STEP_DURATION_S
            };
        }
    }

    pub fn deactivate_track(&mut self, track_idx: usize) {
        if track_idx < MAX_TRACKS {
            let tr = &mut self.tracks[track_idx];
            tr.active = false;
            tr.length = 0;
            tr.notes = [REST_NOTE; MAX_STEPS];
            tr.inst_kinds = [DEFAULT_SYNTH_PATCH_ID; MAX_STEPS];
        }
    }

    pub fn clear_tracks(&mut self) {
        for tr in &mut self.tracks {
            tr.active = false;
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
            let linear_gain = 10.0_f32.powf(gain_db / 20.0);
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

    pub fn trigger_note(&mut self, inst_id: i32, freq: f32, vel: f32, dur_s: f32) {
        match inst_id {
            INST_DRUM_KICK => {
                self.drums.trigger_kick(vel);
                self.sidechain.trigger_kick();
            }
            INST_DRUM_SNARE => self.drums.trigger_snare(vel),
            INST_DRUM_HH_CLOSED => self.drums.trigger_hh(vel, false),
            INST_DRUM_HH_OPEN => self.drums.trigger_hh(vel, true),
            INST_DRUM_CLAP => self.drums.trigger_clap(vel),
            INST_DRUM_RIDE => self.drums.trigger_ride(vel),
            INST_DRUM_TOM => self.drums.trigger_tom(vel, freq),
            INST_DRUM_SNARE_CRACK | INST_DRUM_SNARE_RIM => self.drums.trigger_snare_crack(vel),
            INST_DRUM_SNARE_WIRE => self.drums.trigger_snare_wire(vel),
            INST_DRUM_SNARE_BODY => self.drums.trigger_snare_body(vel),
            INST_DRUM_SNARE_GHOST => self.drums.trigger_snare_ghost(vel),
            INST_DRUM_RIDE_BELL => self.drums.trigger_ride_bell(vel),
            INST_DRUM_TOM_HIGH => self.drums.trigger_tom_high(vel),
            INST_DRUM_TOM_MID => self.drums.trigger_tom_mid(vel),
            INST_DRUM_TOM_LOW => self.drums.trigger_tom_low(vel),
            INST_DRUM_CRASH_16 => self.drums.trigger_crash_16(vel),
            INST_DRUM_CRASH_17 => self.drums.trigger_crash_17(vel),
            INST_DRUM_CRASH_18 => self.drums.trigger_crash_18(vel),
            INST_DRUM_SPLASH => self.drums.trigger_splash(vel),
            INST_DRUM_CHINA => self.drums.trigger_china(vel),
            INST_DRUM_COWBELL => self.drums.trigger_cowbell(vel),
            _ => {
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
                            self.voices[v_idx].glide_to(
                                freq,
                                vel,
                                self.sample_rate,
                                patch.glide,
                                dur_s,
                            );
                            return;
                        } else {
                            // Retrigger same voice (prevents muddy bass buildup)
                            self.voice_bus_map[v_idx] = target_bus;
                            self.voices[v_idx].trigger(
                                freq,
                                vel,
                                pid,
                                &patch,
                                self.sample_rate,
                                dur_s,
                            );
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
                        self.voices[oldest_idx].trigger(
                            freq,
                            vel,
                            pid,
                            &patch,
                            self.sample_rate,
                            dur_s,
                        );
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
        }
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
        self.bitcrush_drive.drive = drive.clamp(0.0, 1.0);
        self.bitcrush_drive.bit_depth = bit_depth.clamp(1.0, 16.0);
        self.bitcrush_drive.sample_hold = sample_hold.clamp(1.0, 16.0);
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

    #[inline(always)]
    fn tick_sequencer_step(&mut self) {
        let step = self.current_step;
        let mut events: [(i32, f32, f32, f32); MAX_TRACKS] = [(-1, 0.0, 0.0, 0.0); MAX_TRACKS];
        let mut count = 0;

        for tr in &self.tracks {
            if !tr.active || (self.solo_active && !tr.solo) || tr.muted {
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
                let freq = if !is_drum_inst(inst_id) && inst_id != INST_CLICK {
                    midi_to_freq(note as f32)
                } else if inst_id == INST_CLICK {
                    DEFAULT_CLICK_FREQ_HZ
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
            let drum_sample = self.drums.process_sample(self.sample_rate);
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
            out_l[i] = soft_clip(filtered_l * 0.95);
            out_r[i] = soft_clip(filtered_r * 0.95);
        }
    }
}
