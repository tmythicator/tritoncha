use tritoncha_dsp::domain::drums::perc::{
    NoiseFilterMode, PercBodyType, PercPatch, PercVoice, PercVoicePool, MAX_PERC_VOICES,
    PERC_PATCH_PARAM_COUNT,
};
use tritoncha_dsp::DEFAULT_SAMPLE_RATE;

#[test]
fn test_perc_patch_from_packed_array_and_genuine_zero() {
    assert_eq!(PERC_PATCH_PARAM_COUNT, 24);
    assert_eq!(MAX_PERC_VOICES, 16);

    let raw: [f32; PERC_PATCH_PARAM_COUNT] = [
        1.0,    // 0: ModalBank
        180.0,  // 1: pitch_hz
        90.0,   // 2: pitch_drop
        0.025,  // 3: pitch_decay_s
        0.35,   // 4: decay_s
        0.0,    // 5: click_level = genuine 0.0!
        3200.0, // 6: click_freq_hz
        4.0,    // 7: burst_count
        0.011,  // 8: burst_spacing_s
        0.4,    // 9: fm_depth
        2.5,    // 10: fm_ratio
        0.0,    // 11: noise_mix = genuine 0.0!
        0.2,    // 12: noise_decay_s
        4200.0, // 13: noise_cutoff_hz
        0.5,    // 14: noise_q
        1.0,    // 15: Bandpass
        1.5,    // 16: modal_spread
        1.8,    // 17: drive
        0.0,    // 18: fold = genuine 0.0!
        0.25,   // 19: crush
        0.3,    // 20: vel_to_pitch
        0.5,    // 21: vel_to_tone
        0.2,    // 22: variation
        2.0,    // 23: choke_group = 2
    ];

    let patch = PercPatch::from(raw);
    assert_eq!(patch.body_type, PercBodyType::ModalBank);
    assert_eq!(patch.click_level, 0.0, "click_level 0.0 must remain 0.0");
    assert_eq!(patch.noise_mix, 0.0, "noise_mix 0.0 must remain 0.0");
    assert_eq!(patch.fold, 0.0, "fold 0.0 must remain 0.0");
    assert_eq!(patch.burst_count, 4);
    assert_eq!(patch.noise_filter_mode, NoiseFilterMode::Bandpass);
    assert_eq!(patch.choke_group, 2);
}

#[test]
fn test_perc_voice_sample_rate_invariance() {
    let kick = PercPatch {
        body_type: PercBodyType::PitchedOsc,
        pitch_hz: 52.0,
        pitch_drop: 160.0,
        pitch_decay_s: 0.03,
        decay_s: 0.25,
        click_level: 0.3,
        noise_mix: 0.0,
        variation: 0.0,
        ..PercPatch::default()
    };

    let measure_tail_ratio = |sr: f32| -> f32 {
        let mut voice = PercVoice::new();
        voice.trigger(&kick, 0.9, None, sr);

        let early_n = (sr * 0.05) as usize;
        let mid_n = (sr * 0.15) as usize;

        let mut early_energy = 0.0_f32;
        for _ in 0..early_n {
            let s = voice.process(sr);
            early_energy += s * s;
        }
        let early_rms = (early_energy / early_n as f32).sqrt();

        let mut mid_energy = 0.0_f32;
        for _ in 0..mid_n {
            let s = voice.process(sr);
            mid_energy += s * s;
        }
        let mid_rms = (mid_energy / mid_n as f32).sqrt();
        mid_rms / early_rms.max(1e-6)
    };

    let ratio_44 = measure_tail_ratio(44100.0);
    let ratio_48 = measure_tail_ratio(48000.0);
    let ratio_96 = measure_tail_ratio(96000.0);

    assert!(
        (ratio_44 - ratio_48).abs() < 0.05,
        "Decay envelope must be sample-rate invariant: 44.1k={ratio_44}, 48k={ratio_48}"
    );
    assert!(
        (ratio_96 - ratio_48).abs() < 0.05,
        "Decay envelope must be sample-rate invariant: 96k={ratio_96}, 48k={ratio_48}"
    );
}

#[test]
fn test_perc_voice_velocity_to_tone_monotonicity() {
    let snare = PercPatch {
        body_type: PercBodyType::PitchedOsc,
        pitch_hz: 185.0,
        decay_s: 0.15,
        noise_mix: 0.9,
        noise_decay_s: 0.18,
        noise_cutoff_hz: 1800.0,
        noise_q: 0.4,
        noise_filter_mode: NoiseFilterMode::Bandpass,
        vel_to_tone: 0.8,
        variation: 0.0,
        ..PercPatch::default()
    };

    let measure_zcr = |vel: f32| -> f32 {
        let mut voice = PercVoice::new();
        voice.trigger(&snare, vel, None, DEFAULT_SAMPLE_RATE);
        let mut crossings = 0usize;
        let mut prev = 0.0_f32;
        for _ in 0..2048 {
            let s = voice.process(DEFAULT_SAMPLE_RATE);
            if (prev <= 0.0 && s > 0.0) || (prev >= 0.0 && s < 0.0) {
                crossings += 1;
            }
            prev = s;
        }
        crossings as f32
    };

    let soft_zcr = measure_zcr(0.25);
    let hard_zcr = measure_zcr(1.0);
    assert!(
        hard_zcr > soft_zcr,
        "Higher velocity with vel_to_tone > 0 must yield brighter output (hard_zcr={hard_zcr}, soft_zcr={soft_zcr})"
    );
}

