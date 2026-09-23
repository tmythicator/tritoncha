(ns app.audio.dsp.busses
  "Audio bus registry, normalization, routing mappings, and bus targets."
  (:require [app.audio.dsp.instruments.catalog :as catalog :refer [find-instrument-spec
                                                                   normalize-category]]))

(def valid-busses
  "Canonical set of all hardware audio mixing busses."
  #{:bus/master :bus/direct :bus/click :bus/drums :bus/bass :bus/space :bus/lead})

(def category-default-busses
  "Default mixer bus destination for each instrument category."
  {:drums :bus/drums
   :bass  :bus/bass
   :pads  :bus/space
   :leads :bus/lead
   :fx    :bus/lead})

(defn category-default-bus
  "Resolves the canonical default audio bus for an instrument category keyword.
  Examples: (category-default-bus :bass) -> :bus/bass, (category-default-bus :pads) -> :bus/space."
  [cat]
  (get category-default-busses (normalize-category cat) :bus/master))

(def ^:private standalone-instrument-busses
  {:click :bus/direct})

(defn normalize-bus-key
  "Ensures a keyword is in the :bus/<name> format and maps :bus/click to :bus/direct.
  Examples: (normalize-bus-key :drums) -> :bus/drums, (normalize-bus-key :click) -> :bus/direct."
  [k]
  (when k
    (let [norm (if (keyword? k)
                 (if (= (namespace k) "bus") k (keyword "bus" (name k)))
                 (let [s (str k)]
                   (if (.startsWith s "bus/") (keyword s) (keyword "bus" (name s)))))]
      (if (= norm :bus/click) :bus/direct norm))))

(defn valid-bus?
  "Checks if a keyword represents a valid registered audio bus.
  Examples: (valid-bus? :bus/drums) -> true, (valid-bus? :bus/unknown) -> false."
  [k]
  (contains? valid-busses (normalize-bus-key k)))

(defn instrument-bus
  "Resolves the target audio bus for an instrument, track, or category keyword.
  Priority:
    1. Direct :bus in map spec (e.g. {:bus :bus/space, :osc ...})
    2. Mini-notation drum pattern defaults to drum bus
    3. :bus in referenced instrument (e.g. {:inst :tracker-lead})
    4. Direct category keyword (e.g. :bass, :pads, :drums, :leads, :fx)
    5. Drum keyword
    6. Standalone bus override (e.g. :click)
    7. :bus declared in catalog instrument spec (REPL registry, user custom, or core)
    8. Category default bus fallback determined by instrument category
    9. Fallback :bus/master"
  [x]
  (cond
    (nil? x) :bus/master

    ;; 1. Direct map with explicit :bus
    (and (map? x) (:bus x))
    (normalize-bus-key (:bus x))

    ;; 2. Mini-notation drum pattern defaults to drum bus
    (and (map? x) (:pattern x))
    :bus/drums

    ;; 3. Map referencing an instrument keyword
    (and (map? x) (or (:inst x) (:inst-key x)))
    (instrument-bus (or (:inst x) (:inst-key x)))

    ;; 4. Direct category keyword
    (contains? catalog/category-aliases x)
    (category-default-bus x)

    ;; 5. Drum keyword
    (contains? catalog/drum-keywords (if (keyword? x) x (keyword (str x))))
    :bus/drums

    ;; 6. Standalone bus override (e.g. :click)
    (contains? standalone-instrument-busses x)
    (get standalone-instrument-busses x)

    ;; 7. Lookup instrument keyword or string in catalog
    :else
    (let [k (if (keyword? x) x (keyword (str x)))]
      (if-let [spec (find-instrument-spec k)]
        (or (some-> (:bus spec) normalize-bus-key)
            (category-default-bus (:category spec)))
        :bus/master))))
