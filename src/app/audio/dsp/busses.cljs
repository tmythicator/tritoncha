(ns app.audio.dsp.busses
  "Audio bus registry, normalization, routing mappings, and category predicates."
  (:require [app.custom.instruments :refer [user-instruments]]
            [app.lib.drums :refer [core-drum-instruments core-drum-voices]]
            [app.lib.instruments :refer [core-instruments]]
            [app.state :refer [repl-registry]]))

(def valid-busses
  #{:bus/master :bus/direct :bus/drums :bus/bass :bus/space :bus/lead :bus/glitch})

(def category-default-busses
  {:drums :bus/drums
   :bass  :bus/bass
   :pads  :bus/space
   :leads :bus/lead
   :fx    :bus/lead})

(def category-aliases
  {:pad   :pads
   :pads  :pads
   :lead  :leads
   :leads :leads
   :bass  :bass
   :drum  :drums
   :drums :drums
   :fx    :fx})

(defn normalize-category
  "Normalizes category keyword to standard plural form (:pads, :leads, :bass, :drums, :fx).
  Examples: (normalize-category :lead) -> :leads, (normalize-category :pad) -> :pads."
  [cat]
  (get category-aliases cat cat))

(defn category-default-bus
  "Resolves the canonical default audio bus for an instrument category keyword.
  Examples: (category-default-bus :bass) -> :bus/bass, (category-default-bus :pads) -> :bus/space."
  [cat]
  (get category-default-busses (normalize-category cat) :bus/master))

(def default-instrument-busses category-default-busses)

(def ^:private standalone-instrument-busses
  {:glitch         :bus/glitch
   :glitch-texture :bus/glitch
   :noise          :bus/glitch
   :click          :bus/direct
   :util-click     :bus/direct
   :metronome      :bus/direct})

(def sub-voices
  #{:sub :sub-bass :sub-sine :808-sub :sub-pure :sub-808 :808})

(defn find-instrument-spec
  "Looks up an instrument specification map across REPL, custom, and core catalogs."
  [k]
  (let [canonical (get {:bass :bass-analog, :sub :sub-pure, :pad :pad-cinema, :lead :lead-pluck} k k)]
    (or (get (:instruments @repl-registry) canonical)
        (get user-instruments canonical)
        (get core-instruments canonical)
        (get core-drum-instruments canonical)
        (when (contains? core-drum-voices canonical)
          (if (= canonical :click)
            {:category :fx :bus :bus/direct}
            {:category :drums :bus :bus/drums})))))

(defn normalize-bus-key
  "Ensures a keyword is in the :bus/<name> format.
  Examples: (normalize-bus-key :drums) -> :bus/drums, (normalize-bus-key :bus/bass) -> :bus/bass."
  [k]
  (when k
    (if (keyword? k)
      (if (= (namespace k) "bus") k (keyword "bus" (name k)))
      (let [s (str k)]
        (if (.startsWith s "bus/") (keyword s) (keyword "bus" (name s)))))))

(defn valid-bus?
  "Checks if a keyword represents a valid registered audio bus."
  [k]
  (contains? valid-busses (normalize-bus-key k)))

(defn instrument-bus
  "Resolves the destination audio bus for an instrument, track, or category keyword.
  Priority:
    1. Direct :bus in map spec (e.g. {:bus :bus/space, :osc ...})
    2. :bus in referenced instrument (e.g. {:inst :tracker-lead})
    3. Direct category keyword (e.g. :bass, :pads, :drums, :leads, :fx)
    4. Standalone bus override (e.g. :glitch-texture, :click)
    5. :bus declared in catalog instrument spec (REPL registry, user custom, or core)
    6. Category default bus fallback determined by instrument category
    7. Fallback :bus/master"
  [x]
  (cond
    (nil? x) :bus/master

    ;; 1. Direct map with explicit :bus
    (and (map? x) (:bus x))
    (normalize-bus-key (:bus x))

    ;; 2. Map referencing an instrument keyword
    (and (map? x) (or (:inst x) (:inst-key x)))
    (instrument-bus (or (:inst x) (:inst-key x)))

    ;; 3. Direct category keyword
    (contains? category-aliases x)
    (category-default-bus x)

    ;; 4. Standalone bus override (e.g. :glitch-texture, :click)
    (contains? standalone-instrument-busses x)
    (get standalone-instrument-busses x)

    ;; 5. Lookup instrument keyword or string in catalog
    :else
    (let [k (if (keyword? x) x (keyword (str x)))]
      (if-let [spec (find-instrument-spec k)]
        (or (some-> (:bus spec) normalize-bus-key)
            (category-default-bus (:category spec)))
        :bus/master))))

(defn sound-category
  "Resolves the high-level category keyword for an instrument or track:
  :drums, :bass, :leads, :pads, or :fx based fundamentally on its explicit :category declaration.
  Examples: (sound-category :kick) -> :drums, (sound-category :fx-laser) -> :fx."
  [x]
  (if-let [direct (get category-aliases x)]
    direct
    (let [spec         (if (map? x) x (find-instrument-spec x))
          explicit-cat (or (:category spec)
                           (when (map? x)
                             (when-let [inst-key (or (:inst x) (:inst-key x))]
                               (:category (find-instrument-spec inst-key)))))]
      (if explicit-cat
        (normalize-category explicit-cat)
        (cond
          (= (:bus spec) :bus/drums) :drums
          (= (:bus spec) :bus/space) :pads
          (= (:bus spec) :bus/bass)  :bass
          (= (:bus spec) :bus/lead)  :leads
          :else :leads)))))

(defn drum?
  "Returns true if key or spec belongs to the :drums category.
  Examples: (drum? :kick) -> true, (drum? :bass-analog) -> false."
  [x]
  (= (sound-category x) :drums))

(defn bass?
  "Returns true if key or spec belongs to the :bass category.
  Examples: (bass? :bass-analog) -> true, (bass? :lead-pluck) -> false."
  [x]
  (= (sound-category x) :bass))

(defn lead?
  "Returns true if key or spec belongs to the :leads category.
  Examples: (lead? :lead-pluck) -> true, (lead? :kick) -> false."
  [x]
  (= (sound-category x) :leads))

(defn pad?
  "Returns true if key or spec belongs to the :pads category.
  Examples: (pad? :pad-cinema) -> true, (pad? :bass-analog) -> false."
  [x]
  (= (sound-category x) :pads))

(defn fx?
  "Returns true if key or spec belongs to the :fx category.
  Examples: (fx? :fx-laser) -> true, (fx? :bass-analog) -> false."
  [x]
  (= (sound-category x) :fx))

(defn sub?
  "Returns true if key, spec, or track corresponds to a sub-bass voice.
  Examples: (sub? :sub-pure) -> true, (sub? :lead-pluck) -> false."
  [x]
  (let [spec (if (map? x) x (find-instrument-spec x))
        k    (cond
               (keyword? x) x
               (map? x) (or (:inst x) (:inst-key x))
               :else (keyword (str x)))]
    (or (contains? sub-voices k)
        (and (bass? x)
             (= (get-in spec [:osc :type]) :sine)))))

(defn synth?
  "Returns true if key or spec is any tonal or FX synthesizer voice (non-drum).
  Examples: (synth? :bass-analog) -> true, (synth? :kick) -> false."
  [x]
  (not= (sound-category x) :drums))
