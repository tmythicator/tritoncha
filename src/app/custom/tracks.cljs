(ns app.custom.tracks
  "User custom tracks, live-coding sets, and arrangements."
  (:require [app.audio.theory.harmony :refer [_ chord deg]]))

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
   {:name   "Liquid Sunrise Drift"
    :mod    :natural
    :bpm    172
    :scale  [:e :hirajoshi 2]
    :geom   :torus-knot
    :colors ["#040a14" "#00ffaa"]
    :cutoff 4200
    :tracks
    {:drums   {:notes [[:kick 1.0 "D1"] [:hh-c 0.4] [:ride 0.6] [:hh-c 0.4]
                       [:sn-rs 1.0 "G3"] [:hh-c 0.4] [:kick 0.85 "D1"] [:hh-o 0.75]
                       [:hh-c 0.4] [:kick 0.9 "D1"] [:sn-rs 1.0 "G3"] [:ride-bell 0.7]
                       [:hh-c 0.4] [:sn-gh 0.35] [:splash 0.65] [:hh-o 0.6]]
               :step "16n"}
     :bass    {:inst :bass-reese
               :notes (deg :e :hirajoshi [1 _ 1 2 _ 1 4 3 1 _ 5 4 _ 2 1 _] {:octave 1})
               :step "16n" :dur "16n" :vel 0.92}
     :sub     {:inst :sub-pure
               :notes (deg :e :hirajoshi [1 _ _ _ 2 _ _ _ 1 _ _ _ 5 _ 4 _] {:octave 1})
               :step "16n" :dur "8n" :vel 1.0}
     :strings {:inst :pad-shimmer
               :notes [(chord :e :min9 {:octave 3})
                       (chord :c :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}
     :keys    {:inst :lead-bell
               :notes (deg :e :hirajoshi [1 _ 2 _ 4 _ 5 _ 7 _ 5 _ 4 _ 2 _] {:octave 3})
               :step "16n" :dur "16n" :vel 0.3}}}})