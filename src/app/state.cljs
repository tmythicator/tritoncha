(ns app.state
  "Single source of truth for app state atoms and UI atom."
  (:require [reagent.core :as r]))

(defonce state
  (r/atom {:active?      false
           :bpm          132
           :pulse        0.0
           :hud-visible?   true
           :stats-visible? false
           :current-jam    :roller
           :mesh-type      :torus-knot
           :wireframe?     true
           :bg-color       "#050510"
           :mesh-color     "#00ffcc"
           :wire-color     "#ff007f"
           :sensitivity    1.6
           :camera-speed   0.005}))

(defonce visual-pulse (atom 0.0))
(defonce three-ctx    (atom nil))

(defonce tone-ctx          (atom nil))
(defonce active-tracks     (atom {}))
(defonce solo-mode?        (atom false))
(defonce last-clock-sample (atom nil))

(defonce global-key (atom {:root :e :mode :dorian :octave 2}))

(defonce repl-tracks      (atom {}))
(defonce repl-instruments (atom {}))
(defonce repl-routes      (atom {}))

(defonce events-bound?   (atom false))
(defonce root-container  (atom nil))

(defn pulse!
  ([] (pulse! 1.5))
  ([intensity] (reset! visual-pulse intensity)))
