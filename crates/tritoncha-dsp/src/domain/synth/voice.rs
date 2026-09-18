//! Modular synthesizer voice DSP implementation with bandlimited anti-aliased oscillators.

pub use super::envelope::{
    AdsrEnvelope, EnvelopeStage, ENVELOPE_SILENCE_THRESHOLD, SUSTAIN_ACTIVE_THRESHOLD,
};
pub use super::oscillator::*;
pub use super::patch::*;
use crate::core::math::{
    calc_rate, sin_phase, soft_clip, time_to_samples, wrap_phase, xorshift32_norm,
    SEMITONES_PER_OCTAVE,
};
use crate::domain::effects::{LadderFilter, StateVariableFilter};

// Voice Tuning and Timing Constants
pub const MIN_PORTAMENTO_TIME_SEC: f32 = 0.001;
pub const MIN_GLIDE_RATE: f32 = 0.0001;
pub const MAX_GLIDE_RATE: f32 = 1.0;
pub const PORTAMENTO_EPSILON: f32 = 0.05;

pub const MIN_VELOCITY: f32 = 0.0;
pub const MAX_VELOCITY: f32 = 1.0;

pub const PITCH_MOD_ACTIVE_THRESHOLD: f32 = 0.001;

pub const VCO_BASE_DRIFT_HZ: f32 = 0.2;
pub const VCO_DRIFT_INCREMENT_HZ: f32 = 0.06;
pub const NUM_DRIFT_VOICE_SLOTS: usize = 7;

pub const KARPLUS_BUFFER_SIZE: usize = 1024;
pub const KARPLUS_MIN_LEN: f32 = 4.0;
pub const KARPLUS_MAX_LEN: f32 = 1020.0;
pub const KARPLUS_FEEDBACK_COEFF: f32 = 0.495;
pub const KARPLUS_OUTPUT_GAIN: f32 = 1.5;

pub const DEFAULT_INITIAL_NOISE_SEED: u32 = 0x9e3779b9;
pub const NOISE_SEED_PRIME: u32 = 2654435761;

/// Polyphonic synthesizer voice.
pub struct SynthVoice {
    pub active: bool,
    pub patch_id: usize,
    pub freq: f32,
    pub target_freq: f32,
    pub glide_rate: f32,
    pub velocity: f32,
    pub age: u32,
    pub amp_env: AdsrEnvelope,
    pub mod_env: AdsrEnvelope,
    phase: f32,
    phase_inc: f32,
    sub_phase: f32,
    mod_phase: f32,
    filter: StateVariableFilter,
    ladder_filter: LadderFilter,
    supersaw_phases: [f32; NUM_SUPERSAW_VOICES],

    // Karplus-Strong string synthesis buffer
    ks_buffer: [f32; KARPLUS_BUFFER_SIZE],
    ks_pos: usize,
    ks_len: usize,

    // Analog synthesis state modeling
    noise_seed: u32,
    pitch_snap_level: f32,
    pitch_snap_rate: f32,
    drift_phase: f32,
    drift_phase_inc: f32,
}

impl SynthVoice {
    pub fn new() -> Self {
        Self {
            active: false,
            patch_id: PATCH_SAW_BASS,
            freq: 440.0,
            target_freq: 440.0,
            glide_rate: 1.0,
            velocity: 0.0,
            age: 0,
            amp_env: AdsrEnvelope::new(),
            mod_env: AdsrEnvelope::new(),
            phase: 0.0,
            phase_inc: 0.0,
            sub_phase: 0.0,
            mod_phase: 0.0,
            filter: StateVariableFilter::new(),
            ladder_filter: LadderFilter::new(),
            supersaw_phases: [0.0; NUM_SUPERSAW_VOICES],
            ks_buffer: [0.0; KARPLUS_BUFFER_SIZE],
            ks_pos: 0,
            ks_len: 200,
            noise_seed: DEFAULT_INITIAL_NOISE_SEED,
            pitch_snap_level: 0.0,
            pitch_snap_rate: 0.01,
            drift_phase: 0.0,
            drift_phase_inc: 0.0001,
        }
    }

