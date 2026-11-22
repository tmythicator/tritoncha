/**
 * Tritoncha Master AudioWorklet & WebAssembly Engine Bridge
 * 128-sample real-time DSP audio processor forwarding messages to the Rust WASM engine.
 */

class TritonchaDSPProcessor extends AudioWorkletProcessor {
  constructor() {
    super();

    this.sampleRate = sampleRate || 48000.0;
    this.wasmExports = null;
    this.wasmEnginePtr = 0;
    this.pendingMessages = [];

    // Engine-owned linear memory buffer pointers (resolved dynamically from WASM engine)
    this.outLeftPtr = 0;
    this.outRightPtr = 0;
    this.instIdsBufferPtr = 0;
    this.notesBufferPtr = 0;
    this.velsBufferPtr = 0;
    this.dursBufferPtr = 0;
    this.patchParamsPtr = 0;

    this.outLeftView = null;
    this.outRightView = null;
    this.instIdsBufferView = null;
    this.notesBufferView = null;
    this.velsBufferView = null;
    this.dursBufferView = null;
    this.patchParamsView = null;

    this.port.onmessage = (event) => {
      const data = event.data;
      if (!data) return;

      if (data.type === 'initWasm') {
        const wasmBuf = data.wasmBinary;
        if (wasmBuf) {
          WebAssembly.instantiate(wasmBuf).then((result) => {
            this.wasmExports = result.instance.exports;
            this.wasmEnginePtr = this.wasmExports.tritoncha_dsp_create(this.sampleRate);

            this.outLeftPtr = this.wasmExports.tritoncha_dsp_out_l_ptr(this.wasmEnginePtr);
            this.outRightPtr = this.wasmExports.tritoncha_dsp_out_r_ptr(this.wasmEnginePtr);
            this.instIdsBufferPtr = this.wasmExports.tritoncha_dsp_track_inst_ids_ptr(this.wasmEnginePtr);
            this.notesBufferPtr = this.wasmExports.tritoncha_dsp_track_notes_ptr(this.wasmEnginePtr);
            this.velsBufferPtr = this.wasmExports.tritoncha_dsp_track_vels_ptr(this.wasmEnginePtr);
            this.dursBufferPtr = this.wasmExports.tritoncha_dsp_track_durs_ptr(this.wasmEnginePtr);
            this.patchParamsPtr = this.wasmExports.tritoncha_dsp_patch_params_ptr(this.wasmEnginePtr);

            this.ensureMemoryViews();

            let wasmVersion = '1.8.0';
            try {
              const ptr = this.wasmExports.tritoncha_dsp_version_ptr();
              const len = this.wasmExports.tritoncha_dsp_version_len();
              wasmVersion = new TextDecoder('utf-8').decode(new Uint8Array(this.wasmExports.memory.buffer, ptr, len));
            } catch (_) {}
            this.port.postMessage({ type: 'ready', version: wasmVersion });

            if (this.pendingMessages.length > 0) {
              const queued = this.pendingMessages;
              this.pendingMessages = [];
              for (const msg of queued) {
                this.handleMessage(msg);
              }
            }
          }).catch((err) => {
            console.error('WASM instantiate error in AudioWorklet:', err);
          });
        }
      } else if (!this.wasmEnginePtr) {
        this.pendingMessages.push(data);
      } else {
        this.handleMessage(data);
      }
    };
  }

  ensureMemoryViews() {
    if (!this.wasmExports || !this.wasmExports.memory) return;
    const wasmMemory = this.wasmExports.memory;
    if (
      !this.outLeftView ||
      this.outLeftView.buffer !== wasmMemory.buffer ||
      this.outLeftView.buffer.byteLength === 0
    ) {
      this.outLeftView = new Float32Array(wasmMemory.buffer, this.outLeftPtr, 128);
      this.outRightView = new Float32Array(wasmMemory.buffer, this.outRightPtr, 128);
      this.instIdsBufferView = new Int16Array(wasmMemory.buffer, this.instIdsBufferPtr, 128);
      this.notesBufferView = new Int16Array(wasmMemory.buffer, this.notesBufferPtr, 128);
      this.velsBufferView = new Float32Array(wasmMemory.buffer, this.velsBufferPtr, 128);
      this.dursBufferView = new Float32Array(wasmMemory.buffer, this.dursBufferPtr, 128);
      this.patchParamsView = new Float32Array(wasmMemory.buffer, this.patchParamsPtr, 24);
    }
  }

  writePatchParams(params, maxLen) {
    this.ensureMemoryViews();
    const n = Math.min(params.length, maxLen);
    for (let i = 0; i < n; i++) {
      this.patchParamsView[i] = params[i];
    }
    return n;
  }

