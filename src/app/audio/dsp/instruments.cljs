(ns app.audio.dsp.instruments
  "Instrument lifecycle, node factory, bus routing, and audio trigger dispatcher."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.worklet :as worklet]
            [app.audio.dsp.worklet.protocol :as protocol]
            [app.custom.drums :refer [user-drums]]
            [app.custom.synth :refer [user-synths]]
            [app.lib.drums :refer [core-drums]]
            [app.lib.synth :refer [core-synths]]
            [app.state :refer [audio-state pulse! repl-registry]]
            [app.utils.audio :as audio-utils]))

(defn register-instrument!
  "Registers or updates a dynamic user instrument preset in the REPL registry.
  Examples: (register-instrument! :supersaw {:type :mono :bus :bus/space :osc {:type :supersaw}})."
  [inst-key spec]
  (swap! repl-registry assoc-in [:instruments inst-key] spec)
  inst-key)

(defn all-drums
  "Returns a merged map of core built-in drums, user custom drums, and REPL drums.
  Examples: (all-drums)."
  []
  (merge core-drums user-drums (:instruments @repl-registry)))

(defn all-synths
  "Returns a merged map of core built-in synthesizers, user custom synths, and REPL synths.
  Examples: (all-synths)."
  []
  (merge core-synths user-synths (:instruments @repl-registry)))

(defn all-instruments
  "Returns a merged map of core built-in instruments, user custom instruments, and REPL instruments.
  Examples: (all-instruments)."
  []
  (merge core-synths user-synths core-drums user-drums (:instruments @repl-registry)))

(def instrument-aliases
  {;; Generic shortcuts
   :bass         :bass-analog
   :sub          :sub-pure
   :pad          :pad-cinema
   :lead         :lead-pluck
   :strings      :pad-strings
   :acid         :bass-303
   :tb303        :bass-303
   :reese        :liquid-reese
   :slap         :bass-slap
   :neuro        :bass-neuro
   :808          :sub-808
   :choir        :pad-vocal
   :glass        :pad-glass
   :drone        :pad-drone
   :pluck        :lead-pluck
   :supersaw     :lead-supersaw
   :fm           :lead-fm
   :blade        :lead-blade
   :cs80         :lead-blade
   :hoover       :lead-hoover
   :chiptune     :lead-8bit
   :8bit         :lead-8bit
   :karplus      :lead-string
   :bell         :lead-bell
   :laser        :fx-laser
   :siren        :fx-siren
   :util-click   :click})

(defn find-instrument-spec
  "Looks up an instrument specification map across REPL, custom, and core catalogs.
  Examples: (find-instrument-spec :bass) -> {:type :mono ...}."
  [spec]
  (busses/find-instrument-spec spec))

(def resolve-instrument-spec
  "Resolves an instrument keyword or map, expanding canonical aliases (:bass, :sub, :pad).
  Examples: (resolve-instrument-spec :bass) -> {:type :mono ...}."
  find-instrument-spec)

(defn sync-instrument-dsp!
  "Transmits instrument DSP configuration to Rust WASM engine without touching REPL registry.
  Examples: (sync-instrument-dsp! :kick spec)."
  [inst-name spec]
  (let [ik (keyword inst-name)]
    (if (or (busses/drum? ik) (busses/drum? spec) (= (:category spec) :drums))
      (let [dtype (or (:type spec) :kick)
            did   (get protocol/drum-type->id dtype 0)]
        (protocol/register-custom-drum-id! ik did)
        (worklet/set-worklet-drum-patch! ik spec))
      (let [patch-id (worklet/register-custom-patch-id! ik)]
        (worklet/set-worklet-voice-patch! patch-id spec)))
    ik))

(defn definst!
  "Declares an instrument preset (synth voice or drum model) and syncs it with the Rust WASM audio engine.
  Examples: (definst! :fat-kick {:category :drums :type :kick :base-pitch 42}),
            (definst! :fat-saw {:osc {:type :saw}})."
  [inst-name spec]
  (let [ik (keyword inst-name)]
    (register-instrument! ik spec)
    (sync-instrument-dsp! ik spec)))

(defn patch!
  "Tweaks a parameter on an existing synthesizer or drum live in REPL.
  Examples: (patch! :bass :cutoff 2400), (patch! :kick :base-pitch 48)."
  ([inst-name param-key val]
   (patch! inst-name {param-key val}))
  ([inst-name spec-map]
   (let [ik        (keyword inst-name)
         canonical (get instrument-aliases ik ik)
         old-spec  (resolve-instrument-spec ik)
         new-spec  (merge old-spec (if (map? spec-map) spec-map {}))]
     (definst! ik new-spec)
     (when (not= ik canonical)
       (definst! canonical new-spec))
     new-spec)))

(defn reset-instrument!
  "Resets an instrument's parameters back to its original baseline catalog definition.
  Examples: (reset-instrument! :ethereal-pad), (reset-instrument! :kick)."
  [synth-name]
  (let [sk        (keyword synth-name)
        canonical (get instrument-aliases sk sk)
        orig-spec (or (get core-synths canonical)
                      (get core-drums canonical)
                      (get user-synths canonical)
                      (get user-drums canonical))]
    (when orig-spec
      (swap! repl-registry update :instruments dissoc sk canonical)
      (sync-instrument-dsp! canonical orig-spec)
      orig-spec)))

(defn reload-instruments!
  "Recompiles and syncs all instruments in the active audio engine context."
  []
  (swap! repl-registry update :instruments
         (fn [insts]
           (into {}
                 (remove (fn [[k spec]]
                           (let [canonical (get instrument-aliases k k)]
                             (= spec (or (get core-synths k)
                                         (get core-drums k)
                                         (get user-synths k)
                                         (get user-drums k)
                                         (get core-synths canonical)
                                         (get core-drums canonical)
                                         (get user-synths canonical)
                                         (get user-drums canonical)))))
                         insts))))
  (doseq [[inst-key spec] (all-instruments)]
    (sync-instrument-dsp! inst-key spec))
  :reloaded)

(defn trigger-note!
  "Triggers a note or chord on an instrument with velocity, duration, and visual pulse."
  ([inst-key note-val] (trigger-note! inst-key note-val "16n" 0.9))
  ([inst-key note-val dur] (trigger-note! inst-key note-val dur 0.9))
  ([inst-key note-val dur vel]
   (let [kw    (keyword inst-key)
         v     (or vel 0.9)
         bpm   (:bpm @audio-state 168)
         dur-s (audio-utils/dur->seconds (or dur "16n") bpm)]
     (if (vector? note-val)
       (let [chord-notes (filterv some? note-val)
             n-count     (count chord-notes)
             scale       (if (> n-count 1) (/ 1.0 (js/Math.sqrt n-count)) 1.0)
             chord-v     (* v scale)]
         (doseq [n chord-notes]
           (worklet/trigger-worklet-note! kw n chord-v dur-s)))
       (when note-val
         (worklet/trigger-worklet-note! kw note-val v dur-s)))
     (pulse! kw (* 1.8 v))))
  ([_synth-node note-val dur _time vel inst-key]
   (trigger-note! inst-key note-val dur vel)))

(defn trigger-drum!
  "Triggers a drum voice (:kick, :snare, :sn-rs, :hh-c, :hh-o, etc.)."
  ([drum-key] (trigger-drum! drum-key 0.9))
  ([drum-key vel] (trigger-note! drum-key "C3" "16n" vel))
  ([drum-key _pitch _dur _time vel] (trigger-drum! drum-key vel)))