    /// Returns current amplitude envelope stage.
    #[inline(always)]
    pub fn env_stage(&self) -> EnvelopeStage {
        self.amp_env.stage
    }

    /// Returns current amplitude envelope level.
    #[inline(always)]
    pub fn env_level(&self) -> f32 {
        self.amp_env.level
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
        self.velocity = vel.clamp(MIN_VELOCITY, MAX_VELOCITY);
        let time_s = glide_time.max(MIN_PORTAMENTO_TIME_SEC);
        self.glide_rate = (1.0 / (time_s * sample_rate)).clamp(MIN_GLIDE_RATE, MAX_GLIDE_RATE);
        self.age = 0;
        self.amp_env.hold_samples =
            time_to_samples(dur_s, sample_rate, 10.0 / sample_rate, MAX_HOLD_SEC);
        self.amp_env.retrigger_attack();
        self.mod_env.retrigger_attack();
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
        self.velocity = vel.clamp(MIN_VELOCITY, MAX_VELOCITY);
        self.age = 0;
        self.phase = 0.0;
        self.sub_phase = 0.0;
        self.mod_phase = 0.0;
        self.phase_inc = self.freq / sample_rate;

        let hold_time = if dur_s > MIN_HOLD_SEC {
            dur_s
        } else if patch.polyphony > 1 {
            DEFAULT_POLY_HOLD_SEC
        } else {
            DEFAULT_MONO_HOLD_SEC
        };

        self.amp_env.trigger(
            patch.attack.max(MIN_ATTACK_SEC),
            patch.decay.max(MIN_DECAY_SEC),
            patch.sustain.clamp(0.0, 1.0),
            patch.release.max(MIN_RELEASE_SEC),
            hold_time,
            sample_rate,
        );

        self.mod_env.trigger_ad(
            patch.mod_attack.max(MIN_ATTACK_SEC),
            patch.mod_decay.max(MIN_DECAY_SEC),
            sample_rate,
        );

        if patch.osc_type == OSC_KARPLUS {
            // Initialize Karplus-Strong string burst with white noise excitation
            self.ks_len =
                (sample_rate / self.freq).clamp(KARPLUS_MIN_LEN, KARPLUS_MAX_LEN) as usize;
            self.ks_pos = 0;
            let mut seed: u32 = DEFAULT_INITIAL_NOISE_SEED;
            for i in 0..self.ks_len {
                self.ks_buffer[i] = xorshift32_norm(&mut seed);
            }
        }

        // Initialize noise seed deterministically per voice trigger to eliminate inter-voice correlation
        self.noise_seed = DEFAULT_INITIAL_NOISE_SEED
            ^ (self.age.wrapping_mul(NOISE_SEED_PRIME)).wrapping_add((self.freq as u32) << 8);

        // Pitch attack transient snap
        if patch.pitch_snap > 0.01 {
            self.pitch_snap_level = 1.0;
            let decay_s = patch
                .pitch_snap_decay
                .clamp(MIN_PITCH_SNAP_DECAY_SEC, MAX_PITCH_SNAP_DECAY_SEC);
            self.pitch_snap_rate = calc_rate(decay_s, sample_rate, MIN_PITCH_SNAP_DECAY_SEC);
        } else {
            self.pitch_snap_level = 0.0;
            self.pitch_snap_rate = 0.0;
        }

        // Low-frequency VCO drift rate (0.2 .. 0.6 Hz)
        let drift_hz = VCO_BASE_DRIFT_HZ
            + ((patch_id % NUM_DRIFT_VOICE_SLOTS) as f32) * VCO_DRIFT_INCREMENT_HZ;
        self.drift_phase_inc = drift_hz / sample_rate;

        // Initialize free-running supersaw phase offsets
        if patch.osc_type == OSC_SUPERSAW {
            let mut seed = self.noise_seed;
            for phase in &mut self.supersaw_phases {
                *phase = (xorshift32_norm(&mut seed) * 0.5 + 0.5).fract();
            }
        }

        self.ladder_filter.reset();
        self.filter.reset();
    }

