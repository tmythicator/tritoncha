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
   {:name   "Acid 303 Roller (174 BPM)"
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
   {:name   "Hirajoshi Ambient Drift (160 BPM)"
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
     :arp     {:inst :pad-glass
               :notes (arp (scale :e :hirajoshi {:octave 3}) :up-down)
               :step "16n" :dur "16n" :vel 0.32}
     :strings {:inst :pad
               :notes [(chord :e :min9 {:octave 3})
                       (chord :c :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.3}}}

   :orbital-roller
   {:name   "Orbital Cyber-Jungle (174 BPM)"
    :bpm    174
    :scale  [:e :dorian 1]
    :geom   :dodecahedron
    :colors ["#05081c" "#00ffff"]
    :cutoff 3500
    :tracks
    {:drums   {:notes [[:kick 1.0 "E1"] [:sn-gh 0.3 "G3"] [:ride 0.65] [:hh-c 0.45]
                       [:sn-crack 1.0 "G3"] [:hh-c 0.4] [:ride 0.7] [:kick 0.85 "E1"]
                       [:hh-o 0.75] [:kick 0.9 "E1"] [:sn-crack 1.0 "G3"] [:ride 0.65]
                       [:tom 0.85 "A2"] [:sn-gh 0.35 "G3"] [:sn-crack 0.95 "G3"] [:sn-crack 1.0 "A3"]]
               :step "16n"}
     :bass    {:inst :bass-reese
               :notes (deg :e :dorian [1 _ 1 _ _ 1 _ :b7 1 _ _ 3 _ 4 :b5 5] {:octave 1})
               :step "16n" :dur "16n" :vel 0.95}
     :sub     {:inst :sub
               :notes (deg :e :dorian [1 _ 1 _ _ 1 _ :b7 1 _ _ 3 _ 4 :b5 5] {:octave 1})
               :step "16n" :dur "16n" :vel 1.0}
     :strings {:inst :pad-shimmer
               :notes [(chord :e :min9 {:octave 3})
                       (chord :d :maj9 {:octave 3})
                       (chord :c :maj7 {:octave 3})
                       (chord :b :min7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.28}
     :arp     {:inst :lead-pluck
               :notes (deg :e :dorian [1 3 5 7 1 3 5 4 3 5 7 1 3 5 4 2] {:octave 3})
               :step "16n" :dur "32n" :vel 0.24}}}

   :liquid-roller
   {:name   "Soulful Liquid D&B (172 BPM)"
    :bpm    172
    :scale  [:g :dorian 1]
    :geom   :torus-knot
    :colors ["#100418" "#ffaa00"]
    :cutoff 4800
    :tracks
    {:drums   {:notes [[:kick 0.95 "G1"] [:hh-c 0.4] [:ride 0.6] [:hh-c 0.4]
                       [:sn-rs 1.0 "Bb3"] [:hh-c 0.4] [:kick 0.8 "G1"] [:hh-o 0.7]
                       [:hh-c 0.4] [:kick 0.9 "G1"] [:sn-rs 1.0 "Bb3"] [:sn-gh 0.35]
                       [:hh-c 0.4] [:sn-gh 0.3] [:ride 0.65] [:hh-o 0.6]]
               :step "16n"}
     :bass    {:inst :bass-reese
               :notes (deg :g :dorian [1 _ _ _ 4 _ _ _ 5 _ _ _ 7 _ 5 _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.9}
     :sub     {:inst :sub
               :notes (deg :g :dorian [1 _ _ _ 4 _ _ _ 5 _ _ _ 7 _ 5 _] {:octave 1})
               :step "16n" :dur "8n" :vel 1.0}
     :strings {:inst :pad-strings
               :notes [(chord :g :min9 {:octave 3})
                       (chord :f :maj9 {:octave 3})
                       (chord :eb :maj7 {:octave 3})
                       (chord :d :min7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.4}}}

   :neuro-tech
   {:name   "Dark Neurofunk Techstep (175 BPM)"
    :bpm    175
    :scale  [:f :phrygian 1]
    :geom   :box
    :colors ["#020402" "#00ff33"]
    :cutoff 3200
    :tracks
    {:drums   {:notes [[:kick 1.0 "F1"] [:sn-gh 0.3] [:hh-c 0.5] [:kick 0.85 "F1"]
                       [:sn-crack 1.0 "F3"] [:hh-c 0.4] [:sn-gh 0.35] [:hh-o 0.7]
                       [:kick 0.9 "F1"] [:hh-c 0.45] [:sn-crack 1.0 "F3"] [:sn-gh 0.4]
                       [:tom 0.8 "G2"] [:sn-crack 0.9 "F3"] [:hh-c 0.4] [:sn-roll 0.85 "G3"]]
               :step "16n"}
     :bass    {:inst :bass-neuro
               :notes (deg :f :phrygian [1 1 _ 2 _ 1 _ :b5 1 _ :b7 1 _ 3 2 _] {:octave 1})
               :step "16n" :dur "16n" :vel 1.0}
     :sub     {:inst :sub
               :notes (deg :f :phrygian [1 1 _ 2 _ 1 _ :b5 1 _ :b7 1 _ 3 2 _] {:octave 1})
               :step "16n" :dur "16n" :vel 1.0}
     :strings {:inst :pad-shimmer
               :notes [(chord :f :min9 {:octave 3})
                       (chord :gb :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.3}}}

   :cyber-dub
   {:name   "Deep Dub Techno (130 BPM)"
    :bpm    130
    :scale  [:c :minor 1]
    :geom   :icosahedron
    :colors ["#05080c" "#00e5a3"]
    :cutoff 2600
    :tracks
    {:drums   {:notes [[:kick 1.0 "C1"] [:hh-c 0.35] [:hh-c 0.45] [:hh-c 0.3]
                       [:snare 0.85 "C3"] [:hh-c 0.35] [:hh-o 0.5] [:hh-c 0.3]
                       [:kick 0.95 "C1"] [:hh-c 0.35] [:hh-c 0.4] [:hh-c 0.3]
                       [:snare 0.85 "C3"] [:hh-c 0.35] [:hh-o 0.55] [:hh-c 0.35]]
               :step "16n"}
     :bass    {:inst :sub-pure
               :notes (deg :c :minor [1 _ _ _ 1 _ _ _ 1 _ _ _ :b7 _ 5 _] {:octave 1})
               :step "16n" :dur "8n" :vel 1.0}
     :chords  {:inst :pad-glass
               :notes [(chord :c :min9 {:octave 3}) nil nil nil
                       (chord :bb :sus4 {:octave 3}) nil nil nil]
               :step "4n" :dur "8n" :vel 0.35}}}

   :deep-minimal
   {:name   "Hypnotic Minimal 4x4 (126 BPM)"
    :bpm    126
    :scale  [:d :dorian 1]
    :geom   :octahedron
    :colors ["#0a0b0d" "#e2e8f0"]
    :cutoff 3000
    :tracks
    {:drums   {:notes [[:kick 1.0 "D1"] [:hh-c 0.3] [:hh-o 0.6] [:hh-c 0.3]
                       [:kick 0.95 "D1"] [:hh-c 0.3] [:hh-o 0.65] [:hh-c 0.3]
                       [:kick 1.0 "D1"] [:hh-c 0.3] [:hh-o 0.6] [:hh-c 0.3]
                       [:kick 0.95 "D1"] [:hh-c 0.3] [:hh-o 0.7] [:hh-c 0.35]]
               :step "16n"}
     :bass    {:inst :bass-analog
               :notes (deg :d :dorian [_ 1 _ 1 _ 1 _ 1 _ 1 _ 1 _ :b7 _ 1] {:octave 1})
               :step "16n" :dur "32n" :vel 0.85}
     :synth   {:inst :lead-pluck
               :notes (deg :d :dorian [1 _ _ 3 _ 5 _ 4 _ _ 7 _ 5 _ 4 _] {:octave 2})
               :step "16n" :dur "16n" :vel 0.35}}}

   :synthwave-run
   {:name   "80s Cyber Outrun (124 BPM)"
    :bpm    124
    :scale  [:a :minor 1]
    :geom   :torus-knot
    :colors ["#12031a" "#ff00aa"]
    :cutoff 5500
    :tracks
    {:drums   {:notes [[:kick 1.0 "A1"] [:hh-c 0.4] [:hh-c 0.4] [:hh-c 0.4]
                       [:snare 1.0 "A3"] [:hh-c 0.4] [:kick 0.85 "A1"] [:hh-c 0.4]
                       [:kick 0.95 "A1"] [:hh-c 0.4] [:hh-c 0.4] [:hh-c 0.4]
                       [:snare 1.0 "A3"] [:hh-c 0.4] [:hh-o 0.6] [:hh-c 0.4]]
               :step "16n"}
     :bass    {:inst :bass-analog
               :notes (deg :a :minor [1 1 1 1 1 1 1 1 :b7 :b7 :b7 :b7 6 6 6 6] {:octave 1})
               :step "16n" :dur "16n" :vel 0.9}
     :strings {:inst :lead-blade
               :notes [(chord :a :min7 {:octave 3})
                       (chord :f :maj7 {:octave 3})
                       (chord :c :maj7 {:octave 3})
                       (chord :g :dom7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.38}
     :arp     {:inst :lead-pluck
               :notes (deg :a :minor [1 3 5 8 5 3 1 3] {:octave 3})
               :step "16n" :dur "16n" :vel 0.4}}}

   :electro-break
   {:name   "808 Electro Funk (134 BPM)"
    :bpm    134
    :scale  [:e :phrygian 1]
    :geom   :dodecahedron
    :colors ["#040a18" "#00aaff"]
    :cutoff 4000
    :tracks
    {:drums   {:notes [[:kick 1.0 "E1"] [:hh-c 0.4] [:hh-c 0.45] [:sn-clk 0.5]
                       [:snare 1.0 "E3"] [:hh-c 0.4] [:kick 0.85 "E1"] [:hh-o 0.6]
                       [:hh-c 0.4] [:kick 0.9 "E1"] [:snare 1.0 "E3"] [:sn-gh 0.3]
                       [:kick 0.8 "E1"] [:hh-c 0.4] [:sn-clk 0.6] [:hh-o 0.7]]
               :step "16n"}
     :bass    {:inst :bass-slap
               :notes (deg :e :phrygian [1 _ 1 _ 2 _ 1 _ _ 1 _ 3 2 _ 1 _] {:octave 1})
               :step "16n" :dur "16n" :vel 0.95}
     :sub     {:inst :sub-808
               :notes (deg :e :phrygian [1 _ _ _ _ _ _ _ 2 _ _ _ 1 _ _ _] {:octave 1})
               :step "16n" :dur "4n" :vel 1.0}}}

   :hardcore-rave
   {:name   "Oldskool 90s Hardcore Rave (165 BPM)"
    :bpm    165
    :scale  [:c :dorian 1]
    :geom   :box
    :colors ["#050505" "#ffee00"]
    :cutoff 6000
    :tracks
    {:drums   {:notes [[:kick 1.0 "C1"] [:hh-c 0.45] [:sn-crack 0.9] [:hh-c 0.4]
                       [:snare 1.0 "C3"] [:hh-c 0.45] [:kick 0.9 "C1"] [:hh-o 0.75]
                       [:kick 1.0 "C1"] [:sn-crack 0.8] [:snare 1.0 "C3"] [:hh-c 0.4]
                       [:sn-rs 0.9] [:kick 0.85 "C1"] [:sn-roll 0.8] [:sn-roll 0.9]]
               :step "16n"}
     :bass    {:inst :bass-reese
               :notes (deg :c :dorian [1 _ 1 _ 3 _ 1 _ :b7 _ 1 _ 5 _ 4 _] {:octave 1})
               :step "16n" :dur "16n" :vel 0.95}
     :lead    {:inst :lead-hoover
               :notes (deg :c :dorian [1 _ _ 3 _ 5 _ 8 _ 7 _ 5 _ 4 _ 3] {:octave 2})
               :step "16n" :dur "16n" :vel 0.45}}}

   :dubstep-wobble
   {:name   "Deep Sub 140 Dubstep (140 BPM)"
    :bpm    140
    :scale  [:d :minor 1]
    :geom   :octahedron
    :colors ["#0a0212" "#bb00ff"]
    :cutoff 3200
    :tracks
    {:drums   {:notes [[:kick 1.0 "D1"] nil nil nil
                       [:snare 1.0 "D3"] nil nil nil
                       nil nil [:kick 0.85 "D1"] nil
                       [:snare 1.0 "D3"] nil nil nil]
               :step "16n"}
     :sub     {:inst :sub-pure
               :notes (deg :d :minor [1 _ _ _ 1 _ _ _ :b7 _ _ _ 5 _ 4 _] {:octave 1})
               :step "16n" :dur "8n" :vel 1.0}
     :strings {:inst :pad-cinema
               :notes [(chord :d :min9 {:octave 3})
                       (chord :c :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}}}

   :ambient-temple
   {:name   "Zen Temple Meditation (110 BPM)"
    :bpm    110
    :scale  [:e :in-sen 2]
    :geom   :sphere
    :colors ["#030a08" "#d4af37"]
    :cutoff 4000
    :tracks
    {:drums   {:notes [[:tom 0.7 "E2"] nil nil nil
                       [:sn-gh 0.25] nil [:hh-c 0.3] nil
                       [:tom 0.8 "A2"] nil nil nil
                       [:sn-gh 0.2] nil [:hh-o 0.35] nil]
               :step "16n"}
     :bells   {:inst :pad-glass
               :notes (deg :e :in-sen [1 _ 2 _ 3 _ 5 _ 4 _ 3 _ 2 _ 1 _] {:octave 3})
               :step "16n" :dur "8n" :vel 0.25}
     :drone   {:inst :pad-drone
               :notes [(chord :e :min9 {:octave 2})]
               :step "1m" :dur "1m" :vel 0.3}}}

   :glitch-hop
   {:name   "Syncopated Glitch Funk (105 BPM)"
    :bpm    105
    :scale  [:g :blues 1]
    :geom   :torus-knot
    :colors ["#120814" "#00ffb7"]
    :cutoff 4200
    :tracks
    {:drums   {:notes [[:kick 1.0 "G1"] [:hh-clk 0.4] nil [:kick 0.8 "G1"]
                       [:snare 1.0 "G3"] nil [:hh-c 0.4] [:sn-gh 0.35]
                       nil [:kick 0.9 "G1"] [:sn-crack 0.85] nil
                       [:snare 1.0 "G3"] [:hh-o 0.5] nil [:sn-clk 0.4]]
               :step "16n"}
     :bass    {:inst :bass-slap
               :notes (deg :g :blues [1 _ 1 _ :b3 _ 3 _ 4 _ :b5 5 _ 1 _ _] {:octave 1})
               :step "16n" :dur "16n" :vel 0.95}
     :lead    {:inst :lead-pluck
               :notes (deg :g :blues [_ 1 _ :b3 _ 4 _ 5 _ :b7 _ 8 _ 5 _ 3] {:octave 2})
               :step "16n" :dur "16n" :vel 0.4}}}

   :industrial-techno
   {:name   "Raw Warehouse Techno (138 BPM)"
    :bpm    138
    :scale  [:b :phrygian 1]
    :geom   :box
    :colors ["#080808" "#ff1122"]
    :cutoff 2800
    :tracks
    {:drums   {:notes [[:kick 1.0 "B1"] [:hh-c 0.4] [:hh-o 0.6] [:hh-c 0.4]
                       [:kick 1.0 "B1"] [:hh-c 0.4] [:hh-o 0.65] [:hh-c 0.4]
                       [:kick 1.0 "B1"] [:hh-c 0.4] [:hh-o 0.6] [:hh-c 0.4]
                       [:kick 1.0 "B1"] [:hh-c 0.4] [:hh-o 0.7] [:hh-c 0.45]]
               :step "16n"}
     :bass    {:inst :bass-analog
               :notes (deg :b :phrygian [1 1 1 1 1 1 1 1 2 2 2 2 1 1 1 1] {:octave 1})
               :step "16n" :dur "32n" :vel 0.9}
     :drone   {:inst :lead-fm
               :notes (deg :b :phrygian [1 _ _ _ _ _ _ _ 2 _ _ _ _ _ _ _] {:octave 2})
               :step "16n" :dur "8n" :vel 0.4}}}

   :chiptune-odyssey
   {:name   "8-Bit Arcade Fantasy (150 BPM)"
    :bpm    150
    :scale  [:c :major 1]
    :geom   :octahedron
    :colors ["#020b08" "#00ff66"]
    :cutoff 7000
    :tracks
    {:drums   {:notes [[:kick 0.9 "C1"] [:hh-c 0.35] [:hh-c 0.4] [:hh-c 0.3]
                       [:snare 0.9 "C3"] [:hh-c 0.35] [:kick 0.8 "C1"] [:hh-o 0.5]
                       [:kick 0.9 "C1"] [:hh-c 0.35] [:snare 0.9 "C3"] [:hh-c 0.4]
                       [:hh-c 0.35] [:kick 0.8 "C1"] [:sn-rs 0.85] [:hh-c 0.3]]
               :step "16n"}
     :bass    {:inst :lead-8bit
               :notes (deg :c :major [1 1 1 1 5 5 5 5 6 6 6 6 4 4 4 4] {:octave 1})
               :step "16n" :dur "16n" :vel 0.85}
     :lead    {:inst :lead-8bit
               :notes (deg :c :major [1 3 5 8 5 3 1 3 4 6 8 6 5 3 2 1] {:octave 3})
               :step "16n" :dur "16n" :vel 0.45}}}

   :psy-trance
   {:name   "Rolling Psychedelic Hypnosis (145 BPM)"
    :bpm    145
    :scale  [:f :phrygian 1]
    :geom   :icosahedron
    :colors ["#050014" "#ff00d4"]
    :cutoff 4800
    :tracks
    {:drums   {:notes [[:kick 1.0 "F1"] [:hh-c 0.4] [:hh-o 0.7] [:hh-c 0.4]
                       [:kick 1.0 "F1"] [:hh-c 0.4] [:hh-o 0.7] [:hh-c 0.4]
                       [:kick 1.0 "F1"] [:hh-c 0.4] [:hh-o 0.7] [:hh-c 0.4]
                       [:kick 1.0 "F1"] [:hh-c 0.4] [:hh-o 0.75] [:hh-c 0.45]]
               :step "16n"}
     :bass    {:inst :bass-analog
               :notes (deg :f :phrygian [_ 1 1 1 _ 1 1 1 _ 1 1 1 _ 2 2 1] {:octave 1})
               :step "16n" :dur "32n" :vel 0.9}
     :lead    {:inst :lead-supersaw
               :notes (deg :f :phrygian [1 _ 2 _ 3 _ 5 _ 4 _ 3 _ 2 _ 1 _] {:octave 3})
               :step "16n" :dur "16n" :vel 0.35}}}

   :downtempo-chill
   {:name   "Lo-Fi Trip-Hop Chill (88 BPM)"
    :bpm    88
    :scale  [:c :dorian 1]
    :geom   :sphere
    :colors ["#120c08" "#ffaa77"]
    :cutoff 3200
    :tracks
    {:drums   {:notes [[:kick 0.95 "C1"] nil [:hh-c 0.3] nil
                       [:snare 0.85 "C3"] nil [:hh-c 0.35] [:hh-o 0.4]
                       nil [:kick 0.85 "C1"] nil nil
                       [:snare 0.85 "C3"] nil [:hh-c 0.3] nil]
               :step "16n"}
     :bass    {:inst :sub-pure
               :notes (deg :c :dorian [1 _ _ _ 3 _ _ _ 4 _ _ _ 5 _ _ _] {:octave 1})
               :step "16n" :dur "4n" :vel 0.9}
     :strings {:inst :pad-strings
               :notes [(chord :c :min9 {:octave 3})
                       (chord :f :dom7 {:octave 3})
                       (chord :bb :maj7 {:octave 3})
                       (chord :eb :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}}}

   :future-garage
   {:name   "Atmospheric 2-Step Garage (136 BPM)"
    :bpm    136
    :scale  [:f :minor 1]
    :geom   :dodecahedron
    :colors ["#080c14" "#88aacc"]
    :cutoff 3600
    :tracks
    {:drums   {:notes [[:kick 1.0 "F1"] nil [:hh-c 0.35] nil
                       [:sn-rs 0.95 "F3"] nil [:hh-c 0.35] [:kick 0.8 "F1"]
                       nil [:hh-o 0.5] [:sn-rs 0.95 "F3"] nil
                       [:hh-c 0.35] [:kick 0.75 "F1"] nil [:hh-c 0.4]]
               :step "16n"}
     :bass    {:inst :bass-organ
               :notes (deg :f :minor [1 _ _ 1 _ _ :b7 _ _ 5 _ _ 4 _ _ _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.9}
     :pad     {:inst :pad-shimmer
               :notes [(chord :f :min9 {:octave 3})
                       (chord :db :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}}}})
