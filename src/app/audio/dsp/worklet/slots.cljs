(ns app.audio.dsp.worklet.slots
  "Sequencer hardware slot allocator managing 16 real-time playback tracks."
  (:require [app.audio.dsp.busses :refer [sound-category]]
            [clojure.string :as str]))

(defonce track-slot-assignments
  (atom {}))

(defn track-slot
  "Returns the hardware sequencer track slot index for a track keyword.
  Examples: (track-slot :kick) -> 0."
  [track-key]
  (let [tk (keyword track-key)]
    (or (get @track-slot-assignments tk)
        (case (sound-category tk)
          :drums (get @track-slot-assignments :drums)
          :bass  (or (get @track-slot-assignments :bass)
                     (get @track-slot-assignments :sub))
          :pads  (or (get @track-slot-assignments :strings)
                     (get @track-slot-assignments :pad))
          :leads (get @track-slot-assignments :lead)
          nil)
        (when (contains? #{:click :metronome} tk)
          (get @track-slot-assignments :click)))))

(defn all-track-slots
  "Returns the complete map of track keyword to hardware slot assignments.
  Examples: (all-track-slots) -> {:kick 0, :bass 1}."
  []
  @track-slot-assignments)

(defn track-slots-for
  "Returns all sequencer slot indices assigned to a track keyword, including chord voice sub-slots.
  Examples: (track-slots-for :pad) -> [4 5 6 7]."
  [track-key]
  (let [tk   (keyword track-key)
        pfx  (str (name tk) "-v")
        base (track-slot tk)]
    (distinct
     (keep identity
           (concat (when base [base])
                   (keep (fn [[k s]]
                           (when (and (keyword? k) (str/starts-with? (name k) pfx))
                             s))
                         @track-slot-assignments))))))

(defn get-or-assign-track-slot!
  "Retrieves existing track slot or allocates a free hardware slot in 0..15.
  Examples: (get-or-assign-track-slot! :lead) -> 2."
  [track-key]
  (let [tk (keyword track-key)]
    (if-let [slot (get @track-slot-assignments tk)]
      slot
      (let [used (set (vals @track-slot-assignments))
            free (first (filter #(not (contains? used %)) (range 16)))]
        (swap! track-slot-assignments assoc tk (or free 0))
        (or free 0)))))

(defn clear-track-slots!
  "Resets all hardware sequencer slot assignments.
  Examples: (clear-track-slots!)."
  []
  (reset! track-slot-assignments {}))
