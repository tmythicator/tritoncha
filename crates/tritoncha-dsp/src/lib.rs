pub mod core;
pub mod domain;
pub mod engine;
pub mod services;

use crate::domain::drums::{PercPatch, PERC_PATCH_PARAM_COUNT};
use crate::domain::effects::CompressorConfig;
use crate::domain::synth::{ModularPatch, VOICE_PATCH_PARAM_COUNT};
use crate::domain::BusId;
pub use engine::{TritonchaEngine, DEFAULT_SAMPLE_RATE};

// WebAssembly C-ABI Foreign Function Interface (FFI)

macro_rules! ffi_engine_ptr_fn {
    ($(
        $(#[$meta:meta])*
        $fn_name:ident -> *mut $elem_ty:ty : $field:ident;
    )*) => {
        $(
            $(#[$meta])*
            ///
            /// # Safety
            /// `ptr` must be either null or a valid pointer to an initialized `TritonchaEngine`.
            #[no_mangle]
            pub unsafe extern "C" fn $fn_name(ptr: *mut TritonchaEngine) -> *mut $elem_ty {
                ptr.as_mut()
                    .map_or(std::ptr::null_mut(), |e| e.ffi.$field.as_mut_ptr())
            }
        )*
    };
}

ffi_engine_ptr_fn! {
    /// Returns a writable pointer to the engine-owned 128-sample left output buffer.
    tritoncha_dsp_out_l_ptr -> *mut f32 : out_l;
    /// Returns a writable pointer to the engine-owned 128-sample right output buffer.
    tritoncha_dsp_out_r_ptr -> *mut f32 : out_r;
    /// Returns a writable pointer to the engine-owned 128-step track instrument ID buffer.
    tritoncha_dsp_track_inst_ids_ptr -> *mut i16 : track_inst_ids;
    /// Returns a writable pointer to the engine-owned 128-step track MIDI note buffer.
    tritoncha_dsp_track_notes_ptr -> *mut i16 : track_notes;
    /// Returns a writable pointer to the engine-owned 128-step track velocity buffer.
    tritoncha_dsp_track_vels_ptr -> *mut f32 : track_vels;
    /// Returns a writable pointer to the engine-owned 128-step track duration buffer.
    tritoncha_dsp_track_durs_ptr -> *mut f32 : track_durs;
    /// Returns a writable pointer to the engine-owned 24-float patch parameter scratch buffer.
    tritoncha_dsp_patch_params_ptr -> *mut f32 : patch_params;
}

#[inline(always)]
unsafe fn read_packed_patch(params_ptr: *const f32, len: i32) -> Option<ModularPatch> {
    if !params_ptr.is_null() && (len as usize) >= VOICE_PATCH_PARAM_COUNT {
        let slice = std::slice::from_raw_parts(params_ptr, VOICE_PATCH_PARAM_COUNT);
        let mut arr = [0.0_f32; VOICE_PATCH_PARAM_COUNT];
        arr.copy_from_slice(slice);
        Some(ModularPatch::from(arr))
    } else {
        None
    }
}

/// Assigns the active modular synthesizer patch owned directly by a sequencer track (0..15).
///
/// # Safety
/// `ptr` must be a valid pointer to `TritonchaEngine`, and `params_ptr` must point to at least `len` floats.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_set_track_patch(
    ptr: *mut TritonchaEngine,
    track_idx: i32,
    params_ptr: *const f32,
    len: i32,
) {
    if let (Some(engine), Some(patch)) = (ptr.as_mut(), read_packed_patch(params_ptr, len)) {
        if track_idx >= 0 {
            engine.set_track_patch(track_idx as usize, patch);
        }
    }
}

#[inline(always)]
unsafe fn read_packed_perc(params_ptr: *const f32, len: i32) -> Option<PercPatch> {
    if !params_ptr.is_null() && (len as usize) >= PERC_PATCH_PARAM_COUNT {
        let slice = std::slice::from_raw_parts(params_ptr, PERC_PATCH_PARAM_COUNT);
        let mut arr = [0.0_f32; PERC_PATCH_PARAM_COUNT];
        arr.copy_from_slice(slice);
        Some(PercPatch::from(arr))
    } else {
        None
    }
}

/// Triggers a one-shot live synthesizer note from a packed 22-float patch array.
///
/// # Safety
/// `ptr` must be a valid pointer to `TritonchaEngine`, and `params_ptr` must point to at least `len` floats.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_trigger_one_shot_synth(
    ptr: *mut TritonchaEngine,
    params_ptr: *const f32,
    len: i32,
    freq: f32,
    vel: f32,
    dur_s: f32,
) {
    if let (Some(engine), Some(patch)) = (ptr.as_mut(), read_packed_patch(params_ptr, len)) {
        engine.trigger_one_shot_synth(patch, freq, vel, dur_s);
    }
}

