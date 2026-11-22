(ns app.audio.dsp.worklet.compiler
  "Pure compilers and parsers for score notation, articulation, track kits, and DSP patches."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.instruments.catalog :refer [drum-keyword? resolve-target-inst]]
            [app.audio.dsp.worklet.protocol :refer [bus-key->id drum-remaps filter-type->id
                                                    noise-filter->id osc-type->id perc-body->id
                                                    synth-step-inst-id]]
            [app.audio.theory.patterns :refer [extract-articulation]]
            [app.utils.audio :refer [parse-midi-note]]))

(def ^:private rest-tokens #{:_ :- :rest :nil :none "." "0"})

(defn- step-rest [def-inst-key]
  {:inst-key (keyword def-inst-key) :note -1 :vel 0.0})

(defn- step-hit
  ([inst-key note vel]
   {:inst-key (keyword inst-key) :note (int note) :vel (float vel)})
  ([inst-key def-inst-key note vel]
   (let [target (resolve-target-inst inst-key def-inst-key)]
     {:inst-key (keyword target) :note (int note) :vel (float vel)})))

(defn parse-step-hit
  "Parses a step hit into {:inst-key :note :vel} respecting default-vel.
  Drum hits without an explicit pitch carry :note 0 (use patch base pitch);
  melodic notes carry MIDI note numbers (1..127); rests carry :note -1."
  ([hit default-inst-key] (parse-step-hit hit default-inst-key 0.9))
  ([hit default-inst-key default-vel]
   (let [def-v    (float (or default-vel 0.9))
         def-kw   (keyword default-inst-key)
         def-drum (drum-keyword? def-kw)]
     (cond
       (or (nil? hit) (false? hit))
       (step-rest def-kw)

       (true? hit)
       (step-hit def-kw (if def-drum 0 60) def-v)

       (number? hit)
       (if (neg? hit) (step-rest def-kw) (step-hit def-kw hit def-v))

       (and (vector? hit) (keyword? (first hit)))
       (let [[k v n]           hit
             [clean-k art-vel] (extract-articulation k def-v)
             target-kw         (keyword clean-k)
             midi-n            (if n (parse-midi-note n) (if (drum-keyword? target-kw) 0 60))]
         (step-hit target-kw def-kw midi-n (or v art-vel def-v)))

       (vector? hit)
       (if (seq hit)
         (step-hit def-kw (parse-midi-note (first hit)) def-v)
         (step-rest def-kw))

       (or (keyword? hit) (string? hit))
       (let [raw-str         (if (keyword? hit) (name hit) (str hit))
             [clean art-vel] (extract-articulation raw-str def-v)
             resolved-alias  (get drum-remaps clean)
             [final-clean final-vel]
             (if resolved-alias
               (extract-articulation (name resolved-alias) (if (= art-vel def-v) def-v art-vel))
               [clean art-vel])
             clean-kw        (keyword final-clean)]
         (cond
           (contains? rest-tokens clean-kw)
           (step-rest def-kw)

           (or resolved-alias (drum-keyword? clean-kw))
           (step-hit clean-kw def-kw 0 final-vel)

           :else
           (let [m (parse-midi-note final-clean)]
             (if (neg? m)
               (step-hit clean-kw def-kw 0 final-vel)
               (step-hit def-kw m final-vel)))))

       :else
       (step-rest def-kw)))))

(defn compile-track-steps
  "Compiles a sequence of track steps into track-local kit assignments and typed step vectors.
  Returns {:kit [drum-kw ...] :inst-ids [...] :notes [...] :vels [...]}.
  Drum steps receive track-local kit indices (0..15); synth steps receive synth-step-inst-id (-1)."
  [default-inst-key hits-vec vel-fn]
  (let [parsed (mapv (fn [i hit]
                       (parse-step-hit hit default-inst-key (vel-fn i)))
                     (range (count hits-vec))
                     hits-vec)
        {:keys [kit kit-map]}
        (reduce (fn [{:keys [kit kit-map] :as acc} {:keys [inst-key note]}]
                  (if (and (>= note 0)
                           (drum-keyword? inst-key)
                           (not (contains? kit-map inst-key))
                           (< (count kit) 16))
                    {:kit     (conj kit inst-key)
                     :kit-map (assoc kit-map inst-key (count kit))}
                    acc))
                {:kit [] :kit-map {}}
                parsed)
        fallback-id (if (seq kit) 0 synth-step-inst-id)]
    {:kit      kit
     :inst-ids (mapv (fn [{:keys [inst-key note]}]
                       (if (neg? note)
                         fallback-id
                         (get kit-map inst-key synth-step-inst-id)))
                     parsed)
     :notes    (mapv :note parsed)
     :vels     (mapv :vel parsed)}))

(def synth-patch-spec
  "Schema and baseline defaults for the Rust modular synthesizer voice patch."
  {:osc       {:type :saw :sub-level 0.0 :pulse-width 0.5 :noise 0.0 :drift 0.0}
   :filter    {:type :lowpass :cutoff 1200.0 :env-amount 3000.0 :key-track 2.0 :q 0.4 :drive 0.0}
   :amp-env   {:attack 0.005 :decay 0.2 :sustain 0.3 :release 0.2}
   :mod-env   {:attack 0.005 :decay 0.2}
   :pitch-env {:amount 0.0 :decay 0.015}
   :glide     0.0
   :polyphony 8})

(defn compile-patch-params
  "Compiles a declarative Clojure synth patch map into a packed 22-float JS array matching Rust ModularPatch."
  [patch-spec]
  (let [spec     (merge synth-patch-spec patch-spec)
        osc      (merge (:osc synth-patch-spec) (:osc spec))
        flt      (merge (:filter synth-patch-spec) (:filter spec))
        amp      (merge (:amp-env synth-patch-spec) (:amp-env spec))
        mod-e    (merge (:mod-env synth-patch-spec) {:attack (:attack amp) :decay (:decay amp)} (:mod-env spec))
        pitch-e  (merge (:pitch-env synth-patch-spec) (:pitch-env spec))
        bus-key  (or (:bus spec)
                     (busses/instrument-bus spec)
                     (busses/category-default-bus (:category spec))
                     :bus/lead)]
    #js [(float (osc-type->id (:type osc)))
         (float (:sub-level osc))
         (float (:pulse-width osc))
         (float (filter-type->id (:type flt)))
         (float (:cutoff flt))
         (float (:env-amount flt))
         (float (:key-track flt))
         (float (:q flt))
         (float (:attack amp))
         (float (:decay amp))
         (float (:sustain amp))
         (float (:release amp))
         (float (:attack mod-e))
         (float (:decay mod-e))
         (float (int (bus-key->id bus-key)))
         (float (int (if (= (:type spec) :mono) 1 (:polyphony spec 8))))
         (float (:glide spec))
         (float (:drive flt))
         (float (:noise osc))
         (float (:amount pitch-e))
         (float (:decay pitch-e))
         (float (:drift osc))]))

