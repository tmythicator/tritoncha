(ns app.ui.stats.telemetry
  "Audio engine telemetry grid subcomponent.")

(defn telemetry-component
  "Render lean real-time WebAudio telemetry metrics grid."
  [{:keys [sample-rate base-latency base-lat clock-drift drift
           hardware-clock tone-now position pos xrun-count]}]
  (let [sr       (if sample-rate (str sample-rate " Hz") "N/A")
        raw-lat  (or base-latency base-lat)
        lat-str  (if (and (number? raw-lat) (pos? raw-lat))
                   (str (.toFixed raw-lat 2) " ms")
                   "N/A")
        dr       (or clock-drift drift "N/A")
        hw-clk   (or hardware-clock tone-now)
        clk-str  (if (number? hw-clk) (str (.toFixed hw-clk 3) " s") "N/A")
        cur-pos  (or position pos "0:0:0")
        xruns    (or xrun-count 0)]
    [:div.neo-section
     [:div.neo-section-label "$ engine_telemetry"]
     [:div.neo-grid
      [:div.neo-item [:span.neo-k "Sample Rate: "] [:span.neo-v sr]]
      [:div.neo-item [:span.neo-k "Base Latency: "]
       [:span.neo-v {:class (if (= lat-str "N/A") "" "v-cyan")} lat-str]]
      [:div.neo-item [:span.neo-k "Clock Drift: "] [:span.neo-v.v-cyan dr]]
      [:div.neo-item [:span.neo-k "X-Runs (Drops): "]
       [:span.neo-v {:class (if (pos? xruns) "v-pink" "v-cyan")}
        (if (zero? xruns) "0 (CLEAN)" (str xruns " GLITCHES"))]]
      [:div.neo-item [:span.neo-k "Hardware Clock: "] [:span.neo-v clk-str]]
      [:div.neo-item [:span.neo-k "Position: "] [:span.neo-v cur-pos]]]]))
