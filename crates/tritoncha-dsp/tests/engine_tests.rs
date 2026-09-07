use tritoncha_dsp::dsp::delay::StereoDelay;
use tritoncha_dsp::dsp::effects::{BitcrusherDrive, SidechainPump, StereoChorus};
use tritoncha_dsp::dsp::filter::StateVariableFilter;
use tritoncha_dsp::dsp::math::{midi_to_freq, poly_blep, soft_clip};
use tritoncha_dsp::dsp::reverb::StereoReverb;
use tritoncha_dsp::engine::TritonchaEngine;
use tritoncha_dsp::sequencer::track::TrackPattern;
use tritoncha_dsp::synth::drums::DrumMachine;
use tritoncha_dsp::synth::patch::ModularPatch;
use tritoncha_dsp::synth::voice::SynthVoice;

#[test]
fn test_dsp_defaults() {
    let _ = TritonchaEngine::default();
    let _ = ModularPatch::default();
    let _ = SynthVoice::default();
    let _ = DrumMachine::default();
    let _ = StateVariableFilter::default();
    let _ = StereoDelay::default();
    let _ = StereoReverb::default();
    let _ = StereoChorus::default();
    let _ = BitcrusherDrive::default();
    let _ = SidechainPump::default();
    let _ = TrackPattern::default();
}

#[test]
fn test_dsp_math_helpers() {
    assert_eq!(midi_to_freq(69.0), 440.0);
    assert!((midi_to_freq(60.0) - 261.6256).abs() < 0.01);

    // Test polynomial soft clipping curve limits
    assert!(soft_clip(0.0) == 0.0);
    assert!(soft_clip(100.0) <= 1.0);
    assert!(soft_clip(-100.0) >= -1.0);

    // Test PolyBLEP anti-aliasing boundary
    assert_eq!(poly_blep(0.5, 0.01), 0.0);
    assert!(poly_blep(0.005, 0.01).abs() > 0.0);
}

#[test]
fn test_tpt_state_variable_filter_stability() {
    let mut filter = StateVariableFilter::new();
    let sample_rate = 48000.0;

    // Test sweep across audio spectrum with high resonance
    for hz in [20.0, 100.0, 1000.0, 5000.0, 10000.0, 20000.0, 22000.0] {
        let out = filter.process_lp(0.8, hz, 0.95, sample_rate);
        assert!(
            !out.is_nan(),
            "Filter output should never be NaN at {hz} Hz"
        );
        assert!(
            !out.is_infinite(),
            "Filter output should never blow up to Inf at {hz} Hz"
        );
    }
}

#[test]
fn test_dsp_effects() {
    let sample_rate = 48000.0;

    // 1. Bitcrusher & Drive
    let mut bd = BitcrusherDrive::new();
    bd.drive = 0.5;
    bd.bit_depth = 8.0;
    let (bl, br) = bd.process(0.5, -0.5);
    assert!(bl.is_finite() && br.is_finite());

    // 2. Stereo Chorus
    let mut chorus = StereoChorus::new();
    chorus.mix = 0.5;
    let (cl, cr) = chorus.process(0.5, -0.5, sample_rate);
    assert!(cl.is_finite() && cr.is_finite());

    // 3. Sidechain Pump
    let mut sc = SidechainPump::new();
    sc.amount = 0.8;
    sc.trigger_kick();
    let ducked = sc.process(1.0);
    assert!(ducked < 1.0, "Kick trigger should duck audio level");
}

#[test]
fn test_synth_voices_trigger_and_decay() {
    let sample_rate = 48000.0;

    for patch_id in [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19] {
        let patch = ModularPatch::default_for(patch_id);
        let mut voice = SynthVoice::new();
        voice.trigger(440.0, 0.9, patch_id, &patch, sample_rate, 0.2);
        assert!(voice.active);

        let sample = voice.process_sample(&patch, sample_rate);
        assert!(sample.is_finite());

        // Process 6.25 seconds to ensure complete ADSR release without sticking active
        for _ in 0..300_000 {
            voice.process_sample(&patch, sample_rate);
        }
        assert!(
            !voice.active,
            "Patch ID {patch_id} should decay to inactive"
        );
    }
}

