use tritoncha_dsp::domain::effects::FilterMode;
use tritoncha_dsp::domain::synth::{ModularPatch, OscillatorType, VOICE_PATCH_PARAM_COUNT};
use tritoncha_dsp::domain::{BusId, BusTarget};
use tritoncha_dsp::{
    tritoncha_dsp_create, tritoncha_dsp_destroy, tritoncha_dsp_out_l_ptr, tritoncha_dsp_out_r_ptr,
    tritoncha_dsp_patch_params_ptr, tritoncha_dsp_process, tritoncha_dsp_set_track_patch,
    tritoncha_dsp_trigger_one_shot_synth, TritonchaEngine, DEFAULT_SAMPLE_RATE,
};

#[test]
fn test_two_tracks_independent_mono_patches_no_voice_stealing() {
    let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);

    let mono_bass = ModularPatch {
        osc_type: OscillatorType::Saw,
        bus_id: BusTarget::Bass,
        polyphony: 1.into(),
        glide: 0.04.into(),
        ..ModularPatch::default()
    };
    let mono_lead = ModularPatch {
        osc_type: OscillatorType::Pulse,
        bus_id: BusTarget::Lead,
        polyphony: 1.into(),
        glide: 0.02.into(),
        ..ModularPatch::default()
    };

    engine.set_track_patch(0, mono_bass);
    engine.set_track_patch(1, mono_lead);

    engine.trigger_track_synth(0, 55.0, 0.9, 0.3);
    engine.trigger_track_synth(1, 440.0, 0.9, 0.3);

    let mut out_l = [0.0; 128];
    let mut out_r = [0.0; 128];
    engine.process_block(&mut out_l, &mut out_r);

    let active_count = engine.voices.iter().filter(|v| v.active).count();
    assert_eq!(
        active_count, 2,
        "Two mono tracks must each hold their own active voice without stealing each other"
    );

    // Triggering a second note on Track 0 must glide/reuse Track 0's voice (still 2 active total)
    engine.trigger_track_synth(0, 110.0, 0.9, 0.3);
    engine.process_block(&mut out_l, &mut out_r);
    let active_after_glide = engine.voices.iter().filter(|v| v.active).count();
    assert_eq!(
        active_after_glide, 2,
        "Second note on mono Track 0 must glide rather than allocate a 3rd voice"
    );
}

#[test]
fn test_track_sequencer_dispatches_own_track_patch() {
    let mut engine = TritonchaEngine::new(DEFAULT_SAMPLE_RATE);
    engine.clear_bus_chain(BusId::MASTER);
    engine.set_bpm(180.0);
    engine.set_playing(true);

    // Track 0 plays a click patch on Direct bus; inst_ids = [-1] means "track's own synth patch"
    let click_patch = ModularPatch {
        osc_type: OscillatorType::Click,
        bus_id: BusTarget::Direct,
        ..ModularPatch::default()
    };
    engine.set_track_patch(0, click_patch);
    engine.set_track(0, &[-1, -1], &[72, -1], &[1.0, 0.0], &[0.05, 0.05], 1);

    let mut out_l = [0.0; 1280];
    let mut out_r = [0.0; 1280];
    engine.process_block(&mut out_l, &mut out_r);

    let peak = out_l.iter().fold(0.0_f32, |m, &s| m.max(s.abs()));
    assert!(
        peak > 0.01,
        "Track 0 with inst_id -1 must trigger its own bound patch, got peak {peak}"
    );
}

#[test]
fn test_c_abi_set_track_patch_and_one_shot_synth() {
    unsafe {
        let engine = tritoncha_dsp_create(DEFAULT_SAMPLE_RATE);
        assert!(!engine.is_null());

        let out_l = tritoncha_dsp_out_l_ptr(engine);
        let out_r = tritoncha_dsp_out_r_ptr(engine);
        let params_ptr = tritoncha_dsp_patch_params_ptr(engine);

        let siren_params: [f32; VOICE_PATCH_PARAM_COUNT] = [
            OscillatorType::Sine as u8 as f32,
            0.0,
            0.5,
            FilterMode::Lowpass as u8 as f32,
            4000.0,
            2000.0,
            1.0,
            0.3,
            0.002,
            0.2,
            0.6,
            0.2,
            0.002,
            0.2,
            BusTarget::Lead as u8 as f32,
            4.0,
            0.0,
            0.1,
            0.0,
            12.0,
            0.05,
            0.0,
        ];

        let slice = std::slice::from_raw_parts_mut(params_ptr, VOICE_PATCH_PARAM_COUNT);
        slice.copy_from_slice(&siren_params);

        // Configure Track 2 patch via FFI
        tritoncha_dsp_set_track_patch(engine, 2, params_ptr, VOICE_PATCH_PARAM_COUNT as i32);

        // Trigger a one-shot synth via FFI
        tritoncha_dsp_trigger_one_shot_synth(
            engine,
            params_ptr,
            VOICE_PATCH_PARAM_COUNT as i32,
            660.0,
            0.9,
            0.2,
        );

        tritoncha_dsp_process(engine, out_l, out_r, 128);
        let left = std::slice::from_raw_parts(out_l, 128);
        let peak = left.iter().fold(0.0_f32, |m, &s| m.max(s.abs()));
        assert!(
            peak > 0.01,
            "One-shot synth trigger via FFI must produce audible output, got peak {peak}"
        );

        tritoncha_dsp_destroy(engine);
    }
}
