(ns app.audio.dsp.worklet
  "Unified public facade for the WebAudio AudioWorklet processor and Rust WASM DSP."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.instruments.catalog :as catalog]
            [app.audio.dsp.worklet.compiler :as compiler]
            [app.audio.dsp.worklet.protocol :as protocol]
            [app.audio.dsp.worklet.slots :as slots]
            [app.audio.dsp.worklet.transport :as transport]
            [app.audio.theory.harmony :as harmony]
            [app.config :as cfg]
            [app.state :refer [audio-state repl-registry]]
            [app.utils.audio :as audio-utils]))

;; Low-level WebAudio Transport and Lifecycle
(def init-audio-worklet! transport/init-audio-worklet!)
(def init-worklet-engine! transport/init-audio-worklet!)
(def worklet-ready? transport/worklet-ready?)
(def get-audio-context transport/get-audio-context)
(def on-worklet-ready! transport/on-worklet-ready!)
(def on-trigger-event! transport/on-trigger-event!)
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

;; Master Clock and Sequencer Commands
(defn set-bpm!
  "Configures master tempo in Beats Per Minute for the WASM sequencer."
  [^number bpm]
  (transport/send-msg! #js {:type "setBpm" :bpm bpm}))

(defn set-playing!
  "Starts or stops the hardware sample-accurate WASM sequencer clock."
  [playing?]
  (transport/send-msg! #js {:type "setPlaying" :playing (boolean playing?)}))

(defn set-track!
  "Sends a track's hits into the Rust WASM sequencer with note durations and velocities."
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
  "Clears all hardware sequencer tracks and resets slot assignments."
  []
  (slots/clear-track-slots!)
  (transport/send-msg! #js {:type "clearTracks"}))

(defn deactivate-track!
  "Silences and deactivates a specific hardware sequencer slot."
  [track-idx]
  (transport/send-msg! #js {:type "deactivateTrack" :trackIdx (int track-idx)}))

(defn mute-track!
  "Mutes or unmutes a specific hardware sequencer slot."
  [track-idx muted?]
  (transport/send-msg! #js {:type "muteTrack" :trackIdx (int track-idx) :muted (boolean muted?)}))

(defn solo-track!
  "Solos or unsolos a specific hardware sequencer slot."
  [track-idx solo?]
  (transport/send-msg! #js {:type "soloTrack" :trackIdx (int track-idx) :solo (boolean solo?)}))

(defn- chord-progression?
  "Returns true if notes is a sequence of chords (vectors of pitch notes or frequencies)."
  [notes]
  (and (sequential? notes)
       (seq notes)
       (sequential? (first notes))
       (not (keyword? (first (first notes))))))

(defn- clear-voice-slots!
  "Deactivates and unassigns hardware sequencer sub-slots for polyphonic voice indices."
  [track-key voice-indices]
  (doseq [v-idx voice-indices]
    (let [sub-tk (keyword (str (name track-key) "-v" (inc v-idx)))]
      (when-let [sub-slot (get @slots/track-slot-assignments sub-tk)]
        (deactivate-track! sub-slot)
        (swap! slots/track-slot-assignments dissoc sub-tk)))))

(defn set-worklet-voice-patch!
  "Compiles and transmits a declarative synth patch into Rust WASM modular DSP."
  [patch-id patch-spec]
  (transport/send-msg! (compiler/compile-voice-patch-msg patch-id patch-spec)))

(defn resolve-track-inst
  "Resolves the effective instrument key for a track, dynamically creating and compiling
  a derived DSP voice patch when the track specifies a custom audio bus override for a synth."
  [tk pat-data]
  (let [inst-k      (or (:inst pat-data) tk)
        custom-bus  (when-let [b (:bus pat-data)] (busses/normalize-bus-key b))
        default-bus (busses/instrument-bus inst-k)]
    (if (and custom-bus
             (busses/valid-bus? custom-bus)
             (not (catalog/drum? inst-k))
             (not= custom-bus default-bus))
      (let [derived-k (keyword (str (name inst-k) "--" (name custom-bus)))]
        (when-let [base (catalog/find-instrument-spec inst-k)]
          (let [derived-spec (assoc base :bus custom-bus)
                patch-id     (protocol/register-custom-patch-id! derived-k)]
            (swap! repl-registry assoc-in [:instruments derived-k] derived-spec)
            (set-worklet-voice-patch! patch-id derived-spec)))
        derived-k)
      inst-k)))

(defn sync-track-to-worklet!
  "Sends normalized pattern data to the Rust WASM sequencer, supporting velocity and polyphonic chords.
  Resolves scale degrees against the active musical key if not already resolved."
  [tk pat-data]
  (let [inst-k   (resolve-track-inst tk pat-data)
        raw-hits (or (:notes pat-data) [true])
        key-ctx  (get @audio-state :key cfg/default-key)
        track-o  (:oct pat-data)
        hits     (harmony/resolve-track-notes raw-hits
                                              [(:root key-ctx) (:mode key-ctx) (or track-o (:octave key-ctx))]
                                              track-o)
        notes    (if (sequential? hits) hits [hits])
        step-m   (audio-utils/step->mult (:step pat-data))
        bpm      (:bpm @audio-state 168)
        dur-raw  (or (:dur pat-data) (:step pat-data) "16n")
        dur-s    (audio-utils/dur->seconds dur-raw bpm)
        base-vel (or (:vel pat-data) 0.9)]
    (if (chord-progression? notes)
      (let [max-voices   (min 4 (apply max 1 (map #(if (sequential? %) (count %) 1) notes)))
            scale-factor (if (> max-voices 1) (/ 1.0 (js/Math.sqrt max-voices)) 1.0)
            voice-vel    (if (number? base-vel)
                           (* (float base-vel) scale-factor)
                           (mapv #(* % scale-factor) (if (sequential? base-vel) base-vel [0.9])))]
        (doseq [v-idx (range max-voices)]
          (let [sub-tk      (keyword (str (name tk) "-v" (inc v-idx)))
                voice-notes (mapv #(if (sequential? %) (nth % v-idx nil) (when (zero? v-idx) %)) notes)
                slot        (slots/get-or-assign-track-slot! sub-tk)]
            (set-track! slot inst-k voice-notes step-m dur-s voice-vel)))
        (clear-voice-slots! tk (range max-voices 4)))
      (let [slot (slots/get-or-assign-track-slot! tk)]
        (set-track! slot inst-k (vec notes) step-m dur-s base-vel)
        (clear-voice-slots! tk (range 1 4))))))

(def sync-track! sync-track-to-worklet!)

;; Mixer Bus and Patch Routing Commands
(defn set-bus-params!
  "Configures gain, FX send routing, and master FX bypass status for a specific audio bus."
  ([bus-key gain-db muted? send-delay send-reverb]
   (set-bus-params! bus-key gain-db muted? send-delay send-reverb false))
  ([bus-key gain-db muted? send-delay send-reverb bypass-master-fx?]
   (let [idx (protocol/bus-key->id bus-key)]
     (transport/send-msg! #js {:type "setBusParams"
                               :busIdx (int idx)
                               :gainDb (float (or gain-db 0.0))
                               :muted (boolean muted?)
                               :sendDelay (float (or send-delay 0.0))
                               :sendReverb (float (or send-reverb 0.0))
                               :bypassMasterFx (boolean bypass-master-fx?)}))))

(defn set-worklet-master-volume!
  "Adjusts master output volume in decibels (-60.0 dB to +6.0 dB) in Rust WASM."
  [^number gain-db]
  (transport/send-msg! #js {:type "setVolume"
                            :gainDb (float (or gain-db 0.0))}))

(defn set-drum-mode!
  "Configures character synthesis mode across all drum voices in Rust WASM.
  Supported modes: :analog, :natural, :idm, :industrial."
  [mode-kw]
  (let [mode-id (protocol/drum-mod->id mode-kw)]
    (transport/send-msg! #js {:type "setDrumMode"
                              :mode mode-id})))

(defn set-worklet-drum-patch!
  "Transmits drum sound design parameters into the Rust WASM drum synthesis engine."
  [drum-key drum-spec]
  (when-let [msg (compiler/compile-drum-patch-msg drum-key drum-spec)]
    (transport/send-msg! msg)))

(defn trigger-worklet-note!
  "Plays a live note on the Rust WASM modular synth or drum engine."
  ([pitch] (trigger-worklet-note! :bass-analog pitch 0.9 0.0))
  ([inst-key pitch] (trigger-worklet-note! inst-key pitch 0.9 0.0))
  ([inst-key pitch vel] (trigger-worklet-note! inst-key pitch vel 0.0))
  ([inst-key pitch vel dur-s]
   (let [inst-id (protocol/inst-keyword->id inst-key)
         freq    (audio-utils/note->freq pitch)
         v       (or vel 0.9)
         d       (float (or dur-s 0.0))]
     (transport/send-msg! #js {:type "noteOn"
                               :instId (int inst-id)
                               :freq freq
                               :velocity v
                               :dur d}))))

;; DSP Effects and Automations
(defn set-worklet-filter!
  "Sets cutoff frequency and resonance on the TPT state-variable filter."
  [^number cutoff-hz ^number resonance]
  (transport/send-msg! #js {:type "setFilter"
                            :cutoffHz cutoff-hz
                            :resonance resonance}))

(defn sweep-worklet-filter!
  "Initiates an automated filter frequency sweep over a time window."
  [^number from-hz ^number to-hz ^number duration-secs]
  (transport/send-msg! #js {:type "sweepFilter"
                            :fromHz from-hz
                            :toHz to-hz
                            :durationSecs duration-secs}))

(defn set-worklet-drive-bitcrush!
  "Adjusts master bus saturation drive, bit depth reduction, and sample-rate decimation."
  [drive bit-depth sample-hold]
  (transport/send-msg! #js {:type "setDriveBitcrush"
                            :drive (float (or drive 0.0))
                            :bitDepth (float (or bit-depth 16.0))
                            :sampleHold (float (or sample-hold 1.0))}))

(defn set-worklet-chorus!
  "Configures stereo chorus modulation rate, depth, and mix level."
  [rate-hz depth mix]
  (transport/send-msg! #js {:type "setChorus"
                            :rateHz (float (or rate-hz 0.8))
                            :depth (float (or depth 0.4))
                            :mix (float (or mix 0.0))}))

(defn set-worklet-sidechain!
  "Sets drum kick sidechain ducking compression depth."
  [amount]
  (transport/send-msg! #js {:type "setSidechain"
                            :amount (float (or amount 0.0))}))

(defn set-worklet-delay!
  "Configures master ping-pong stereo delay line parameters."
  [^number time-s ^number feedback ^number wet]
  (transport/send-msg! #js {:type "setDelay"
                            :timeS time-s
                            :feedback feedback
                            :wet wet}))

(defn set-worklet-reverb!
  "Configures master algorithmic warehouse reverb space and wet mix."
  [^number room-size ^number wet]
  (transport/send-msg! #js {:type "setReverb"
                            :roomSize room-size
                            :wet wet}))

(defn set-worklet-drive-mode!
  "Configures saturation algorithm mode (0 = Classic Pade, 1 = ADAA-1 Antialiased)."
  [mode-kw]
  (let [norm (if (= (keyword mode-kw) :classic) :classic :adaa)
        mode-id (if (= norm :classic) 0 1)]
    (transport/send-msg! #js {:type "setDriveMode" :mode mode-id})
    norm))

(defn set-worklet-reverb-mode!
  "Configures reverb algorithm mode (0 = Freeverb, 1 = 8-Channel Householder FDN)."
  [mode-kw]
  (let [norm (if (= (keyword mode-kw) :freeverb) :freeverb :fdn)
        mode-id (if (= norm :freeverb) 0 1)]
    (transport/send-msg! #js {:type "setReverbMode" :mode mode-id})
    norm))

(defn set-worklet-compressor!
  "Configures stereo bus glue compressor parameters."
  ([enabled?]
   (set-worklet-compressor! enabled? -12.0 4.0 0.010 0.100 2.5 1.0))
  ([enabled? threshold-db ratio attack-s release-s makeup-db mix]
   (transport/send-msg! #js {:type "setCompressor"
                             :enabled (boolean enabled?)
                             :thresholdDb (float (or threshold-db -12.0))
                             :ratio (float (or ratio 4.0))
                             :attackS (float (or attack-s 0.010))
                             :releaseS (float (or release-s 0.100))
                             :makeupDb (float (or makeup-db 2.5))
                             :mix (float (or mix 1.0))})))

(defn set-worklet-bus-chain!
  "Configures the modular insert effects chain and output routing for an audio bus in the Rust WASM engine."
  [bus-idx target-out? inserts]
  (let [inserts-js (clj->js (or inserts []))]
    (transport/send-msg! #js {:type "setBusChain"
                              :busIdx (int (or bus-idx 0))
                              :targetOut (boolean target-out?)
                              :inserts inserts-js})))

(defn update-worklet-bus-processor!
  "Updates parameters for a specific processor type on a bus (or globally if bus-idx is -1)."
  [bus-idx processor params]
  (let [msg (clj->js (merge {:type "updateBusProcessor"
                             :busIdx (int (or bus-idx -1))
                             :processor (name processor)}
                            params))]
    (transport/send-msg! msg)))

