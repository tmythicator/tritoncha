(ns app.perf
  "Standalone audio engine stress test, latency profiler, and x-run risk benchmark."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.theory.harmony :as harmony :refer [_]]
            [app.audio.theory.patterns :as patterns]
            [app.utils.math :as math :refer [db->gain]]))

(defn- now-ms
  "Returns high-resolution timestamp in milliseconds."
  []
  (if (and (exists? js/process) (exists? (.-hrtime js/process)))
    (let [hr (.hrtime js/process)]
      (+ (* (aget hr 0) 1000.0) (/ (aget hr 1) 1000000.0)))
    (if (and (exists? js/performance) (exists? (.-now js/performance)))
      (.now js/performance)
      (.getTime (js/Date.)))))

(defn- get-heap-mb
  "Returns current Node.js heap usage in Megabytes."
  []
  (if (and (exists? js/process) (exists? (.-memoryUsage js/process)))
    (/ (.. js/process memoryUsage -heapUsed) 1048576.0)
    0.0))

(defn- create-stress-tracks
  "Creates an intensive 16-track scenario mimicking a full Algorave performance."
  []
  [{:id :kick   :notes (patterns/pattern "k . . .  k . . .  . . k .  . . . .") :step "16n"}
   {:id :snare  :notes (patterns/pattern ". . . .  s . . .  . . . .  s . . .") :step "16n"}
   {:id :sn-gh  :notes (patterns/euclid 11 16 "G3") :step "16n"}
   {:id :hh-c   :notes (harmony/deg :e :phrygian [1 _ 1 _ 1 _ 1 _ 1 _ 1 _ 1 _ 1 _] {:octave 4}) :step "16n"}
   {:id :hh-o   :notes (patterns/pattern ". . h .  . . h .  . . h .  . . h .") :step "16n"}
   {:id :bass   :notes (harmony/deg :e :phrygian [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _] {:octave 1}) :step "16n"}
   {:id :sub    :notes (harmony/deg :e :phrygian [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _] {:octave 0}) :step "16n"}
   {:id :lead   :notes (harmony/arp (harmony/chord :e :min9 3) :up-down) :step "16n"}
   {:id :acid   :notes (harmony/deg :e :phrygian [1 2 _ 1 4 3 _ 1  5 _ 4 2 7 6 5 4] {:octave 2}) :step "16n"}
   {:id :pad    :notes [(harmony/chord :e :min9 3) (harmony/chord :a :min7 3) (harmony/chord :c :maj7 3) (harmony/chord :b :dom7 3)] :step "1m"}
   {:id :poly   :notes (harmony/arp (harmony/chord :e :min9 4) :random) :step "16n"}
   {:id :drone  :notes ["E1"] :step "1m"}
   {:id :fm     :notes (patterns/pattern "g . . .  . . g .  . g . .  . . . .") :step "16n"}
   {:id :stab   :notes (harmony/deg :e :phrygian [1 _ _ _ _ _ 3 _  _ _ 5 _ _ _ 7 _] {:octave 3}) :step "16n"}
   {:id :click  :notes ["C6" "G5" "G5" "G5"] :step "4n"}
   {:id :perc   :notes (patterns/euclid 7 16 "A4") :step "16n"}])

(defn- simulate-track-event!
  "Simulates triggering a single track's note/chord event, counting discrete audio sub-hits."
  [{:keys [id notes]} step total-events]
  (let [note-idx (mod step (count notes))
        note     (nth notes note-idx)
        _bus-key (busses/instrument-bus id)
        _gain    (db->gain -3.0)]
    (when note
      (swap! total-events inc)
      (when (vector? note)
        (doseq [sub-n note]
          (when sub-n (swap! total-events inc)))))))

(defn- simulate-tick!
  "Executes a complete tick across all 16 parallel tracks."
  [tracks step total-events]
  (doseq [track tracks]
    (simulate-track-event! track step total-events)))

(defn- calculate-percentiles
  "Computes sorted percentiles (P50, P95, P99, Max) from a latency sample vector."
  [latencies]
  (let [sorted (sort latencies)
        n      (count sorted)]
    {:p50 (nth sorted (int (* n 0.50)))
     :p95 (nth sorted (int (* n 0.95)))
     :p99 (nth sorted (int (* n 0.99)))
     :max (last sorted)}))

(defn- assess-xrun-risk
  "Calculates estimated x-run risk percentage and status string based on P99 spike latency."
  [p99-us]
  (cond
    (> p99-us 2000.0) {:pct 95.0 :status "CRITICAL"}
    (> p99-us 1000.0) {:pct 45.0 :status "HIGH_JITTER"}
    (> p99-us 500.0)  {:pct 10.0 :status "ELEVATED"}
    (> p99-us 200.0)  {:pct 1.0  :status "STABLE"}
    :else             {:pct 0.1  :status "OPTIMAL"}))

