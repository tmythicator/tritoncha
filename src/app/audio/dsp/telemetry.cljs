(ns app.audio.dsp.telemetry
  "WebAudio hardware clock, latency diagnostics, clock drift calculation and status."
  (:require [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx]]
            [app.utils.math :refer [ms->sec sec->ms]]
            [clojure.string :as str]))

(defn- format-active-loops-summary [tracks]
  (if (empty? tracks)
    "0 (idle)"
    (let [details (for [[kw info] tracks]
                    (let [pat    @(:pattern info)
                          muted? (boolean (:muted? pat))
                          solo?  (boolean (:solo? pat))
                          step   (or (:step pat) cfg/default-step)
                          inst   (or (:inst pat) kw)
                          status (cond solo? "[SOLO]" muted? "[MUTED]" :else "live")]
                      (str (name kw) " (" (name inst) ", " step ", " status ")")))]
      (str (count tracks) " active -> [" (str/join ", " details) "]"))))

(defn telemetry-snapshot
  "Computes a real-time diagnostics snapshot of WebAudio hardware clock, latency and drift."
  []
  (let [tone-engine   (:tone @engine-ctx)
        ^js transport (when tone-engine (:transport tone-engine))
        ^js ctx       (when transport (.-context transport))
        ^js raw-ctx   (when ctx (.-rawContext ctx))
        sample-rate   (when raw-ctx (.-sampleRate raw-ctx))
        base-lat      (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (sec->ms (.-baseLatency raw-ctx)))
        lookahead     (when ctx (sec->ms (.-lookAhead ctx)))
        ctx-state    (if ctx (.-state ctx) "uninitialized")
        raw-pos      (when transport (str (.-position transport)))
        pos          (if raw-pos (first (str/split raw-pos #"\.")) "0:0:0")
        bpm-val      (if transport (.. transport -bpm -value) (:bpm @audio-state cfg/default-bpm))
        bpm-str      (if (number? bpm-val) (.toFixed bpm-val 0) (str bpm-val))
        transport-st (if transport (.-state transport) "stopped")
        tone-now     (if ctx
                       (try (if (fn? (.-now ctx)) (.now ctx) 0.0) (catch js/Object _ 0.0))
                       0.0)
        sys-now      (if (exists? js/performance)
                       (ms->sec (.now js/performance))
                       0.0)
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
     :hardware-clock  tone-now
     :clock-drift     drift
     :active-tracks   (format-active-loops-summary (:active-tracks @audio-state))}))

(defn audio-status
  "Prints WebAudio hardware telemetry, clock drift, and active loop status to console."
  []
  (let [snap (telemetry-snapshot)]
    (println "--- WebAudio Engine Diagnostics ---")
    (println (str "State:          " (:ctx-state snap)))
    (println (str "Sample Rate:    " (if-let [sr (:sample-rate snap)] (str sr " Hz") "N/A")))
    (println (str "Hardware Clock: " (.toFixed (:hardware-clock snap) 4) " s"))
    (println (str "Clock Drift:    " (:clock-drift snap)))
    (println (str "Base Latency:   " (if-let [bl (:base-latency snap)] (str (.toFixed bl 2) " ms") "unavailable")))
    (println (str "Lookahead:      " (if-let [la (:lookahead snap)] (str (.toFixed la 1) " ms") "N/A")))
    (println (str "Latency Hint:   " (or (:latency-hint snap) "N/A")))
    (println (str "Transport:      " (str/upper-case (:transport-state snap)) " @ " (:bpm-str snap) " BPM (Pos: " (:position snap) ")"))
    (println (str "Active Loops:   " (:active-tracks snap)))
    (println "-----------------------------------")
    snap))
