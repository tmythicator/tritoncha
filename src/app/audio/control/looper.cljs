(ns app.audio.control.looper
  "Live looper, scheduler and master transport engine."
  (:require [app.audio.control.scheduler :as sched]
            [app.audio.dsp.engine :refer [create-sequence init-audio!]]
            [app.audio.dsp.instruments :as inst]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx]]
            [app.utils.audio :refer [is-drum-track?]]
            [app.utils.math :refer [clamp]]))

(defn- ensure-transport-running! []
  (when-let [t (:transport (:tone @engine-ctx))]
    (when (not= (.-state ^js t) "started")
      (.start ^js t "+0.05"))))

(defn- trigger-hit!
  "Dispatches a single rhythm/melodic hit to Tone.js instrument or drum voice."
  [hit inst-key synth-node dur time vel]
  (cond
    (and (vector? hit) (keyword? (first hit)))
    (let [[k v n] hit
          final-vel (or v vel)]
      (if (is-drum-track? k)
        (inst/trigger-drum! k n dur time final-vel)
        (inst/trigger-note! (or (get (:tone @engine-ctx) k) synth-node) n dur time final-vel k)))

    (and (keyword? hit) (is-drum-track? hit))
    (inst/trigger-drum! hit nil dur time vel)

    (and (true? hit) (is-drum-track? inst-key))
    (inst/trigger-drum! inst-key nil dur time vel)

    :else
    (inst/trigger-note! synth-node hit dur time vel inst-key)))

(defn- resolve-track-synth
  "Pure helper determining the target synthesizer node and instrument key."
  [track-key pat-data]
  (let [inst-k (or (:inst pat-data) (:synth pat-data) track-key)
        synth  (or (get (:tone @engine-ctx) inst-k) (get (:tone @engine-ctx) :saw-bass))]
    [inst-k synth]))

(defn- execute-step-callback!
  "Zero-allocation step callback executed on each quantization tick of Tone.Sequence."
  [track-info time step-idx synth-node inst-key]
  (let [pat @(:pattern track-info)]
    (when (sched/track-audible? pat (:solo-mode? @audio-state))
      (when-let [{:keys [hit vel dur]} (sched/calculate-step-hit pat step-idx)]
        (trigger-hit! hit inst-key synth-node dur time vel)))))

(defn- create-track-sequence
  "Helper instantiating a new Tone.Sequence for a track."
  [track-info synth inst-k step]
  (create-sequence #(execute-step-callback! track-info %1 %2 synth inst-k)
                   (into-array (range cfg/sequence-length))
                   step))

(defn loop!
  "Schedules or hot-swaps an audio loop track in the live-coding session.
  Examples: (loop! :bass {:notes (d [1 2 3]) :step \"16n\"})."
  [track-name pattern-map]
  (init-audio!)
  (let [tk             (keyword track-name)
        pat-data       (sched/normalize-pattern-data tk pattern-map)
        step           (:step pat-data)
        [inst-k synth] (resolve-track-synth tk pat-data)]
    (if-let [tr (get (:active-tracks @audio-state) tk)]
      (let [old-pat  @(:pattern tr)
            old-step (:step old-pat)
            old-inst (:inst-key tr)]
        (swap! (:pattern tr) merge (assoc pat-data :muted? (:muted? old-pat false) :solo? (:solo? old-pat false)))
        (if (or (not= step old-step) (not= inst-k old-inst))
          (do
            (when-let [s (:sequence tr)]
              (try (.dispose ^js s) (catch js/Object _)))
            (let [tr-info (assoc tr :synth synth :inst-key inst-k)
                  seq-obj (create-track-sequence tr-info synth inst-k step)]
              (swap! audio-state assoc-in [:active-tracks tk] (assoc tr-info :sequence seq-obj))))
          (when (not= synth (:synth tr))
            (swap! audio-state assoc-in [:active-tracks tk :synth] synth))))
      (let [pat-atom (atom (assoc pat-data :muted? false :solo? false))
            tr-info  {:pattern pat-atom :synth synth :inst-key inst-k}
            seq-obj  (create-track-sequence tr-info synth inst-k step)]
        (swap! audio-state assoc-in [:active-tracks tk] (assoc tr-info :sequence seq-obj))))
    (ensure-transport-running!)
    (swap! audio-state assoc :active? true)
    tk))

(defn set-bpm!
  "Updates the master tempo in BPM.
  Examples: (set-bpm! 174)."
  [bpm]
  (let [clamped-bpm (clamp bpm cfg/min-bpm cfg/max-bpm)]
    (when-let [t (:transport (:tone @engine-ctx))]
      (set! (.. ^js t -bpm -value) clamped-bpm))
    (swap! audio-state assoc :bpm clamped-bpm)
    clamped-bpm))

(defn stop-loop!
  "Stops and removes active tracks by keyword(s).
  Examples: (stop-loop! :arp), (stop-loop! :kick :snare :hat)."
  [& track-keys]
  (let [tks (flatten track-keys)]
    (doseq [track-key tks]
      (let [tk (keyword track-key)]
        (when-let [tr (get (:active-tracks @audio-state) tk)]
          (when-let [s (:sequence tr)]
            (try (.dispose ^js s) (catch js/Object _)))
          (swap! audio-state update :active-tracks dissoc tk))))
    (vec tks)))

(def unstack! stop-loop!)

(defn clear-loops!
  "Stops and deletes all active loops."
  []
  (doseq [[_ tr] (:active-tracks @audio-state)]
    (when-let [s (:sequence tr)]
      (try (.dispose ^js s) (catch js/Object _))))
  (swap! audio-state assoc :active-tracks {})
  :cleared)

(defn stop!
  "Stops Tone.js Transport and cancels all active loops."
  []
  (when-let [t (:transport (:tone @engine-ctx))]
    (try
      (.stop ^js t)
      (.cancel ^js t)
      (catch js/Object _)))
  (clear-loops!)
  (swap! audio-state assoc :active? false :solo-mode? false)
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
      (loop! :click
             {:inst :click
              :notes ["C6" "G5" "G5" "G5"]
              :step "4n"
              :dur "32n"
              :vel 0.4})
      :click-on)))

(defn- parse-stack-args
  "Transforms variadic arguments, pair vectors, or track maps into a canonical vector of [key spec] pairs."
  [args]
  (cond
    (and (= 1 (count args)) (map? (first args)))
    (vec (first args))

    (and (= 1 (count args)) (vector? (first (first args))))
    (first args)

    :else
    (vec args)))

(defn stack!
  "Launches multiple live loops simultaneously from variadic vectors or a track map.
  Examples:
    (stack!
      [:kick  (pat \"k . . .  k . . .\")]
      [:snare (pat \". . . .  s . . .\")])"
  [& args]
  (let [pairs (parse-stack-args args)
        tks   (set (map first pairs))]
    (when (some #{:kick :snare :hat} tks) (stop-loop! :drums))
    (when (contains? tks :drums) (stop-loop! :kick :snare :hat))
    (doseq [[k spec] pairs]
      (when (and k spec) (loop! k spec)))
    (mapv first pairs)))
