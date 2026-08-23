(ns app.ui.stats.telemetry
  "Audio engine telemetry grid subcomponent."
  (:require [clojure.string :as str]))

(defn telemetry-component
  [{:keys [ctx-state drift tone-now sample-rate lookahead base-lat transport-st bpm-str pos key-str scene-name]}]
  [:div.neo-section
   [:div.neo-section-label "$ engine_telemetry"]
   [:div.neo-grid
    [:div.neo-item [:span.neo-k "Context: "] [:span.neo-v {:class (if (= ctx-state "running") "v-cyan" "v-pink")} (str/upper-case ctx-state)]]
    [:div.neo-item [:span.neo-k "Clock Drift: "] [:span.neo-v.v-cyan drift]]
    [:div.neo-item [:span.neo-k "Hardware Clock: "] [:span.neo-v (if (number? tone-now) (str (.toFixed tone-now 2) "s") "N/A")]]
    [:div.neo-item [:span.neo-k "Sample Rate: "] [:span.neo-v (if sample-rate (str sample-rate " Hz") "N/A")]]
    [:div.neo-item [:span.neo-k "Lookahead: "] [:span.neo-v (if lookahead (str (.toFixed lookahead 0) "ms") "N/A")]]
    [:div.neo-item [:span.neo-k "Base Latency: "] [:span.neo-v (if base-lat (str (.toFixed base-lat 2) "ms") "N/A")]]
    [:div.neo-item [:span.neo-k "Transport: "] [:span.neo-v (str (str/upper-case transport-st) " @ " bpm-str " BPM")]]
    [:div.neo-item [:span.neo-k "Position: "] [:span.neo-v pos]]
    [:div.neo-item [:span.neo-k "Harmonic Key: "] [:span.neo-v.v-cyan key-str]]
    [:div.neo-item [:span.neo-k "3D Scene: "] [:span.neo-v scene-name]]]])
