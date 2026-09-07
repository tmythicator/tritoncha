(ns app.audio.dsp.worklet
  "Unified public facade for the WebAudio AudioWorklet processor and Rust WASM DSP."
  (:require [app.audio.dsp.worklet.compiler :as compiler]
            [app.audio.dsp.worklet.protocol :as protocol]
            [app.audio.dsp.worklet.slots :as slots]
            [app.audio.dsp.worklet.transport :as transport]))

;; Low-level WebAudio Transport and Lifecycle
(def init-audio-worklet! transport/init-audio-worklet!)
(def init-worklet-engine! transport/init-audio-worklet!)
(def worklet-ready? transport/worklet-ready?)
(def get-audio-context transport/get-audio-context)
(def on-worklet-ready! transport/on-worklet-ready!)
(def send-msg! transport/send-msg!)

;; Hardware Sequencer Slot Management
(def track-slot-assignments slots/track-slot-assignments)
(def track-slot slots/track-slot)
(def all-track-slots slots/all-track-slots)
(def track-slots-for slots/track-slots-for)
(def get-or-assign-track-slot! slots/get-or-assign-track-slot!)

;; DSP Protocol, Enums, and Numeric ID Encoders
(def canonical-inst-ids protocol/canonical-inst-ids)
(def custom-synth-patch-ids protocol/custom-synth-patch-ids)
(def register-custom-patch-id! protocol/register-custom-patch-id!)
(def inst-keyword->id protocol/inst-keyword->id)
(def bus-key->id protocol/bus-key->id)
(def osc-type->id protocol/osc-type->id)
(def filter-type->id protocol/filter-type->id)
(def drum-mod->id protocol/drum-mod->id)

;; Score and Pattern Compilation
(def parse-freq compiler/parse-freq)
(def parse-midi-note compiler/parse-midi-note)
(def parse-step-hit compiler/parse-step-hit)

