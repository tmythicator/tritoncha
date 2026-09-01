(ns app.audio.dsp.instruments
  "Instrument lifecycle, node factory, bus routing, and audio trigger dispatcher."
  (:require ["tone" :as tone]
            [app.audio.dsp.busses :as busses]
            [app.config :as cfg]
            [app.custom.instruments :refer [user-instruments]]
            [app.lib.drums :refer [core-drum-instruments core-drum-voices]]
            [app.lib.instruments :refer [core-instruments]]
            [app.state :refer [engine-ctx pulse! repl-registry]]))

(defn all-drum-keys
  "Returns a set of all valid drum voice keywords."
  []
  (set (keys core-drum-voices)))

(defn register-instrument!
  "Registers or updates a dynamic user instrument preset in the REPL registry.
  Examples: (register-instrument! :supersaw {:type :mono :bus :bus/space :options {...}})."
  [inst-key spec]
  (swap! repl-registry assoc-in [:instruments inst-key] spec)
  inst-key)

(defn all-instruments
  "Returns a merged map of core built-in instruments, user custom instruments, and REPL instruments.
  Examples: (all-instruments)."
  []
  (merge core-instruments core-drum-instruments user-instruments (:instruments @repl-registry)))

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

(defn create-instrument
  "Instantiates a Tone.js audio node from an instrument preset map or preset keyword.
  Examples: (create-instrument :acid-bass), (create-instrument :bass)."
  [spec]
  (when-let [spec-map (resolve-instrument-spec spec)]
    (let [{:keys [type options maxPolyphony]} spec-map
          js-opts (if (map? options) (clj->js options) (or options #js {}))]
      (case type
        :synth     (tone/Synth. js-opts)
        :mono      (tone/MonoSynth. js-opts)
        :fm        (tone/FMSynth. js-opts)
        :am        (tone/AMSynth. js-opts)
        :membrane  (tone/MembraneSynth. js-opts)
        :noise     (tone/NoiseSynth. js-opts)
        :poly      (let [ps (tone/PolySynth. tone/Synth #js {:maxPolyphony (or maxPolyphony cfg/default-max-polyphony)})]
                     (when options (.set ps js-opts))
                     ps)
        (tone/Synth. js-opts)))))

(defn- connect-to-bus!
  "Connects a synth audio node output directly to its target bus volume node or master destination."
  [^js synth bus-key busses]
  (when synth
    (let [norm-bus (busses/normalize-bus-key bus-key)
          bus-node (or (get busses norm-bus)
                       (get busses :bus/direct)
                       (tone/getDestination))]
      (when bus-node
        (.connect synth bus-node)))))

(defn create-default-instruments!
  "Initializes all default core, user and drum instruments, connecting them to their respective bus channels."
  [busses]
  (let [inst-map (all-instruments)
        synths   (reduce-kv (fn [acc k spec]
                              (let [synth (create-instrument spec)]
                                (connect-to-bus! synth (:bus spec :bus/direct) busses)
                                (assoc acc k synth)))
                            {}
                            inst-map)
        aliases  {:bass (get synths :saw-bass)
                  :sub  (get synths :sub-sine)
                  :pad  (get synths :dark-pad)}]
    (merge synths aliases)))

(defn reload-instruments!
  "Recompiles and replaces all instruments in the active audio engine context."
  []
  (when-let [{:keys [busses]} (:tone @engine-ctx)]
    (let [new-instruments (create-default-instruments! busses)]
      (swap! engine-ctx update :tone merge new-instruments)
      :reloaded)))

(def ^:private inst-pulses
  {:kick 2.6 :snare 1.8 :hh-o 1.2 :bass 1.4 :saw-bass 1.4 :sub 1.5 :sub-sine 1.5 :pad 1.1})

(defn- trigger-synth-voice!
  [^js node note dur time vel]
  (when (and node (some? (.-triggerAttackRelease node)))
    (try
      (if (exists? (.-noise node))
        (.triggerAttackRelease ^js node (or dur cfg/default-step) time (or vel 0.9))
        (when note
          (.triggerAttackRelease ^js node note (or dur cfg/default-step) time (or vel 0.9))))
      (catch js/Object _))))

(defn trigger-drum!
  "Triggers a composite drum voice (:kick, :snare, :sn-rs, :hh-c, :hh-o, etc.) with layered synthesis."
  [drum-key pitch dur time vel]
  (when-let [voice-spec (get core-drum-voices (keyword drum-key))]
    (let [tone-nodes (:tone @engine-ctx)
          v          (or vel 0.9)
          pulse-val  (:pulse voice-spec 1.0)
          layers     (or (:layers voice-spec) [voice-spec])]
      (try
        (dotimes [i (count layers)]
          (let [layer       (nth layers i)
                target-node (get tone-nodes (:node layer))
                target-note (or pitch (:default-note layer))
                target-dur  (or dur (:dur layer) (:dur voice-spec) cfg/default-step)
                target-vel  (* v (or (:vel-scale layer) 1.0))]
            (trigger-synth-voice! target-node target-note target-dur time target-vel)))
        (pulse! pulse-val)
        (catch js/Object e
          (println "Drum trigger error for" drum-key ":" e))))))

(defn trigger-note!
  "Triggers a note or chord on an instrument node with velocity, duration, and visual pulse."
  [^js synth-node note-val dur time vel inst-key]
  (when (and synth-node note-val (some? (.-triggerAttackRelease synth-node)))
    (let [v (or vel 0.9)
          d (or dur cfg/default-step)]
      (try
        (if (vector? note-val)
          (if (exists? (.-maxPolyphony synth-node))
            (.triggerAttackRelease ^js synth-node (to-array note-val) d time v)
            (dotimes [i (count note-val)]
              (when-let [n (nth note-val i)]
                (.triggerAttackRelease ^js synth-node n d time v))))
          (.triggerAttackRelease ^js synth-node (if (keyword? note-val) (name note-val) (str note-val)) d time v))
        (pulse! (get inst-pulses (keyword inst-key) 0.8))
        (catch js/Object e
          (println "Note trigger error for" inst-key note-val ":" e))))))
