(ns app.audio.dsp.instruments.catalog
  "Pure instrument catalog, specification resolution, category taxonomy, and drum hit predicates."
  (:require [app.audio.dsp.worklet.protocol :refer [drum-remaps]]
            [app.custom.drums :refer [user-drums]]
            [app.custom.synth :refer [user-synths]]
            [app.lib.drums :refer [core-drums]]
            [app.lib.synth :refer [core-synths]]
            [app.state :refer [repl-registry]]))

(defn all-drums
  "Returns a merged map of core built-in drums, user custom drums, and REPL drums."
  []
  (merge core-drums user-drums (:instruments @repl-registry)))

(defn all-synths
  "Returns a merged map of core built-in synthesizers, user custom synths, and REPL synths."
  []
  (merge core-synths user-synths (:instruments @repl-registry)))

(defn all-instruments
  "Returns a merged map of core built-in instruments, user custom instruments, and REPL instruments."
  []
  (merge core-synths user-synths core-drums user-drums (:instruments @repl-registry)))

(def default-category-instruments
  {:bass :bass-analog
   :sub  :sub-pure
   :pad  :pad-cinema
   :lead :lead-pluck})

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
   :zap          :fx-zap
   :siren        :fx-siren
   :nbell        :lead-nbell})

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
  "Normalizes category keyword to standard plural form (:pads, :leads, :bass, :drums, :fx)."
  [cat]
  (get category-aliases cat cat))

(def drum-keywords
  "Unified set of all drum instrument keywords."
  (into #{:drum :drums :kick :kicks :snare :snares :sn :hat :hats :hat-closed :hat-open :hh-c :hh-o :hh-clk
          :tom :toms :tom-high :tom-mid :tom-low :cymb :cymbal :cymbals :ride :ride-bell :crash :crash-16 :crash-18
          :splash :china :clap :claps :cowbell :rim :sn-rs :sn-clk :sn-gh :sn-roll :perc :percs :percussion
          :shaker :tamb :tambourine :break}
        (concat (keys core-drums)
                (keys user-drums))))

(def sub-voices
  #{:sub :sub-pure :sub-808 :808 :sub-moog :moog-sub})

(defn find-instrument-spec
  "Looks up an instrument specification map across REPL, custom, and core catalogs."
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

(defn sound-category
  "Resolves the high-level category keyword for an instrument or track:
  :drums, :bass, :leads, :pads, or :fx based fundamentally on its explicit :category declaration."
  [x]
  (cond
    (nil? x) nil
    (get category-aliases x) (get category-aliases x)
    (contains? drum-keywords (if (keyword? x) x (keyword (str x)))) :drums
    :else
    (let [spec         (if (map? x) x (find-instrument-spec x))
          explicit-cat (or (:category spec)
                           (when (map? x)
                             (when-let [inst-key (or (:inst x) (:inst-key x))]
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
  "Returns true if key or spec belongs to the :drums category."
  [x]
  (= (sound-category x) :drums))

(defn drum-keyword?
  "Checks if a keyword represents a drum instrument or drum hit."
  [k]
  (or (contains? drum-keywords (keyword k))
      (drum? k)))

(def drum-modes
  "Set of all supported drum character synthesis mode keywords."
  #{:analog :natural :idm :industrial})

(defn drum-mode?
  "Checks if a value represents a known drum character synthesis mode."
  [x]
  (and (keyword? x) (contains? drum-modes x)))

(defn composite-drums?
  "Returns true if the track key represents an all-in-one composite drum pattern (:drums, :drum, :kit, :break)."
  [k]
  (and (some? k) (contains? #{:drums :drum :break :kit} (keyword k))))

(defn individual-drum?
  "Returns true if the track or key is an individual drum voice (kick, snare, hi-hat, toms, cymbals, etc.)."
  [k]
  (boolean (and (drum? k) (not (composite-drums? k)))))

(defn bass?
  "Returns true if key or spec belongs to the :bass category."
  [x]
  (= (sound-category x) :bass))

(defn lead?
  "Returns true if key or spec belongs to the :leads category."
  [x]
  (= (sound-category x) :leads))

(defn pad?
  "Returns true if key or spec belongs to the :pads category."
  [x]
  (= (sound-category x) :pads))

(defn fx?
  "Returns true if key or spec belongs to the :fx category."
  [x]
  (= (sound-category x) :fx))

(defn sub?
  "Returns true if key, spec, or track corresponds to a sub-bass voice."
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
  "Returns true if key or spec is any tonal or FX synthesizer voice (non-drum)."
  [x]
  (and (some? x) (not (drum? x))))

(defn inst-base-type
  "Returns the underlying drum type or instrument spec type for an instrument keyword or token."
  [k]
  (when k
    (or (:type (find-instrument-spec k))
        (get drum-remaps (name k))
        k)))

(defn resolve-target-inst
  "Resolves the actual instrument to trigger for a pattern hit.
  If the hit matches the base drum type of default-inst-key, default-inst-key is used."
  [hit-kw default-inst-key]
  (if (or (nil? default-inst-key) (= default-inst-key hit-kw))
    hit-kw
    (let [hit-type (inst-base-type hit-kw)
          def-type (inst-base-type default-inst-key)]
      (if (or (= hit-type def-type)
              (not (drum-keyword? default-inst-key))
              (contains? #{:drum :drums :hit :beat :x :1} hit-kw))
        default-inst-key
        hit-kw))))
