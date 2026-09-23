(ns app.audio.dsp.worklet.compiler
  "Pure compilers and parsers for score notation, articulation, and DSP patches."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.instruments.catalog :refer [drum-keyword? find-instrument-spec resolve-target-inst]]
            [app.audio.dsp.worklet.protocol :refer [bus-key->id drum-mod->id drum-remaps filter-type->id
                                                    inst-keyword->id osc-type->id]]
            [app.audio.theory.patterns :refer [extract-articulation]]
            [app.utils.audio :refer [parse-midi-note]]))

(def ^:private rest-tokens #{:_ :- :rest :nil :none "." "0"})

(defn- step-rest [def-id]
  {:inst-id def-id :note -1 :vel 0.0})

(defn- step-hit
  ([def-id note vel]
   {:inst-id def-id :note (int note) :vel (float vel)})
  ([inst-key def-inst-key note vel]
   (let [target (resolve-target-inst inst-key def-inst-key)
         id     (inst-keyword->id target (find-instrument-spec target))]
     {:inst-id id :note (int note) :vel (float vel)})))

(defn parse-step-hit
  "Parses a step hit into {:inst-id :note :vel} map respecting default-vel.
  Examples: (parse-step-hit \"C3\" :bass 0.4) -> {:inst-id 4, :note 48, :vel 0.4}."
  ([hit default-inst-key] (parse-step-hit hit default-inst-key 0.9))
  ([hit default-inst-key default-vel]
   (let [def-v  (float (or default-vel 0.9))
         def-id (inst-keyword->id default-inst-key (find-instrument-spec default-inst-key))]
     (cond
       (or (nil? hit) (false? hit))
       (step-rest def-id)

       (true? hit)
       (step-hit def-id 60 def-v)

       (number? hit)
       (if (neg? hit) (step-rest def-id) (step-hit def-id hit def-v))

       (and (vector? hit) (keyword? (first hit)))
       (let [[k v n]           hit
             [clean-k art-vel] (extract-articulation k def-v)]
         (step-hit (keyword clean-k) default-inst-key (if n (parse-midi-note n) 60) (or v art-vel def-v)))

       (vector? hit)
       (if (seq hit)
         (step-hit def-id (parse-midi-note (first hit)) def-v)
         (step-rest def-id))

       (or (keyword? hit) (string? hit))
       (let [raw-str         (if (keyword? hit) (name hit) (str hit))
             [clean art-vel] (extract-articulation raw-str def-v)
             resolved-alias  (get drum-remaps clean)
             clean-kw        (or resolved-alias (keyword clean))]
         (cond
           (contains? rest-tokens clean-kw)
           (step-rest def-id)

           (or resolved-alias (drum-keyword? clean-kw))
           (step-hit clean-kw default-inst-key 60 art-vel)

           :else
           (let [m (parse-midi-note clean)]
             (if (neg? m)
               (step-hit clean-kw default-inst-key 60 art-vel)
               (step-hit def-id m art-vel)))))

       :else
       (step-rest def-id)))))

(def synth-patch-spec
  "Schema and baseline defaults for the Rust modular synthesizer voice patch."
  {:osc       {:type :saw :sub-level 0.0 :pulse-width 0.5 :noise 0.0 :drift 0.0}
   :filter    {:type :lowpass :cutoff 1200.0 :env-amount 3000.0 :key-track 2.0 :q 0.4 :drive 0.0}
   :amp-env   {:attack 0.005 :decay 0.2 :sustain 0.3 :release 0.2}
   :mod-env   {:attack 0.005 :decay 0.2}
   :pitch-env {:amount 0.0 :decay 0.015}
   :glide     0.0
   :polyphony 8})

(defn compile-voice-patch-msg
  "Compiles a declarative Clojure synth patch map into a WebAudio postMessage payload."
  [patch-id patch-spec]
  (let [spec    (merge synth-patch-spec patch-spec)
        osc     (merge (:osc synth-patch-spec) (:osc spec))
        flt     (merge (:filter synth-patch-spec) (:filter spec))
        amp     (merge (:amp-env synth-patch-spec) (:amp-env spec))
        mod-e   (merge (:mod-env synth-patch-spec) {:attack (:attack amp) :decay (:decay amp)} (:mod-env spec))
        pitch-e (merge (:pitch-env synth-patch-spec) (:pitch-env spec))
        bus-key (or (:bus spec)
                    (busses/instrument-bus spec)
                    (busses/category-default-bus (:category spec))
                    :bus/lead)]
    #js {:type           "setVoicePatch"
         :patchId        (int patch-id)
         :oscType        (osc-type->id (:type osc))
         :subLevel       (float (:sub-level osc))
         :pulseWidth     (float (:pulse-width osc))
         :filterType     (filter-type->id (:type flt))
         :cutoffBase     (float (:cutoff flt))
         :cutoffEnvAmt   (float (:env-amount flt))
         :cutoffKeyTrack (float (:key-track flt))
         :resonance      (float (:q flt))
         :attack         (float (:attack amp))
         :decay          (float (:decay amp))
         :sustain        (float (:sustain amp))
         :release        (float (:release amp))
         :modAttack      (float (:attack mod-e))
         :modDecay       (float (:decay mod-e))
         :busId          (int (bus-key->id bus-key))
         :polyphony      (int (if (= (:type spec) :mono) 1 (:polyphony spec 8)))
         :glide          (float (:glide spec))
         :filterDrive    (float (:drive flt))
         :noiseLevel     (float (:noise osc))
         :pitchEnvAmt    (float (:amount pitch-e))
         :pitchEnvDecay  (float (:decay pitch-e))
         :analogDrift    (float (:drift osc))}))

(def drum-type-specs
  "Parameter contracts for the Rust drum synthesis engine."
  {:kick     {:id 0  :fields [:base-pitch :pitch-drop :pitch-decay :decay :click :drive]}
   :snare    {:id 1  :fields [:base-freq :tone-decay :noise-decay :cutoff :snappy]}
   :hat      {:id 2  :fields [:cutoff :decay-closed :decay-open]}
   :membrane {:id nil :fields [:start-pitch :min-pitch :pitch-decay :decay :drive]}
   :metallic {:id nil :fields [:cutoff :resonance :decay :drive]}
   :clap     {:id 18 :fields [:cutoff :resonance :decay :drive]}})

(defn compile-drum-patch-msg
  "Compiles a drum sound design map into a setDrumPatch message payload for Rust WASM."
  [drum-key drum-spec]
  (let [dk   (keyword drum-key)
        base (find-instrument-spec dk)
        spec (if drum-spec (merge base drum-spec) base)
        type (or (:type spec) dk)]
    (when-let [{:keys [id fields]} (get drum-type-specs type)]
      (let [drum-id (or id (inst-keyword->id dk))
            params  (mapv #(float (get spec % 0.0)) fields)
            p       (into params (repeat (- 6 (count params)) 0.0))]
        #js {:type   "setDrumPatch"
             :drumId drum-id
             :p0     (nth p 0)
             :p1     (nth p 1)
             :p2     (nth p 2)
             :p3     (nth p 3)
             :p4     (nth p 4)
             :p5     (nth p 5)
             :p6     (drum-mod->id (:mod spec))}))))
