(ns app.state
  "Source of truth for UI, audio, visuals, REPL and engine states."
  (:require [app.config :as cfg]))

(defonce ui-state
  (atom {:hud-visible?              true
         :stats-visible?            false
         :tutorial-visible?         false
         :mobile-notice-dismissed?  false}))

(defonce audio-state
  (atom {:active?          false
         :bpm              cfg/default-bpm
         :current-jam      :roller
         :current-routing  :default
         :key              cfg/default-key
         :active-tracks    {}
         :solo-mode?       false}))

(defonce audio-metrics
  (atom {:xrun-count      0
         :min-headroom-ms nil
         :clock-origin    nil}))

(defonce visual-state
  (atom {:current-scene cfg/default-scene
         :mesh-type     cfg/default-geometry
         :wireframe?    true
         :bg-color      (:bg cfg/default-scene-colors)
         :mesh-color    (:mesh cfg/default-scene-colors)
         :wire-color    (:wire cfg/default-scene-colors)
         :colors        cfg/default-scene-colors
         :sensitivity   cfg/default-sensitivity
         :camera-speed  cfg/default-camera-speed}))

(defonce visual-pulse (atom 0.0))

(defn pulse!
  "Triggers a visual scale and lighting impulse for 3D shaders."
  ([] (pulse! 1.5))
  ([intensity] (reset! visual-pulse (max @visual-pulse intensity))))

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
