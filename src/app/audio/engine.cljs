(ns app.audio.engine
  "WebAudio context initialization and engine diagnostics."
  (:require ["tone" :as tone]
            [clojure.string :as str]
            [app.state :refer [state tone-ctx last-clock-sample active-tracks]]
            [app.audio.voices :as voices]
            [app.audio.routing :as routing]))

(defn- init-instruments!
  "Instantiates and routes all preset synthesizers and drums to audio busses."
  [busses]
  (into {}
        (for [[k _spec] (voices/all-instruments)]
          (let [node (voices/create-instrument k)]
            (voices/route-instrument! k node busses)
            [k node]))))

(defn resume-audio-context! []
  (try
    (tone/start)
    (when-let [^js ctx (.-context tone)]
      (when (not= (.-state ctx) "running")
        (.resume ctx)
        (when-let [^js raw (.-rawContext ctx)]
          (.resume raw))))
    (catch js/Object _)))

(defn init-audio!
  "Initializes the WebAudio context, compiles the DSP routing graph and registers instruments."
  []
  (resume-audio-context!)
  (when-not @tone-ctx
    (set! (.. tone -context -lookAhead) 0.2)
    (set! (.. tone -context -latencyHint) "playback")

    (let [busses      (routing/build-graph!)
          instruments (init-instruments! busses)]
      (reset! tone-ctx (merge busses
                              instruments
                              {:drums-muted?   (atom false)
                               :click-enabled? (atom false)}))))
  (swap! state assoc :active? true))

(defn audio-status
  "Print WebAudio context status, loop count, latency and clock drift."
  []
  (let [ctx (when @tone-ctx (.-context tone))
        transport (.-Transport tone)
        active-loops (count (if-let [{:keys [loops]} @tone-ctx] @loops []))
        perf (.-memory js/window.performance)
        raw-ctx (when ctx (.-rawContext ctx))
        base-lat (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (* 1000 (.-baseLatency raw-ctx)))
        tone-now (tone/now)
        sys-now  (/ (.now js/performance) 1000)
        drift (if-let [{:keys [t-tone t-sys]} @last-clock-sample]
                (let [dt-tone (- tone-now t-tone)
                      dt-sys  (- sys-now t-sys)
                      d-ms    (* 1000 (- dt-tone dt-sys))]
                  (reset! last-clock-sample {:t-tone tone-now :t-sys sys-now})
                  (str (if (pos? d-ms) "+" "") (.toFixed d-ms 3) " ms"))
                (do
                  (reset! last-clock-sample {:t-tone tone-now :t-sys sys-now})
                  "0.000 ms (calibrated)"))]
    (js/console.log
     (str/join "\n"
               ["--- WebAudio Engine Diagnostics ---"
                (str "State:          " (if ctx (.-state ctx) "uninitialized"))
                (str "Hardware Clock: " (if (number? tone-now) (.toFixed tone-now 4) "N/A") " s")
                (str "Clock Drift:    " drift)
                (str "Base Latency:   " (if base-lat (str (.toFixed base-lat 2) " ms") "unavailable"))
                (str "Lookahead:      " (if ctx (str (* 1000 (.-lookAhead ctx)) " ms") "N/A"))
                (str "Latency Hint:   " (if ctx (.-latencyHint ctx) "N/A"))
                (str "Transport BPM:  " (if transport (.. transport -bpm -value) "N/A"))
                (str "Active Loops:   " active-loops)
                (str "JS Heap Memory: "
                     (if perf
                       (str (.toFixed (/ (.-usedJSHeapSize perf) 1048576) 2) " MB / "
                            (.toFixed (/ (.-totalJSHeapSize perf) 1048576) 2) " MB")
                       "performance.memory unavailable"))
                "-----------------------------------"]))
    {:state        (if ctx (.-state ctx) :uninitialized)
     :sample-rate  sample-rate
     :clock        tone-now
     :clock-drift  drift
     :base-latency base-lat
     :lookahead    (when ctx (* 1000 (.-lookAhead ctx)))
     :bpm          (when transport (.. transport -bpm -value))
     :position     pos
     :active-loops active-count
     :tracks       (keys tracks-map)}))