/// Assigns a layered percussion patch to a track-local kit slot (`0..15` on track `0..15`).
///
/// # Safety
/// `ptr` must be a valid pointer to `TritonchaEngine`, and `params_ptr` must point to at least `len` floats.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_set_track_kit_slot(
    ptr: *mut TritonchaEngine,
    track_idx: i32,
    slot_idx: i32,
    params_ptr: *const f32,
    len: i32,
) {
    if let (Some(engine), Some(patch)) = (ptr.as_mut(), read_packed_perc(params_ptr, len)) {
        if track_idx >= 0 && slot_idx >= 0 {
            engine.set_track_kit_slot(track_idx as usize, slot_idx as usize, patch);
        }
    }
}

/// Triggers a live one-shot percussion hit from a packed 24-float patch array.
/// Pass `freq <= 0.0` to use the patch's own base pitch, or `freq > 0.0` to override pitch.
///
/// # Safety
/// `ptr` must be a valid pointer to `TritonchaEngine`, and `params_ptr` must point to at least `len` floats.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_trigger_one_shot_perc(
    ptr: *mut TritonchaEngine,
    params_ptr: *const f32,
    len: i32,
    freq: f32,
    vel: f32,
) {
    if let (Some(engine), Some(patch)) = (ptr.as_mut(), read_packed_perc(params_ptr, len)) {
        let note_freq = if freq > 0.0 { Some(freq) } else { None };
        engine.trigger_one_shot_perc(patch, vel, note_freq);
    }
}

macro_rules! ffi_engine_fn {
    ($(
        $(#[$meta:meta])*
        $fn_name:ident(
            |$engine:ident $(, $arg:ident : $arg_ty:ty)*| $body:expr
        );
    )*) => {
        $(
            $(#[$meta])*
            ///
            /// # Safety
            /// `ptr` must be a valid non-null pointer to an initialized `TritonchaEngine`.
            #[no_mangle]
            pub unsafe extern "C" fn $fn_name(ptr: *mut TritonchaEngine $(, $arg: $arg_ty)*) {
                if let Some($engine) = ptr.as_mut() {
                    $body;
                }
            }
        )*
    };
}

/// Creates a new heap-allocated DSP engine instance.
#[no_mangle]
pub extern "C" fn tritoncha_dsp_create(sample_rate: f32) -> *mut TritonchaEngine {
    let engine = Box::new(TritonchaEngine::new(sample_rate));
    Box::into_raw(engine)
}

/// Frees an existing DSP engine instance.
///
/// # Safety
/// Caller must ensure that `ptr` is either null or was obtained from `tritoncha_dsp_create`.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_destroy(ptr: *mut TritonchaEngine) {
    if !ptr.is_null() {
        drop(Box::from_raw(ptr));
    }
}

/// Sets musical pattern events and durations for a sequencer track.
///
/// # Safety
/// `ptr` must point to an initialized `TritonchaEngine`.
/// When `len > 0`, pointers `inst_ids_ptr`, `notes_ptr`, and `vels_ptr` must point to readable memory of at least `len` elements.
/// If `durs_ptr` is non-null, it must also point to at least `len` floats.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_set_track(
    ptr: *mut TritonchaEngine,
    track_idx: i32,
    inst_ids_ptr: *const i16,
    notes_ptr: *const i16,
    vels_ptr: *const f32,
    durs_ptr: *const f32,
    len: i32,
    step_mult: i32,
) {
    if let Some(engine) = ptr.as_mut() {
        if len > 0 && !notes_ptr.is_null() && !vels_ptr.is_null() && !inst_ids_ptr.is_null() {
            let n = len as usize;
            let inst_ids = std::slice::from_raw_parts(inst_ids_ptr, n);
            let notes = std::slice::from_raw_parts(notes_ptr, n);
            let vels = std::slice::from_raw_parts(vels_ptr, n);
            let durs = if !durs_ptr.is_null() {
                std::slice::from_raw_parts(durs_ptr, n)
            } else {
                &[]
            };
            engine.set_track(
                track_idx as usize,
                inst_ids,
                notes,
                vels,
                durs,
                step_mult.max(1) as usize,
            );
        }
    }
}