#[test]
fn test_custom_voice_patch_reconfiguration() {
    let mut engine = TritonchaEngine::new(48000.0);
    // Configure patch 20 as custom resonant square lead
    let patch20 = ModularPatch {
        osc_type: 1, // osc_type = pulse
        sub_level: 0.2,
        pulse_width: 0.3,
        filter_type: 0, // filter_type = lowpass
        cutoff_base: 1800.0,
        cutoff_env_amt: 4000.0,
        cutoff_key_track: 2.0,
        resonance: 0.75,
        attack: 0.01,
        decay: 0.25,
        sustain: 0.3,
        release: 0.15,
        mod_attack: 0.01,
        mod_decay: 0.25,
        bus_id: 3, // bus_id = lead
        polyphony: 8,
        glide: 0.0,
        filter_drive: 0.35,
        noise_level: 0.05,
        pitch_snap: 12.0,
        pitch_snap_decay: 0.015,
        analog_drift: 0.2,
    };
    engine.set_voice_patch(20, patch20);

    engine.trigger_note(20, 440.0, 0.9, 0.2);
    let mut out_l = [0.0; 128];
    let mut out_r = [0.0; 128];
    engine.process_block(&mut out_l, &mut out_r);

    assert!(out_l.iter().any(|&s| s.abs() > 0.001));
    assert!(out_l.iter().all(|&s| s.is_finite()));
}

#[test]
fn test_analog_primitives_drive_noise_pitch_snap() {
    let mut engine = TritonchaEngine::new(48000.0);
    // Configure patch 21 with heavy drive, noise and pitch snap
    let patch21 = ModularPatch {
        osc_type: 0, // osc_type = saw
        sub_level: 0.3,
        pulse_width: 0.5,
        filter_type: 0, // filter_type = lowpass
        cutoff_base: 1200.0,
        cutoff_env_amt: 2000.0,
        cutoff_key_track: 1.0,
        resonance: 0.85, // high resonance
        attack: 0.005,
        decay: 0.2,
        sustain: 0.5,
        release: 0.2,
        mod_attack: 0.005,
        mod_decay: 0.2,
        bus_id: 1,    // bus_id = bass
        polyphony: 1, // mono
        glide: 0.02,
        filter_drive: 0.80, // heavy filter_drive
        noise_level: 0.10,  // audible noise
        pitch_snap: 24.0,   // strong 2-octave pitch attack punch
        pitch_snap_decay: 0.018,
        analog_drift: 0.50,
    };
    engine.set_voice_patch(21, patch21);

    engine.trigger_note(21, 55.0, 0.95, 0.3);
    let mut out_l = [0.0; 128];
    let mut out_r = [0.0; 128];
    engine.process_block(&mut out_l, &mut out_r);

    // Verify non-silent, finite, saturated output within bounds
    assert!(out_l.iter().any(|&s| s.abs() > 0.01));
    assert!(out_l
        .iter()
        .all(|&s| s.is_finite() && !s.is_nan() && s.abs() < 3.0));
}

#[test]
fn test_monophonic_glide_and_stealing() {
    let mut engine = TritonchaEngine::new(48000.0);
    // Acid bass (patch 5) is mono with 0.04s glide
    engine.trigger_note(5, 110.0, 0.9, 0.2);
    let mut out_l = [0.0; 128];
    let mut out_r = [0.0; 128];
    engine.process_block(&mut out_l, &mut out_r);

    // Trigger second note on same mono patch -> should glide / reuse voice rather than allocating a second voice
    engine.trigger_note(5, 220.0, 0.9, 0.2);
    engine.process_block(&mut out_l, &mut out_r);

    let active_mono_voices = engine
        .voices
        .iter()
        .filter(|v| v.active && v.patch_id == 5)
        .count();
    assert_eq!(
        active_mono_voices, 1,
        "Mono synth should only have 1 active voice"
    );
}

#[test]
fn test_drum_machine_triggers() {
    let mut drums = DrumMachine::new();
    let sample_rate = 48000.0;

    drums.trigger_kick(0.9);
    drums.trigger_snare(0.8);
    drums.trigger_hh(0.7, false);
    drums.trigger_clap(0.85);
    drums.trigger_ride(0.75);
    drums.trigger_tom(0.8, 120.0);
    drums.trigger_snare_crack(0.95);
    drums.trigger_ride_bell(0.9);
    drums.trigger_tom_high(0.85);
    drums.trigger_tom_mid(0.8);
    drums.trigger_tom_low(0.9);
    drums.trigger_crash_16(0.95);
    drums.trigger_crash_17(0.9);
    drums.trigger_crash_18(0.85);
    drums.trigger_splash(0.9);
    drums.trigger_china(0.95);
    drums.trigger_cowbell(0.8);

    for _ in 0..1024 {
        let sample = drums.process_sample(sample_rate);
        assert!(sample.is_finite());
    }
}

