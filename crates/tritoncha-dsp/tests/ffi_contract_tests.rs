use tritoncha_dsp::domain::effects::FilterMode;
use tritoncha_dsp::domain::synth::{ModularPatch, OscillatorType, VOICE_PATCH_PARAM_COUNT};
use tritoncha_dsp::domain::BusTarget;
use tritoncha_dsp::{
    tritoncha_dsp_create, tritoncha_dsp_destroy, tritoncha_dsp_note_on, tritoncha_dsp_out_l_ptr,
    tritoncha_dsp_out_r_ptr, tritoncha_dsp_patch_params_ptr, tritoncha_dsp_process,
    tritoncha_dsp_set_voice_patch_packed, tritoncha_dsp_track_durs_ptr,
    tritoncha_dsp_track_inst_ids_ptr, tritoncha_dsp_track_notes_ptr, tritoncha_dsp_track_vels_ptr,
    DEFAULT_SAMPLE_RATE,
};

#[test]
fn test_modular_patch_from_packed_array_and_clamping() {
    assert_eq!(VOICE_PATCH_PARAM_COUNT, 22);

    let raw: [f32; 22] = [
        4.0,    // 0: osc_type = Supersaw (4)
        0.35,   // 1: sub_level
        0.5,    // 2: pulse_width
        4.0,    // 3: filter_type = Ladder24 (4)
        2400.0, // 4: cutoff_base
        4500.0, // 5: cutoff_env_amt
        2.0,    // 6: cutoff_key_track
        0.55,   // 7: resonance
        0.005,  // 8: attack
        0.22,   // 9: decay
        0.35,   // 10: sustain
        0.20,   // 11: release
        0.005,  // 12: mod_attack
        0.22,   // 13: mod_decay
        3.0,    // 14: bus_id = Lead (3)
        8.0,    // 15: polyphony = 8
        0.035,  // 16: glide
        0.35,   // 17: filter_drive
        0.10,   // 18: noise_level
        12.0,   // 19: pitch_snap
        0.020,  // 20: pitch_snap_decay
        0.25,   // 21: analog_drift
    ];

    let patch = ModularPatch::from(raw);
    assert_eq!(patch.osc_type, OscillatorType::Supersaw);
    assert!((*patch.sub_level - 0.35).abs() < 1e-5);
    assert!((*patch.pulse_width - 0.5).abs() < 1e-5);
    assert_eq!(patch.filter_type, FilterMode::Ladder24);
    assert!((*patch.cutoff_base - 2400.0).abs() < 1e-3);
    assert!((*patch.cutoff_env_amt - 4500.0).abs() < 1e-3);
    assert!((*patch.cutoff_key_track - 2.0).abs() < 1e-5);
    assert!((*patch.resonance - 0.55).abs() < 1e-5);
    assert!((*patch.attack - 0.005).abs() < 1e-5);
    assert!((*patch.decay - 0.22).abs() < 1e-5);
    assert!((*patch.sustain - 0.35).abs() < 1e-5);
    assert!((*patch.release - 0.20).abs() < 1e-5);
    assert!((*patch.mod_attack - 0.005).abs() < 1e-5);
    assert!((*patch.mod_decay - 0.22).abs() < 1e-5);
    assert_eq!(patch.bus_id, BusTarget::Lead);
    assert_eq!(*patch.polyphony, 8);
    assert!((*patch.glide - 0.035).abs() < 1e-5);
    assert!((*patch.filter_drive - 0.35).abs() < 1e-5);
    assert!((*patch.noise_level - 0.10).abs() < 1e-5);
    assert!((*patch.pitch_snap - 12.0).abs() < 1e-5);
    assert!((*patch.pitch_snap_decay - 0.020).abs() < 1e-5);
    assert!((*patch.analog_drift - 0.25).abs() < 1e-5);

    // Out-of-bounds values must be clamped by domain value types
    let extreme: [f32; 22] = [
        99.0,    // osc_type -> fallback Saw
        5.0,     // sub_level -> clamped 1.0
        -1.0,    // pulse_width -> clamped 0.05
        99.0,    // filter_type -> fallback Lowpass
        99999.0, // cutoff_base -> clamped 20000.0
        0.0, 0.0, 5.0,   // resonance -> clamped 0.98
        -1.0,  // attack -> clamped 0.001
        100.0, // decay -> clamped 10.0
        2.0,   // sustain -> clamped 1.0
        0.0,   // release -> clamped 0.001
        0.0, 0.0, 99.0, // bus_id -> fallback Drums
        99.0, // polyphony -> clamped 16
        -1.0, // glide -> clamped 0.0
        5.0,  // filter_drive -> clamped 1.0
        -1.0, // noise_level -> clamped 0.0
        99.0, // pitch_snap -> clamped 48.0
        0.0,  // pitch_snap_decay -> clamped 0.002
        5.0,  // analog_drift -> clamped 1.0
    ];
    let clamped = ModularPatch::from(extreme);
    assert_eq!(clamped.osc_type, OscillatorType::Saw);
    assert_eq!(*clamped.sub_level, 1.0);
    assert_eq!(*clamped.pulse_width, 0.05);
    assert_eq!(*clamped.cutoff_base, 20000.0);
    assert_eq!(*clamped.resonance, 0.98);
    assert_eq!(*clamped.polyphony, 16);
}

