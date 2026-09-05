use crate::dsp::filter::StateVariableFilter;
use crate::dsp::math::{poly_blep, soft_clip};
pub use crate::synth::patch::*;
use std::f32::consts::PI;

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
    env_stage: u8, // 1=A, 2=D, 3=S, 4=R
    sustain_level: f32,
    hold_counter: u32,
    hold_samples: u32,
    mod_env_level: f32,
    mod_env_stage: u8,
    attack_rate: f32,
    decay_rate: f32,
    release_rate: f32,
    mod_attack_rate: f32,
    mod_decay_rate: f32,
    filter: StateVariableFilter,

    // Karplus-Strong delay line buffer
    ks_buffer: [f32; 1024],
    ks_pos: usize,
    ks_len: usize,

    // Analog synthesis state
    noise_seed: u32,
    pitch_env_level: f32,
    pitch_env_rate: f32,
    drift_phase: f32,
    drift_phase_inc: f32,
}

impl SynthVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            patch_id: 4,
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
            env_stage: 0,
            sustain_level: 0.0,
            hold_counter: 0,
            hold_samples: 0,
            mod_env_level: 0.0,
            mod_env_stage: 0,
            attack_rate: 0.01,
            decay_rate: 0.001,
            release_rate: 0.001,
            mod_attack_rate: 0.01,
            mod_decay_rate: 0.001,
            filter: StateVariableFilter::new(),
            ks_buffer: [0.0; 1024],
            ks_pos: 0,
            ks_len: 200,
            noise_seed: 0x9e3779b9,
            pitch_env_level: 0.0,
            pitch_env_rate: 0.01,
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
        self.velocity = vel.clamp(0.0, 1.0);
        let time_s = glide_time.max(0.001);
        self.glide_rate = (1.0 / (time_s * sample_rate)).clamp(0.0001, 1.0);
        self.age = 0;
        self.hold_counter = 0;
        self.hold_samples = (dur_s * sample_rate).clamp(10.0, MAX_HOLD_SEC * sample_rate) as u32;
        // Retrigger envelopes so consecutive step notes articulate clearly with punchy attack
        self.env_stage = 1;
        self.mod_env_stage = 1;
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
        self.velocity = vel.clamp(0.0, 1.0);
        self.age = 0;
        self.phase = 0.0;
        self.sub_phase = 0.0;
        self.mod_phase = 0.0;
        self.phase_inc = self.freq / sample_rate;

        self.env_stage = 1;
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

        self.mod_env_stage = 1;
        self.mod_attack_rate = 1.0 / (patch.mod_attack.max(0.001) * sample_rate);
        self.mod_decay_rate = 1.0 / (patch.mod_decay.max(0.005) * sample_rate);

        if patch.osc_type == 5 {
            // Initialize Karplus-Strong string burst
            self.ks_len = (sample_rate / self.freq).clamp(4.0, 1020.0) as usize;
            self.ks_pos = 0;
            let mut seed: u32 = 0x9e3779b9;
            for i in 0..self.ks_len {
                seed = seed.wrapping_mul(1664525).wrapping_add(1013904223);
                self.ks_buffer[i] = ((seed >> 9) as f32 / 4194304.0) - 1.0;
            }
        }

        // Initialize noise seed deterministically per voice trigger to avoid correlation
        self.noise_seed =
            0x9e3779b9 ^ (self.age.wrapping_mul(2654435761)).wrapping_add((self.freq as u32) << 8);

        // Pitch attack transient snap
        if patch.pitch_env_amt > 0.01 {
            self.pitch_env_level = 1.0;
            let decay_s = patch
                .pitch_env_decay
                .clamp(MIN_PITCH_SNAP_DECAY_SEC, MAX_PITCH_SNAP_DECAY_SEC);
            self.pitch_env_rate = 1.0 / (decay_s * sample_rate);
        } else {
            self.pitch_env_level = 0.0;
            self.pitch_env_rate = 0.0;
        }

        // Low-frequency VCO drift rate (0.2 .. 0.6 Hz)
        let drift_hz = 0.2 + ((patch_id % 7) as f32) * 0.06;
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
        if (self.freq - self.target_freq).abs() > 0.05 {
            self.freq += (self.target_freq - self.freq) * self.glide_rate;
        }

        // Pitch Envelope Decay (Transient Snap)
        if self.pitch_env_level > 0.0 {
            self.pitch_env_level -= self.pitch_env_rate;
            if self.pitch_env_level < 0.0 {
                self.pitch_env_level = 0.0;
            }
        }

        // Calculate pitch modulation from transient snap and VCO analog drift
        let pitch_snap_st = patch.pitch_env_amt * self.pitch_env_level;
        let drift_st = if patch.analog_drift > 0.001 {
            self.drift_phase += self.drift_phase_inc;
            if self.drift_phase >= 1.0 {
                self.drift_phase -= 1.0;
            }
            (self.drift_phase * 2.0 * PI).sin() * patch.analog_drift * MAX_ANALOG_DRIFT_SEMITONES
        } else {
            0.0
        };

        let total_mod_st = pitch_snap_st + drift_st;
        let eff_freq = if total_mod_st.abs() > 0.001 {
            self.freq * (2.0f32).powf(total_mod_st / 12.0)
        } else {
            self.freq
        };
        self.phase_inc = eff_freq / sample_rate;

        // 4-Stage ADSR Envelope
        match self.env_stage {
            1 => {
                // Attack
                self.env_level += self.attack_rate;
                if self.env_level >= 1.0 {
                    self.env_level = 1.0;
                    self.env_stage = 2;
                }
            }
            2 => {
                // Decay
                if self.env_level > self.sustain_level {
                    self.env_level -= self.decay_rate;
                    if self.env_level <= self.sustain_level {
                        self.env_level = self.sustain_level;
                        self.env_stage = if self.sustain_level > 0.001 { 3 } else { 4 };
                    }
                } else {
                    self.env_level = self.sustain_level;
                    self.env_stage = if self.sustain_level > 0.001 { 3 } else { 4 };
                }
            }
            3 => {
                // Sustain
                self.hold_counter = self.hold_counter.saturating_add(1);
                if self.hold_counter >= self.hold_samples {
                    self.env_stage = 4;
                }
            }
            _ => {
                // Release
                self.env_level -= self.release_rate;
                if self.env_level <= 0.0001 {
                    self.env_level = 0.0;
                    self.active = false;
                    return 0.0;
                }
            }
        }

        // Mod Envelope (for filter sweep)
        if self.mod_env_stage == 1 {
            self.mod_env_level += self.mod_attack_rate;
            if self.mod_env_level >= 1.0 {
                self.mod_env_level = 1.0;
                self.mod_env_stage = 2;
            }
        } else {
            self.mod_env_level -= self.mod_decay_rate;
            if self.mod_env_level < 0.0 {
                self.mod_env_level = 0.0;
            }
        }

        let dt = self.phase_inc;
        let mut osc = match patch.osc_type {
            // Saw
            0 => 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt),

            // Pulse / Square with PWM
            1 => {
                let pw = patch.pulse_width.clamp(0.05, 0.95);
                let raw = if self.phase < pw { 1.0 } else { -1.0 };
                raw - poly_blep(self.phase, dt) + poly_blep((self.phase + (1.0 - pw)) % 1.0, dt)
            }

            // Triangle
            2 => 2.0 * (2.0 * (self.phase - 0.5).abs() - 0.5),

            // Sine
            3 => (self.phase * 2.0 * PI).sin(),

            // SuperSaw (7 detuned saws - clean, punchy, phase-corrected)
            4 => {
                let mut sum = 0.0;
                let detunes = [-0.012, -0.007, -0.003, 0.0, 0.003, 0.007, 0.012];
                for &d in &detunes {
                    let p = (self.phase * (1.0 + d)).fract();
                    sum += 2.0 * p - 1.0 - poly_blep(p, dt * (1.0 + d));
                }
                sum * 0.25
            }

            // Karplus-Strong
            5 => {
                let r_pos = self.ks_pos;
                let next_pos = (self.ks_pos + 1) % self.ks_len;
                let out = self.ks_buffer[r_pos];
                let new_val = (self.ks_buffer[r_pos] + self.ks_buffer[next_pos]) * 0.495;
                self.ks_buffer[r_pos] = new_val;
                self.ks_pos = next_pos;
                out * 1.5
            }

            // Organ
            6 => {
                let h1 = (self.phase * 2.0 * PI).sin();
                let h2 = (self.phase * 4.0 * PI).sin() * 0.5;
                let h3 = (self.phase * 6.0 * PI).sin() * 0.25;
                let h4 = (self.phase * 8.0 * PI).sin() * 0.125;
                (h1 + h2 + h3 + h4) * 0.6
            }

            // Chiptune
            7 => {
                let pw = patch.pulse_width.clamp(0.1, 0.9);
                if self.phase < pw {
                    0.8
                } else {
                    -0.8
                }
            }

            // FM Synth
            8 => {
                let mod_freq = dt * 2.0;
                self.mod_phase += mod_freq;
                if self.mod_phase >= 1.0 {
                    self.mod_phase -= 1.0;
                }
                let mod_val = (self.mod_phase * 2.0 * PI).sin() * 2.8 * self.env_level;
                (self.phase * 2.0 * PI + mod_val).sin()
            }

            // Reese
            9 => {
                let dt1 = dt * 0.992;
                let dt2 = dt * 1.008;
                let saw1 = 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt1);
                let p2 = (self.phase * 1.016) % 1.0;
                let saw2 = 2.0 * p2 - 1.0 - poly_blep(p2, dt2);
                saw1 * 0.5 + saw2 * 0.5
            }

            // Blade (CS-80 dual detuned saw)
            10 => {
                let saw1 = 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt);
                let p2 = (self.phase * 1.004) % 1.0;
                let saw2 = 2.0 * p2 - 1.0 - poly_blep(p2, dt * 1.004);
                (saw1 + saw2) * 0.45
            }

            // Hoover
            11 => {
                let pwm = 0.5 + 0.3 * (self.phase * 4.0 * PI).sin();
                let pulse = if self.phase < pwm { 0.7 } else { -0.7 };
                let sub = if self.sub_phase < 0.5 { 0.4 } else { -0.4 };
                pulse * 0.7 + sub * 0.3
            }

            // Click
            12 => {
                let click_pulse = if self.phase < 0.5 { 1.0 } else { -1.0 };
                let click_freq = if self.freq > 1800.0 { 2600.0 } else { 1600.0 };
                let click_sine = (self.phase * 2.0 * PI).sin();
                (click_sine * 0.8 + click_pulse * 0.4) * (click_freq / 2000.0)
            }

            _ => 2.0 * self.phase - 1.0 - poly_blep(self.phase, dt),
        };

        // Sub-oscillator
        if patch.sub_level > 0.0 {
            let sub = (self.sub_phase * 2.0 * PI).sin();
            osc = osc * (1.0 - patch.sub_level * 0.5) + sub * patch.sub_level;
        }

        // Per-voice noise generator (White noise via xorshift32 PRNG)
        if patch.noise_level > 0.001 {
            self.noise_seed ^= self.noise_seed << 13;
            self.noise_seed ^= self.noise_seed >> 17;
            self.noise_seed ^= self.noise_seed << 5;
            let white = (self.noise_seed.cast_signed() as f32) / 2147483648.0;
            osc += white * patch.noise_level.clamp(0.0, 1.0);
        }

        self.phase += dt;
        if self.phase >= 1.0 {
            self.phase -= 1.0;
        }

        self.sub_phase += dt * 0.5;
        if self.sub_phase >= 1.0 {
            self.sub_phase -= 1.0;
        }

        osc *= self.velocity * self.env_level;

        // Dynamic Cutoff with base + key tracking + envelope modulation
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
