(ns app.ui.instrument-browser.components
  "Reusable micro-components, routing graph query helpers and code formatters for Instrument Studio."
  (:require
   [app.audio.dsp.routing :as routing]
   [app.lib.routes :refer [default-graph]]
   [app.state :refer [audio-state]]
   [clojure.string :as str]))

(defn param-slider
  "Render slider input with numeric readout.
  Examples: [param-slider {:label \"Cutoff\" :val-str \"2400 Hz\" :min 40 :max 14000 :step 50 :value 2400}]."
  [{:keys [label val-str min max step value on-down on-change]}]
  [:div.inst-param-slider
   [:div.inst-slider-header
    [:span.inst-slider-label label]
    [:span.neo-v.v-cyan val-str]]
   [:input {:type            "range"
            :min             min
            :max             max
            :step            step
            :value           value
            :on-pointer-down on-down
            :on-change       on-change}]])

(defn pill-selector
  "Render compact pill button selector.
  Examples: [pill-selector {:label \"WAVEFORM\" :items [:saw :pulse] :current :saw :on-select f}]."
  [{:keys [label items current on-select]}]
  [:div.inst-pill-selector
   (when label
     [:span.inst-pill-label label])
   [:div.inst-pill-group
    (for [item items]
      (let [active? (= current item)]
        ^{:key (str item)}
        [:button.inst-pill-btn
         {:class    (when active? "active")
          :on-click #(on-select item)}
         (str/upper-case (name item))]))]])

(defn bus-badge-class
  "Return CSS class for bus badge styling.
  Examples: (bus-badge-class :bus/bass) -> \"bus-bass\"."
  [bus-key]
  (case bus-key
    :bus/bass   "bus-bass"
    :bus/space  "bus-space"
    :bus/lead   "bus-lead"
    :bus/drums  "bus-drums"
    :bus/direct "bus-direct"
    "bus-direct"))

(defn resolve-bus-chain
  "Trace audio processing sequence starting from bus-key to destination.
  Examples: (resolve-bus-chain :bus/space routes 10) -> [:bus/space :delay :reverb :destination]."
  [bus-key routes-list max-depth]
  (let [adj (reduce (fn [acc chain]
                      (reduce (fn [m [src dst]] (assoc m src dst))
                              acc
                              (partition 2 1 chain)))
                    {}
                    routes-list)
        step (fn step [curr visited depth]
               (cond
                 (or (nil? curr) (= curr :destination) (visited curr) (>= depth max-depth))
                 (when curr [curr])

                 :else
                 (cons curr (step (get adj curr) (conj visited curr) (inc depth)))))]
    (vec (step bus-key #{} 0))))

(defn proc-label
  "Format processor keyword to human readable title.
  Examples: (proc-label :master-filter) -> \"Master Filter\"."
  [proc-key]
  (case proc-key
    :delay         "Delay"
    :reverb        "Reverb"
    :freeverb      "Freeverb"
    :distort       "Drive"
    :distortion    "Distortion"
    :chorus        "Chorus"
    :crusher       "Bitcrush"
    :bitcrusher    "Bitcrush"
    :master-filter "Master Filter"
    :filter        "Filter"
    :limiter       "Limiter"
    (-> (name proc-key) str/capitalize (str/replace #"-" " "))))

(defn active-routing-spec
  "Retrieve active routing graph specification.
  Examples: (active-routing-spec) -> {:routes [...]}."
  []
  (let [routings    (routing/all-routings)
        cur-route-k (:current-routing @audio-state :default)]
    (or (get routings cur-route-k)
        default-graph)))

(defn bus-fx-summary
  "Dynamically resolve FX chain for a bus by traversing current routing graph.
  Examples: (bus-fx-summary :bus/space) -> \"Stereo Ping-Pong Delay + Freeverb Reverb\"."
  [bus-key]
  (let [target-bus (or bus-key :bus/direct)]
    (if (= target-bus :bus/direct)
      "Direct Master (Clean Line)"
      (let [graph   (active-routing-spec)
            routes  (:routes graph)
            chain   (resolve-bus-chain target-bus routes 8)
            fx-only (into []
                          (comp
                           (remove #{target-bus :destination :master-filter})
                           (map proc-label))
                          chain)]
        (if (seq fx-only)
          (str/join " + " fx-only)
          "Direct Output")))))

(defn- format-drum-spec-map
  "Generate ClojureScript defdrum! map for drum voices.
  Examples: (format-drum-spec-map :kick spec) -> \"(defdrum! :kick ...)\"."
  [inst-key spec]
  (let [bus (or (:bus spec) :bus/drums)
        mod-val (or (:mod spec) :natural)]
    (case (:type spec)
      :kick
      (str "(defdrum! " inst-key "\n"
           "  {:category    :drums\n"
           "   :type        :kick\n"
           "   :bus         " bus "\n"
           "   :mod         " mod-val "\n"
           "   :base-pitch  " (.toFixed (or (:base-pitch spec) 48.0) 1) "\n"
           "   :pitch-drop  " (.toFixed (or (:pitch-drop spec) 180.0) 1) "\n"
           "   :pitch-decay " (.toFixed (or (:pitch-decay spec) 0.040) 3) "\n"
           "   :decay       " (.toFixed (or (:decay spec) 0.28) 2) "\n"
           "   :click       " (.toFixed (or (:click spec) 0.35) 2) "\n"
           "   :drive       " (.toFixed (or (:drive spec) 1.6) 2) "})")

      :snare
      (str "(defdrum! " inst-key "\n"
           "  {:category    :drums\n"
           "   :type        :snare\n"
           "   :bus         " bus "\n"
           "   :mod         " mod-val "\n"
           "   :base-freq   " (.toFixed (or (:base-freq spec) 185.0) 1) "\n"
           "   :tone-decay  " (.toFixed (or (:tone-decay spec) 0.9985) 4) "\n"
           "   :noise-decay " (.toFixed (or (:noise-decay spec) 0.9991) 4) "\n"
           "   :cutoff      " (Math/round (or (:cutoff spec) 2400.0)) "\n"
           "   :snappy      " (.toFixed (or (:snappy spec) 0.85) 2) "})")

      :hat
      (str "(defdrum! " inst-key "\n"
           "  {:category     :drums\n"
           "   :type         :hat\n"
           "   :bus          " bus "\n"
           "   :mod          " mod-val "\n"
           "   :cutoff       " (Math/round (or (:cutoff spec) 7200.0)) "\n"
           "   :decay-closed " (.toFixed (or (:decay-closed spec) 0.04) 2) "\n"
           "   :decay-open   " (.toFixed (or (:decay-open spec) 0.24) 2) "})")

      :membrane
      (str "(defdrum! " inst-key "\n"
           "  {:category    :drums\n"
           "   :type        :membrane\n"
           "   :bus         " bus "\n"
           "   :mod         " mod-val "\n"
           "   :start-pitch " (.toFixed (or (:start-pitch spec) 180.0) 1) "\n"
           "   :min-pitch   " (.toFixed (or (:min-pitch spec) 105.0) 1) "\n"
           "   :pitch-decay " (.toFixed (or (:pitch-decay spec) 0.015) 3) "\n"
           "   :decay       " (.toFixed (or (:decay spec) 0.40) 2) "\n"
           "   :drive       " (.toFixed (or (:drive spec) 1.10) 2) "})")

      :metallic
      (str "(defdrum! " inst-key "\n"
           "  {:category  :drums\n"
           "   :type      :metallic\n"
           "   :bus       " bus "\n"
           "   :mod       " mod-val "\n"
           "   :cutoff    " (Math/round (or (:cutoff spec) 3500.0)) "\n"
           "   :resonance " (.toFixed (or (:resonance spec) 0.35) 2) "\n"
           "   :decay     " (.toFixed (or (:decay spec) 0.85) 2) "\n"
           (if (:bandpass spec)
             "   :bandpass  true\n"
             "")
           "   :drive     " (.toFixed (or (:drive spec) 1.0) 2) "})")

      :clap
      (str "(defdrum! " inst-key "\n"
           "  {:category  :drums\n"
           "   :type      :clap\n"
           "   :bus       " bus "\n"
           "   :mod       " mod-val "\n"
           "   :cutoff    " (Math/round (or (:cutoff spec) 1200.0)) "\n"
           "   :resonance " (.toFixed (or (:resonance spec) 0.70) 2) "\n"
           "   :decay     " (.toFixed (or (:decay spec) 0.28) 2) "\n"
           "   :drive     " (.toFixed (or (:drive spec) 1.0) 2) "})")

      (str "(defdrum! " inst-key "\n"
           "  " (pr-str spec) ")"))))

(defn format-spec-map
  "Generate ClojureScript defsynth! or defdrum! map matching instrument definitions.
  Examples: (format-spec-map :saw-bass spec) -> \"(defsynth! :saw-bass ...)\"."
  [inst-key spec]
  (if (= (:category spec) :drums)
    (format-drum-spec-map inst-key spec)
    (let [osc      (or (:osc spec) {:type :saw})
          flt      (or (:filter spec) {:type :lowpass :cutoff 2000 :q 0.2})
          amp      (or (:amp-env spec) {:attack 0.01 :decay 0.2 :sustain 0.5 :release 0.3})
          mod-e    (:mod-env spec)
          pitch-e  (:pitch-env spec)
          bus      (or (:bus spec) :bus/lead)
          poly?    (= (:type spec) :poly)
          glide    (:glide spec)]
      (str "(defsynth! " inst-key "\n"
           "  {:osc     {:type " (or (:type osc) :saw)
           (if (and (:sub-level osc) (> (:sub-level osc) 0.001))
             (str " :sub-level " (.toFixed (:sub-level osc) 2))
             "")
           (if (contains? #{:pulse :blade} (:type osc))
             (str " :pulse-width " (.toFixed (or (:pulse-width osc) 0.5) 2))
             "")
           (if (and (:noise osc) (> (:noise osc) 0.001))
             (str " :noise " (.toFixed (:noise osc) 2))
             "")
           (if (and (:drift osc) (> (:drift osc) 0.001))
             (str " :drift " (.toFixed (:drift osc) 2))
             "")
           "}\n"
           "   :filter  {:type " (or (:type flt) :lowpass)
           " :cutoff " (Math/round (or (:cutoff flt) 2000))
           " :q " (.toFixed (or (:q flt) 0.2) 2)
           (if (and (:drive flt) (> (:drive flt) 0.001))
             (str " :drive " (.toFixed (:drive flt) 2))
             "")
           (if (and (:env-amount flt) (not= (Math/round (:env-amount flt)) 0))
             (str " :env-amount " (Math/round (:env-amount flt)))
             "")
           (if (and (:key-track flt) (not= (:key-track flt) 0.0))
             (str " :key-track " (.toFixed (:key-track flt) 1))
             "")
           "}\n"
           "   :amp-env {:attack " (.toFixed (or (:attack amp) 0.01) 3)
           " :decay " (.toFixed (or (:decay amp) 0.2) 3)
           " :sustain " (.toFixed (or (:sustain amp) 0.5) 2)
           " :release " (.toFixed (or (:release amp) 0.3) 3) "}\n"
           (if mod-e
             (str "   :mod-env {:attack " (.toFixed (or (:attack mod-e) 0.01) 3)
                  " :decay " (.toFixed (or (:decay mod-e) 0.2) 3) "}\n")
             "")
           (if (and pitch-e (> (or (:amount pitch-e) 0) 0))
             (str "   :pitch-env {:amount " (Math/round (:amount pitch-e))
                  " :decay " (.toFixed (or (:decay pitch-e) 0.015) 3) "}\n")
             "")
           (if poly?
             "   :type    :poly\n"
             "")
           (if (and glide (> glide 0.001))
             (str "   :glide   " (.toFixed glide 3) "\n")
             "")
           "   :bus     " bus "})"))))
