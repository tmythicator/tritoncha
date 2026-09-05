(ns app.audio.dsp.instruments
  "Instrument lifecycle, node factory, bus routing, and audio trigger dispatcher."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.worklet :as worklet]
            [app.custom.instruments :refer [user-instruments]]
            [app.lib.drums :refer [core-drum-instruments core-drum-voices]]
            [app.lib.instruments :refer [core-instruments]]
            [app.state :refer [audio-state pulse! repl-registry]]
            [app.utils.audio :as audio-utils]))

(defn all-drum-keys
  "Returns a set of all valid drum voice keywords."
  []
  (set (keys core-drum-voices)))

(defn register-instrument!
  "Registers or updates a dynamic user instrument preset in the REPL registry.
  Examples: (register-instrument! :supersaw {:type :mono :bus :bus/space :osc {:type :supersaw}})."
  [inst-key spec]
  (swap! repl-registry assoc-in [:instruments inst-key] spec)
  inst-key)

(defn all-instruments
  "Returns a merged map of core built-in instruments, user custom instruments, and REPL instruments.
  Examples: (all-instruments)."
  []
  (merge core-instruments core-drum-instruments user-instruments (:instruments @repl-registry)))

(def instrument-aliases
  {;; Generic shortcuts
   :bass         :bass-analog
   :sub          :sub-pure
   :pad          :pad-cinema
   :lead         :lead-pluck
   :strings      :pad-strings
   :acid         :bass-303
   :tb303        :bass-303
   :reese        :bass-reese
   :slap         :bass-slap
   :neuro        :bass-neuro
   :808          :sub-808
   :shimmer      :pad-shimmer
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

(defn resolve-instrument-spec
  "Resolves an instrument keyword or map, expanding canonical aliases (:bass, :sub, :pad).
  Examples: (resolve-instrument-spec :bass) -> {:type :mono ...}."
  [spec]
  (cond
    (map? spec) spec
    (keyword? spec)
    (let [canonical  (get instrument-aliases spec spec)
          repl-insts (:instruments @repl-registry)
          all        (all-instruments)]
      (or (get repl-insts spec)
          (get repl-insts canonical)
          (get all canonical)
          (get all spec)))
    :else spec))

(defn defsynth!
  "Declares a synthesizer preset in ClojureScript and immediately syncs it with the Rust WASM modular voice engine.
  Examples: (defsynth! :fat-saw {:osc {:type :saw :sub-level 0.4} :filter {:cutoff 1800 :q 0.75} :amp-env {:attack 0.01 :decay 0.2}})."
  [synth-name spec]
  (let [sk       (keyword synth-name)
        patch-id (worklet/register-custom-patch-id! sk)]
    (register-instrument! sk spec)
    (worklet/set-worklet-voice-patch! patch-id spec)
    sk))

(defn patch!
  "Tweaks a parameter on an existing synthesizer sound design live in REPL.
  Examples: (patch! :bass :cutoff 2400), (patch! :lead :q 0.85)."
  ([synth-name param-key val]
   (let [sk        (keyword synth-name)
         canonical (get instrument-aliases sk sk)
         old-spec  (resolve-instrument-spec sk)
         new-spec  (assoc old-spec param-key val)]
     (defsynth! sk new-spec)
     (when (not= sk canonical)
       (defsynth! canonical new-spec))
     new-spec))
  ([synth-name spec-map]
   (let [sk        (keyword synth-name)
         canonical (get instrument-aliases sk sk)
         old-spec  (resolve-instrument-spec sk)
         new-spec  (merge old-spec spec-map)]
     (defsynth! sk new-spec)
     (when (not= sk canonical)
       (defsynth! canonical new-spec))
     new-spec)))

(defn reset-instrument!
  "Resets an instrument's parameters back to its original baseline catalog definition.
  Examples: (reset-instrument! :ethereal-pad), (reset-instrument! :saw-bass)."
  [synth-name]
  (let [sk        (keyword synth-name)
        canonical (get instrument-aliases sk sk)
        orig-spec (or (get core-instruments canonical)
                      (get core-drum-instruments canonical)
                      (get user-instruments canonical))]
    (when orig-spec
      ;; Dissoc user overrides from the REPL registry
      (swap! repl-registry update :instruments dissoc sk canonical)
      ;; Re-sync WASM voice patch with the original definition
      (let [pid (worklet/inst-keyword->id canonical)]
        (when (and (number? pid) (not (busses/drum? orig-spec)))
          (worklet/set-worklet-voice-patch! pid orig-spec)))
      orig-spec)))

(defn reload-instruments!
  "Recompiles and replaces all instruments in the active audio engine context."
  []
  (doseq [[inst-key spec] (all-instruments)]
    (let [pid (worklet/inst-keyword->id inst-key)]
      (when (and (map? spec)
                 (not (busses/drum? spec)))
        (worklet/set-worklet-voice-patch! pid spec))))
  :reloaded)

(def ^:private inst-pulses
  {:kick 2.6 :snare 1.8 :hh-o 1.2 :bass 1.4 :bass-analog 1.4 :sub 1.5 :sub-pure 1.5 :pad 1.1 :pad-cinema 1.1 :worklet 1.4 :worklet-synth 1.4})

(defn trigger-drum!
  "Triggers an analog drum voice (:kick, :snare, :sn-rs, :hh-c, :hh-o, etc.)."
  ([drum-key] (trigger-drum! drum-key 0.9))
  ([drum-key vel]
   (let [v (or vel 0.9)]
     (worklet/trigger-worklet-note! drum-key "C3" v)
     (pulse! (get inst-pulses (keyword drum-key) 1.5))))
  ([drum-key _pitch _dur _time vel]
   (trigger-drum! drum-key vel)))

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
     (pulse! (get inst-pulses kw 1.0))))
  ([_synth-node note-val dur _time vel inst-key]
   (trigger-note! inst-key note-val dur vel)))
