(ns app.audio.control.looper
  "Live looper and track scheduler driving the Rust WASM hardware sequencer."
  (:require [app.audio.control.scheduler :as sched]
            [app.audio.control.transport :as transport]
            [app.audio.control.triggers]
            [app.audio.dsp.busses :as busses]
            [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.instruments :as inst]
            [app.audio.dsp.worklet :as worklet]
            [app.state :refer [audio-state]]
            [app.utils.math :refer [clamp]]
            [reagent.core :as r]))

(defn track-slot
  "Returns the hardware sequencer track slot index for a track keyword."
  [track-key]
  (worklet/track-slot track-key))

(def sync-track-to-worklet!
  "Sends normalized pattern data to the Rust WASM sequencer, supporting velocity and polyphonic chords."
  worklet/sync-track-to-worklet!)

(defn sync-all-active-tracks!
  "Re-transmits all active session tracks to the Rust WASM sequencer."
  []
  (let [active (:active-tracks @audio-state)]
    (doseq [[tk tr] active]
      (let [pat-atom (:pattern tr)
            pat-data (if (satisfies? IDeref pat-atom) @pat-atom pat-atom)]
        (when pat-data
          (sync-track-to-worklet! tk pat-data))))
    (when (and (:active? @audio-state) (seq active))
      (worklet/set-playing! true))))

(defn loop!
  "Schedules or hot-swaps an audio loop track in the live-coding session.
  Examples: (loop! :bass {:notes (d [1 2 3]) :step \"16n\"})."
  [track-name pattern-map]
  (init-audio!)
  (let [tk       (keyword track-name)
        pat-data (sched/normalize-pattern-data tk pattern-map)
        inst-k   (or (:inst pat-data) (:synth pat-data) tk)]
    (let [target-drum (cond (busses/drum? inst-k) inst-k (busses/drum? tk) tk :else nil)]
      (when target-drum
        (when-let [base (inst/resolve-instrument-spec target-drum)]
          (let [spec (if-let [m (:mod pat-data)] (assoc base :mod m) base)]
            (worklet/set-worklet-drum-patch! target-drum spec))))
      (when (and (= target-drum :drums) (:mod pat-data))
        (worklet/set-drum-mode! (:mod pat-data))))
    (if-let [tr (get (:active-tracks @audio-state) tk)]
      (let [old-pat @(:pattern tr)]
        (swap! (:pattern tr) merge (assoc pat-data :muted? (:muted? old-pat false) :solo? (:solo? old-pat false))))
      (let [pat-atom (r/atom (assoc pat-data :muted? false :solo? false))
            tr-info  {:pattern pat-atom :inst-key inst-k}]
        (swap! audio-state assoc-in [:active-tracks tk] tr-info)))
    (sync-track-to-worklet! tk pat-data)
    (worklet/set-playing! true)
    (let [hw-now (if-let [ctx (worklet/get-audio-context)] (.-currentTime ctx) 0.0)]
      (swap! audio-state (fn [st]
                           (cond-> (assoc st :active? true)
                             (nil? (:transport-start st)) (assoc :transport-start hw-now)))))
    tk))

(defn set-track-vel!
  "Sets the velocity or volume multiplier for an active track loop.
  Examples: (set-track-vel! :kick 1.2), (set-track-vel! :bass 0.8)."
  [track-key vel]
  (let [kw (keyword track-key)]
    (when-let [tr (get (:active-tracks @audio-state) kw)]
      (let [old-pat    @(:pattern tr)
            raw-val    (js/parseFloat vel)
            valid-val  (if (js/isNaN raw-val) 0.9 raw-val)
            clamped-v  (clamp valid-val 0.0 2.0)
            new-pat    (assoc old-pat :vel clamped-v)]
        (reset! (:pattern tr) new-pat)
        (sync-track-to-worklet! kw new-pat)
        (swap! audio-state update :tracks-ver (fnil inc 0))))))

(defn stop-loop!
  "Stops and removes active tracks by keyword(s).
  Examples: (stop-loop! :arp), (stop-loop! :kick :snare :hat)."
  [& track-keys]
  (let [kw-set (set (map keyword (flatten track-keys)))]
    (doseq [tk kw-set]
      (doseq [slot (worklet/track-slots-for tk)]
        (worklet/deactivate-track! slot))
      (swap! worklet/track-slot-assignments
             (fn [slots]
               (apply dissoc slots (cons tk (map #(keyword (str (name tk) "-v" %)) (range 1 4)))))))
    (swap! audio-state update :active-tracks #(apply dissoc % kw-set))
    (when (empty? (:active-tracks @audio-state))
      (worklet/set-playing! false)
      (swap! audio-state assoc :active? false :transport-start nil))
    (vec kw-set)))

;; Transport Re-exports
(def clear-loops! transport/clear-loops!)
(def stop! transport/stop!)
(def set-bpm! transport/set-bpm!)
(def set-drum-mode! transport/set-drum-mode!)
(def mod! transport/mod!)
