(ns app.ui.stats.telemetry
  "Audio engine telemetry grid subcomponent."
  (:require [clojure.string :as str]))

(defn telemetry-component
  [{:keys [ctx-state clock-drift drift hardware-clock tone-now sample-rate lookahead latency-hint
           base-latency base-lat transport-state transport-st bpm-str position pos key-str scene-name
           xrun-count min-headroom-ms]}]
  (let [st        (or ctx-state "uninitialized")
        tr-state  (or transport-state transport-st "stopped")
        hw-clk    (or hardware-clock tone-now)
        dr        (or clock-drift drift "0.000 ms")
        lat       (or base-latency base-lat)
        cur-pos   (or position pos "0:0:0")
        xruns     (or xrun-count 0)
        head-min  min-headroom-ms]
    [:div.neo-section
     [:div.neo-section-label "$ engine_telemetry"]
     [:div.neo-grid
      [:div.neo-item [:span.neo-k "Context: "] [:span.neo-v {:class (if (= st "running") "v-cyan" "v-pink")} (str/upper-case st)]]
      [:div.neo-item [:span.neo-k "Min Headroom: "] [:span.neo-v {:class (if (and head-min (< head-min 30.0)) "v-pink" "v-cyan")}
                                                     (if (number? head-min) (str (.toFixed head-min 1) " ms") "N/A")]]
      [:div.neo-item [:span.neo-k "X-Runs (Drops): "] [:span.neo-v {:class (if (pos? xruns) "v-pink" "v-cyan")}
                                                       (if (zero? xruns) "0 (CLEAN)" (str xruns " GLITCHES"))]]
      [:div.neo-item [:span.neo-k "Clock Drift: "] [:span.neo-v.v-cyan dr]]
      [:div.neo-item [:span.neo-k "Hardware Clock: "] [:span.neo-v (if (number? hw-clk) (str (.toFixed hw-clk 3) " s") "N/A")]]
      [:div.neo-item [:span.neo-k "Sample Rate: "] [:span.neo-v (if sample-rate (str sample-rate " Hz") "N/A")]]
      [:div.neo-item [:span.neo-k "Latency Hint: "] [:span.neo-v.v-cyan (if latency-hint (str/upper-case latency-hint) "N/A")]]
      [:div.neo-item [:span.neo-k "Lookahead: "] [:span.neo-v (if lookahead (str (.toFixed lookahead 0) " ms") "N/A")]]
      [:div.neo-item [:span.neo-k "Base Latency: "] [:span.neo-v (if (and (number? lat) (pos? lat)) (str (.toFixed lat 2) " ms") "N/A")]]
      [:div.neo-item [:span.neo-k "Transport: "] [:span.neo-v (if bpm-str (str (str/upper-case tr-state) " @ " bpm-str " BPM") (str/upper-case tr-state))]]
      [:div.neo-item [:span.neo-k "Position: "] [:span.neo-v (or cur-pos "0:0:0")]]
      [:div.neo-item [:span.neo-k "Harmonic Key: "] [:span.neo-v.v-cyan (or key-str "N/A")]]
      [:div.neo-item [:span.neo-k "3D Scene: "] [:span.neo-v (or scene-name "NONE")]]]]))