(defn compile-track-patch-msg
  "Compiles a declarative Clojure synth patch map into a track-centric setTrackPatch payload."
  [track-idx patch-spec]
  #js {:type     "setTrackPatch"
       :trackIdx (int track-idx)
       :params   (compile-patch-params patch-spec)})

(def perc-patch-defaults
  "Baseline defaults for the 24-float layered percussion voice contract (Rust PercPatch)."
  {:body          :membrane
   :pitch         52.0
   :pitch-drop    0.0
   :pitch-decay   0.030
   :decay         0.25
   :click         0.20
   :click-freq    2800.0
   :burst-count   1
   :burst-spacing 0.011
   :fm-depth      0.0
   :fm-ratio      2.5
   :noise-mix     0.0
   :noise-decay   0.18
   :noise-cutoff  3200.0
   :noise-q       0.45
   :noise-filter  :hp
   :modal-spread  1.0
   :drive         1.3
   :fold          0.0
   :crush         0.0
   :vel->pitch    0.12
   :vel->tone     0.35
   :variation     0.08
   :choke-group   0})

(defn compile-perc-params
  "Compiles a declarative Clojure percussion spec into a packed 24-float JS array matching Rust PercPatch.
  Preserves genuine 0.0 values."
  [perc-spec]
  (let [s (merge perc-patch-defaults perc-spec)]
    #js [(float (perc-body->id (:body s)))
         (float (:pitch s))
         (float (:pitch-drop s))
         (float (:pitch-decay s))
         (float (:decay s))
         (float (:click s))
         (float (:click-freq s))
         (float (:burst-count s))
         (float (:burst-spacing s))
         (float (:fm-depth s))
         (float (:fm-ratio s))
         (float (:noise-mix s))
         (float (:noise-decay s))
         (float (:noise-cutoff s))
         (float (:noise-q s))
         (float (noise-filter->id (:noise-filter s)))
         (float (:modal-spread s))
         (float (:drive s))
         (float (:fold s))
         (float (:crush s))
         (float (:vel->pitch s))
         (float (:vel->tone s))
         (float (:variation s))
         (float (:choke-group s))]))

(defn compile-track-kit-slot-msg
  "Compiles a layered percussion spec into a track-local setTrackKitSlot payload."
  [track-idx slot-idx perc-spec]
  #js {:type     "setTrackKitSlot"
       :trackIdx (int track-idx)
       :slotIdx  (int slot-idx)
       :params   (compile-perc-params perc-spec)})
