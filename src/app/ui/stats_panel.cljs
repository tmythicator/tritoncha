(ns app.ui.stats-panel
  (:require ["tone" :as tone]
            [clojure.string :as str]
            [app.state :refer [state active-tracks tone-ctx last-clock-sample]]))

(defn stats-panel-component []
  (let [ctx          (when @tone-ctx (.-context tone))
        transport    (.-Transport tone)
        raw-ctx      (when ctx (.-rawContext ctx))
        sample-rate  (when raw-ctx (.-sampleRate raw-ctx))
        base-lat     (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (* 1000 (.-baseLatency raw-ctx)))
        lookahead    (when ctx (* 1000 (.-lookAhead ctx)))
        latency-hint (when ctx (.-latencyHint ctx))
        ctx-state    (if ctx (.-state ctx) "uninitialized")
        pos          (when transport (str (.-position transport)))
        bpm-val      (if transport (.. transport -bpm -value) (:bpm @state))
        transport-st (if transport (.-state transport) "stopped")
        tone-now     (tone/now)
        sys-now      (/ (.now js/performance) 1000)
        drift        (if-let [{:keys [t-tone t-sys]} @last-clock-sample]
                       (let [dt-tone (- tone-now t-tone)
                             dt-sys  (- sys-now t-sys)
                             d-ms    (* 1000 (- dt-tone dt-sys))]
                         (reset! last-clock-sample {:t-tone tone-now :t-sys sys-now})
                         (str (if (pos? d-ms) "+" "") (.toFixed d-ms 3) " ms"))
                       (do
                         (reset! last-clock-sample {:t-tone tone-now :t-sys sys-now})
                         "0.000 ms (calibrated)"))
        tracks-map   @active-tracks
        active-count (count tracks-map)
        perf         (.-memory js/window.performance)
        heap-used    (when perf (.toFixed (/ (.-usedJSHeapSize perf) 1048576) 1))
        heap-total   (when perf (.toFixed (/ (.-totalJSHeapSize perf) 1048576) 1))]
    [:div.neo-stats-card
     [:div.neo-header
      [:div.neo-title
       [:span.neo-prompt "> "]
       [:span "SYSTEM AUDIO STATUS"]]
      [:div.neo-status-badge
       [:span.neo-dot {:class (if (= ctx-state "running") "online" "offline")}]
       [:span (if (= ctx-state "running") "ONLINE" "OFFLINE")]]]

     [:div.neo-body
      [:div.neo-section-label "$ engine_telemetry"]
      [:div.neo-grid
       [:div.neo-item [:span.neo-k "Context: "] [:span.neo-v {:class (if (= ctx-state "running") "v-cyan" "v-pink")} (str/upper-case ctx-state)]]
       [:div.neo-item [:span.neo-k "Clock Drift: "] [:span.neo-v.v-cyan drift]]
       [:div.neo-item [:span.neo-k "Hardware Clock: "] [:span.neo-v (if (number? tone-now) (str (.toFixed tone-now 2) "s") "N/A")]]
       [:div.neo-item [:span.neo-k "Sample Rate: "] [:span.neo-v (if sample-rate (str sample-rate " Hz") "N/A")]]
       [:div.neo-item [:span.neo-k "Lookahead Buffer: "] [:span.neo-v (if lookahead (str (.toFixed lookahead 0) "ms") "N/A")]]
       [:div.neo-item [:span.neo-k "Base Latency: "] [:span.neo-v (if base-lat (str (.toFixed base-lat 2) "ms") "N/A")]]
       [:div.neo-item [:span.neo-k "Transport: "] [:span.neo-v (str (str/upper-case transport-st) " @ " bpm-val " BPM")]]
       [:div.neo-item [:span.neo-k "Position: "] [:span.neo-v (or pos "0:0:0")]]
       [:div.neo-item [:span.neo-k "Latency Hint: "] [:span.neo-v (or latency-hint "playback")]]
       [:div.neo-item [:span.neo-k "JS Heap Memory: "] [:span.neo-v (if perf (str heap-used "/" heap-total " MB") "N/A")]]]

      [:div.neo-section-label (str "# ACTIVE LOOPS (" active-count ")")]
      (if (empty? tracks-map)
        [:div.neo-empty "no active loops"]
        [:div.neo-tracks-grid
         (for [[kw info] tracks-map
               :let [muted? (boolean @(:muted? info))
                     solo?  (boolean @(:solo? info))
                     pat    @(:pattern info)
                     step   (or (:step pat) "16n")
                     inst   (or (:inst pat) kw)]]
           ^{:key kw}
           [:div.neo-track-box {:class (cond solo? "box-solo" muted? "box-muted" :else "box-live")}
            [:span.track-name (name kw)]
            [:span.track-sep " / "]
            [:span.track-inst (name inst)]
            [:span.track-step (str " [" step "]")]
            [:span.track-state (cond solo? " SOLO" muted? " MUTED" :else " LIVE")]])])]

     [:div.neo-footer
      [:span.neo-foot-cmd "> ./tritoncha --stats"]
      [:span.neo-foot-hint "[Press I or click STATS to close]"]]]))
