(ns app.audio.dsp.worklet
  "Bridge to the native WebAudio AudioWorklet processor driving Rust WASM DSP."
  (:require [app.lib.drums :refer [drum-keyword? drum-keywords mini-notation-aliases]]
            [app.utils.audio :refer [midi->freq note->midi]]
            [clojure.string :as str]))

(defonce ^:private worklet-state
  (atom {:ctx nil :node nil :wasm-module nil :ready? false}))

(defonce track-slot-assignments
  (atom {}))

(def ^:private canonical-inst-ids
  {;; Analog Drum Voices (fixed DSP algorithms in Rust)
   :kick          0 :bd 0
   :snare         1 :sn-rs 1 :sn-roll 1
   :hh-c          2 :hh 2 :hh-clk 2 :hat-closed 2 :hat 2
   :hh-o          3 :hat-open 3
   :util-click    11 :click 11
   :clap          18
   :ride          20
   :tom           21
   :sn-crack      22
   :snare-wire    23 :sn-wire 23
   :snare-body    24 :sn-body 24
   :snare-ghost   25 :sn-gh 25
   :snare-rim     26 :sn-clk 26
   :ride-bell     64 :rb 64
   :tom-high      65 :th 65 :tom-h 65
   :tom-mid       66 :tm 66 :tom-m 66
   :tom-low       67 :tl 67 :tom-l 67
   :crash-16      68 :cr16 68 :crash 68 :cr 68
   :crash-17      69 :cr17 69
   :crash-18      70 :cr18 70
   :splash        71 :sp 71
   :china         72 :ch 72
   :cowbell       73 :cb 73

   ;; Melodic & Harmonic Synthesizer Patches (Rust modular voice slots 4..41)
   :bass-analog   4  :bass 4
   :bass-303      5  :acid 5
   :sub-pure      6  :sub 6
   :pad-cinema    7  :pad 7
   :lead-pluck    8  :lead 8 :pluck 8 :arp 8
   :lead-fm       9  :fm 9
   :bass-reese    10 :reese 10
   :lead-supersaw 12 :supersaw 12
   :lead-blade    13
   :lead-hoover   14 :hoover 14
   :lead-string   15 :karplus 15
   :lead-organ    16 :organ 16
   :lead-8bit     17 :8bit 17
   :pad-glass     19 :glass 19
   :pad-shimmer   27 :shimmer 27
   :pad-strings   29 :strings 29
   :pad-vocal     30 :choir 30
   :pad-drone     31 :drone 31
   :bass-neuro    33 :neuro 33
   :sub-808       34 :808 34
   :bass-slap     35 :slap 35
   :bass-organ    36
   :lead-bell     38 :bell 38
   :fx-siren      40 :siren 40
   :fx-laser      41 :laser 41})