#[test]
fn test_engine_owned_ffi_buffer_pointers_and_packed_patch_render() {
    unsafe {
        let engine = tritoncha_dsp_create(DEFAULT_SAMPLE_RATE);
        assert!(!engine.is_null());

        let out_l = tritoncha_dsp_out_l_ptr(engine);
        let out_r = tritoncha_dsp_out_r_ptr(engine);
        let inst_ids = tritoncha_dsp_track_inst_ids_ptr(engine);
        let notes = tritoncha_dsp_track_notes_ptr(engine);
        let vels = tritoncha_dsp_track_vels_ptr(engine);
        let durs = tritoncha_dsp_track_durs_ptr(engine);
        let patch_params = tritoncha_dsp_patch_params_ptr(engine);

        assert!(!out_l.is_null());
        assert!(!out_r.is_null());
        assert!(!inst_ids.is_null());
        assert!(!notes.is_null());
        assert!(!vels.is_null());
        assert!(!durs.is_null());
        assert!(!patch_params.is_null());
        assert_ne!(out_l, out_r);

        // Null pointer safety
        assert!(tritoncha_dsp_out_l_ptr(std::ptr::null_mut()).is_null());
        assert!(tritoncha_dsp_patch_params_ptr(std::ptr::null_mut()).is_null());

        // Write a packed patch into engine-owned patch_params buffer and apply to slot 4
        let raw_patch: [f32; 22] = [
            0.0, 0.4, 0.5, 0.0, 1800.0, 2400.0, 1.0, 0.4, 0.002, 0.15, 0.5, 0.1, 0.002, 0.15, 1.0,
            1.0, 0.0, 0.2, 0.0, 0.0, 0.015, 0.02,
        ];
        let param_slice = std::slice::from_raw_parts_mut(patch_params, 22);
        param_slice.copy_from_slice(&raw_patch);

        tritoncha_dsp_set_voice_patch_packed(engine, 4, patch_params, 22);
        tritoncha_dsp_note_on(engine, 4, 110.0, 0.9, 0.2);
        tritoncha_dsp_process(engine, out_l, out_r, 128);

        let left_slice = std::slice::from_raw_parts(out_l, 128);
        let peak = left_slice.iter().fold(0.0_f32, |m, &s| m.max(s.abs()));
        assert!(
            peak > 0.01,
            "Packed voice patch on slot 4 must produce audible output, got peak {peak}"
        );

        tritoncha_dsp_destroy(engine);
    }
}
