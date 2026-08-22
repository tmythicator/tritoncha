(ns app.audio.voices
  "Voice lifecycle, node factory, bus routing and audio trigger dispatcher."
  (:require ["tone" :as tone]
            [app.utils :refer [enforce-stereo-mode!]]
            [app.state :refer [tone-ctx pulse! repl-instruments]]
            [app.lib.instruments :refer [core-instruments drum-voices]]
            [app.custom.instruments :refer [user-instruments]]))

;; Drum Keys

(defn all-drum-keys
  "Returns a set of all valid drum voice keywords."
  []
  (set (keys drum-voices)))

;; Instrument Registry (Built-in + Custom User + REPL Live Instruments)

(defn register-instrument!
  "Registers or updates a dynamic user instrument preset in the REPL registry.
  Examples: (register-instrument! :supersaw {:type :mono :bus :space :options {...}})."
  [inst-key spec]
  (swap! repl-instruments assoc inst-key spec)
  inst-key)

(def definst! register-instrument!)

(defn all-instruments
  "Returns a merged map of core built-in instruments, user custom instruments, and REPL instruments.
  Examples: (all-instruments)."
  []
  (merge core-instruments user-instruments @repl-instruments))

(def instruments all-instruments)

(def ^:private instrument-aliases
  {:bass :saw-bass
   :sub  :sub-sine
   :pad  :dark-pad})

(defn resolve-instrument-spec
  "Resolves an instrument keyword or map, expanding canonical aliases (:bass, :sub, :pad).
  Examples: (resolve-instrument-spec :bass) -> {:type :mono ...}."
  [spec]
  (cond
    (map? spec) spec
    (keyword? spec) (let [canonical (get instrument-aliases spec spec)]
                      (get (all-instruments) canonical spec))
    :else spec))

;; Node Factory + Routing

(defn create-instrument
  "Instantiates a Tone.js audio node from an instrument preset map or preset keyword.
  Examples: (create-instrument :acid-bass), (create-instrument :bass)."
  [spec]
  (let [inst-map (resolve-instrument-spec spec)
        {:keys [type options maxPolyphony]} inst-map
        opts-js (clj->js options)
        node (case type
               :mono     (tone/MonoSynth. opts-js)
               :synth    (tone/Synth. opts-js)
               :poly     (tone/PolySynth. tone/Synth opts-js #js {:maxPolyphony (or maxPolyphony 6)})
               :fm       (tone/FMSynth. opts-js)
               :membrane (tone/MembraneSynth. opts-js)
               :noise    (tone/NoiseSynth. opts-js)
               (tone/Synth. opts-js))]
    (enforce-stereo-mode! node)
    node))

(defn- synth-bus-target [inst-key busses]
  (let [spec (resolve-instrument-spec (keyword inst-key))
        bus-type (or (:bus spec) :master)]
    (case bus-type
      :drums  (:drum-bus busses)
      :bass   (:bass-bus busses)
      :space  (:space-bus busses)
      :direct (:direct-bus busses)
      (or (:master-filter busses) (:filter busses)))))

(defn route-instrument!
  "Connects an instrument node to its designated audio mixer bus."
  [inst-key ^js inst busses]
  (when (and inst busses)
    (when-let [^js target (synth-bus-target inst-key busses)]
      (.connect inst target)))
  inst)

(defn get-or-create-instrument!
  "Retrieves an active instrument node from context, or lazily instantiates and routes it."
  [inst-key ctx]
  (let [kw (keyword inst-key)]
    (if-let [inst (get ctx kw)]
      inst
      (when-let [spec (resolve-instrument-spec kw)]
        (let [^js inst (create-instrument spec)]
          (route-instrument! kw inst ctx)
          (swap! tone-ctx assoc kw inst)
          inst)))))

(defn reload-instruments!
  "Rebuilds and re-routes all instrument synth nodes from updated definitions in all-instruments."
  []
  (when-let [ctx @tone-ctx]
    (doseq [[k _spec] (all-instruments)]
      (when-let [^js old-node (get ctx k)]
        (try (.dispose old-node) (catch js/Object _)))
      (let [new-node (create-instrument k)]
        (route-instrument! k new-node ctx)
        (swap! tone-ctx assoc k new-node))))
  :ok)

(def refresh-instruments! reload-instruments!)

;; Voice Trigger Dispatcher

(defn- trigger-synth!
  "Plays single notes or polyphonic chords on a Tone.js synth node."
  [^js inst note dur time vel]
  (when (and inst note)
    (let [t (or time (tone/now))
          d (or dur "16n")
          v (or vel 0.85)]
      (try
        (if (sequential? note)
          (if (instance? tone/PolySynth inst)
            (.triggerAttackRelease inst (clj->js note) d t v)
            (doseq [n note]
              (.triggerAttackRelease inst n d t v)))
          (.triggerAttackRelease inst note d t v))
        (catch js/Object _)))))

(defn- trigger-drum-hit!
  [^js node note dur time vel]
  (when node
    (let [t (or time (tone/now))
          d (or dur "16n")
          v (or vel 1.0)]
      (try
        (if (and note (not (instance? tone/NoiseSynth node)))
          (.triggerAttackRelease node note d t v)
          (.triggerAttackRelease node d t v))
        (catch js/Object _)))))

(defn- trigger-drum!
  "Triggers layered drum voices from the declarative drum-voices map."
  [drum-key note dur time vel ctx]
  (when-let [spec (get drum-voices (keyword drum-key))]
    (let [t         (or time (tone/now))
          base-vel  (or vel 1.0)
          pulse-val (:pulse spec 1.0)]
      (if-let [layers (:layers spec)]
        (doseq [layer layers]
          (let [layer-node (get ctx (:node layer))
                layer-note (if (instance? tone/NoiseSynth layer-node)
                             nil
                             (or (:default-note layer) note))
                layer-dur  (or (:dur layer) dur)
                layer-vel  (* base-vel (:vel-scale layer 1.0))]
            (trigger-drum-hit! layer-node layer-note layer-dur t layer-vel)))
        (let [node (get ctx (:node spec))
              n    (if (instance? tone/NoiseSynth node)
                     nil
                     (or note (:default-note spec)))
              d    (or (:dur spec) dur)
              v    (* base-vel (:vel-scale spec 1.0))]
          (trigger-drum-hit! node n d t v)))
      (pulse! (* base-vel pulse-val)))))

(defn play-instrument!
  "Triggers an instrument voice with pitch, duration, scheduled WebAudio time, and velocity."
  [inst-key note dur time vel ctx]
  (try
    (let [kw (keyword inst-key)
          t  (or time (tone/now))]
      (if (contains? (all-drum-keys) kw)
        (trigger-drum! kw note dur t vel ctx)
        (when-let [^js inst (get-or-create-instrument! kw ctx)]
          (trigger-synth! inst note dur t vel)
          (pulse! (* (or vel 1.0) 0.9)))))
    (catch js/Object e
      (js/console.warn "Playback error on instrument" inst-key e))))