(def ^:private drum-id-set
  (into #{} (keep canonical-inst-ids drum-keywords)))

(defonce custom-synth-patch-ids
  (atom {}))

(defn register-custom-patch-id!
  "Resolves or allocates a patch ID for an instrument.
  Built-in synths preserve their canonical patch ID.
  Truly custom user synths are allocated from the safe range 42..63."
  [synth-key]
  (let [sk (keyword synth-key)]
    (if-let [cid (get canonical-inst-ids sk)]
      ;; If it's a known built-in instrument and not a drum, use its canonical ID!
      (if (not (contains? drum-id-set cid))
        (do
          (swap! custom-synth-patch-ids assoc sk cid)
          cid)
        cid)
      (if-let [id (get @custom-synth-patch-ids sk)]
        id
        ;; Allocate dynamic patch in the safe custom synth voice range (42..63)
        (let [used (set (vals @custom-synth-patch-ids))
              free (first (filter #(not (contains? used %)) (range 42 64)))]
          (swap! custom-synth-patch-ids assoc sk (or free 42))
          (or free 42))))))

(defn track-slot [track-key]
  (let [tk (keyword track-key)]
    (or (get @track-slot-assignments tk)
        (when (drum-keyword? tk)
          (get @track-slot-assignments :drums))
        (when (contains? #{:bass-analog :sub-pure :acid :bass-303 :moog :moog-bass :bass-reese :bass :sub} tk)
          (or (get @track-slot-assignments :bass)
              (get @track-slot-assignments :sub)))
        (when (contains? #{:pad :pad-cinema :pad-glass :glass :juno :pad-shimmer} tk)
          (or (get @track-slot-assignments :strings)
              (get @track-slot-assignments :pad)))
        (when (contains? #{:click :metronome} tk)
          (get @track-slot-assignments :click)))))

(defn all-track-slots []
  @track-slot-assignments)

(defn track-slots-for
  "Returns all sequencer slot indices assigned to a track keyword, including chord voice sub-slots.
  Examples: (track-slots-for :pad) -> [4 5 6 7]."
  [track-key]
  (let [tk   (keyword track-key)
        pfx  (str (name tk) "-v")
        base (track-slot tk)]
    (distinct
     (keep identity
           (concat (when base [base])
                   (keep (fn [[k s]]
                           (when (and (keyword? k) (str/starts-with? (name k) pfx))
                             s))
                         @track-slot-assignments))))))

(defn get-or-assign-track-slot! [track-key]
  (let [tk (keyword track-key)]
    (if-let [slot (get @track-slot-assignments tk)]
      slot
      (let [used (set (vals @track-slot-assignments))
            free (first (filter #(not (contains? used %)) (range 16)))]
        (swap! track-slot-assignments assoc tk (or free 0))
        (or free 0)))))

(defn worklet-ready? []
  (:ready? @worklet-state))

(defn get-audio-context []
  (:ctx @worklet-state))

(defn parse-freq [pitch]
  (cond
    (number? pitch) (if (> pitch 127) pitch (midi->freq pitch))
    (string? pitch) (if-let [m (note->midi pitch)] (midi->freq m) 440.0)
    (keyword? pitch) (if-let [m (note->midi (name pitch))] (midi->freq m) 440.0)
    :else 440.0))

(defn parse-midi-note [pitch]
  (cond
    (nil? pitch) -1
    (number? pitch) (int pitch)
    (string? pitch) (if-let [m (note->midi pitch)] (int m) -1)
    (keyword? pitch) (if-let [m (note->midi (name pitch))] (int m) -1)
    :else -1))

(defn inst-keyword->id [inst-key]
  (let [k (keyword inst-key)]
    (or (get canonical-inst-ids k)
        (get @custom-synth-patch-ids k)
        (register-custom-patch-id! k))))

(defn bus-key->id [bus-key]
  (case (keyword bus-key)
    (:bus/drums :drums) 0
    (:bus/bass :bass) 1
    (:bus/space :space) 2
    (:bus/lead :lead) 3
    (:bus/direct :direct :click) 4
    4))

(defn osc-type->id [osc]
  (case (keyword osc)
    (:saw :sawtooth) 0
    (:pulse :square) 1
    (:tri :triangle) 2
    (:sine) 3
    (:supersaw :prophet) 4
    (:karplus :pluck) 5
    (:organ :drawbar) 6
    (:chiptune :8bit :nes) 7
    (:fm :fm-synth) 8
    (:reese) 9
    (:blade :cs80) 10
    (:hoover :mentasm) 11
    (:click) 12
    0))

(defn filter-type->id [ft]
  (case (keyword ft)
    (:lp :lowpass) 0
    (:hp :highpass) 1
    (:bp :bandpass) 2
    (:notch) 3
    0))

(defn- extract-articulation
  "Splits a string or keyword into [clean-token vel].
  Supports '!' suffix for accents (vel: 1.15 base) and '_' suffix for ghost notes (vel: 0.35 base)."
  ([token] (extract-articulation token 0.9))
  ([token default-vel]
   (let [s        (if (keyword? token) (name token) (str token))
         base-vel (float (or default-vel 0.9))]
     (cond
       (str/ends-with? s "!")
       [(subs s 0 (dec (count s))) (min 1.25 (* base-vel (/ 1.15 0.9)))]

       (and (> (count s) 1) (str/ends-with? s "_"))
       [(subs s 0 (dec (count s))) (* base-vel (/ 0.35 0.9))]

       :else
       [s base-vel]))))

(defn parse-step-hit
  "Parses a step hit into {:inst-id :note :vel} map respecting default-vel.
  Examples: (parse-step-hit \"C3\" :bass 0.4) -> {:inst-id 2, :note 48, :vel 0.4}."
  ([hit default-inst-key] (parse-step-hit hit default-inst-key 0.9))
  ([hit default-inst-key default-vel]
   (let [def-v (float (or default-vel 0.9))]
     (cond
       (nil? hit)
       {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}

       (boolean? hit)
       (if hit
         {:inst-id (inst-keyword->id default-inst-key) :note 60 :vel def-v}
         {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0})

       (and (vector? hit) (keyword? (first hit)))
       (let [[k v n]           hit
             [clean-k art-vel] (extract-articulation k def-v)]
         {:inst-id (inst-keyword->id (keyword clean-k))
          :note    (if n (parse-midi-note n) 60)
          :vel     (float (or v art-vel def-v))})

       (vector? hit)
       {:inst-id (inst-keyword->id default-inst-key)
        :note    (if (seq hit) (parse-midi-note (first hit)) -1)
        :vel     (if (seq hit) def-v 0.0)}

       (keyword? hit)
       (let [[clean-name art-vel] (extract-articulation hit def-v)
             resolved-alias       (get mini-notation-aliases clean-name)
             clean-kw             (or resolved-alias (keyword clean-name))]
         (cond
           (contains? #{:_ :- :rest :nil :none} clean-kw)
           {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}

           (drum-keyword? clean-kw)
           {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)}

           :else
           (if-let [m (note->midi clean-name)]
             {:inst-id (inst-keyword->id default-inst-key) :note (int m) :vel (float art-vel)}
             {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)})))

       (number? hit)
       (if (neg? hit)
         {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}
         {:inst-id (inst-keyword->id default-inst-key) :note (int hit) :vel def-v})

       (string? hit)
       (let [[clean-name art-vel] (extract-articulation hit def-v)
             resolved-alias       (get mini-notation-aliases clean-name)
             clean-kw             (or resolved-alias (keyword clean-name))]
         (cond
           (contains? #{:_ :- :rest :nil :none "." "0"} clean-name)
           {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}

           (drum-keyword? clean-kw)
           {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)}

           :else
           (let [m (parse-midi-note clean-name)]
             (if (neg? m)
               {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)}
               {:inst-id (inst-keyword->id default-inst-key) :note m :vel (float art-vel)}))))

       :else
       {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}))))

(defonce ^:private pending-messages
  (atom []))

(defonce ^:private ready-callbacks
  (atom []))

(defn on-worklet-ready!
  "Registers a callback to execute when the AudioWorklet WASM engine reports ready."
  [cb]
  (if (:ready? @worklet-state)
    (cb)
    (swap! ready-callbacks conj cb)))

(defn- send-msg! [msg-js]
  (if-let [^js node (:node @worklet-state)]
    (.postMessage (.-port node) msg-js)
    (swap! pending-messages conj msg-js)))

(defn- flush-pending-messages!
  "Flushes any messages queued before the AudioWorklet node was attached."
  [^js node]
  (let [queued @pending-messages]
    (reset! pending-messages [])
    (doseq [msg queued]
      (.postMessage (.-port node) msg))))

(defn- notify-worklet-ready!
  "Marks worklet state as ready and invokes registered ready callbacks."
  []
  (swap! worklet-state assoc :ready? true)
  (let [cbs @ready-callbacks]
    (reset! ready-callbacks [])
    (doseq [cb cbs]
      (try (cb) (catch js/Object _)))))

(defn- fetch-wasm-bytes
  "Fetches the compiled Tritoncha DSP WebAssembly binary as an ArrayBuffer."
  [wasm-url]
  (-> (js/fetch wasm-url)
      (.then (fn [^js resp] (.arrayBuffer resp)))))

(defn- attach-worklet-node!
  "Constructs and connects the AudioWorkletNode, wiring its port messages."
  [^js audio-ctx ^js wasm-buffer]
  (let [node (js/AudioWorkletNode. audio-ctx "tritoncha-dsp-processor"
                                   #js {:numberOfInputs 0
                                        :numberOfOutputs 1
                                        :outputChannelCount #js [2]
                                        :processorOptions #js {:sampleRate (.-sampleRate audio-ctx)}})]
    (.connect node (.-destination audio-ctx))
    (set! (.-onmessage (.-port node))
          (fn [^js ev]
            (when (= (.-type (.-data ev)) "ready")
              (notify-worklet-ready!))))
    (.postMessage (.-port node) #js {:type "initWasm" :wasmBinary wasm-buffer :wasmBytes wasm-buffer})
    (swap! worklet-state assoc :node node)
    (flush-pending-messages! node)
    true))

(defn- create-audio-context
  "Instantiates and stores the native WebAudio context, resuming immediately if suspended."
  []
  (let [AudioCtx (or (.-AudioContext js/window) (.-webkitAudioContext js/window))
        ctx      (or (:ctx @worklet-state) (AudioCtx.))]
    (swap! worklet-state assoc :ctx ctx)
    (when (= (.-state ctx) "suspended")
      (.resume ctx))
    ctx))

(defn init-audio-worklet!
  "Asynchronously loads WASM binary and attaches Tritoncha AudioWorklet processor."
  ([]
   (if (exists? js/window)
     (init-audio-worklet! (create-audio-context))
     (js/Promise.resolve false)))
  ([^js audio-ctx]
   (cond
     (nil? audio-ctx)
     (js/Promise.resolve false)

     (:node @worklet-state)
     (js/Promise.resolve (boolean (:ready? @worklet-state)))

     :else
     (do
       (swap! worklet-state assoc :ctx audio-ctx)
       (when (= (.-state audio-ctx) "suspended")
         (.resume audio-ctx))
       (-> (.addModule (.-audioWorklet audio-ctx) "/worklets/tritoncha_dsp.js")
           (.then #(fetch-wasm-bytes "/wasm/tritoncha_dsp.wasm"))
           (.then #(attach-worklet-node! audio-ctx %))
           (.catch (fn [err]
                     (println "AudioWorklet init notice:" (.-message err))
                     false)))))))

(def init-worklet-engine! init-audio-worklet!)

(defn set-bus-params! [bus-key gain-db muted? send-delay send-reverb]
  (let [idx (bus-key->id bus-key)]
    (send-msg! #js {:type "setBusParams"
                    :busIdx (int idx)
                    :gainDb (float (or gain-db 0.0))
                    :muted (boolean muted?)
                    :sendDelay (float (or send-delay 0.0))
                    :sendReverb (float (or send-reverb 0.0))})))

(defn set-bpm! [^number bpm]
  (send-msg! #js {:type "setBpm" :bpm bpm}))

(defn set-playing! [playing?]
  (send-msg! #js {:type "setPlaying" :playing (boolean playing?)}))

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
                                 (parse-step-hit hit default-inst-key (vel-fn i)))
                               hits-vec)
         inst-ids (mapv :inst-id parsed)
         notes    (mapv :note parsed)
         vels     (mapv :vel parsed)
         durs     (if (sequential? dur-s)
                    (mapv float dur-s)
                    (vec (repeat (count parsed) (float (or dur-s 0.2)))))]
     (send-msg! #js {:type "setTrack"
                     :trackIdx (int track-idx)
                     :instIds (to-array inst-ids)
                     :notes (to-array notes)
                     :vels (to-array vels)
                     :durs (to-array durs)
                     :dur (float (if (number? dur-s) dur-s 0.2))
                     :stepMult (or step-mult 1)}))))

(defn clear-tracks! []
  (reset! track-slot-assignments {})
  (send-msg! #js {:type "clearTracks"}))

(defn deactivate-track! [track-idx]
  (send-msg! #js {:type "deactivateTrack" :trackIdx (int track-idx)}))

(defn mute-track! [track-idx muted?]
  (send-msg! #js {:type "muteTrack" :trackIdx (int track-idx) :muted (boolean muted?)}))

(defn solo-track! [track-idx solo?]
  (send-msg! #js {:type "soloTrack" :trackIdx (int track-idx) :solo (boolean solo?)}))

(defn set-worklet-voice-patch!
  "Compiles and transmits a declarative synth patch into Rust WASM modular DSP."
  [patch-id patch-spec]
  (let [pid       (int patch-id)
        osc       (:osc patch-spec)
        flt       (:filter patch-spec)
        amp       (:amp-env patch-spec)
        mod-e     (:mod-env patch-spec)
        pitch-e   (:pitch-env patch-spec)
        bus-id    (int (bus-key->id (or (:bus patch-spec)
                                        (case (:category patch-spec)
                                          :bass :bus/bass
                                          :pads :bus/space
                                          :drums :bus/drums
                                          :bus/lead))))
        poly      (int (if (= (:type patch-spec) :mono) 1 8))]
    (send-msg! #js {:type           "setVoicePatch"
                    :patchId        pid
                    :oscType        (osc-type->id (:type osc :saw))
                    :subLevel       (float (:sub-level osc 0.0))
                    :pulseWidth     (float (:pulse-width osc 0.5))
                    :filterType     (filter-type->id (:type flt :lowpass))
                    :cutoffBase     (float (:cutoff flt 1200.0))
                    :cutoffEnvAmt   (float (:env-amount flt 3000.0))
                    :cutoffKeyTrack (float (:key-track flt 2.0))
                    :resonance      (float (:q flt 0.4))
                    :attack         (float (:attack amp 0.005))
                    :decay          (float (:decay amp 0.2))
                    :sustain        (float (:sustain amp 0.3))
                    :release        (float (:release amp 0.2))
                    :modAttack      (float (:attack mod-e (:attack amp 0.005)))
                    :modDecay       (float (:decay mod-e (:decay amp 0.2)))
                    :busId          bus-id
                    :polyphony      poly
                    :glide          (float (:glide patch-spec 0.0))
                    :filterDrive    (float (:drive flt 0.0))
                    :noiseLevel     (float (:noise osc 0.0))
                    :pitchEnvAmt    (float (:amount pitch-e 0.0))
                    :pitchEnvDecay  (float (:decay pitch-e 0.015))
                    :analogDrift    (float (:drift osc 0.0))})))

(defn drum-mod->id
  "Maps symbolic drum character mode keyword to numeric float ID for Rust WASM DSP.
  Supported modes: :analog (0.0), :natural (1.0), :idm (2.0), :industrial (3.0)."
  [m]
  (case (keyword (or m :analog))
    (:analog :classic :808 :909) 0.0
    (:natural :acoustic :organic :wood) 1.0
    (:idm :glitch :laser :chirp) 2.0
    (:industrial :distort :hard :crush) 3.0
    0.0))

(defn set-drum-mode!
  "Configures the character synthesis mode across all drum voices in Rust WASM.
  Supported modes: :analog, :natural, :idm, :industrial.
  Examples: (set-drum-mode! :idm), (set-drum-mode! :natural)."
  [mode-kw]
  (let [mode-id (drum-mod->id mode-kw)]
    (send-msg! #js {:type "setDrumMode"
                    :mode mode-id})))

(defn set-worklet-drum-patch!
  "Transmits drum sound design parameters into the Rust WASM drum synthesis engine.
  Examples: (set-worklet-drum-patch! :kick {:base-pitch 48 :pitch-drop 180 :decay 0.28 :click 0.35 :drive 1.6 :mod :idm})."
  [drum-key drum-spec]
  (let [dk      (keyword drum-key)
        type    (or (:type drum-spec) dk)
        inst-id (inst-keyword->id dk)
        mod-id  (drum-mod->id (:mod drum-spec))]
    (cond
      (or (= type :kick) (contains? #{:kick :drum-kick :808-kick :909-kick} dk))
      (let [base-pitch   (float (or (:base-pitch drum-spec) (:pitch drum-spec) 48.0))
            pitch-drop   (float (or (:pitch-drop drum-spec) (:snap drum-spec) 180.0))
            pitch-decay  (float (or (:pitch-decay drum-spec) (:sweep drum-spec) 0.040))
            decay-s      (float (or (:decay drum-spec) 0.28))
            click-level  (float (or (:click drum-spec) 0.35))
            drive        (float (or (:drive drum-spec) 1.6))]
        (send-msg! #js {:type   "setDrumPatch"
                        :drumId 0
                        :p0     base-pitch
                        :p1     pitch-drop
                        :p2     pitch-decay
                        :p3     decay-s
                        :p4     click-level
                        :p5     drive
                        :p6     mod-id}))

      (or (= type :snare) (contains? #{:snare :snare-crack :snare-body :snare-wire :snare-ghost :snare-rim} dk))
      (let [base-freq   (float (or (:base-freq drum-spec) (:pitch drum-spec) 185.0))
            tone-decay  (float (or (:tone-decay drum-spec) 0.9985))
            noise-decay (float (or (:noise-decay drum-spec) 0.9991))
            cutoff-hz   (float (or (:cutoff drum-spec) 2400.0))
            snappy      (float (or (:snappy drum-spec) (:noise drum-spec) 0.85))]
        (send-msg! #js {:type   "setDrumPatch"
                        :drumId 1
                        :p0     base-freq
                        :p1     tone-decay
                        :p2     noise-decay
                        :p3     cutoff-hz
                        :p4     snappy
                        :p5     0.0
                        :p6     mod-id}))

      (or (= type :hat) (contains? #{:hat :hat-closed :hat-open :hh-closed :hh-open} dk))
      (let [cutoff-hz   (float (or (:cutoff drum-spec) 7200.0))
            decay-close (float (or (:decay-closed drum-spec) (:decay drum-spec) 0.04))
            decay-open  (float (or (:decay-open drum-spec) 0.24))]
        (send-msg! #js {:type   "setDrumPatch"
                        :drumId 2
                        :p0     cutoff-hz
                        :p1     decay-close
                        :p2     decay-open
                        :p3     0.0
                        :p4     0.0
                        :p5     0.0
                        :p6     mod-id}))

      (or (= type :membrane) (contains? #{:tom :tom-high :tom-mid :tom-low :th :tm :tl} dk))
      (let [start-p (float (or (:start-pitch drum-spec) 180.0))
            min-p   (float (or (:min-pitch drum-spec) 105.0))
            p-decay (float (or (:pitch-decay drum-spec) 0.015))
            decay-s (float (or (:decay drum-spec) 0.40))
            drive   (float (or (:drive drum-spec) 1.10))]
        (send-msg! #js {:type   "setDrumPatch"
                        :drumId inst-id
                        :p0     start-p
                        :p1     min-p
                        :p2     p-decay
                        :p3     decay-s
                        :p4     drive
                        :p5     0.0
                        :p6     mod-id}))

      (or (= type :metallic) (contains? #{:ride :ride-bell :crash-16 :crash-17 :crash-18 :splash :china :cowbell :cr :cb :rb} dk))
      (let [cutoff-hz (float (or (:cutoff drum-spec) 3500.0))
            res       (float (or (:resonance drum-spec) 0.35))
            decay-s   (float (or (:decay drum-spec) 0.85))
            drive     (float (or (:drive drum-spec) 1.0))]
        (send-msg! #js {:type   "setDrumPatch"
                        :drumId inst-id
                        :p0     cutoff-hz
                        :p1     res
                        :p2     decay-s
                        :p3     drive
                        :p4     0.0
                        :p5     0.0
                        :p6     mod-id}))

      (or (= type :clap) (= dk :clap))
      (let [cutoff-hz (float (or (:cutoff drum-spec) 1200.0))
            res       (float (or (:resonance drum-spec) 0.70))
            decay-s   (float (or (:decay drum-spec) 0.28))
            drive     (float (or (:drive drum-spec) 1.0))]
        (send-msg! #js {:type   "setDrumPatch"
                        :drumId 18
                        :p0     cutoff-hz
                        :p1     res
                        :p2     decay-s
                        :p3     drive
                        :p4     0.0
                        :p5     0.0
                        :p6     mod-id}))

      :else
      nil)))

(defn trigger-worklet-note!
  "Plays a live note on the Rust WASM modular synth or drum engine."
  ([pitch] (trigger-worklet-note! :bass-analog pitch 0.9 0.0))
  ([inst-key pitch] (trigger-worklet-note! inst-key pitch 0.9 0.0))
  ([inst-key pitch vel] (trigger-worklet-note! inst-key pitch vel 0.0))
  ([inst-key pitch vel dur-s]
   (let [inst-id (inst-keyword->id inst-key)
         freq    (parse-freq pitch)
         v       (or vel 0.9)
         d       (float (or dur-s 0.0))]
     (send-msg! #js {:type "noteOn"
                     :instId (int inst-id)
                     :freq freq
                     :velocity v
                     :dur d}))))

(defn set-worklet-master-filter! [^number cutoff-hz ^number resonance]
  (send-msg! #js {:type "setMasterFilter"
                  :cutoffHz cutoff-hz
                  :resonance resonance}))

(defn sweep-worklet-master-filter! [^number from-hz ^number to-hz ^number duration-secs]
  (send-msg! #js {:type "sweepMasterFilter"
                  :fromHz from-hz
                  :toHz to-hz
                  :durationSecs duration-secs}))

(defn set-worklet-drive-bitcrush! [drive bit-depth sample-hold]
  (send-msg! #js {:type "setDriveBitcrush"
                  :drive (float (or drive 0.0))
                  :bitDepth (float (or bit-depth 16.0))
                  :sampleHold (float (or sample-hold 1.0))}))

(defn set-worklet-chorus! [rate-hz depth mix]
  (send-msg! #js {:type "setChorus"
                  :rateHz (float (or rate-hz 0.8))
                  :depth (float (or depth 0.4))
                  :mix (float (or mix 0.0))}))

(defn set-worklet-sidechain! [amount]
  (send-msg! #js {:type "setSidechain"
                  :amount (float (or amount 0.0))}))

(defn set-worklet-delay! [^number time-s ^number feedback ^number wet]
  (send-msg! #js {:type "setDelay"
                  :timeS time-s
                  :feedback feedback
                  :wet wet}))

(defn set-worklet-reverb! [^number room-size ^number wet]
  (send-msg! #js {:type "setReverb"
                  :roomSize room-size
                  :wet wet}))