/// Retrieves and resets the bitmask of tracks that triggered steps in the sequencer.
///
/// # Safety
/// `ptr` must be a valid non-null pointer to an initialized `TritonchaEngine`.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_take_trigger_mask(ptr: *mut TritonchaEngine) -> u32 {
    ptr.as_mut()
        .map_or(0, |engine| engine.take_triggered_tracks_mask())
}

/// Processes an audio block and fills stereo output buffers.
///
/// # Safety
/// `ptr` must point to an initialized `TritonchaEngine`.
/// `out_left_ptr` and `out_right_ptr` must point to writable memory slices of at least `num_samples` floats.
#[no_mangle]
pub unsafe extern "C" fn tritoncha_dsp_process(
    ptr: *mut TritonchaEngine,
    out_left_ptr: *mut f32,
    out_right_ptr: *mut f32,
    num_samples: i32,
) {
    if let Some(engine) = ptr.as_mut() {
        let n = num_samples as usize;
        let out_l = std::slice::from_raw_parts_mut(out_left_ptr, n);
        let out_r = std::slice::from_raw_parts_mut(out_right_ptr, n);
        engine.process_block(out_l, out_r);
    }
}

ffi_engine_fn! {
    /// Sets the master BPM tempo for the sequencer.
    tritoncha_dsp_set_bpm(|e, bpm: f32| e.set_bpm(bpm));

    /// Starts or stops internal sequencer playback.
    tritoncha_dsp_set_playing(|e, playing: i32| e.set_playing(playing != 0));

    /// Deactivates a sequencer track by index.
    tritoncha_dsp_deactivate_track(|e, track_idx: i32| e.deactivate_track(track_idx as usize));

    /// Clears and stops all sequencer tracks.
    tritoncha_dsp_clear_tracks(|e| e.clear_tracks());

    /// Mutes or unmutes a sequencer track.
    tritoncha_dsp_mute_track(|e, track_idx: i32, muted: i32| e.mute_track(track_idx as usize, muted != 0));

    /// Solos or unsolos a sequencer track.
    tritoncha_dsp_solo_track(|e, track_idx: i32, solo: i32| e.solo_track(track_idx as usize, solo != 0));

    /// Configures mixer bus gain, mute status, and effect send levels.
    tritoncha_dsp_set_bus_params(
        |e, bus_idx: i32, gain_db: f32, muted: i32, send_delay: f32, send_reverb: f32, bypass_master_fx: i32|
            e.set_bus_params(bus_idx as usize, gain_db, muted != 0, send_delay, send_reverb, bypass_master_fx != 0)
    );

    /// Sets master lowpass filter cutoff frequency and resonance.
    tritoncha_dsp_set_master_filter(|e, cutoff_hz: f32, resonance: f32| e.set_master_filter(cutoff_hz, resonance));

    /// Automates a smooth frequency sweep on the master filter.
    tritoncha_dsp_sweep_master_filter(|e, from_hz: f32, to_hz: f32, duration_secs: f32| e.sweep_master_filter(from_hz, to_hz, duration_secs));

    /// Configures master overdrive saturation, bit depth, and sample rate reduction.
    tritoncha_dsp_set_drive_bitcrush(|e, drive: f32, bit_depth: f32, sample_hold: f32| e.set_drive_bitcrush(drive, bit_depth, sample_hold));

    /// Configures master overdrive saturation algorithm mode (0 = Classic Pade, 1 = ADAA-1 Antialiased).
    tritoncha_dsp_set_drive_mode(|e, mode: i32| e.set_drive_mode(mode.max(0) as u8));

    /// Sets stereo chorus modulation rate, depth, and mix level.
    tritoncha_dsp_set_chorus(|e, rate_hz: f32, depth: f32, mix: f32| e.set_chorus(rate_hz, depth, mix));

    /// Sets sidechain ducking compression amount.
    tritoncha_dsp_set_sidechain(|e, amount: f32| e.set_sidechain(amount));

    /// Sets stereo ping-pong delay time, feedback, and wet balance.
    tritoncha_dsp_set_delay(|e, time_s: f32, feedback: f32, wet: f32| e.set_delay(time_s, feedback, wet));

    /// Sets stereo diffusion reverb room size and wet balance.
    tritoncha_dsp_set_reverb(|e, room_size: f32, wet: f32| e.set_reverb(room_size, wet));

    /// Configures reverb algorithm mode (0 = Freeverb, 1 = 8-Channel Householder FDN).
    tritoncha_dsp_set_reverb_mode(|e, mode: i32| e.set_reverb_mode(mode.max(0) as u8));

    /// Configures master bus glue compressor parameters.
    tritoncha_dsp_set_master_compressor(
        |e, enabled: i32, threshold_db: f32, ratio: f32, attack_s: f32, release_s: f32, makeup_db: f32, mix: f32|
            e.set_master_compressor(CompressorConfig {
                enabled: enabled != 0,
                threshold_db,
                ratio,
                attack_sec: attack_s,
                release_sec: release_s,
                makeup_gain_db: makeup_db,
                mix,
            })
    );

    /// Sets master output volume in decibels.
    tritoncha_dsp_set_master_volume(|e, gain_db: f32| e.set_master_volume(gain_db));

    /// Clears all effect inserts and resets target_out for a bus chain.
    tritoncha_dsp_clear_bus_chain(|e, bus_idx: i32| e.clear_bus_chain(bus_idx as usize));

    /// Sets whether a bus terminates in direct out (bypassing master chain) or master.
    tritoncha_dsp_set_bus_target_out(|e, bus_idx: i32, target_out: i32| e.set_bus_target_out(bus_idx as usize, target_out != 0));

    /// Appends a lowpass filter insert to a bus chain.
    tritoncha_dsp_add_bus_filter(|e, bus_idx: i32, cutoff_hz: f32, resonance: f32| e.add_bus_filter(bus_idx as usize, cutoff_hz, resonance));

    /// Appends a stereo delay insert to a bus chain.
    tritoncha_dsp_add_bus_delay(|e, bus_idx: i32, time_s: f32, feedback: f32, wet: f32| e.add_bus_delay(bus_idx as usize, time_s, feedback, wet));

    /// Appends an overdrive/bitcrusher insert to a bus chain.
    tritoncha_dsp_add_bus_distort(|e, bus_idx: i32, drive: f32, bits: f32, sample_hold: f32| e.add_bus_distort(bus_idx as usize, drive, bits, sample_hold));

    /// Appends a stereo chorus insert to a bus chain.
    tritoncha_dsp_add_bus_chorus(|e, bus_idx: i32, rate_hz: f32, depth: f32, mix: f32| e.add_bus_chorus(bus_idx as usize, rate_hz, depth, mix));

    /// Appends a stereo reverb insert to a bus chain.
    tritoncha_dsp_add_bus_reverb(|e, bus_idx: i32, room_size: f32, wet: f32| e.add_bus_reverb(bus_idx as usize, room_size, wet));

    /// Appends a bus compressor insert to a bus chain.
    tritoncha_dsp_add_bus_compressor(
        |e, bus_idx: i32, threshold_db: f32, ratio: f32, attack_s: f32, release_s: f32, makeup_db: f32, mix: f32|
            e.add_bus_compressor(bus_idx as usize, CompressorConfig {
                enabled: true,
                threshold_db,
                ratio,
                attack_sec: attack_s,
                release_sec: release_s,
                makeup_gain_db: makeup_db,
                mix,
            })
    );

    /// Updates parameters for a filter on a bus or globally if bus_idx < 0.
    tritoncha_dsp_update_bus_filter(|e, bus_idx: i32, cutoff_hz: f32, resonance: f32| e.update_bus_filter(BusId::from_ffi(bus_idx), cutoff_hz, resonance));

    /// Updates parameters for a delay on a bus or globally if bus_idx < 0.
    tritoncha_dsp_update_bus_delay(|e, bus_idx: i32, time_s: f32, feedback: f32, wet: f32| e.update_bus_delay(BusId::from_ffi(bus_idx), time_s, feedback, wet));

    /// Updates parameters for an overdrive/bitcrusher on a bus or globally if bus_idx < 0.
    tritoncha_dsp_update_bus_distort(|e, bus_idx: i32, drive: f32, bits: f32, sample_hold: f32| e.update_bus_distort(BusId::from_ffi(bus_idx), drive, bits, sample_hold));

    /// Updates parameters for a chorus on a bus or globally if bus_idx < 0.
    tritoncha_dsp_update_bus_chorus(|e, bus_idx: i32, rate_hz: f32, depth: f32, mix: f32| e.update_bus_chorus(BusId::from_ffi(bus_idx), rate_hz, depth, mix));

    /// Updates parameters for a reverb on a bus or globally if bus_idx < 0.
    tritoncha_dsp_update_bus_reverb(|e, bus_idx: i32, room_size: f32, wet: f32| e.update_bus_reverb(BusId::from_ffi(bus_idx), room_size, wet));

    /// Updates parameters for a compressor on a bus or globally if bus_idx < 0.
    tritoncha_dsp_update_bus_compressor(
        |e, bus_idx: i32, threshold_db: f32, ratio: f32, attack_s: f32, release_s: f32, makeup_db: f32, mix: f32|
            e.update_bus_compressor(BusId::from_ffi(bus_idx), CompressorConfig {
                enabled: true,
                threshold_db,
                ratio,
                attack_sec: attack_s,
                release_sec: release_s,
                makeup_gain_db: makeup_db,
                mix,
            })
    );
}

// Engine Version and Build Metadata

pub const TRITONCHA_DSP_VERSION: &str = env!("CARGO_PKG_VERSION");
pub const TRITONCHA_DSP_BUILD_INFO: &str =
    "tritoncha-dsp 1.8.0 (Alexandr Timchenko <tmythicator>, AGPL-3.0-or-later)";

/// Returns a pointer to the static DSP engine build info string.
#[no_mangle]
pub extern "C" fn tritoncha_dsp_version_ptr() -> *const u8 {
    TRITONCHA_DSP_BUILD_INFO.as_ptr()
}

/// Returns the byte length of the static DSP engine build info string.
#[no_mangle]
pub extern "C" fn tritoncha_dsp_version_len() -> usize {
    TRITONCHA_DSP_BUILD_INFO.len()
}
