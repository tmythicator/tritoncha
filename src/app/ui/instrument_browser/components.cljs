(ns app.ui.instrument-browser.components
  "Reusable micro-components and routing graph query helpers for Instrument Studio."
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