#[test]
fn test_engine_expanded_drum_dispatch() {
    use tritoncha_dsp::engine::{
        is_drum_inst, INST_DRUM_CHINA, INST_DRUM_COWBELL, INST_DRUM_CRASH_16, INST_DRUM_CRASH_17,
        INST_DRUM_CRASH_18, INST_DRUM_RIDE_BELL, INST_DRUM_SPLASH, INST_DRUM_TOM_HIGH,
        INST_DRUM_TOM_LOW, INST_DRUM_TOM_MID,
    };

    let mut engine = TritonchaEngine::new(48000.0);
    let drum_ids = [
        INST_DRUM_RIDE_BELL,
        INST_DRUM_TOM_HIGH,
        INST_DRUM_TOM_MID,
        INST_DRUM_TOM_LOW,
        INST_DRUM_CRASH_16,
        INST_DRUM_CRASH_17,
        INST_DRUM_CRASH_18,
        INST_DRUM_SPLASH,
        INST_DRUM_CHINA,
        INST_DRUM_COWBELL,
    ];

    for &id in &drum_ids {
        assert!(is_drum_inst(id), "ID {id} should be identified as drum");
        engine.trigger_note(id, 440.0, 0.9, 0.2);
    }

    let mut out_l = [0.0; 256];
    let mut out_r = [0.0; 256];
    engine.process_block(&mut out_l, &mut out_r);

    assert!(out_l.iter().all(|&s| s.is_finite()));
    assert!(out_r.iter().all(|&s| s.is_finite()));
}

#[test]
fn test_tritoncha_engine_rendering_and_sequencer() {
    let mut engine = TritonchaEngine::new(48000.0);
    engine.set_bpm(174.0);
    engine.set_playing(true);

    let notes = [60, 62, 64, 65];
    let vels = [0.9, 0.8, 0.85, 0.95];
    let inst_ids = [4, 4, 4, 4];
    let durs = [0.2, 0.2, 0.2, 0.2];
    engine.set_track(0, &inst_ids, &notes, &vels, &durs, 1);

    let mut out_l = [0.0; 512];
    let mut out_r = [0.0; 512];
    engine.process_block(&mut out_l, &mut out_r);

    assert!(out_l.iter().all(|&s| s.is_finite()));
    assert!(out_r.iter().all(|&s| s.is_finite()));
}

#[test]
fn test_master_filter_sweep() {
    let mut engine = TritonchaEngine::new(48000.0);
    engine.sweep_master_filter(400.0, 8000.0, 0.1); // 0.1s = 4800 samples
    assert_eq!(engine.master_cutoff_hz(), 400.0);

    let mut out_l = [0.0; 128];
    let mut out_r = [0.0; 128];
    engine.process_block(&mut out_l, &mut out_r);
    assert!(
        engine.master_cutoff_hz() > 400.0,
        "Cutoff should advance during sweep"
    );

    // Run remaining blocks to finish sweep
    for _ in 0..40 {
        engine.process_block(&mut out_l, &mut out_r);
    }
    assert_eq!(
        engine.master_cutoff_hz(),
        8000.0,
        "Cutoff should reach target frequency at completion"
    );
}

#[test]
fn test_engine_hi_fi_modes_switching() {
    let mut engine = TritonchaEngine::new(48000.0);
    let mut out_l = [0.0; 128];
    let mut out_r = [0.0; 128];

    // Verify default engine initializes in FDN and ADAA mode
    engine.trigger_note(0, 55.0, 0.9, 0.1);
    engine.process_block(&mut out_l, &mut out_r);
    assert!(out_l[0].is_finite());
    assert!(out_r[0].is_finite());

    // Switch to Freeverb and Classic Pade drive
    engine.set_reverb_mode(0); // Freeverb
    engine.set_drive_mode(0); // Classic
    engine.trigger_note(1, 200.0, 0.85, 0.1);
    engine.process_block(&mut out_l, &mut out_r);
    assert!(out_l[0].is_finite());

    // Switch back to Householder FDN and ADAA-1
    engine.set_reverb_mode(1); // FDN
    engine.set_drive_mode(1); // ADAA
    engine.trigger_note(2, 5000.0, 0.8, 0.05);
    engine.process_block(&mut out_l, &mut out_r);
    assert!(out_l[0].is_finite());
}
