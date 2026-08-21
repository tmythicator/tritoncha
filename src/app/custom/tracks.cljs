(ns app.custom.tracks
  "User custom tracks, live-coding sets, and arrangements."
  (:require [app.audio.theory :refer [_ d chord]]))

;; Custom Tracks Catalog (Same format as app.lib.tracks)
;;
;; Track parameters:
;;   :name   - Title displayed in HUD
;;   :bpm    - Master tempo (e.g. 172)
;;   :scale  - Vector [root mode octave] (e.g. [:e :hirajoshi 2])
;;   :geom   - 3D visual geometry (:torus-knot, :icosahedron, :octahedron, :box, :sphere)
;;   :colors - Vector of [background-hex mesh-hex]
;;   :cutoff - Master filter cutoff frequency in Hz (e.g. 4200)
;;   :tracks - Map of loops to launch simultaneously

(def user-tracks
  {:liquid-roller
   {:name   "Liquid Roller (172 BPM)"
    :bpm    172
    :scale  [:e :hirajoshi 2]
    :geom   :torus-knot
    :colors ["#040a14" "#00ffaa"]
    :cutoff 4200
    :tracks
    {:drums   {:hits [[:kick 1.0 "D1"] [:hh-c 0.4] [[:snare 1.0 "G3"] [:hh-c 0.45]] [:hh-c 0.4]
                     [:kick 0.9 "D1"] [:hh-o 0.7] [[:sn-rs 0.95 "G3"] [:hh-c 0.4]] [:sn-gh 0.35]]
               :step "16n"}
     :bass    {:inst :bass :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _] 1) :step "16n"}
     :sub     {:inst :sub  :notes (d [1 _ _ _ 2 _ _ _  1 _ _ _ 5 _ 4 _] 1) :step "16n" :dur "8n"}
     :strings {:inst :pad  :notes [(chord :e :min9 3) (chord :c :maj7 3)] :step "1m" :dur "1m"}}}})