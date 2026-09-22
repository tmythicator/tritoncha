(ns app.audio.dsp.busses
  "Audio bus registry, normalization, routing mappings, and category predicates."
  (:require [app.custom.drums :refer [user-drums]]
            [app.custom.synth :refer [user-synths]]
            [app.lib.drums :refer [core-drums]]
            [app.lib.synth :refer [core-synths]]
            [app.state :refer [repl-registry]]))

(def valid-busses
  #{:bus/master :bus/direct :bus/click :bus/drums :bus/bass :bus/space :bus/lead})

(def category-default-busses
  {:drums :bus/drums
   :bass  :bus/bass
   :pads  :bus/space
   :leads :bus/lead
   :fx    :bus/lead})

(def category-aliases
  {;; Drums and Percussion
   :drum       :drums
   :drums      :drums
   :hat        :drums
   :hats       :drums
   :cymb       :drums
   :cymbal     :drums
   :cymbals    :drums
   :kick       :drums
   :kicks      :drums
   :snare      :drums
   :snares     :drums
   :sn         :drums
   :tom        :drums
   :toms       :drums
   :ride       :drums
   :crash      :drums
   :splash     :drums
   :china      :drums
   :clap       :drums
   :claps      :drums
   :perc       :drums
   :percs      :drums
   :percussion :drums
   :break      :drums
   :rim        :drums
   :shaker     :drums
   :tamb       :drums
   :tambourine :drums
   :cowbell    :drums
   ;; Bass
   :bass       :bass
   :sub        :bass
   :sub-bass   :bass
   :acid       :bass
   ;; Leads
   :lead       :leads
   :leads      :leads
   :arp        :leads
   :arps       :leads
   :melody     :leads
   ;; Pads and Space
   :pad        :pads
   :pads       :pads
   :keys       :pads
   :ambient    :pads
   :atmos      :pads
   :strings    :pads
   :drone      :pads
   ;; FX
   :fx         :fx})

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

(def ^:private standalone-instrument-busses
  {:click      :bus/direct
   :util-click :bus/direct})

(def sub-voices
  #{:sub :sub-pure :sub-808 :808 :sub-moog :moog-sub})

(def ^:private default-category-instruments
  {:bass :bass-analog
   :sub  :sub-pure
   :pad  :pad-cinema
   :lead :lead-pluck})

(defn find-instrument-spec
  "Looks up an instrument specification map across REPL, custom, and core catalogs.
  Examples: (find-instrument-spec :bass) -> {:title \"Analog Saw\" ...}."
  [x]
  (cond
    (map? x) x
    (nil? x) nil
    :else
    (let [k         (if (keyword? x) x (keyword (str x)))
          canonical (get default-category-instruments k k)]
      (or (get (:instruments @repl-registry) k)
          (get (:instruments @repl-registry) canonical)
          (get user-synths canonical)
          (get user-drums canonical)
          (get core-synths canonical)
          (get core-drums canonical)
          (get user-synths k)
          (get user-drums k)
          (get core-synths k)
          (get core-drums k)))))

(def drum-keywords
  "Unified set of all drum instrument keywords."
  (into #{:drum :drums :kick :kicks :snare :snares :sn :hat :hats :hat-closed :hat-open :hh-c :hh-o :hh-clk
          :tom :toms :tom-high :tom-mid :tom-low :cymb :cymbal :cymbals :ride :ride-bell :crash :crash-16 :crash-18
          :splash :china :clap :claps :cowbell :rim :sn-rs :sn-clk :sn-gh :sn-roll :perc :percs :percussion
          :shaker :tamb :tambourine :break}
        (concat (keys core-drums)
                (keys user-drums))))

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
  "Checks if a keyword represents a valid registered audio bus."
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
    (and (map? x) (or (:inst x) (:synth x) (:inst-key x)))
    (instrument-bus (or (:inst x) (:synth x) (:inst-key x)))

    ;; 4. Direct category keyword
    (contains? category-aliases x)
    (category-default-bus x)

    ;; 5. Drum keyword
    (contains? drum-keywords (if (keyword? x) x (keyword (str x))))
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

(defn sound-category
  "Resolves the high-level category keyword for an instrument or track:
  :drums, :bass, :leads, :pads, or :fx based fundamentally on its explicit :category declaration.
  Examples: (sound-category :kick) -> :drums, (sound-category :fx-laser) -> :fx."
  [x]
  (cond
    (nil? x) nil
    (get category-aliases x) (get category-aliases x)
    (contains? drum-keywords (if (keyword? x) x (keyword (str x)))) :drums
    :else
    (let [spec         (if (map? x) x (find-instrument-spec x))
          explicit-cat (or (:category spec)
                           (when (map? x)
                             (when-let [inst-key (or (:inst x) (:synth x) (:inst-key x))]
                               (:category (find-instrument-spec inst-key)))))]
      (if explicit-cat
        (normalize-category explicit-cat)
        (cond
          (and (map? x) (contains? x :pattern)) :drums
          (and (map? x) (or (:notes x) (:chord x)))
          (cond
            (= (:bus spec) :bus/space) :pads
            (= (:bus spec) :bus/bass)  :bass
            (= (:bus spec) :bus/lead)  :leads
            :else :leads)
          (= (:bus spec) :bus/drums) :drums
          (= (:bus spec) :bus/space) :pads
          (= (:bus spec) :bus/bass)  :bass
          (= (:bus spec) :bus/lead)  :leads
          :else nil)))))

(defn drum?
  "Returns true if key or spec belongs to the :drums category.
  Examples: (drum? :kick) -> true, (drum? :bass-analog) -> false."
  [x]
  (= (sound-category x) :drums))

(defn drum-keyword?
  "Checks if a keyword represents a drum instrument or drum hit.
  Examples: (drum-keyword? :kick) -> true, (drum-keyword? :bass-analog) -> false."
  [k]
  (or (contains? drum-keywords (keyword k))
      (drum? k)))

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
  (and (some? x) (not (drum? x))))