(defn- compute-summary
  "Assembles all benchmark measurements into a comprehensive telemetry report."
  [{:keys [total-steps num-tracks total-events start-time end-time initial-heap final-heap latencies]}]
  (let [bpm                   180
        step-seconds          (/ 60.0 bpm 4.0)
        simulated-realtime-s  (* total-steps step-seconds)
        total-time-ms         (- end-time start-time)
        total-time-s          (/ total-time-ms 1000.0)
        percentiles           (calculate-percentiles (vec latencies))
        p99                   (:p99 percentiles)
        quantum-budget-us     2900.0
        headroom-pct          (* (- 1.0 (/ p99 quantum-budget-us)) 100.0)
        risk                  (assess-xrun-risk p99)]
    {:total-steps          total-steps
     :total-tracks         num-tracks
     :total-events         @total-events
     :total-time-ms        total-time-ms
     :simulated-realtime-s simulated-realtime-s
     :realtime-factor      (if (pos? total-time-s) (/ simulated-realtime-s total-time-s) 0.0)
     :mean-step-us         (/ (* total-time-ms 1000.0) total-steps)
     :mean-event-us        (/ (* total-time-ms 1000.0) @total-events)
     :p50-us               (:p50 percentiles)
     :p95-us               (:p95 percentiles)
     :p99-us               p99
     :max-us               (:max percentiles)
     :quantum-budget-us    quantum-budget-us
     :headroom-pct         headroom-pct
     :initial-heap-mb      initial-heap
     :final-heap-mb        final-heap
     :heap-delta-mb        (- final-heap initial-heap)
     :xrun-risk-pct        (:pct risk)
     :status-verdict       (:status risk)}))

(defn run-benchmark
  "Executes a high-density scheduler simulation across 16 parallel tracks for N steps.
  Returns detailed performance telemetry map."
  ([] (run-benchmark 10000))
  ([total-steps]
   (let [tracks       (create-stress-tracks)
         num-tracks   (count tracks)
         batch-size   50
         num-batches  (int (/ total-steps batch-size))
         latencies    (make-array num-batches)
         total-events (atom 0)
         initial-heap (get-heap-mb)
         start-time   (now-ms)]

     (dotimes [b num-batches]
       (let [batch-start (now-ms)]
         (dotimes [s batch-size]
           (let [step (+ (* b batch-size) s)]
             (simulate-tick! tracks step total-events)))
         (let [batch-duration-ms (- (now-ms) batch-start)
               step-us           (/ (* batch-duration-ms 1000.0) batch-size)]
           (aset latencies b step-us))))

     (let [end-time   (now-ms)
           final-heap (get-heap-mb)]
       (compute-summary {:total-steps total-steps
                         :num-tracks  num-tracks
                         :total-events total-events
                         :start-time  start-time
                         :end-time    end-time
                         :initial-heap initial-heap
                         :final-heap  final-heap
                         :latencies   latencies})))))

(defn format-report
  "Formats benchmark telemetry into a clean CLI report."
  [m]
  (str
   "\n--- Audio Scheduler Stress Benchmark ---\n"
   "Tracks:           " (:total-tracks m) " parallel tracks @ 180 BPM\n"
   "Steps:            " (:total-steps m) " (" (.toFixed (:simulated-realtime-s m) 1) "s simulated playback)\n"
   "Total Events:     " (:total-events m) " note triggers\n"
   "Wall Time:        " (.toFixed (:total-time-ms m) 2) " ms\n"
   "Real-time Factor: " (.toFixed (:realtime-factor m) 1) "x\n"
   "Mean Time / Step: " (.toFixed (:mean-step-us m) 2) " us\n"
   "Mean Time / Event:" (.toFixed (:mean-event-us m) 2) " us\n"
   "Latency P50:      " (.toFixed (:p50-us m) 2) " us\n"
   "Latency P95:      " (.toFixed (:p95-us m) 2) " us\n"
   "Latency P99:      " (.toFixed (:p99-us m) 2) " us\n"
   "Latency Max:      " (.toFixed (:max-us m) 2) " us\n"
   "Quantum Headroom: " (.toFixed (:headroom-pct m) 2) " % (budget: " (.toFixed (:quantum-budget-us m) 0) " us)\n"
   "Heap Delta:       " (.toFixed (:heap-delta-mb m) 2) " MB\n"
   "Status:           " (:status-verdict m) " (x-run risk: " (.toFixed (:xrun-risk-pct m) 2) " %)\n"
   "-----------------------------------------\n"))

(defn main []
  (let [result (run-benchmark 10000)]
    (println (format-report result))
    (if (< (:xrun-risk-pct result) 10.0)
      (when (exists? js/process) (.exit js/process 0))
      (when (exists? js/process) (.exit js/process 1)))))
