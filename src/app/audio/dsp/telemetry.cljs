(ns app.audio.dsp.telemetry
  "WebAudio hardware clock, latency diagnostics, clock drift calculation and status."
  (:require [app.config :as cfg]
            [app.state :refer [audio-metrics audio-state engine-ctx]]
            [app.utils.dom :as dom]
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
        ^js native-ctx (or (when raw-ctx (.-_nativeAudioContext raw-ctx)) raw-ctx ctx)
        sample-rate   (when native-ctx (.-sampleRate native-ctx))
        base-lat      (when (and native-ctx (number? (.-baseLatency native-ctx))) (sec->ms (.-baseLatency native-ctx)))
        out-lat       (when (and native-ctx (number? (.-outputLatency native-ctx))) (sec->ms (.-outputLatency native-ctx)))
        lookahead     (when ctx (sec->ms (.-lookAhead ctx)))
        ctx-state     (if ctx (.-state ctx) "uninitialized")
        raw-pos       (when transport (str (.-position transport)))
        pos           (if raw-pos (first (str/split raw-pos #"\.")) "0:0:0")
        bpm-val       (if transport (.. transport -bpm -value) (:bpm @audio-state cfg/default-bpm))
        bpm-str       (if (number? bpm-val) (.toFixed bpm-val 0) (str bpm-val))
        transport-st  (if transport (.-state transport) "stopped")
        tone-now      (if raw-ctx (.-currentTime raw-ctx) 0.0)
        sys-now       (if (exists? js/performance) (ms->sec (.now js/performance)) 0.0)

        drift-str     (if-let [{:keys [t-tone-start t-sys-start]} (:clock-origin @audio-metrics)]
                        (if (pos? tone-now)
                          (let [elapsed-tone (- tone-now t-tone-start)
                                elapsed-sys  (- sys-now t-sys-start)
                                drift-ms     (sec->ms (- elapsed-tone elapsed-sys))]
                            (str (if (pos? drift-ms) "+" "") (.toFixed drift-ms 3) " ms"))
                          "0.000 ms")
                        (do
                          (when (pos? tone-now)
                            (swap! audio-metrics assoc :clock-origin {:t-tone-start tone-now :t-sys-start sys-now}))
                          "0.000 ms (calibrated)"))

        lookahead-ms  (or lookahead (sec->ms (dom/active-lookahead)) 100.0)
        min-headroom  (or (:min-headroom-ms @audio-metrics) lookahead-ms)
        xruns         (:xrun-count @audio-metrics 0)]
    {:ctx-state       ctx-state
     :sample-rate     sample-rate
     :base-latency    base-lat
     :output-latency  out-lat
     :lookahead       lookahead
     :latency-hint    (or (when ctx (.-latencyHint ctx))
                          (when raw-ctx (.-latencyHint raw-ctx)))
     :bpm             bpm-val
     :bpm-str         bpm-str
     :position        pos
     :transport-state transport-st
     :hardware-clock  tone-now
     :clock-drift     drift-str
     :headroom-ms     lookahead-ms
     :min-headroom-ms min-headroom
     :xrun-count      xruns
     :active-tracks   (format-active-loops-summary (:active-tracks @audio-state))}))

(defn reset-telemetry-metrics!
  "Resets accumulated peak jitter, clock origin, and x-run glitch counters."
  []
  (swap! audio-metrics assoc
         :clock-origin nil
         :min-headroom-ms nil
         :xrun-count 0)
  :reset)

(defn audio-status
  "Prints WebAudio hardware telemetry, clock drift, and active loop status to console."
  []
  (let [snap (telemetry-snapshot)]
    (println "--- WebAudio Engine Diagnostics ---")
    (println (str "State:          " (:ctx-state snap)))
    (println (str "Sample Rate:    " (if-let [sr (:sample-rate snap)] (str sr " Hz") "N/A")))
    (println (str "Hardware Clock: " (.toFixed (:hardware-clock snap) 4) " s"))
    (println (str "Clock Drift:    " (:clock-drift snap)))
    (println (str "Min Headroom:   " (.toFixed (:min-headroom-ms snap) 1) " ms"))
    (println (str "X-Runs (Drops): " (:xrun-count snap)))
    (println (str "Base Latency:   " (if-let [bl (:base-latency snap)] (str (.toFixed bl 2) " ms") "unavailable")))
    (println (str "Lookahead:      " (if-let [la (:lookahead snap)] (str (.toFixed la 1) " ms") "N/A")))
    (println (str "Latency Hint:   " (or (:latency-hint snap) "N/A")))
    (println (str "Transport:      " (str/upper-case (:transport-state snap)) " @ " (:bpm-str snap) " BPM (Pos: " (:position snap) ")"))
    (println (str "Active Loops:   " (:active-tracks snap)))
    (println "-----------------------------------")
    snap))