    #[inline(always)]
    pub fn process_sample(&mut self, patch: &ModularPatch, sample_rate: f32) -> f32 {
        if !self.active {
            return 0.0;
        }

        self.age = self.age.wrapping_add(1);

        // Portamento / Pitch Glide
        if (self.freq - self.target_freq).abs() > PORTAMENTO_EPSILON {
            self.freq += (self.target_freq - self.freq) * self.glide_rate;
        }

        // Pitch Envelope Decay (Transient Snap)
        if self.pitch_snap_level > 0.0 {
            self.pitch_snap_level = (self.pitch_snap_level - self.pitch_snap_rate).max(0.0);
        }

        // Calculate pitch modulation from transient snap and VCO analog drift
        let pitch_snap_st = patch.pitch_snap * self.pitch_snap_level;
        let drift_st = if patch.analog_drift > 0.001 {
            self.drift_phase = wrap_phase(self.drift_phase + self.drift_phase_inc);
            sin_phase(self.drift_phase) * patch.analog_drift * MAX_ANALOG_DRIFT_SEMITONES
        } else {
            0.0
        };

        let total_mod_st = pitch_snap_st + drift_st;
        let eff_freq = if total_mod_st.abs() > PITCH_MOD_ACTIVE_THRESHOLD {
            self.freq * (total_mod_st / SEMITONES_PER_OCTAVE).exp2()
        } else {
            self.freq
        };
        self.phase_inc = eff_freq / sample_rate;

        // Advance amplitude and modulation envelopes
        let env_level = self.amp_env.process_sample();
        if !self.amp_env.is_active() {
            self.active = false;
            return 0.0;
        }
        let mod_env_level = self.mod_env.process_sample();

        let dt = self.phase_inc;
        let osc_type = OscillatorType::from(patch.osc_type);

        let mut osc = match osc_type {
            OscillatorType::Saw => render_saw(self.phase, dt),
            OscillatorType::Pulse => render_pulse(self.phase, dt, patch.pulse_width),
            OscillatorType::Triangle => render_triangle(self.phase),
            OscillatorType::Sine => render_sine(self.phase),
            OscillatorType::Supersaw => {
                advance_supersaw_phases(&mut self.supersaw_phases, dt);
                render_supersaw(&self.supersaw_phases, dt)
            }
            OscillatorType::Organ => render_organ(self.phase),
            OscillatorType::Chiptune => render_chiptune(self.phase, patch.pulse_width),
            OscillatorType::Fm => {
                let mod_freq = dt * FM_MOD_RATIO;
                self.mod_phase = wrap_phase(self.mod_phase + mod_freq);
                render_fm(self.phase, self.mod_phase, env_level)
            }
            OscillatorType::Reese => render_reese(self.phase, dt),
            OscillatorType::Blade => render_blade(self.phase, dt),
            OscillatorType::Hoover => render_hoover(self.phase, self.sub_phase),
            OscillatorType::Click => render_click(self.phase, self.freq),
            OscillatorType::Karplus => {
                let r_pos = self.ks_pos;
                let next_pos = (self.ks_pos + 1) % self.ks_len;
                let out = self.ks_buffer[r_pos];
                let new_val =
                    (self.ks_buffer[r_pos] + self.ks_buffer[next_pos]) * KARPLUS_FEEDBACK_COEFF;
                self.ks_buffer[r_pos] = new_val;
                self.ks_pos = next_pos;
                out * KARPLUS_OUTPUT_GAIN
            }
        };

        // Sub-oscillator synthesis
        if patch.sub_level > 0.0 {
            let sub = sin_phase(self.sub_phase);
            osc = osc * (1.0 - patch.sub_level * 0.5) + sub * patch.sub_level;
        }

        // Per-voice white noise injection via xorshift32 PRNG
        if patch.noise_level > 0.001 {
            let white = xorshift32_norm(&mut self.noise_seed);
            osc += white * patch.noise_level.clamp(0.0, 1.0);
        }

        self.phase = wrap_phase(self.phase + dt);
        self.sub_phase = wrap_phase(self.sub_phase + dt * 0.5);

        // Dynamic filter cutoff with base + key tracking + envelope modulation
        let cutoff = (patch.cutoff_base
            + self.freq * patch.cutoff_key_track
            + patch.cutoff_env_amt * mod_env_level)
            .clamp(20.0, 20000.0);

        let filtered = if patch.filter_type == FILTER_LADDER_24DB {
            self.ladder_filter.process(
                osc,
                cutoff,
                patch.resonance,
                sample_rate,
                patch.filter_drive,
            )
        } else {
            self.filter.process_with_drive(
                osc,
                cutoff,
                patch.resonance,
                sample_rate,
                patch.filter_type,
                patch.filter_drive,
            )
        };

        // Post-filter VCA stage (amp envelope + velocity)
        soft_clip(filtered * self.velocity * env_level)
    }
}