;; Master Clock and Sequencer Commands
(defn set-bpm!
  "Configures master tempo in Beats Per Minute for the WASM sequencer.
  Examples: (set-bpm! 174)."
  [^number bpm]
  (transport/send-msg! #js {:type "setBpm" :bpm bpm}))

(defn set-playing!
  "Starts or stops the hardware sample-accurate WASM sequencer clock.
  Examples: (set-playing! true), (set-playing! false)."
  [playing?]
  (transport/send-msg! #js {:type "setPlaying" :playing (boolean playing?)}))

(defn set-track!
  "Sends a track's hits into the Rust WASM sequencer with note durations and velocities.
  Examples: (set-track! 0 :kick [true nil true nil] 1 0.2 0.9)."
  ([track-idx default-inst-key hits-vec step-mult]
   (set-track! track-idx default-inst-key hits-vec step-mult 0.2 0.9))
  ([track-idx default-inst-key hits-vec step-mult dur-s]
   (set-track! track-idx default-inst-key hits-vec step-mult dur-s 0.9))
  ([track-idx default-inst-key hits-vec step-mult dur-s vel-spec]
   (let [vel-fn   (cond
                    (sequential? vel-spec)
                    (let [v-cycle (cycle vel-spec)]
                      (fn [idx] (nth v-cycle idx 0.9)))
                    (number? vel-spec)
                    (constantly (float vel-spec))
                    :else
                    (constantly 0.9))
         parsed   (map-indexed (fn [i hit]
                                 (compiler/parse-step-hit hit default-inst-key (vel-fn i)))
                               hits-vec)
         inst-ids (mapv :inst-id parsed)
         notes    (mapv :note parsed)
         vels     (mapv :vel parsed)
         durs     (if (sequential? dur-s)
                    (mapv float dur-s)
                    (vec (repeat (count parsed) (float (or dur-s 0.2)))))]
     (transport/send-msg! #js {:type "setTrack"
                               :trackIdx (int track-idx)
                               :instIds (to-array inst-ids)
                               :notes (to-array notes)
                               :vels (to-array vels)
                               :durs (to-array durs)
                               :dur (float (if (number? dur-s) dur-s 0.2))
                               :stepMult (or step-mult 1)}))))

(defn clear-tracks!
  "Clears all hardware sequencer tracks and resets slot assignments.
  Examples: (clear-tracks!)."
  []
  (slots/clear-track-slots!)
  (transport/send-msg! #js {:type "clearTracks"}))

(defn deactivate-track!
  "Silences and deactivates a specific hardware sequencer slot.
  Examples: (deactivate-track! 3)."
  [track-idx]
  (transport/send-msg! #js {:type "deactivateTrack" :trackIdx (int track-idx)}))

(defn mute-track!
  "Mutes or unmutes a specific hardware sequencer slot.
  Examples: (mute-track! 0 true), (mute-track! 0 false)."
  [track-idx muted?]
  (transport/send-msg! #js {:type "muteTrack" :trackIdx (int track-idx) :muted (boolean muted?)}))

(defn solo-track!
  "Solos or unsolos a specific hardware sequencer slot.
  Examples: (solo-track! 1 true)."
  [track-idx solo?]
  (transport/send-msg! #js {:type "soloTrack" :trackIdx (int track-idx) :solo (boolean solo?)}))

;; Mixer Bus and Patch Routing Commands
(defn set-bus-params!
  "Configures gain and FX send routing for a specific audio bus.
  Examples: (set-bus-params! :bus/drums -3.0 false 0.05 0.10)."
  [bus-key gain-db muted? send-delay send-reverb]
  (let [idx (protocol/bus-key->id bus-key)]
    (transport/send-msg! #js {:type "setBusParams"
                              :busIdx (int idx)
                              :gainDb (float (or gain-db 0.0))
                              :muted (boolean muted?)
                              :sendDelay (float (or send-delay 0.0))
                              :sendReverb (float (or send-reverb 0.0))})))

(defn set-worklet-voice-patch!
  "Compiles and transmits a declarative synth patch into Rust WASM modular DSP.
  Examples: (set-worklet-voice-patch! 4 {:osc {:type :saw} :filter {:cutoff 2000}})."
  [patch-id patch-spec]
  (transport/send-msg! (compiler/compile-voice-patch-msg patch-id patch-spec)))

(defn set-drum-mode!
  "Configures character synthesis mode across all drum voices in Rust WASM.
  Supported modes: :analog, :natural, :idm, :industrial.
  Examples: (set-drum-mode! :idm), (set-drum-mode! :natural)."
  [mode-kw]
  (let [mode-id (protocol/drum-mod->id mode-kw)]
    (transport/send-msg! #js {:type "setDrumMode"
                              :mode mode-id})))

(defn set-worklet-drum-patch!
  "Transmits drum sound design parameters into the Rust WASM drum synthesis engine.
  Examples: (set-worklet-drum-patch! :kick {:base-pitch 48 :decay 0.28 :click 0.35 :drive 1.6 :mod :idm})."
  [drum-key drum-spec]
  (when-let [msg (compiler/compile-drum-patch-msg drum-key drum-spec)]
    (transport/send-msg! msg)))

(defn trigger-worklet-note!
  "Plays a live note on the Rust WASM modular synth or drum engine.
  Examples: (trigger-worklet-note! :bass-analog \"C2\" 0.9 0.4)."
  ([pitch] (trigger-worklet-note! :bass-analog pitch 0.9 0.0))
  ([inst-key pitch] (trigger-worklet-note! inst-key pitch 0.9 0.0))
  ([inst-key pitch vel] (trigger-worklet-note! inst-key pitch vel 0.0))
  ([inst-key pitch vel dur-s]
   (let [inst-id (protocol/inst-keyword->id inst-key)
         freq    (compiler/parse-freq pitch)
         v       (or vel 0.9)
         d       (float (or dur-s 0.0))]
     (transport/send-msg! #js {:type "noteOn"
                               :instId (int inst-id)
                               :freq freq
                               :velocity v
                               :dur d}))))

;; Master DSP Effects and Automations
(defn set-worklet-master-filter!
  "Sets cutoff frequency and resonance on the master TPT state-variable filter.
  Examples: (set-worklet-master-filter! 4200.0 0.70)."
  [^number cutoff-hz ^number resonance]
  (transport/send-msg! #js {:type "setMasterFilter"
                            :cutoffHz cutoff-hz
                            :resonance resonance}))

(defn sweep-worklet-master-filter!
  "Initiates an automated master filter frequency sweep over a time window.
  Examples: (sweep-worklet-master-filter! 400.0 6000.0 4.0)."
  [^number from-hz ^number to-hz ^number duration-secs]
  (transport/send-msg! #js {:type "sweepMasterFilter"
                            :fromHz from-hz
                            :toHz to-hz
                            :durationSecs duration-secs}))

(defn set-worklet-drive-bitcrush!
  "Adjusts master bus saturation drive, bit depth reduction, and sample-rate decimation.
  Examples: (set-worklet-drive-bitcrush! 1.2 12.0 2.0)."
  [drive bit-depth sample-hold]
  (transport/send-msg! #js {:type "setDriveBitcrush"
                            :drive (float (or drive 0.0))
                            :bitDepth (float (or bit-depth 16.0))
                            :sampleHold (float (or sample-hold 1.0))}))

(defn set-worklet-chorus!
  "Configures stereo chorus modulation rate, depth, and mix level.
  Examples: (set-worklet-chorus! 0.8 0.4 0.25)."
  [rate-hz depth mix]
  (transport/send-msg! #js {:type "setChorus"
                            :rateHz (float (or rate-hz 0.8))
                            :depth (float (or depth 0.4))
                            :mix (float (or mix 0.0))}))

(defn set-worklet-sidechain!
  "Sets drum kick sidechain ducking compression depth.
  Examples: (set-worklet-sidechain! 0.85)."
  [amount]
  (transport/send-msg! #js {:type "setSidechain"
                            :amount (float (or amount 0.0))}))

(defn set-worklet-delay!
  "Configures master ping-pong stereo delay line parameters.
  Examples: (set-worklet-delay! 0.35 0.60 0.40)."
  [^number time-s ^number feedback ^number wet]
  (transport/send-msg! #js {:type "setDelay"
                            :timeS time-s
                            :feedback feedback
                            :wet wet}))

(defn set-worklet-reverb!
  "Configures master algorithmic warehouse reverb space and wet mix.
  Examples: (set-worklet-reverb! 0.85 0.30)."
  [^number room-size ^number wet]
  (transport/send-msg! #js {:type "setReverb"
                            :roomSize room-size
                            :wet wet}))

(defn set-worklet-drive-mode!
  "Configures saturation algorithm mode (0 = Classic Pade, 1 = ADAA-1 Antialiased).
  Examples: (set-worklet-drive-mode! :adaa), (set-worklet-drive-mode! :classic)."
  [mode-kw]
  (let [norm (if (= (keyword mode-kw) :classic) :classic :adaa)
        mode-id (if (= norm :classic) 0 1)]
    (transport/send-msg! #js {:type "setDriveMode" :mode mode-id})
    norm))

(defn set-worklet-reverb-mode!
  "Configures reverb algorithm mode (0 = Freeverb, 1 = 8-Channel Householder FDN).
  Examples: (set-worklet-reverb-mode! :fdn), (set-worklet-reverb-mode! :freeverb)."
  [mode-kw]
  (let [norm (if (= (keyword mode-kw) :freeverb) :freeverb :fdn)
        mode-id (if (= norm :freeverb) 0 1)]
    (transport/send-msg! #js {:type "setReverbMode" :mode mode-id})
    norm))
