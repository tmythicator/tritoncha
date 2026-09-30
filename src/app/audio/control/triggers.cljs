(ns app.audio.control.triggers
  "Audio-to-visual reactive bridge dispatching hardware sequencer step triggers to Three.js."
  (:require [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx pulse!]]))

(defn- hit?
  "Returns true if the sequencer track slot was triggered in the step mask."
  [mask slot]
  (and slot (pos? (bit-and mask (bit-shift-left 1 slot)))))

(defn- deref-or-val
  "Extracts value from IDeref atom or returns raw value."
  [x]
  (if (satisfies? IDeref x) @x x))

(defn- trigger-figure!
  "Emits a visual pulse to a figure bound to the track pattern."
  [{:keys [figure fig muted? vel pulse] :or {vel cfg/default-trigger-velocity}}]
  (let [target (or figure fig)]
    (when (and target (not muted?) (not (#{:none false} target)))
      (pulse! (keyword target) (or pulse vel)))))

(defn- handle-sequencer-triggers!
  "Dispatches hardware sequencer step triggers strictly to bound visual figures."
  [mask]
  (when (pos? mask)
    (let [assignments   @worklet/track-slot-assignments
          active        (:active-tracks @audio-state)
          multi-figure? (seq (get-in @engine-ctx [:three :figures]))]
      ;; Pulse visual figures bound to triggered track slots
      (doseq [[tk slot] assignments
              :when (hit? mask slot)
              :let  [pat (deref-or-val (get-in active [tk :pattern]))]]
        (trigger-figure! pat))
      ;; Pulse default scene background or mesh on kick hits if no multi-figure setup is active
      (when (and (not multi-figure?) (hit? mask (:kick assignments)))
        (pulse! :default cfg/default-kick-pulse)))))

(worklet/on-trigger-event! handle-sequencer-triggers!)
