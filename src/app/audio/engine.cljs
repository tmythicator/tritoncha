(ns app.audio.engine
  "WebAudio context initialization and engine diagnostics."
  (:require ["tone" :as tone]
            [clojure.string :as str]
            [app.state :refer [audio-state engine-ctx]]
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
  (when-not (:tone @engine-ctx)
    (set! (.. tone -context -lookAhead) 0.25)
    (set! (.. tone -context -latencyHint) "playback")

    (let [busses      (routing/build-graph!)
          instruments (init-instruments! busses)]
      (swap! engine-ctx assoc :tone
             (merge busses
                    instruments
                    {:drums-muted?   (atom false)
                     :click-enabled? (atom false)})))))

(defn- format-track-summary [tracks]
  (if (empty? tracks)
    "0 (idle)"
    (let [n (count tracks)
          details (for [[kw info] tracks]
                    (let [muted?  (boolean @(:muted? info))
                          solo?   (boolean @(:solo? info))
                          pat     @(:pattern info)
                          step    (or (:step pat) "16n")
                          inst    (or (:inst pat) kw)
                          status  (cond solo? "[SOLO]" muted? "[MUTED]" :else "live")]
                      (str (name kw) " (" (name inst) ", " step ", " status ")")))]
      (str n " active -> [" (str/join ", " details) "]"))))

(defn audio-status
  "Print WebAudio context status, loop count, latency and clock drift."
  []
  (let [ctx          (when (:tone @engine-ctx) (.-context tone))
        transport    (.-Transport tone)
        tracks-map   (:active-tracks @audio-state)
        active-count (count tracks-map)
        tracks-desc  (format-track-summary tracks-map)
        raw-ctx      (when ctx (.-rawContext ctx))
        sample-rate  (when raw-ctx (.-sampleRate raw-ctx))
        base-lat     (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (* 1000 (.-baseLatency raw-ctx)))
        pos          (when transport (str (.-position transport)))
        tone-now     (tone/now)
        sys-now      (/ (.now js/performance) 1000)
        drift        (if-let [{:keys [t-tone t-sys]} (:clock-sample @audio-state)]
                       (let [dt-tone (- tone-now t-tone)
                             dt-sys  (- sys-now t-sys)
                             d-ms    (* 1000 (- dt-tone dt-sys))]
                         (swap! audio-state assoc :clock-sample {:t-tone tone-now :t-sys sys-now})
                         (str (if (pos? d-ms) "+" "") (.toFixed d-ms 3) " ms"))
                       (do
                         (swap! audio-state assoc :clock-sample {:t-tone tone-now :t-sys sys-now})
                         "0.000 ms (calibrated)"))]
    (js/console.log
     (str/join "\n"
               ["--- WebAudio Engine Diagnostics ---"
                (str "State:          " (if ctx (.-state ctx) "uninitialized"))
                (str "Sample Rate:    " (if sample-rate (str sample-rate " Hz") "N/A"))
                (str "Hardware Clock: " (if (number? tone-now) (str (.toFixed tone-now 4) " s") "N/A"))
                (str "Clock Drift:    " drift)
                (str "Base Latency:   " (if base-lat (str (.toFixed base-lat 2) " ms") "unavailable"))
                (str "Lookahead:      " (if ctx (str (* 1000 (.-lookAhead ctx)) " ms") "N/A"))
                (str "Latency Hint:   " (if ctx (.-latencyHint ctx) "N/A"))
                (str "Transport:      " (if transport (str (if (= (.-state transport) "started") "RUNNING" "STOPPED")
                                                           " @ " (.. transport -bpm -value) " BPM (Pos: " pos ")") "N/A"))
                (str "Active Loops:   " tracks-desc)
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