  applyBusProcessor(fnPrefix, busIdx, p) {
    const e = this.wasmExports;
    const ptr = this.wasmEnginePtr;
    switch (p.type || p.processor) {
      case 'filter':
        e[`${fnPrefix}_filter`](
          ptr,
          busIdx,
          p.cutoffHz !== undefined ? p.cutoffHz : 18000.0,
          p.resonance !== undefined ? p.resonance : 0.0
        );
        break;
      case 'delay':
        e[`${fnPrefix}_delay`](
          ptr,
          busIdx,
          p.timeS !== undefined ? p.timeS : 0.35,
          p.feedback !== undefined ? p.feedback : 0.4,
          p.wet !== undefined ? p.wet : 0.3
        );
        break;
      case 'distort':
        e[`${fnPrefix}_distort`](
          ptr,
          busIdx,
          p.drive !== undefined ? p.drive : 0.0,
          p.bits !== undefined ? p.bits : 16.0,
          p.sampleHold !== undefined ? p.sampleHold : 1.0
        );
        break;
      case 'chorus':
        e[`${fnPrefix}_chorus`](
          ptr,
          busIdx,
          p.rateHz !== undefined ? p.rateHz : 0.8,
          p.depth !== undefined ? p.depth : 0.4,
          p.mix !== undefined ? p.mix : 0.3
        );
        break;
      case 'reverb':
        e[`${fnPrefix}_reverb`](
          ptr,
          busIdx,
          p.roomSize !== undefined ? p.roomSize : 0.7,
          p.wet !== undefined ? p.wet : 0.3
        );
        break;
      case 'compressor':
        e[`${fnPrefix}_compressor`](
          ptr,
          busIdx,
          p.thresholdDb !== undefined ? p.thresholdDb : -12.0,
          p.ratio !== undefined ? p.ratio : 4.0,
          p.attackS !== undefined ? p.attackS : 0.010,
          p.releaseS !== undefined ? p.releaseS : 0.100,
          p.makeupDb !== undefined ? p.makeupDb : 2.5,
          p.mix !== undefined ? p.mix : 1.0
        );
        break;
    }
  }

