(ns app.audio.control.transport
  "Master transport controls, playback lifecycle, tempo clock, and drum synthesis modes."
  (:require [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-state]]
            [app.utils.math :refer [clamp]]))

(defn set-bpm!
  "Updates the master tempo in BPM.
  Examples: (set-bpm! 174)."
  [bpm]
  (let [clamped-bpm (clamp bpm cfg/min-bpm cfg/max-bpm)]
    (worklet/set-bpm! clamped-bpm)
    (swap! audio-state assoc :bpm clamped-bpm)
    clamped-bpm))

(defn set-drum-mode!
  "Configures the global percussion genre style across all active drum tracks.
  Accepts a style keyword (:natural, :idm, :industrial, or custom defstyle!)
  or a weighted style blend map (e.g. {:natural 0.7 :industrial 0.3}).
  Examples: (set-drum-mode! :idm), (mod! {:natural 0.7 :industrial 0.3})."
  [style-spec]
  (let [m (cond
            (map? style-spec) style-spec
            (some? style-spec) (keyword style-spec)
            :else :natural)]
    (swap! audio-state assoc :drum-mode m)
    (doseq [[tk tr] (:active-tracks @audio-state)]
      (let [pat-atom (:pattern tr)
            pat-data (if (satisfies? IDeref pat-atom) @pat-atom pat-atom)]
        (when pat-data
          (worklet/sync-track-to-worklet! tk pat-data))))
    m))

(def mod!
  "Shortcut for set-drum-mode!. Configures drum character mode.
  Examples: (mod! :natural), (mod! :idm), (mod! :industrial)."
  set-drum-mode!)

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
