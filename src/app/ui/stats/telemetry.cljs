(ns app.ui.stats.telemetry
  "Audio engine telemetry grid subcomponent."
  (:require [clojure.string :as str]))

(defn telemetry-component
  [{:keys [ctx-state drift tone-now sample-rate lookahead base-latency base-lat transport-state transport-st bpm-str position pos key-str scene-name]}]
  (let [st       (or ctx-state "uninitialized")
        tr-state (or transport-state transport-st "stopped")
        lat      (or base-latency base-lat)
        cur-pos  (or position pos "0:0:0")]
    [:div.neo-section
     [:div.neo-section-label "$ engine_telemetry"]
     [:div.neo-grid
      [:div.neo-item [:span.neo-k "Context: "] [:span.neo-v {:class (if (= st "running") "v-cyan" "v-pink")} (str/upper-case st)]]
      [:div.neo-item [:span.neo-k "Clock Drift: "] [:span.neo-v.v-cyan (or drift "0.000 ms")]]
      [:div.neo-item [:span.neo-k "Hardware Clock: "] [:span.neo-v (if (number? tone-now) (str (.toFixed tone-now 2) "s") "N/A")]]
      [:div.neo-item [:span.neo-k "Sample Rate: "] [:span.neo-v (if sample-rate (str sample-rate " Hz") "N/A")]]
      [:div.neo-item [:span.neo-k "Lookahead: "] [:span.neo-v (if lookahead (str (.toFixed lookahead 0) "ms") "N/A")]]
      [:div.neo-item [:span.neo-k "Base Latency: "] [:span.neo-v (if lat (str (.toFixed lat 2) "ms") "N/A")]]
      [:div.neo-item [:span.neo-k "Transport: "] [:span.neo-v (str (str/upper-case tr-state) " @ " (or bpm-str "168") " BPM")]]
      [:div.neo-item [:span.neo-k "Position: "] [:span.neo-v cur-pos]]
      [:div.neo-item [:span.neo-k "Harmonic Key: "] [:span.neo-v.v-cyan (or key-str "E PHRYGIAN")]]
      [:div.neo-item [:span.neo-k "3D Scene: "] [:span.neo-v (or scene-name "CYBER-TORUS")]]]]))
