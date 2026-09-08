(ns app.lib.tracks
  "Built-in track library and baseline catalog for Tritoncha."
  (:require [app.audio.theory.harmony :refer [_ chord deg]]
            [app.audio.theory.patterns :refer [pattern]]))

(def core-tracks
  {:orbital-matrix
   {:name   "Orbital Multi-Object Matrix"
    :mod    :analog
    :bpm    168
    :scale  [:e :phrygian 1]
    :colors ["#050410" "#00ffcc"]
    :cutoff 5200
    :figures
    {:core   {:geom :torus-knot  :pos [0 0 0]     :scale 1.05 :colors {:mesh "#ff007f" :wire "#00ffff"} :rot-speed [0.006 0.009 0.0]}
     :halo   {:geom :torus       :pos [0 2.8 0]   :rot [1.57 0 0] :scale 0.75 :colors {:mesh "#00ffff" :wire "#ffffff"} :rot-speed [0.002 0.010 0.0]}
     :pillar {:geom :cylinder    :pos [-4.2 0 0]  :scale 0.85 :colors {:mesh "#9d4edd" :wire "#00ffcc"} :rot-speed [0.008 0.005 0.0]}
     :sat    {:geom :octahedron  :pos [4.2 0 0]   :scale 0.80 :colors {:mesh "#00ffcc" :wire "#9d4edd"} :rot-speed [0.009 0.007 0.004]}
     :ring   {:geom :dodecahedron :pos [0 -2.8 0] :scale 0.75 :colors {:mesh "#ffe600" :wire "#ff3300"} :rot-speed [0.005 0.009 0.003]}}
    :tracks
    {:kick    {:figure :core
               :notes (pattern "k_ . . .  . . k_ .  . . . .  . . k_ .
                               k_ . . .  . . k_ .  . . k_ .  . . . k_")
               :step "16n"}
     :snare   {:figure :halo
               :notes (pattern ". . . .  rs . . .  . . . .  rs . g_ .
                               . . . .  rs . . g_  . . . .  rs . clk_ .")
               :step "16n"}
     :hats    {:figure :ring
               :notes (pattern "hc_ . h_ .  hc_ . o_ .  hc_ . h_ clk_  hc_ . o_ .
                               hc_ . h_ .  hc_ . o_ .  hc_ clk_ h_ .  hc_ . o_ .")
               :step "16n"}
     :bass    {:figure :pillar
               :inst :bass-liquid
               :notes (deg :e :phrygian [1 _ 1 2  _ 1 4 3  1 _ 5 4  _ 2 1 _
                                         1 _ _ 2  _ 1 :b5 _  1 _ 4 3  _ 2 1 _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.75}
     :echo    {:figure :sat
               :inst :pad-glass
               :bus  :bus/space
               :notes (deg :e :phrygian [nil nil 1 nil  nil 3 nil nil  nil :b5 nil 5  nil 4 nil 2] {:octave 3})
               :step "16n" :dur "16n" :vel 0.35}}}

   :metro-roller
   {:name   "Metro Trip-Hop Roller"
    :mod    :natural
    :bpm    160
    :scale  [:e :phrygian 1]
    :geom   :torus-knot
    :colors ["#080412" "#ff007f"]
    :cutoff 5000
    :tracks
    {:kick    {:notes (pattern "k_ . . .  . . . .  . . k_ .  . . . .
                                . . k_ .  . . . .  . . k_ .  . . . k_")
               :step "16n"}
     :snare   {:notes (pattern ". . . .  rs . . .  . . . .  rs . g_ .
                                . . . .  rs . . g_  . . . .  rs . clk_ .")
               :step "16n"}
     :hats    {:notes (pattern "hc_ . h_ .  hc_ . o_ .  hc_ . h_ clk_  hc_ . o_ .
                                hc_ . h_ .  hc_ . o_ .  hc_ clk_ h_ .  hc_ . o_ .")
               :step "16n"}
     :perc    {:notes (pattern ". . . .  . . . .  . . . .  . . sp_ .
                                . . . .  . . . .  . . . .  . . . .")
               :step "16n"}
     :bass    {:inst :bass-liquid
               :notes (deg :e :phrygian [1 _ _ _  _ _ 3 _  1 _ _ _  _ 4 2 _
                                         1 _ _ _  _ _ 2 _  1 _ _ :b5  _ 4 1 _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.62}
     :strings {:inst :pad-shimmer
               :notes [(chord :e :min9 {:octave 3})
                       (chord :d :min7 {:octave 3})
                       (chord :c :maj7 {:octave 3})
                       (chord :b :7    {:octave 2})]
               :step "2m" :dur "2m" :vel 0.22}
     :echo    {:inst :pad-glass
               :bus  :bus/space
               :notes (deg :e :phrygian [nil nil 1 nil  nil 3 nil nil  nil :b5 nil 5  nil 4 nil 2] {:octave 3})
               :step "16n" :dur "16n" :vel 0.24}}}

   :street-roller
   {:name   "Messenger Street Roller"
    :mod    :natural
    :bpm    172
    :scale  [:f :minor 1]
    :geom   :icosahedron
    :colors ["#030814" "#00e5ff"]
    :cutoff 4400
    :tracks
    {:kick    {:notes (pattern "k_ . . .  . . k_ .  . . k_ .  . . . .") :step "16n"}
     :snare   {:notes (pattern ". . . .  rs . . .  . . . .  rs . g_ .") :step "16n"}
     :hats    {:notes (pattern "h_ . clk .  . . o_ .  h_ . clk .  . . o_ .") :step "16n"}
     :sub     {:inst :sub-pure
               :notes ["F1" "F1" nil "F1" "Ab1" nil "C2" nil "F1" nil "Eb1" "F1" nil "Db1" nil "C1"]
               :step "16n" :dur "16n" :vel 0.70}
     :strings {:inst :pad-glass
               :notes [(chord :f :min9 {:octave 3})
                       (chord :db :maj9 {:octave 3})
                       (chord :eb :dom7 {:octave 3})
                       (chord :c :min7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.26}}}

   :acid-roller
   {:name   "Acid Downtempo Jungle"
    :mod    :analog
    :bpm    130
    :scale  [:a :aeolian 1]
    :geom   :octahedron
    :colors ["#140404" "#ff3300"]
    :cutoff 3800
    :tracks
    {:kick  {:notes (pattern "k_ . . .  . . . k_  . . k_ .  . . . .") :step "16n"}
     :snare {:notes (pattern ". . . .  rs . . .  . . . .  rs . g_ .") :step "16n"}
     :hats  {:notes (pattern "h_ . clk .  . . o_ .  h_ . h_ .  . . o_ .") :step "16n"}
     :bass  {:inst :bass-303
             :notes (deg :a :aeolian [1 _ _ 3 1 _ 4 _ 1 _ :b7 1 _ _ 5 _] {:octave 1})
             :step "16n" :dur "16n" :vel 0.70}
     :sub   {:inst :sub-pure
             :notes (deg :a :aeolian [1 _ _ _ _ _ 1 _ 1 _ _ _ _ _ 5 _] {:octave 1})
             :step "16n" :dur "8n" :vel 0.68}}}

   :ambient-drift
   {:name   "Phobos Moon"
    :mod    :natural
    :bpm    138
    :scale  [:d :phrygian 1]
    :geom   :sphere
    :colors ["#0a0302" "#ff6600"]
    :cutoff 4200
    :tracks
    {:kick    {:notes (pattern "k_ . . .  . . . .  k_ . . .  . . k_ .") :step "16n"}
     :snare   {:notes (pattern ". . . .  crack_ . . .  . . . .  rs . g_ .") :step "16n"}
     :perc    {:notes (pattern ". . clk .  . . . .  . . sp_ .  . . clk_ .") :step "16n"}
     :bass    {:inst :bass-liquid
               :notes (deg :d :phrygian [1 _ _ _ _ _ 2 _ 1 _ _ _ :b5 _ _ _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.65}
     :drone   {:inst :pad-cinema
               :notes [(chord :d :dark-m9 {:octave 2})
                       (chord :bb :maj7 {:octave 2})
                       (chord :eb :maj7 {:octave 2})
                       (chord :c :min7 {:octave 2})]
               :step "2m" :dur "2m" :vel 0.28}
     :echo    {:inst :pad-glass
               :notes (deg :d :phrygian [nil nil 1 nil nil :b2 nil nil nil :b5 nil nil nil 4 nil nil] {:octave 3})
               :step "16n" :dur "16n" :vel 0.24}}}

   :orbital-roller
   {:name   "Facing Moons"
    :mod    :natural
    :bpm    160
    :scale  [:e :minor 1]
    :geom   :dodecahedron
    :colors ["#030514" "#00e5ff"]
    :cutoff 5200
    :tracks
    {:kick      {:notes (pattern "k_ . . .  . . k_ .  . . k_ .  . . . .") :step "16n"}
     :snare     {:notes (pattern ". . . .  rs . . .  . . . .  rs . g_ .") :step "16n"}
     :hats      {:notes (pattern "h_ . h_ .  . . o_ .  h_ . h_ .  . . o_ .") :step "16n"}
     :sub       {:inst :sub-pure
                 :notes (deg :e :minor [1 _ _ _ _ _ _ _ 6 _ _ _ _ _ _ _ 4 _ _ _ _ _ _ _ 7 _ _ _ _ _ _ _] {:octave 1})
                 :step "16n" :dur "4n" :vel 0.70}
     :pad       {:inst :pad-shimmer
                 :notes [(chord :e :min9 {:octave 3})
                         (chord :c :maj9 {:octave 3})
                         (chord :a :min7 {:octave 3})
                         (chord :d :dom7 {:octave 3})]
                 :step "2m" :dur "2m" :vel 0.1}}}

   :neuro-tech
   {:name   "Dystopian Tech Menace"
    :mod    :industrial
    :bpm    175
    :scale  [:f :phrygian 1]
    :geom   :box
    :colors ["#020402" "#00ff33"]
    :cutoff 3200
    :tracks
    {:drums {:notes [[:kick 1.0 "F1"] nil [:hh-c 0.45] nil
                     [:sn-crack 1.0 "F3"] nil nil [:hh-c 0.4]
                     [:kick 0.95 "F1"] nil nil [:hh-c 0.4]
                     [:sn-crack 1.0 "F3"] nil [:hh-o 0.6] nil]
             :step "16n"}
     :bass  {:inst :bass-neuro
             :notes (deg :f :phrygian [1 1 _ 2 _ 1 _ :b5 1 _ :b7 1 _ 3 2 _] {:octave 1})
             :step "16n" :dur "16n" :vel 0.74}
     :sub   {:inst :sub-pure
             :notes (deg :f :phrygian [1 1 _ 2 _ 1 _ :b5 1 _ :b7 1 _ 3 2 _] {:octave 1})
             :step "16n" :dur "16n" :vel 0.72}
     :stab  {:inst :lead-fm
             :notes (deg :f :phrygian [nil nil nil 1 nil nil :b5 nil nil nil 2 nil nil :b7 nil nil] {:octave 2})
             :step "16n" :dur "32n" :vel 0.4}}}

   :cyber-dub
   {:name   "Echo Chamber Cavern"
    :mod    :analog
    :bpm    130
    :scale  [:c :minor 1]
    :geom   :icosahedron
    :colors ["#05080c" "#00e5a3"]
    :cutoff 2600
    :tracks
    {:kick   {:notes (pattern "k . . .  . . . .  k . . .  . . . .") :step "16n"}
     :snare  {:notes (pattern ". . . .  rs . . .  . . . .  rs . . .") :step "16n"}
     :hats   {:notes (pattern "h . clk .  h . o .  h . sp .  h . o .") :step "16n"}
     :bass   {:inst :sub-pure
              :notes (deg :c :minor [1 _ _ _ 1 _ _ _ 1 _ _ _ :b7 _ 5 _] {:octave 1})
              :step "16n" :dur "8n" :vel 0.75}
     :chords {:inst :pad-glass
              :notes [(chord :c :min9 {:octave 3}) nil nil nil
                      (chord :bb :sus4 {:octave 3}) nil nil nil]
              :step "4n" :dur "8n" :vel 0.35}
     :drone  {:inst :pad-drone
              :notes [(chord :c :min9 {:octave 2})]
              :step "1m" :dur "1m" :vel 0.35}}}

   :deep-minimal
   {:name   "Obsidian Minimal Pulse"
    :mod    :analog
    :bpm    126
    :scale  [:d :dorian 1]
    :geom   :octahedron
    :colors ["#0a0b0d" "#e2e8f0"]
    :cutoff 3000
    :tracks
    {:kick  {:notes (pattern "k . . .  k . . .  k . . .  k . . .") :step "16n"}
     :hats  {:notes (pattern ". . o! .  . . o! .  . . o! .  . . o! .") :step "16n"}
     :perc  {:notes (pattern ". clk . .  . . . .  cb . . .  . . clk .") :step "16n"}
     :bass  {:inst :bass-analog
             :notes (deg :d :dorian [_ 1 _ 1 _ 1 _ 1 _ 1 _ 1 _ :b7 _ 1] {:octave 1})
             :step "16n" :dur "32n" :vel 0.85}
     :synth {:inst :lead-pluck
             :notes (deg :d :dorian [1 _ _ 3 _ 5 _ 4 _ _ 7 _ 5 _ 4 _] {:octave 2})
             :step "16n" :dur "16n" :vel 0.35}}}

   :synthwave-run
   {:name   "Neon Highway Outrun"
    :mod    :analog
    :bpm    124
    :scale  [:a :minor 1]
    :geom   :torus-knot
    :colors ["#12031a" "#ff00aa"]
    :cutoff 5500
    :tracks
    {:drums   {:notes [[:kick 1.0 "A1"] [:hh-c 0.4] [:hh-c 0.4] [:hh-c 0.4]
                       [:clap 0.95] [:hh-c 0.4] [:kick 0.85 "A1"] [:hh-c 0.4]
                       [:kick 0.95 "A1"] [:hh-c 0.4] [:hh-c 0.4] [:hh-c 0.4]
                       [:clap 0.95] [:tom-high 0.8 "D3"] [:tom-mid 0.85 "A2"] [:tom-low 0.9 "D2"]]
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
   {:name   "Cyborg Motor Circuit"
    :mod    :analog
    :bpm    134
    :scale  [:e :phrygian 1]
    :geom   :dodecahedron
    :colors ["#040a18" "#00aaff"]
    :cutoff 4000
    :tracks
    {:kick  {:notes (pattern "k! . . .  . . k_ .  . . k! .  k . . .") :step "16n"}
     :snare {:notes (pattern ". . . .  cp! . . .  . . . .  cp! . clk_ .") :step "16n"}
     :hats  {:notes (pattern "h . cb .  . . o! .  h . cb .  . clk o! .") :step "16n"}
     :bass  {:inst :bass-slap
             :notes (deg :e :phrygian [1 _ 1 _ 2 _ 1 _ _ 1 _ 3 2 _ 1 _] {:octave 1})
             :step "16n" :dur "16n" :vel 0.76}
     :sub   {:inst :sub-808
             :notes (deg :e :phrygian [1 _ _ _ _ _ _ _ 2 _ _ _ 1 _ _ _] {:octave 1})
             :step "16n" :dur "4n" :vel 0.75}
     :lead  {:inst :lead-pluck
             :notes (deg :e :phrygian [1 _ _ 2 _ 1 _ _ _ 1 3 _ 2 _ 1 _] {:octave 2})
             :step "16n" :dur "16n" :vel 0.4}}}

   :hardcore-rave
   {:name   "Resurrection Warehouse Rave"
    :mod    :natural
    :bpm    165
    :scale  [:c :dorian 1]
    :geom   :box
    :colors ["#050505" "#ffee00"]
    :cutoff 6000
    :tracks
    {:kick  {:notes (pattern "k . . .  k . . .  k . . .  k . . .") :step "16n"}
     :snare {:notes (pattern ". . . .  s! . . .  . . . .  s! . . s_") :step "16n"}
     :hats  {:notes (pattern ". . o! .  . . o! .  . . o! .  . . o! .") :step "16n"}
     :bass  {:inst :bass-reese
             :notes (deg :c :dorian [1 _ 1 _ 3 _ 1 _ :b7 _ 1 _ 5 _ 4 _] {:octave 1})
             :step "16n" :dur "16n" :vel 0.78}
     :lead  {:inst :lead-hoover
             :notes (deg :c :dorian [1 _ _ 3 _ 5 _ 8 _ 7 _ 5 _ 4 _ 3] {:octave 2})
             :step "16n" :dur "16n" :vel 0.45}}}

   :dubstep-wobble
   {:name   "Seismic Sound System"
    :mod    :natural
    :bpm    140
    :scale  [:d :minor 1]
    :geom   :octahedron
    :colors ["#0a0212" "#bb00ff"]
    :cutoff 3200
    :tracks
    {:drums   {:notes [[:kick 1.0 "D1"] [:hh-c 0.4] [:hh-c 0.35] [:hh-clk 0.3]
                       [:sn-crack 1.0 "D3"] [:hh-c 0.4] [:hh-o 0.6] [:hh-c 0.35]
                       [:kick 0.8 "D1"] [:hh-c 0.4] [:kick 0.9 "D1"] [:hh-clk 0.35]
                       [:sn-crack 1.0 "D3"] [:splash 0.65] [:sn-gh 0.35] [:hh-o 0.55]]
               :step "16n"}
     :sub     {:inst :sub-pure
               :notes (deg :d :minor [1 _ _ _ 1 _ _ _ :b7 _ _ _ 5 _ 4 _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.74}
     :bass    {:inst :bass-neuro
               :notes (deg :d :minor [1 _ _ _ 1 _ _ _ :b7 _ _ _ 5 _ 4 _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.74}
     :strings {:inst :pad-cinema
               :notes [(chord :d :min9 {:octave 3})
                       (chord :c :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}}}

   :ambient-temple
   {:name   "Sanctuary of Silence"
    :mod    :natural
    :bpm    110
    :scale  [:e :in-sen 2]
    :geom   :sphere
    :colors ["#030a08" "#d4af37"]
    :cutoff 4000
    :tracks
    {:drums   {:notes [[:tom-low 0.75 "E2"] [:ride-bell 0.6] nil [:hh-clk 0.3]
                       [:sn-gh 0.25] nil [:hh-c 0.3] [:splash 0.55]
                       [:tom-mid 0.8 "A2"] nil [:hh-c 0.25] nil
                       [:sn-clk 0.35] nil [:hh-o 0.35] nil]
               :step "16n"}
     :bells   {:inst :pad-glass
               :notes (deg :e :in-sen [1 _ 2 _ 3 _ 5 _ 4 _ 3 _ 2 _ 1 _] {:octave 3})
               :step "16n" :dur "8n" :vel 0.25}
     :drone   {:inst :pad-drone
               :notes [(chord :e :min9 {:octave 2})]
               :step "1m" :dur "1m" :vel 0.3}
     :strings {:inst :lead-string
               :notes (deg :e :in-sen [1 _ _ _ 2 _ _ _ 4 _ _ _ 5 _ 4 _] {:octave 3})
               :step "16n" :dur "4n" :vel 0.28}}}

   :glitch-hop
   {:name   "Fractured Quantum Funk"
    :mod    :idm
    :bpm    105
    :scale  [:g :blues 1]
    :geom   :torus-knot
    :colors ["#120814" "#00ffb7"]
    :cutoff 4200
    :tracks
    {:drums   {:notes (pattern "k! hc_ cb k_  s! hc_ h g_  sp k_ crack clk_  s! o_ roll clk!")
               :step "16n"}
     :bass    {:inst :bass-slap
               :notes (deg :g :blues [1 _ 1 _ :b3 _ 3 _ 4 _ :b5 5 _ 1 _ _] {:octave 1})
               :step "16n" :dur "16n" :vel 0.95}
     :lead    {:inst :lead-pluck
               :notes (deg :g :blues [_ 1 _ :b3 _ 4 _ 5 _ :b7 _ 8 _ 5 _ 3] {:octave 2})
               :step "16n" :dur "16n" :vel 0.4}
     :glitch  {:inst :lead-fm
               :notes (deg :g :blues [nil nil 1 nil nil :b5 nil nil nil 4 nil nil nil 8 nil nil] {:octave 3})
               :step "16n" :dur "32n" :vel 0.35}}}

   :industrial-techno
   {:name   "Iron Foundry Assault"
    :mod    :industrial
    :bpm    138
    :scale  [:b :phrygian 1]
    :geom   :box
    :colors ["#080808" "#ff1122"]
    :cutoff 2800
    :tracks
    {:kick  {:notes (pattern "k . . .  k . . .  k . . .  k . . .") :step "16n"}
     :hats  {:notes (pattern ". . o! .  . . o! .  . . o! .  . . o! .") :step "16n"}
     :perc  {:notes (pattern ". . . ch  . rs . .  . cb . .  . . . crack") :step "16n"}
     :bass  {:inst :bass-analog
             :notes (deg :b :phrygian [1 1 1 1 1 1 1 1 2 2 2 2 1 1 1 1] {:octave 1})
             :step "16n" :dur "32n" :vel 0.9}
     :sub   {:inst :sub-pure
             :notes (deg :b :phrygian [1 _ _ _ 1 _ _ _ 1 _ _ _ 2 _ _ _] {:octave 1})
             :step "16n" :dur "8n" :vel 0.95}
     :drone {:inst :lead-fm
             :notes (deg :b :phrygian [1 _ _ _ _ _ _ _ 2 _ _ _ _ _ _ _] {:octave 2})
             :step "16n" :dur "8n" :vel 0.4}}}

   :chiptune-odyssey
   {:name   "Pixelated Starquest"
    :mod    :analog
    :bpm    150
    :scale  [:c :major 1]
    :geom   :octahedron
    :colors ["#020b08" "#00ff66"]
    :cutoff 7000
    :tracks
    {:kick  {:notes (pattern "k . . .  . . . .  k . . .  . . . .") :step "16n"}
     :snare {:notes (pattern ". . . .  s . . .  . . . .  s . . .") :step "16n"}
     :hats  {:notes (pattern ". . h .  . . h .  . . h .  . . o .") :step "16n"}
     :bass  {:inst :lead-8bit
             :notes (deg :c :major [1 1 1 1 5 5 5 5 6 6 6 6 4 4 4 4] {:octave 1})
             :step "16n" :dur "16n" :vel 0.85}
     :lead  {:inst :lead-8bit
             :notes (deg :c :major [1 3 5 8 5 3 1 3 4 6 8 6 5 3 2 1] {:octave 3})
             :step "16n" :dur "16n" :vel 0.45}}}

   :psy-trance
   {:name   "Cosmic Warp Vortex"
    :mod    :analog
    :bpm    145
    :scale  [:f :phrygian 1]
    :geom   :icosahedron
    :colors ["#050014" "#ff00d4"]
    :cutoff 4800
    :tracks
    {:kick  {:notes (pattern "k . . .  k . . .  k . . .  k . . .") :step "16n"}
     :hats  {:notes (pattern ". . o! .  . . o! .  . . o! .  . . o! crack") :step "16n"}
     :bass  {:inst :bass-analog
             :notes (deg :f :phrygian [_ 1 1 1 _ 1 1 1 _ 1 1 1 _ 2 2 1] {:octave 1})
             :step "16n" :dur "32n" :vel 0.78}
     :lead  {:inst :bass-303
             :notes (deg :f :phrygian [nil 1 1 2 nil 1 3 1 nil 1 1 4 nil 3 2 1] {:octave 2})
             :step "16n" :dur "16n" :vel 0.45}}}

   :downtempo-chill
   {:name   "Dusty Twilight Vinyl"
    :mod    :natural
    :bpm    88
    :scale  [:c :dorian 1]
    :geom   :sphere
    :colors ["#120c08" "#ffaa77"]
    :cutoff 3200
    :tracks
    {:drums   {:notes [[:kick 0.95 "C1"] [:hh-clk 0.3] [:hh-c 0.35] [:hh-clk 0.25]
                       [:snare 0.85 "C3"] [:sn-gh 0.25] [:hh-c 0.35] [:hh-o 0.45]
                       [:tom-low 0.65 "D2"] [:kick 0.85 "C1"] [:hh-clk 0.3] [:sn-clk 0.4]
                       [:snare 0.85 "C3"] [:splash 0.5] [:hh-c 0.3] [:sn-gh 0.3]]
               :step "16n"}
     :bass    {:inst :sub-pure
               :notes (deg :c :dorian [1 _ _ _ 3 _ _ _ 4 _ _ _ 5 _ _ _] {:octave 1})
               :step "16n" :dur "4n" :vel 0.70}
     :strings {:inst :pad-strings
               :notes [(chord :c :min9 {:octave 3})
                       (chord :f :dom7 {:octave 3})
                       (chord :bb :maj7 {:octave 3})
                       (chord :eb :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}
     :keys    {:inst :pad-glass
               :notes (deg :c :dorian [1 _ 3 _ 5 _ 7 _ 5 _ 4 _ 3 _ 2 _] {:octave 3})
               :step "16n" :dur "8n" :vel 0.3}}}

   :future-garage
   {:name   "Midnight Drizzle 2-Step"
    :mod    :natural
    :bpm    136
    :scale  [:f :minor 1]
    :geom   :dodecahedron
    :colors ["#080c14" "#88aacc"]
    :cutoff 3600
    :tracks
    {:kick  {:notes (pattern "k . . .  . . . .  . . k .  . . . .") :step "16n"}
     :snare {:notes (pattern ". . . .  rs . . .  . . . .  rs . . .") :step "16n"}
     :hats  {:notes (pattern "hc . h .  . . h .  sp o . .  h . hc .") :step "16n"}
     :bass    {:inst :bass-organ
               :notes (deg :f :minor [1 _ _ 1 _ _ :b7 _ _ 5 _ _ 4 _ _ _] {:octave 1})
               :step "16n" :dur "8n" :vel 0.9}
     :pad     {:inst :pad-shimmer
               :notes [(chord :f :min9 {:octave 3})
                       (chord :db :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.35}
     :vocal   {:inst :pad-vocal
               :notes [(chord :f :min9 {:octave 3})
                       (chord :db :maj7 {:octave 3})]
               :step "1m" :dur "1m" :vel 0.32}}}})
