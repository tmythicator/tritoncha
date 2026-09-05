(ns app.audio.control.looper
  "Live looper, scheduler and master transport engine driving the Rust WASM audio core."
  (:require [app.audio.control.scheduler :as sched]
            [app.audio.dsp.busses :as busses]
            [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-state]]
            [app.utils.audio :as audio-utils]
            [app.utils.math :refer [clamp]]
            [reagent.core :as r]))

(def ^:private click-pattern
  {:inst :click
   :notes ["C6" "G5" "G5" "G5"]
   :step "4n"
   :dur "32n"
   :vel 0.85})

(defn track-slot
  "Returns the hardware sequencer track slot index for a track keyword."
  [track-key]
  (worklet/track-slot track-key))

(defn sync-track-to-worklet!
  "Sends normalized pattern data to the Rust WASM sequencer."
  [tk pat-data]
  (let [slot     (worklet/get-or-assign-track-slot! tk)
        inst-k   (or (:inst pat-data) (:synth pat-data) tk)
        hits     (or (:notes pat-data) (:hits-vec pat-data) [true])
        notes    (if (sequential? hits) hits [hits])
        step-m   (audio-utils/step->mult (:step pat-data))
        bpm      (:bpm @audio-state 168)
        dur-raw  (or (:dur pat-data) (:duration pat-data) (:step pat-data) "16n")
        dur-s    (audio-utils/dur->seconds dur-raw bpm)]
    (worklet/set-track! slot inst-k (vec notes) step-m dur-s)))

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

(worklet/on-worklet-ready! sync-all-active-tracks!)

(defn loop!
  "Schedules or hot-swaps an audio loop track in the live-coding session.
  Examples: (loop! :bass {:notes (d [1 2 3]) :step \"16n\"})."
  [track-name pattern-map]
  (init-audio!)
  (let [tk       (keyword track-name)
        pat-data (sched/normalize-pattern-data tk pattern-map)
        inst-k   (or (:inst pat-data) (:synth pat-data) tk)]
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

(defn set-bpm!
  "Updates the master tempo in BPM.
  Examples: (set-bpm! 174)."
  [bpm]
  (let [clamped-bpm (clamp bpm cfg/min-bpm cfg/max-bpm)]
    (worklet/set-bpm! clamped-bpm)
    (swap! audio-state assoc :bpm clamped-bpm)
    clamped-bpm))

(defn stop-loop!
  "Stops and removes active tracks by keyword(s).
  Examples: (stop-loop! :arp), (stop-loop! :kick :snare :hat)."
  [& track-keys]
  (let [kw-set (set (map keyword (flatten track-keys)))]
    (doseq [tk kw-set]
      (when-let [slot (worklet/track-slot tk)]
        (worklet/deactivate-track! slot)
        (swap! worklet/track-slot-assignments dissoc tk)))
    (swap! audio-state update :active-tracks #(apply dissoc % kw-set))
    (when (empty? (:active-tracks @audio-state))
      (worklet/set-playing! false)
      (swap! audio-state assoc :active? false :transport-start nil))
    (vec kw-set)))

(def unstack! stop-loop!)

(defn clear-loops!
  "Stops and deletes all active loops."
  []
  (worklet/clear-tracks!)
  (swap! audio-state assoc :active-tracks {})
  :cleared)

(defn stop!
  "Stops playback and cancels all active loops."
  []
  (worklet/set-playing! false)
  (clear-loops!)
  (swap! audio-state assoc :active? false :solo-mode? false :transport-start nil)
  :stopped)

(defn toggle-click!
  "Toggles a studio metronome click in headphones/master.
  Examples: (toggle-click!)."
  []
  (if (contains? (:active-tracks @audio-state) :click)
    (do
      (stop-loop! :click)
      :click-off)
    (do
      (loop! :click click-pattern)
      :click-on)))

(defn stack!
  "Launches multiple live loops simultaneously from variadic vectors, track pairs, or a track map.
  Examples:
    (stack!
      [:kick  (pat \"k . . .  k . . .\")]
      [:snare (pat \". . . .  s . . .\")])
    (stack! {:kick (pat \"k . . .\") :snare (pat \"s . . .\")})"
  [& args]
  (let [first-arg (first args)
        pairs     (cond
                    (map? first-arg) first-arg
                    (and (= 1 (count args)) (vector? (first first-arg))) first-arg
                    :else args)
        tks       (into #{} (map first) pairs)]
    (when (some (fn [k] (and (busses/drum? k) (not= k :drums))) tks)
      (stop-loop! :drums))
    (when (contains? tks :drums)
      (doseq [k (filter (fn [k] (and (busses/drum? k) (not= k :drums)))
                        (keys (:active-tracks @audio-state)))]
        (stop-loop! k)))
    (doseq [[k spec] pairs]
      (when (and k spec) (loop! k spec)))
    (mapv first pairs)))