  handleMessage(data) {
    if (!this.wasmExports || !this.wasmEnginePtr) return;
    const e = this.wasmExports;
    const ptr = this.wasmEnginePtr;

    switch (data.type) {
      case 'setBpm':
        e.tritoncha_dsp_set_bpm(ptr, data.bpm || 168.0);
        break;

      case 'setPlaying':
        e.tritoncha_dsp_set_playing(ptr, data.playing ? 1 : 0);
        break;

      case 'setTrack':
        if (data.notes && data.vels) {
          this.ensureMemoryViews();
          const len = Math.min(128, data.notes.length);
          for (let i = 0; i < len; i++) {
            this.instIdsBufferView[i] = data.instIds ? data.instIds[i] : -1;
            this.notesBufferView[i] = data.notes[i];
            this.velsBufferView[i] = typeof data.vels[i] === 'number' ? data.vels[i] : 0.9;
            this.dursBufferView[i] = data.durs
              ? (typeof data.durs[i] === 'number' ? data.durs[i] : 0.2)
              : (data.dur || 0.2);
          }
          e.tritoncha_dsp_set_track(
            ptr,
            data.trackIdx || 0,
            this.instIdsBufferPtr,
            this.notesBufferPtr,
            this.velsBufferPtr,
            this.dursBufferPtr,
            len,
            data.stepMult || 1
          );
        }
        break;

      case 'deactivateTrack':
        e.tritoncha_dsp_deactivate_track(ptr, data.trackIdx || 0);
        break;

      case 'clearTracks':
        e.tritoncha_dsp_clear_tracks(ptr);
        break;

      case 'muteTrack':
        e.tritoncha_dsp_mute_track(ptr, data.trackIdx || 0, data.muted ? 1 : 0);
        break;

      case 'soloTrack':
        e.tritoncha_dsp_solo_track(ptr, data.trackIdx || 0, data.solo ? 1 : 0);
        break;

      case 'setTrackPatch':
        if (data.params) {
          const n = this.writePatchParams(data.params, 22);
          e.tritoncha_dsp_set_track_patch(
            ptr,
            data.trackIdx !== undefined ? data.trackIdx : 0,
            this.patchParamsPtr,
            n
          );
        }
        break;

      case 'setTrackKitSlot':
        if (data.params) {
          const n = this.writePatchParams(data.params, 24);
          e.tritoncha_dsp_set_track_kit_slot(
            ptr,
            data.trackIdx !== undefined ? data.trackIdx : 0,
            data.slotIdx !== undefined ? data.slotIdx : 0,
            this.patchParamsPtr,
            n
          );
        }
        break;

      case 'noteOn':
        if (data.percParams) {
          const n = this.writePatchParams(data.percParams, 24);
          e.tritoncha_dsp_trigger_one_shot_perc(
            ptr,
            this.patchParamsPtr,
            n,
            data.freq || 0.0,
            data.velocity || 0.9
          );
        } else if (data.params) {
          const n = this.writePatchParams(data.params, 22);
          e.tritoncha_dsp_trigger_one_shot_synth(
            ptr,
            this.patchParamsPtr,
            n,
            data.freq || 440.0,
            data.velocity || 0.9,
            data.dur || 0.0
          );
        }
        break;

      case 'setBusParams':
        e.tritoncha_dsp_set_bus_params(
          ptr,
          data.busIdx !== undefined ? data.busIdx : 0,
          data.gainDb !== undefined ? data.gainDb : 0.0,
          data.muted ? 1 : 0,
          data.sendDelay !== undefined ? data.sendDelay : 0.0,
          data.sendReverb !== undefined ? data.sendReverb : 0.0,
          data.bypassMasterFx ? 1 : 0
        );
        break;

      case 'setBusChain': {
        const busIdx = data.busIdx !== undefined ? data.busIdx : 0;
        e.tritoncha_dsp_clear_bus_chain(ptr, busIdx);
        e.tritoncha_dsp_set_bus_target_out(ptr, busIdx, data.targetOut ? 1 : 0);
        if (Array.isArray(data.inserts)) {
          for (const ins of data.inserts) {
            this.applyBusProcessor('tritoncha_dsp_add_bus', busIdx, ins);
          }
        }
        break;
      }

      case 'updateBusProcessor': {
        const busIdx = data.busIdx !== undefined ? data.busIdx : -1;
        this.applyBusProcessor('tritoncha_dsp_update_bus', busIdx, data);
        break;
      }

      case 'setFilter':
        e.tritoncha_dsp_set_master_filter(
          ptr,
          data.cutoffHz !== undefined ? data.cutoffHz : 12000.0,
          data.resonance !== undefined ? data.resonance : 0.0
        );
        break;

      case 'sweepFilter':
        e.tritoncha_dsp_sweep_master_filter(
          ptr,
          data.fromHz !== undefined ? data.fromHz : 400.0,
          data.toHz !== undefined ? data.toHz : 12000.0,
          data.durationSecs !== undefined ? data.durationSecs : 4.0
        );
        break;

      case 'setDriveBitcrush':
        e.tritoncha_dsp_set_drive_bitcrush(
          ptr,
          data.drive !== undefined ? data.drive : 0.0,
          data.bitDepth !== undefined ? data.bitDepth : 16.0,
          data.sampleHold !== undefined ? data.sampleHold : 1.0
        );
        break;

      case 'setChorus':
        e.tritoncha_dsp_set_chorus(
          ptr,
          data.rateHz !== undefined ? data.rateHz : 0.8,
          data.depth !== undefined ? data.depth : 0.4,
          data.mix !== undefined ? data.mix : 0.0
        );
        break;

      case 'setSidechain':
        e.tritoncha_dsp_set_sidechain(ptr, data.amount !== undefined ? data.amount : 0.0);
        break;

      case 'setCompressor':
        e.tritoncha_dsp_set_master_compressor(
          ptr,
          data.enabled ? 1 : 0,
          data.thresholdDb !== undefined ? data.thresholdDb : -12.0,
          data.ratio !== undefined ? data.ratio : 4.0,
          data.attackS !== undefined ? data.attackS : 0.010,
          data.releaseS !== undefined ? data.releaseS : 0.100,
          data.makeupDb !== undefined ? data.makeupDb : 2.5,
          data.mix !== undefined ? data.mix : 1.0
        );
        break;

      case 'setVolume':
        e.tritoncha_dsp_set_master_volume(ptr, data.gainDb !== undefined ? data.gainDb : 0.0);
        break;

      case 'setDelay':
        e.tritoncha_dsp_set_delay(
          ptr,
          data.timeS !== undefined ? data.timeS : 0.35,
          data.feedback !== undefined ? data.feedback : 0.4,
          data.wet !== undefined ? data.wet : 0.25
        );
        break;

      case 'setReverb':
        e.tritoncha_dsp_set_reverb(
          ptr,
          data.roomSize !== undefined ? data.roomSize : 0.75,
          data.wet !== undefined ? data.wet : 0.2
        );
        break;

      case 'setDriveMode':
        e.tritoncha_dsp_set_drive_mode(ptr, data.mode !== undefined ? data.mode : 1);
        break;

      case 'setReverbMode':
        e.tritoncha_dsp_set_reverb_mode(ptr, data.mode !== undefined ? data.mode : 1);
        break;
    }
  }

  process(inputs, outputs) {
    const output = outputs[0];
    if (!output || output.length === 0) return true;

    const channelLeft = output[0];
    const channelRight = output.length > 1 ? output[1] : null;
    const blockSize = channelLeft.length; // 128 samples

    if (this.wasmExports && this.wasmEnginePtr) {
      try {
        this.ensureMemoryViews();

        this.wasmExports.tritoncha_dsp_process(
          this.wasmEnginePtr,
          this.outLeftPtr,
          this.outRightPtr,
          blockSize
        );

        this.ensureMemoryViews();

        channelLeft.set(this.outLeftView);
        if (channelRight) {
          channelRight.set(this.outRightView);
        }

        const triggerMask = this.wasmExports.tritoncha_dsp_take_trigger_mask(this.wasmEnginePtr);
        if (triggerMask > 0) {
          this.port.postMessage({ type: 'triggers', mask: triggerMask });
        }
      } catch (err) {
        console.error('Error in TritonchaDSPProcessor process():', err);
      }
    } else {
      channelLeft.fill(0);
      if (channelRight) {
        channelRight.fill(0);
      }
    }

    return true;
  }
}

registerProcessor('tritoncha-dsp-processor', TritonchaDSPProcessor);
