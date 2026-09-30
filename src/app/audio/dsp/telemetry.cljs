(ns app.audio.dsp.telemetry
  "Hardware audio clock, WASM buffer latency diagnostics, clock drift calculation and status."
  (:require [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-metrics audio-state]]
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

(defn- calculate-transport-position [active? bpm hw-now start-time]
  (if (and active? (number? hw-now) (number? start-time) (pos? start-time) (>= hw-now start-time))
    (let [elapsed      (- hw-now start-time)
          bpm-safe     (max (or bpm 168.0) 1.0)
          sec-per-16th (/ 15.0 bpm-safe)
          total-16ths  (Math/floor (/ elapsed sec-per-16th))
          bar          (Math/floor (/ total-16ths 16.0))
          beat         (Math/floor (/ (mod total-16ths 16.0) 4.0))
          step         (mod total-16ths 4.0)]
      (str bar ":" beat ":" step))
    "0:0:0"))

(defn telemetry-snapshot
  "Computes a real-time diagnostics snapshot of WebAudio hardware clock, WASM latency and drift."
  []
  (let [^js ctx       (worklet/get-audio-context)
        sample-rate   (when ctx (.-sampleRate ctx))
        raw-base-lat  (when (and ctx (number? (.-baseLatency ctx)) (pos? (.-baseLatency ctx)))
                        (sec->ms (.-baseLatency ctx)))
        out-lat       (when (and ctx (number? (.-outputLatency ctx)) (pos? (.-outputLatency ctx)))
                        (sec->ms (.-outputLatency ctx)))
        ctx-state     (if ctx (.-state ctx) "uninitialized")
        bpm-val       (:bpm @audio-state cfg/default-bpm)
        bpm-str       (if (number? bpm-val) (.toFixed bpm-val 0) (str bpm-val))
        active?       (:active? @audio-state false)
        transport-st  (if active? "running" "stopped")
        hw-now        (when ctx (.-currentTime ctx))
        sys-now       (when (exists? js/performance) (ms->sec (.now js/performance)))
        pos           (calculate-transport-position active? bpm-val (or hw-now 0.0) (:transport-start @audio-state))

        drift-str     (if-let [{:keys [t-hw-start t-sys-start]} (:clock-origin @audio-metrics)]
                        (if (and hw-now (pos? hw-now) sys-now)
                          (let [elapsed-hw   (- hw-now t-hw-start)
                                elapsed-sys  (- sys-now t-sys-start)
                                drift-ms     (sec->ms (- elapsed-hw elapsed-sys))]
                            (str (if (pos? drift-ms) "+" "") (.toFixed drift-ms 3) " ms"))
                          "N/A")
                        (if (and hw-now (pos? hw-now) sys-now)
                          (do
                            (swap! audio-metrics assoc :clock-origin {:t-hw-start hw-now :t-sys-start sys-now})
                            "0.000 ms")
                          "N/A"))

        xruns         (:xrun-count @audio-metrics 0)]
    {:engine          "Rust WASM (AudioWorklet)"
     :block-size      128
     :ctx-state       ctx-state
     :sample-rate     sample-rate
     :base-latency    raw-base-lat
     :output-latency  out-lat
     :latency-hint    "interactive"
     :bpm             bpm-val
     :bpm-str         bpm-str
     :position        pos
     :transport-state transport-st
     :hardware-clock  hw-now
     :clock-drift     drift-str
     :min-headroom-ms (:min-headroom-ms @audio-metrics)
     :xrun-count      xruns
     :reverb-mode     (some-> (:reverb-mode @audio-state) name str/upper-case)
     :drive-mode      (some-> (:drive-mode @audio-state) name str/upper-case)
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
    (println (str "Engine:         " (:engine snap)))
    (println (str "DSP Modes:      Reverb: " (or (:reverb-mode snap) "N/A") " | Drive: " (or (:drive-mode snap) "N/A")))
    (println (str "Context:        " (:ctx-state snap)))
    (println (str "Sample Rate:    " (if-let [sr (:sample-rate snap)] (str sr " Hz") "N/A")))
    (println (str "Hardware Clock: " (if-let [hc (:hardware-clock snap)] (str (.toFixed hc 4) " s") "N/A")))
    (println (str "Clock Drift:    " (:clock-drift snap)))
    (println (str "Base Latency:   " (if-let [bl (:base-latency snap)] (str (.toFixed bl 2) " ms") "unavailable")))
    (println (str "X-Runs (Drops): " (:xrun-count snap)))
    (println (str "Transport:      " (str/upper-case (:transport-state snap)) " @ " (:bpm-str snap) " BPM (Pos: " (:position snap) ")"))
    (println (str "Active Loops:   " (:active-tracks snap)))
    (println "-----------------------------------")
    snap))