impl Default for SynthVoice {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::DEFAULT_SAMPLE_RATE;

    #[test]
    fn test_voice_initial_state() {
        let voice = SynthVoice::new();
        assert!(!voice.active);
        assert_eq!(voice.age, 0);
        assert_eq!(voice.env_stage(), EnvelopeStage::Idle);
    }

    #[test]
    fn test_voice_trigger_and_envelope_progression() {
        let mut voice = SynthVoice::new();
        let patch = ModularPatch::default_lead();
        let sr = DEFAULT_SAMPLE_RATE;

        voice.trigger(440.0, 0.9, PATCH_LEAD, &patch, sr, 0.1);
        assert!(voice.active);
        assert_eq!(voice.env_stage(), EnvelopeStage::Attack);
        assert_eq!(voice.freq, 440.0);

        let mut max_sample = 0.0_f32;
        for _ in 0..100 {
            let s = voice.process_sample(&patch, sr);
            max_sample = max_sample.max(s.abs());
        }
        assert!(max_sample > 0.0);
    }

    #[test]
    fn test_voice_portamento_glide() {
        let mut voice = SynthVoice::new();
        let patch = ModularPatch::default_lead();
        let sr = DEFAULT_SAMPLE_RATE;

        voice.trigger(220.0, 0.8, PATCH_LEAD, &patch, sr, 0.5);
        voice.glide_to(440.0, 0.8, sr, 0.05, 0.5);
        assert_eq!(voice.target_freq, 440.0);
        assert!(voice.freq < 440.0);

        for _ in 0..15000 {
            voice.process_sample(&patch, sr);
        }
        assert!((voice.freq - 440.0).abs() < 1.0);
    }

    #[test]
    fn test_all_oscillator_types_bounded_output() {
        let mut voice = SynthVoice::new();
        let sr = DEFAULT_SAMPLE_RATE;

        for osc_id in 0..12 {
            let mut patch = ModularPatch::default_lead();
            patch.osc_type = osc_id;
            voice.trigger(440.0, 0.9, 0, &patch, sr, 0.1);

            for _ in 0..50 {
                let out = voice.process_sample(&patch, sr);
                assert!(out.is_finite());
                assert!(
                    out.abs() <= 1.5,
                    "Oscillator {} exceeded limit: {}",
                    osc_id,
                    out
                );
            }
        }
    }

    #[test]
    fn test_karplus_strong_synthesis() {
        let mut voice = SynthVoice::new();
        let mut patch = ModularPatch::default_lead();
        patch.osc_type = OSC_KARPLUS;
        let sr = DEFAULT_SAMPLE_RATE;

        voice.trigger(330.0, 1.0, 0, &patch, sr, 0.2);
        assert!(voice.active);

        let mut energy = 0.0;
        for _ in 0..200 {
            let s = voice.process_sample(&patch, sr);
            energy += s * s;
        }
        assert!(
            energy > 0.0,
            "Karplus-Strong should generate audible acoustic energy"
        );
    }
}
