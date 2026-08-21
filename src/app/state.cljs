(ns app.state
  (:require [reagent.core :as r]))

(defonce state
  (r/atom {:active?      false
           :bpm          132
           :pulse        0.0
           :hud-visible? true
           :mesh-type    :torus-knot
           :wireframe?   true
           :bg-color     "#050510"
           :mesh-color   "#00ffcc"
           :wire-color   "#ff007f"
           :sensitivity  1.6
           :camera-speed 0.005}))

(defonce visual-pulse (atom 0.0))

(defonce three-ctx (atom nil))
(defonce tone-ctx (atom nil))
(defonce last-clock-sample (atom nil))

(defn pulse!
  ([] (pulse! 1.5))
  ([intensity] (reset! visual-pulse intensity)))