#[test]
fn test_perc_voice_pool_polyphony_and_choke_groups() {
    let mut pool = PercVoicePool::new();
    let sr = DEFAULT_SAMPLE_RATE;

    // 1. Overlapping polyphonic rolls when choke_group == 0
    let tom = PercPatch {
        body_type: PercBodyType::ModalBank,
        pitch_hz: 160.0,
        decay_s: 0.4,
        choke_group: 0,
        ..PercPatch::default()
    };
    pool.trigger(&tom, 0.9, None, sr);
    for _ in 0..256 {
        pool.process(sr);
    }
    pool.trigger(&tom, 0.9, Some(220.0), sr);
    assert_eq!(
        pool.active_count(),
        2,
        "Two unchoked hits must overlap polyphonically in the pool"
    );

    // 2. Choke group dampening (open hat choked by closed hat in group 1)
    pool.reset();
    let open_hat = PercPatch {
        body_type: PercBodyType::MetallicCluster,
        pitch_hz: 320.0,
        decay_s: 1.2,
        noise_mix: 0.5,
        noise_decay_s: 1.2,
        choke_group: 1,
        ..PercPatch::default()
    };
    let closed_hat = PercPatch {
        body_type: PercBodyType::MetallicCluster,
        pitch_hz: 320.0,
        decay_s: 0.025,
        noise_mix: 0.5,
        noise_decay_s: 0.025,
        choke_group: 1,
        ..PercPatch::default()
    };

    pool.trigger(&open_hat, 0.9, None, sr);
    for _ in 0..512 {
        pool.process(sr);
    }
    assert_eq!(pool.active_count(), 1);

    // Trigger closed hat -> open hat must choke smoothly and finish well before its 1.2s tail
    pool.trigger(&closed_hat, 0.9, None, sr);
    for _ in 0..(sr * 0.08) as usize {
        let s = pool.process(sr);
        assert!(s.is_finite());
    }
    assert_eq!(
        pool.active_count(),
        0,
        "Choked open hat and short closed hat must both finish within 80ms"
    );

    // 3. LRU voice stealing beyond MAX_PERC_VOICES
    for i in 0..24 {
        pool.trigger(&tom, 0.8, Some(100.0 + i as f32 * 10.0), sr);
    }
    assert_eq!(pool.active_count(), MAX_PERC_VOICES);
}

#[test]
fn test_perc_voice_all_body_types_clap_and_variation() {
    let sr = DEFAULT_SAMPLE_RATE;

    let patches = [
        PercPatch {
            body_type: PercBodyType::PitchedOsc,
            pitch_hz: 50.0,
            pitch_drop: 180.0,
            fold: 0.5,
            drive: 2.0,
            ..PercPatch::default()
        },
        PercPatch {
            body_type: PercBodyType::ModalBank,
            pitch_hz: 240.0,
            modal_spread: 1.6,
            decay_s: 0.6,
            ..PercPatch::default()
        },
        PercPatch {
            body_type: PercBodyType::MetallicCluster,
            pitch_hz: 400.0,
            noise_mix: 0.6,
            crush: 0.3,
            ..PercPatch::default()
        },
        PercPatch {
            body_type: PercBodyType::NoiseOnly,
            burst_count: 4,
            burst_spacing_s: 0.011,
            noise_mix: 1.0,
            noise_cutoff_hz: 1200.0,
            noise_q: 0.7,
            noise_filter_mode: NoiseFilterMode::Bandpass,
            decay_s: 0.18,
            noise_decay_s: 0.18,
            ..PercPatch::default()
        },
    ];

    for patch in &patches {
        let mut v = PercVoice::new();
        v.trigger(patch, 0.9, None, sr);
        let mut peak = 0.0_f32;
        for _ in 0..4096 {
            let s = v.process(sr);
            assert!(s.is_finite(), "PercVoice must never output NaN/Inf");
            assert!(s.abs() <= 4.0, "PercVoice output must stay bounded");
            peak = peak.max(s.abs());
        }
        assert!(
            peak > 0.02,
            "Body type {:?} must produce audible output",
            patch.body_type
        );
    }

    // Variation > 0 must humanize consecutive hits so they are not bit-identical
    let varied = PercPatch {
        body_type: PercBodyType::PitchedOsc,
        pitch_hz: 150.0,
        variation: 0.5,
        ..PercPatch::default()
    };
    let mut v = PercVoice::new();
    v.trigger(&varied, 0.9, None, sr);
    let hit1: Vec<f32> = (0..512).map(|_| v.process(sr)).collect();
    v.trigger(&varied, 0.9, None, sr);
    let hit2: Vec<f32> = (0..512).map(|_| v.process(sr)).collect();
    let max_diff = hit1
        .iter()
        .zip(hit2.iter())
        .fold(0.0_f32, |m, (a, b)| m.max((a - b).abs()));
    assert!(
        max_diff > 0.005,
        "Consecutive hits with variation > 0 must differ, got max_diff={max_diff}"
    );
}
