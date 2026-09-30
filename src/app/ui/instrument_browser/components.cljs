(ns app.ui.instrument-browser.components
  "Reusable micro-components and routing graph query helpers for Instrument Studio."
  (:require
   [app.audio.dsp.routing :as routing]
   [app.lib.routes :refer [default-graph]]
   [app.state :refer [audio-state]]
   [app.ui.common :as common]
   [clojure.string :as str]))

(def param-slider
  "Render slider input with numeric readout."
  common/param-slider)

(def pill-selector
  "Render compact pill button selector."
  common/pill-selector)

(def bus-badge-class
  "Return CSS class for bus badge styling."
  common/bus-badge-class)

(defn resolve-bus-chain
  "Trace audio processing sequence starting from bus-key to :out."
  [bus-key routes-spec max-depth]
  (let [normalized (routing/normalize-routes routes-spec)]
    (loop [curr    bus-key
           result  [bus-key]
           visited #{bus-key}
           depth   0]
      (if (or (>= depth max-depth) (= curr :out))
        result
        (if-let [{:keys [inserts target]} (get normalized curr)]
          (let [next-nodes (concat inserts (when target [target]))
                new-result (into result next-nodes)]
            (if (or (= target :out) (nil? target) (visited target))
              new-result
              (recur target new-result (conj visited target) (inc depth))))
          (if (not= curr :out)
            (conj result :out)
            result))))))

(defn proc-label
  "Format processor keyword to human readable title."
  [proc-key]
  (case proc-key
    :bus/master "Master"
    :delay      "Delay"
    :reverb     "Reverb"
    :freeverb   "Freeverb"
    :distort    "Drive"
    :distortion "Distortion"
    :chorus     "Chorus"
    :crusher    "Bitcrush"
    :bitcrusher "Bitcrush"
    :filter     "Filter"
    :compressor "Compressor"
    :limiter    "Limiter"
    (-> (name proc-key) str/capitalize (str/replace #"-" " "))))

(defn active-routing-spec
  "Retrieve active routing graph specification."
  []
  (let [routings    (routing/all-routings)
        cur-route-k (:current-routing @audio-state :default)]
    (or (get routings cur-route-k)
        default-graph)))

(defn bus-fx-summary
  "Dynamically resolve FX chain for a bus by traversing current routing graph."
  [bus-key]
  (let [target-bus (or bus-key :bus/direct)]
    (if (= target-bus :bus/direct)
      "Direct Master (Clean Line)"
      (let [graph   (active-routing-spec)
            routes  (:routes graph)
            chain   (resolve-bus-chain target-bus routes 8)
            fx-only (into []
                          (comp
                           (remove #{target-bus :out :filter :bus/master})
                           (map proc-label))
                          chain)]
        (if (seq fx-only)
          (str/join " + " fx-only)
          "Direct Output")))))
