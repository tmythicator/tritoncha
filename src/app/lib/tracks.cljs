(ns app.lib.tracks
  "Built-in track library and baseline catalog for Tritoncha."
  (:require [app.audio.theory.harmony :refer [_ arp chord deg scale]]))

(def core-tracks
  {:roller
   {:name   "Phrygian Bass Roller (168 BPM)"
    :bpm    168
    :scale  [:e :phrygian 1]
    :geom   :torus-knot
    :colors ["#080412" "#ff007f"]
    :cutoff 3400
    :tracks
    {:drums   {:notes [[:kick 1.0 "D1"] [:hh-c 0.45] [:sn-gh 0.35] [:hh-c 0.5]
                       [:snare 1.0 "G3"] [:hh-c 0.4] [:sn-gh 0.4] [:kick 0.9 "D1"]
                       [:hh-o 0.75] [:kick 0.8 "D1"] [:snare 1.0 "G3"] [:hh-c 0.4]
                       [:sn-rs 0.9 "G3"] [:sn-gh 0.35] [:sn-roll 0.8 "A3"] [:sn-roll 0.95 "A3"]]
               :step "16n"}
     :bass    {:inst :bass
               :notes (deg :e :phrygian [1 _ 1 3 _ 1 :b5 5 1 _ :b7 1 _ 3 2 _] {:octave 1})
               :step "16n" :dur "16n" :vel 0.95}
     :sub     {:inst :sub
               :notes (deg :e :phrygian [1 _ 1 3 _ 1 :b5 5 1 _ :b7 1 _ 3 2 _] {:octave 1})
               :step "16n" :dur "16n" :vel 1.0}
     :strings {:inst :pad
               :notes [(chord :e :min9 {:octave 3})
                       (chord :d :min7 {:octave 3})
                       (chord :c :maj7 {:octave 3})
                       (chord :b :7    {:octave 2})]
               :step "1m" :dur "1m" :vel 0.42}}}

   :sub-roller
   {:name   "Deep Sub Roller (172 BPM)"
    :bpm    172
    :scale  [:f :minor 1]
    :geom   :icosahedron
    :colors ["#030814" "#00e5ff"]
    :cutoff 3800
    :tracks
    {:drums   {:notes [[:kick 1.0 "Eb1"] [:hh-c 0.5] [:hh-c 0.4] [:kick 0.7 "Eb1"]
                       [:snare 1.0 "F3"] [:hh-c 0.4] [:sn-gh 0.3] [:hh-c 0.6]
                       [:kick 0.9 "Eb1"] [:hh-o 0.8] [:snare 1.0 "F3"] [:sn-gh 0.35]
                       [:kick 0.8 "Eb1"] [:sn-rs 0.9 "F3"] [:sn-gh 0.4] [:sn-roll 0.85 "G3"]]
               :step "16n"}
     :bass    {:inst :bass
               :notes ["F1" "F1" nil "F1" "Ab1" nil "C2" nil "F1" nil "Eb1" "F1" nil "Db1" nil "C1"]
               :step "16n" :dur "16n" :vel 0.9}
     :sub     {:inst :sub
               :notes ["F1" "F1" nil "F1" "Ab1" nil "C2" nil "F1" nil "Eb1" "F1" nil "Db1" nil "C1"]
               :step "16n" :dur "16n" :vel 1.0}
     :strings {:inst :pad
               :notes [(chord :f :min9 {:octave 3})
                       (chord :db :maj9 {:octave 3})
                       (chord :eb :dom7 {:octave 3})
                       (chord :c :min7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.4}}}

   :acid-roller
   {:name   "Acid Roller (174 BPM)"
    :bpm    174
    :scale  [:a :aeolian 1]
    :geom   :octahedron
    :colors ["#140404" "#ff3300"]
    :cutoff 4500
    :tracks
    {:drums   {:notes [[:kick 1.0 "D1"] [:hh-c 0.4] [:sn-clk 0.4 "A3"] [:hh-c 0.5]
                       [:snare 1.0 "A3"] [:sn-gh 0.35] [:kick 0.85 "D1"] [:hh-o 0.75]
                       [:hh-c 0.4] [:kick 0.9 "D1"] [:snare 1.0 "A3"] [:sn-gh 0.3]
                       [:sn-rs 0.9 "A3"] [:sn-roll 0.8 "C4"] [:sn-roll 0.85 "C4"] [:sn-roll 0.95 "C4"]]
               :step "16n"}
     :bass    {:inst :bass
               :notes (deg :a :aeolian [1 _ 3 1 _ 4 1 7 1 3 _ 5 4 _ 7 5] {:octave 1})
               :step "16n" :dur "16n" :vel 1.0}
     :sub     {:inst :sub
               :notes (deg :a :aeolian [1 _ 3 1 _ 4 1 7 1 3 _ 5 4 _ 7 5] {:octave 1})
               :step "16n" :dur "16n" :vel 1.0}
     :strings {:inst :pad
               :notes [(chord :a :min7 {:octave 3})
                       (chord :f :maj7 {:octave 3})
                       (chord :d :min7 {:octave 3})
                       (chord :e :dom7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.4}}}

   :ambient-drift
   {:name   "Ambient Drift (160 BPM)"
    :bpm    160
    :scale  [:e :hirajoshi 2]
    :geom   :sphere
    :colors ["#020b14" "#00ffcc"]
    :cutoff 5200
    :tracks
    {:drums   {:notes [[:kick 0.85 "E1"] [:hh-c 0.3] [:sn-gh 0.25] [:hh-c 0.35]
                       [:snare 0.85 "A3"] [:hh-c 0.3] [:hh-o 0.5] [:kick 0.7 "E1"]
                       [:hh-clk 0.3] [:kick 0.8 "E1"] [:snare 0.85 "A3"] [:hh-c 0.35]]
               :step "16n"}
     :sub     {:inst :sub
               :notes (deg :e :hirajoshi [1 _ _ _ 2 _ _ _ 1 _ _ _ 5 _ 4 _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.9}
     :arp     {:inst :ambient-glass
               :notes (arp (scale :e :hirajoshi {:octave 3}) :up-down)
               :step "16n" :dur "16n" :vel 0.32}
     :strings {:inst :pad
               :notes [(chord :e :min9 {:octave 3})
                       (chord :c :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.3}}}})
