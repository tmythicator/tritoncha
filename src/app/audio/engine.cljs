(ns app.audio.engine
  "WebAudio context initialization and engine diagnostics."
  (:require ["tone" :as tone]
            [clojure.string :as str]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx]]
            [app.utils :refer [active-lookahead sec->ms ms->sec]]
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
    (try
      (when-let [^js ctx (.-context tone)]
        (set! (.-lookAhead ctx) (active-lookahead)))
      (catch js/Object _))

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

(defn telemetry-snapshot
  "Computes a real-time diagnostics snapshot of WebAudio hardware clock, latency and drift."
  []
  (let [ctx          (when (:tone @engine-ctx) (.-context tone))
        transport    (.-Transport tone)
        raw-ctx      (when ctx (.-rawContext ctx))
        sample-rate  (when raw-ctx (.-sampleRate raw-ctx))
        base-lat     (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (sec->ms (.-baseLatency raw-ctx)))
        lookahead    (when ctx (sec->ms (.-lookAhead ctx)))
        ctx-state    (if ctx (.-state ctx) "uninitialized")
        raw-pos      (when transport (str (.-position transport)))
        pos          (if raw-pos (first (str/split raw-pos #"\.")) "0:0:0")
        bpm-val      (if transport (.. transport -bpm -value) (:bpm @audio-state cfg/default-bpm))
        bpm-str      (if (number? bpm-val) (.toFixed bpm-val 0) (str bpm-val))
        transport-st (if transport (.-state transport) "stopped")
        tone-now     (tone/now)
        sys-now      (ms->sec (.now js/performance))
        drift        (if-let [{:keys [t-tone t-sys]} (:clock-sample @audio-state)]
                       (let [dt-tone (- tone-now t-tone)
                             dt-sys  (- sys-now t-sys)
                             d-ms    (sec->ms (- dt-tone dt-sys))]
                         (swap! audio-state assoc :clock-sample {:t-tone tone-now :t-sys sys-now})
                         (str (if (pos? d-ms) "+" "") (.toFixed d-ms 3) " ms"))
                       (do
                         (swap! audio-state assoc :clock-sample {:t-tone tone-now :t-sys sys-now})
                         "0.000 ms (calibrated)"))]
    {:ctx-state       ctx-state
     :sample-rate     sample-rate
     :base-latency    base-lat
     :lookahead       lookahead
     :latency-hint    (when ctx (.-latencyHint ctx))
     :bpm             bpm-val
     :bpm-str         bpm-str
     :position        pos
     :transport-state transport-st
     :tone-now        tone-now
     :drift           drift}))

(defn audio-status
  "Print WebAudio context status, loop count, latency and clock drift."
  []
  (let [{:keys [ctx-state sample-rate base-latency lookahead latency-hint
                bpm position transport-state tone-now drift]} (telemetry-snapshot)
        tracks-map   (:active-tracks @audio-state)
        active-count (count tracks-map)
        tracks-desc  (format-track-summary tracks-map)]
    (js/console.log
     (str/join "\n"
               ["--- WebAudio Engine Diagnostics ---"
                (str "State:          " ctx-state)
                (str "Sample Rate:    " (if sample-rate (str sample-rate " Hz") "N/A"))
                (str "Hardware Clock: " (if (number? tone-now) (str (.toFixed tone-now 4) " s") "N/A"))
                (str "Clock Drift:    " drift)
                (str "Base Latency:   " (if base-latency (str (.toFixed base-latency 2) " ms") "unavailable"))
                (str "Lookahead:      " (if lookahead (str (.toFixed lookahead 1) " ms") "N/A"))
                (str "Latency Hint:   " (or latency-hint "N/A"))
                (str "Transport:      " (if bpm (str (str/upper-case transport-state) " @ " bpm " BPM (Pos: " position ")") "N/A"))
                (str "Active Loops:   " tracks-desc)
                "-----------------------------------"]))
    {:state        ctx-state
     :sample-rate  sample-rate
     :clock        tone-now
     :clock-drift  drift
     :base-latency base-latency
     :lookahead    lookahead
     :bpm          bpm
     :position     position
     :active-loops active-count
     :tracks       (keys tracks-map)}))