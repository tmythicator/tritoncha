/**
 * Tritoncha Master AudioWorklet & WebAssembly Engine
 * High-performance 128-sample real-time DSP audio processor driving the pure Rust WASM engine.
 */

class TritonchaDSPProcessor extends AudioWorkletProcessor {
  constructor() {
    super();

    this.sampleRate = sampleRate || 48000.0;
    this.useWasm = false;
    this.wasmInstance = null;
    this.wasmExports = null;
    this.wasmEnginePtr = 0;
    this.pendingMessages = [];

    // Linear memory pointers for 128-sample Float32 output blocks & track buffers (128 steps)
    this.outLeftPtr = 16384;
    this.outRightPtr = 16384 + 128 * 4;
    this.instIdsBufferPtr = 20480;
    this.notesBufferPtr = 20480 + 256;
    this.velsBufferPtr = 20480 + 512;
    this.dursBufferPtr = 20480 + 1024;

    this.outLeftView = null;
    this.outRightView = null;
    this.instIdsBufferView = null;
    this.notesBufferView = null;
    this.velsBufferView = null;
    this.dursBufferView = null;

    this.port.onmessage = (event) => {
      const data = event.data;
      if (!data) return;

      if (data.type === 'initWasm') {
        const wasmBuf = data.wasmBinary || data.wasmBytes;
        if (wasmBuf) {
          WebAssembly.instantiate(wasmBuf).then((result) => {
            this.wasmInstance = result.instance;
            this.wasmExports = result.instance.exports;
            this.wasmEnginePtr = this.wasmExports.tritoncha_dsp_create(this.sampleRate);

            const wasmMemory = this.wasmExports.memory;
            this.outLeftView = new Float32Array(wasmMemory.buffer, this.outLeftPtr, 128);
            this.outRightView = new Float32Array(wasmMemory.buffer, this.outRightPtr, 128);
            this.instIdsBufferView = new Int16Array(wasmMemory.buffer, this.instIdsBufferPtr, 128);
            this.notesBufferView = new Int16Array(wasmMemory.buffer, this.notesBufferPtr, 128);
            this.velsBufferView = new Float32Array(wasmMemory.buffer, this.velsBufferPtr, 128);
            this.dursBufferView = new Float32Array(wasmMemory.buffer, this.dursBufferPtr, 128);

            this.useWasm = true;
            this.port.postMessage({ type: 'ready' });

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
      } else if (!this.useWasm) {
        this.pendingMessages.push(data);
      } else {
        this.handleMessage(data);
      }
    };
  }

  handleMessage(data) {
    switch (data.type) {

        case 'setBpm':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_bpm(this.wasmEnginePtr, data.bpm || 168.0);
          }
          break;

        case 'setPlaying':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_playing(this.wasmEnginePtr, data.playing ? 1 : 0);
          }
          break;

        case 'setTrack':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && data.notes && data.vels) {
            const len = Math.min(128, data.notes.length);
            for (let i = 0; i < len; i++) {
              this.instIdsBufferView[i] = data.instIds ? data.instIds[i] : (data.instId || 4);
              this.notesBufferView[i] = data.notes[i];
              this.velsBufferView[i] = (typeof data.vels[i] === 'number') ? data.vels[i] : 0.9;
              this.dursBufferView[i] = data.durs ? ((typeof data.durs[i] === 'number') ? data.durs[i] : 0.2) : (data.dur || 0.2);
            }
            this.wasmExports.tritoncha_dsp_set_track(
              this.wasmEnginePtr,
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
        case 'stopTrack':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && this.wasmExports.tritoncha_dsp_deactivate_track) {
            this.wasmExports.tritoncha_dsp_deactivate_track(this.wasmEnginePtr, data.trackIdx || 0);
          }
          break;

        case 'clearTracks':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_clear_tracks(this.wasmEnginePtr);
          }
          break;

        case 'muteTrack':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_mute_track(
              this.wasmEnginePtr,
              data.trackIdx || 0,
              data.muted ? 1 : 0
            );
          }
          break;

        case 'soloTrack':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_solo_track(
              this.wasmEnginePtr,
              data.trackIdx || 0,
              data.solo ? 1 : 0
            );
          }
          break;

