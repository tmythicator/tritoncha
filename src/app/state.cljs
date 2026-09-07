(ns app.state
  "Source of truth for UI, audio, visuals, REPL and engine states."
  (:require [app.config :as cfg]
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
           :current-jam      cfg/default-jam
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
