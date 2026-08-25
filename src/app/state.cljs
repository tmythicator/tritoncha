(ns app.state
  "Source of truth for UI, audio, visuals, REPL and engine states."
  (:require
   [app.config :as cfg]
   [reagent.core :as r]))

(defonce ui-state
  (r/atom {:hud-visible?              true
           :stats-visible?            false
           :tutorial-visible?         false
           :mobile-notice-dismissed?  false}))

(defonce audio-state
  (r/atom {:active?          false
           :bpm              cfg/default-bpm
           :current-jam      :roller
           :current-routing  :default
           :key              cfg/default-key
           :active-tracks    {}
           :solo-mode?       false
           :clock-sample     nil}))

(defonce visual-state
  (r/atom {:current-scene :cyber-torus
           :mesh-type     :torus-knot
           :wireframe?    true
           :colors        {:bg "#050510" :mesh "#00ffcc" :wire "#ff007f"}
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