        case 'setBusParams':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_bus_params(
              this.wasmEnginePtr,
              data.busIdx !== undefined ? data.busIdx : 0,
              data.gainDb !== undefined ? data.gainDb : 0.0,
              data.muted ? 1 : 0,
              data.sendDelay !== undefined ? data.sendDelay : 0.0,
              data.sendReverb !== undefined ? data.sendReverb : 0.0
            );
          }
          break;

        case 'noteOn':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_note_on(
              this.wasmEnginePtr,
              data.instId || 0,
              data.freq || 440.0,
              data.velocity || 0.9,
              data.dur || 0.0
            );
          }
          break;

        case 'setMasterFilter':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_master_filter(
              this.wasmEnginePtr,
              data.cutoffHz !== undefined ? data.cutoffHz : 12000.0,
              data.resonance !== undefined ? data.resonance : 0.0
            );
          }
          break;

        case 'sweepMasterFilter':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && this.wasmExports.tritoncha_dsp_sweep_master_filter) {
            this.wasmExports.tritoncha_dsp_sweep_master_filter(
              this.wasmEnginePtr,
              data.fromHz !== undefined ? data.fromHz : 400.0,
              data.toHz !== undefined ? data.toHz : 12000.0,
              data.durationSecs !== undefined ? data.durationSecs : 4.0
            );
          }
          break;

        case 'setDriveBitcrush':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_drive_bitcrush(
              this.wasmEnginePtr,
              data.drive !== undefined ? data.drive : 0.0,
              data.bitDepth !== undefined ? data.bitDepth : 16.0,
              data.sampleHold !== undefined ? data.sampleHold : 1.0
            );
          }
          break;

        case 'setChorus':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_chorus(
              this.wasmEnginePtr,
              data.rateHz !== undefined ? data.rateHz : 0.8,
              data.depth !== undefined ? data.depth : 0.4,
              data.mix !== undefined ? data.mix : 0.0
            );
          }
          break;

        case 'setSidechain':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_sidechain(
              this.wasmEnginePtr,
              data.amount !== undefined ? data.amount : 0.0
            );
          }
          break;

        case 'setVoicePatch':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_voice_patch(
              this.wasmEnginePtr,
              data.patchId !== undefined ? data.patchId : 4,
              data.oscType !== undefined ? data.oscType : 0,
              data.subLevel !== undefined ? data.subLevel : 0.0,
              data.pulseWidth !== undefined ? data.pulseWidth : 0.5,
              data.filterType !== undefined ? data.filterType : 0,
              data.cutoffBase !== undefined ? data.cutoffBase : 1200.0,
              data.cutoffEnvAmt !== undefined ? data.cutoffEnvAmt : 3000.0,
              data.cutoffKeyTrack !== undefined ? data.cutoffKeyTrack : 2.0,
              data.resonance !== undefined ? data.resonance : 0.4,
              data.attack !== undefined ? data.attack : 0.005,
              data.decay !== undefined ? data.decay : 0.2,
              data.sustain !== undefined ? data.sustain : 0.3,
              data.release !== undefined ? data.release : 0.2,
              data.modAttack !== undefined ? data.modAttack : 0.005,
              data.modDecay !== undefined ? data.modDecay : 0.2,
              data.busId !== undefined ? data.busId : 3,
              data.polyphony !== undefined ? data.polyphony : 8,
              data.glide !== undefined ? data.glide : 0.0,
              data.filterDrive !== undefined ? data.filterDrive : 0.0,
              data.noiseLevel !== undefined ? data.noiseLevel : 0.0,
              data.pitchEnvAmt !== undefined ? data.pitchEnvAmt : 0.0,
              data.pitchEnvDecay !== undefined ? data.pitchEnvDecay : 0.015,
              data.analogDrift !== undefined ? data.analogDrift : 0.0
            );
          }
          break;

        case 'setDrumPatch':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && this.wasmExports.tritoncha_dsp_set_drum_patch) {
            this.wasmExports.tritoncha_dsp_set_drum_patch(
              this.wasmEnginePtr,
              data.drumId !== undefined ? data.drumId : 0,
              data.p0 !== undefined ? data.p0 : 0.0,
              data.p1 !== undefined ? data.p1 : 0.0,
              data.p2 !== undefined ? data.p2 : 0.0,
              data.p3 !== undefined ? data.p3 : 0.0,
              data.p4 !== undefined ? data.p4 : 0.0,
              data.p5 !== undefined ? data.p5 : 0.0,
              data.p6 !== undefined ? data.p6 : 0.0
            );
          }
          break;

        case 'setDrumMode':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && this.wasmExports.tritoncha_dsp_set_drum_mode) {
            this.wasmExports.tritoncha_dsp_set_drum_mode(
              this.wasmEnginePtr,
              data.mode !== undefined ? data.mode : 0.0
            );
          }
          break;

        case 'setDelay':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_delay(
              this.wasmEnginePtr,
              data.timeS !== undefined ? data.timeS : 0.35,
              data.feedback !== undefined ? data.feedback : 0.4,
              data.wet !== undefined ? data.wet : 0.25
            );
          }
          break;

        case 'setReverb':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
            this.wasmExports.tritoncha_dsp_set_reverb(
              this.wasmEnginePtr,
              data.roomSize !== undefined ? data.roomSize : 0.75,
              data.wet !== undefined ? data.wet : 0.2
            );
          }
          break;

        case 'setDriveMode':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && this.wasmExports.tritoncha_dsp_set_drive_mode) {
            this.wasmExports.tritoncha_dsp_set_drive_mode(
              this.wasmEnginePtr,
              data.mode !== undefined ? data.mode : 1
            );
          }
          break;

        case 'setReverbMode':
          if (this.useWasm && this.wasmExports && this.wasmEnginePtr && this.wasmExports.tritoncha_dsp_set_reverb_mode) {
            this.wasmExports.tritoncha_dsp_set_reverb_mode(
              this.wasmEnginePtr,
              data.mode !== undefined ? data.mode : 1
            );
          }
          break;
      }
  }

  process(inputs, outputs, parameters) {
    const output = outputs[0];
    if (!output || output.length === 0) return true;

    const channelLeft = output[0];
    const channelRight = output.length > 1 ? output[1] : null;
    const blockSize = channelLeft.length; // 128 samples

    if (this.useWasm && this.wasmExports && this.wasmEnginePtr) {
      this.wasmExports.tritoncha_dsp_process(
        this.wasmEnginePtr,
        this.outLeftPtr,
        this.outRightPtr,
        blockSize
      );

      channelLeft.set(this.outLeftView);
      if (channelRight) {
        channelRight.set(this.outRightView);
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
