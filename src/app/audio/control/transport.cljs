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
  "Configures the character synthesis mode across all drum voices in Rust WASM.
  Supported modes: :natural, :analog, :idm, :industrial.
  Examples: (set-drum-mode! :idm), (set-drum-mode! :natural)."
  [mode-kw]
  (worklet/set-drum-mode! mode-kw)
  mode-kw)

(def mod!
  "Shortcut for set-drum-mode!. Configures drum character mode.
  Examples: (mod! :idm), (mod! :analog), (mod! :natural)."
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
