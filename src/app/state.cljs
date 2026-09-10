(ns app.state
  "Source of truth for UI, audio, visuals, REPL and engine states."
  (:require [app.config :as cfg]
            [app.lib.tracks :refer [core-track-order]]
            [reagent.core :as r]))

(defonce ui-state
  (r/atom {:hud-visible?              true
           :stats-visible?            false
           :tutorial-visible?         false
           :track-browser-open?       false
           :instrument-browser-open?  false
           :mobile-notice-dismissed?  false}))

(defonce audio-state
  (r/atom {:active?          false
           :bpm              cfg/default-bpm
           :current-jam      (first core-track-order)
           :current-routing  :default
           :reverb-mode      :fdn
           :drive-mode       :adaa
           :key              cfg/default-key
           :active-tracks    {}
           :solo-mode?       false}))

(defonce audio-metrics
  (atom {:xrun-count      0
         :min-headroom-ms nil
         :clock-origin    nil}))

(defonce visual-state
  (r/atom {:current-scene cfg/default-scene
           :mesh-type     cfg/default-geometry
           :wireframe?    true
           :bg-color      (:bg cfg/default-scene-colors)
           :mesh-color    (:mesh cfg/default-scene-colors)
           :wire-color    (:wire cfg/default-scene-colors)
           :colors        cfg/default-scene-colors
           :sensitivity   cfg/default-sensitivity
           :camera-speed  cfg/default-camera-speed}))

(defonce visual-pulse (atom 0.0))
(defonce visual-pulses (atom {:default 0.0}))

(defn pulse!
  "Triggers a visual scale and lighting impulse for 3D shaders or individual figures.
  Examples: (pulse!), (pulse! 2.0), (pulse! :core 2.5), (pulse! :halo 1.8)."
  ([] (pulse! :default 1.5))
  ([arg]
   (if (number? arg)
     (pulse! :default arg)
     (pulse! arg 1.5)))
  ([fig-key intensity]
   (let [k (if (some? fig-key) (keyword fig-key) :default)
         i (float (or intensity 1.5))]
     (swap! visual-pulses (fn [m] (assoc m k (max (get m k 0.0) i))))
     (when (or (= k :default) (= k :all))
       (reset! visual-pulse (max @visual-pulse i))))))

(defn clear-pulses!
  "Resets all individual figure pulses and the main visual pulse to 0.0.
  Examples: (clear-pulses!)."
  []
  (reset! visual-pulse 0.0)
  (reset! visual-pulses {:default 0.0}))

(defonce repl-registry
  (atom {:tracks      {}
         :instruments {}
         :routes      {}
         :scenes      {}}))

(defonce engine-ctx
  (atom {:tone   nil
         :three  nil
         :events false
         :root   nil}))

;; Domain State Selectors (Dependency Inversion Principle)

(defn select-active-tracks
  "Returns map of active session tracks from audio state."
  [st]
  (:active-tracks st {}))

(defn select-bpm
  "Returns active tempo in BPM from audio state."
  [st]
  (:bpm st cfg/default-bpm))

(defn select-key-info
  "Returns active key context map from audio state."
  [st]
  (:key st cfg/default-key))

(defn select-active?
  "Returns true if master audio transport is currently active."
  [st]
  (boolean (:active? st false)))

(defn select-current-jam
  "Returns keyword ID of currently loaded track preset."
  [st]
  (:current-jam st (first core-track-order)))

(defn select-current-routing
  "Returns keyword ID of active audio routing topology."
  [st]
  (:current-routing st :default))

(defn select-reverb-mode
  "Returns active reverb DSP mode (:fdn or :freeverb)."
  [st]
  (:reverb-mode st :fdn))

(defn select-drive-mode
  "Returns active master saturation mode (:tan-h, :soft, :adaa)."
  [st]
  (:drive-mode st :adaa))

(defn select-hud-visible?
  "Returns true if track loop HUD overlay is visible."
  [st]
  (boolean (:hud-visible? st true)))

(defn select-stats-visible?
  "Returns true if telemetry stats modal overlay is visible."
  [st]
  (boolean (:stats-visible? st false)))

(defn select-tutorial-visible?
  "Returns true if tutorial modal overlay is visible."
  [st]
  (boolean (:tutorial-visible? st false)))

(defn select-track-browser-open?
  "Returns true if track browser modal is open."
  [st]
  (boolean (:track-browser-open? st false)))

(defn select-instrument-browser-open?
  "Returns true if instrument browser overlay is open."
  [st]
  (boolean (:instrument-browser-open? st false)))

