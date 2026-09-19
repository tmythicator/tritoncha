(ns app.lib.tracks
  "Built-in track library and baseline catalog for Tritoncha."
  (:require [app.audio.theory.harmony :refer [_ deg prog]]
            [app.audio.theory.patterns :refer [pattern]]))

(def core-tracks-catalog
  [[:orbital-roller
    {:name   "Facing Moons"
     :mod    :natural
     :bpm    160
     :scale  [:e :minor 1]
     :geom   :dodecahedron
     :colors ["#030514" "#00e5ff"]
     :cutoff 5200
     :tracks
     {:kick      {:notes (pattern "k_ . . .  . . k_ .  . . k_ .  . . . .") :step "16n"}
      :snare     {:notes (pattern ". . . .  s . . .  . . . .  s . g_ .") :step "16n"}
      :hats      {:notes (pattern "h_ . h_ .  . . o_ .  h_ . h_ .  . . o_ .") :step "16n"}
      :sub       {:inst :sub-pure
                  :notes (deg [1 _ _ _ _ _ _ _ 6 _ _ _ _ _ _ _ 4 _ _ _ _ _ _ _ 7 _ _ _ _ _ _ _] 1)
                  :step "16n" :dur "4n" :vel 0.70}
      :pad       {:inst :pad-dreamy
                  :notes (prog [[1 :min9]
                                [6 :maj9]
                                [4 :min7]
                                [7 :dom7]] 3)
                  :step "2n" :dur "2n" :vel 0.23}}}]

   [:street-roller
    {:name   "Messenger On A Fixie"
     :mod    :natural
     :bpm    172
     :scale  [:f :minor 1]
     :geom   :icosahedron
     :colors ["#030814" "#00e5ff"]
     :cutoff 4400
     :tracks
     {:kick    {:notes (pattern "k . . .  . . k .  . . k .  . . . .") :step "16n"}
      :snare   {:notes (pattern ". . . .  s! . . .  . . . .  s! g_ g_ g_") :step "16n"}
      :hats    {:notes (pattern "h_ . hc .  . . o_ .  h_ . hc .  . . o_ .") :step "16n"}
      :sub     {:inst :bass-liquid
                :notes (deg [1 1 _ 1  3 _ 5 _  1 _ 7 1  _ 6 _ 5] 1)
                :step "16n" :dur "8n" :vel 0.55}
      :strings {:inst :pad-drone
                :notes (prog [[1 :min9]
                              [6 :maj9]
                              [7 :dom7]
                              [5 :min7]] 3)
                :step "1m" :dur "1m" :vel 0.26}}}]

   [:orbital-matrix
    {:name   "Orbital Breaking Bits"
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
                :notes (pattern ". . . .  s . . .  . . . .  s . g_ .
                                . . . .  s . . g_  . . . .  s . clk_ .")
                :step "16n"}
      :hats    {:figure :ring
                :notes (pattern "hc_ . h_ .  hc_ . o_ .  hc_ . h_ hc_  hc_ . o_ .
                                hc_ . h_ .  hc_ . o_ .  hc_ hc_ h_ .  hc_ . o_ .")
                :step "16n"}
      :bass    {:figure :pillar
                :inst :bass-liquid
                :notes (deg [1 _ 1 2  _ 1 4 3  1 _ 5 4  _ 2 1 _
                             1 _ _ 2  _ 1 :b5 _  1 _ 4 3  _ 2 1 _] 1)
                :step "16n" :dur "8n" :vel 0.70}
      :echo    {:figure :sat
                :inst :pad-glass
                :bus  :bus/space
                :notes (deg [nil nil 1 nil  nil 3 nil nil  nil :b5 nil 5  nil 4 nil 2] 3)
                :step "16n" :dur "16n" :vel 0.35}}}]

   [:industrial-techno
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
      :perc  {:notes (pattern "s . . ch  s . . .  . cb . .  s_ s_ s_ ck") :step "16n"}
      :bass  {:inst :bass-analog
              :notes (deg [1 1 1 1 1 1 1 1 2 2 2 2 1 1 1 1] 1)
              :step "16n" :dur "32n" :vel 0.45}
      :sub   {:inst :sub-pure
              :notes (deg [1 _ _ _ 1 _ _ _ 1 _ _ _ 2 _ _ _] 1)
              :step "16n" :dur "8n" :vel 0.7}
      :drone {:inst :lead-fm
              :notes (deg [1 _ _ _ _ _ _ _ 2 _ _ _ _ _ _ _] 2)
              :step "16n" :dur "8n" :vel 0.4}}}]

   [:downtempo-chill
    {:name   "Twilight Peaks"
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
      :sub     {:inst :sub-moog
                :notes (deg [1 _ _ _ 3 _ _ _ 4 _ _ _ 5 _ _ _] 3)
                :step "16n" :dur "4n" :vel 0.70}
      :strings {:inst :pad-strings
                :notes (prog [[1 :min9]
                              [4 :dom7]
                              [7 :maj7]
                              [3 :maj7]] 3)
                :step "1m" :dur "1m" :vel 0.35}
      :keys    {:inst :pad-glass
                :notes (deg [1 _ 3 _ 5 _ 7 _ 5 _ 4 _ 3 _ 2 _] 3)
                :step "16n" :dur "8n" :vel 0.3}}}]

   [:acid-roller
    {:name   "Deck 2016"
     :mod    :analog
     :bpm    130
     :scale  [:a :aeolian 1]
     :geom   :octahedron
     :colors ["#140404" "#ff3300"]
     :cutoff 3800
     :tracks
     {:kick  {:notes (pattern "k_ . . .  . . . k_  . . k_ .  . . . .") :step "16n"}
      :snare {:notes (pattern ". . . .  rs . . .  . . . .  rs . g_ .") :step "16n"}
      :hats  {:notes (pattern "h_ . hc .  . . o_ .  h_ . h_ .  . . o_ .") :step "16n"}
      :bass  {:inst :bass-303
              :notes (deg [1 _ _ 3 1 _ 4 _ 1 _ :b7 1 _ _ 5 _] 1)
              :step "16n" :dur "16n" :vel 0.70}
      :sub   {:inst :sub-pure
              :notes (deg [1 _ _ _ _ _ 1 _ 1 _ _ _ _ _ 5 _] 1)
              :step "16n" :dur "8n" :vel 0.68}}}]

   [:martian-drift
    {:name   "Phobos Moon"
     :mod    :natural
     :bpm    138
     :scale  [:d :phrygian 1]
     :geom   :sphere
     :colors ["#0a0302" "#ff6600"]
     :cutoff 4200
     :tracks
     {:kick    {:notes (pattern "k_ . . .  . . . .  k_ . . .  . . k_ .") :step "16n"}
      :snare   {:notes (pattern ". . s! .  ck_ . . .  . . sp_ .  s! . g_ .") :step "16n"}
      :bass    {:inst :bass-liquid
                :notes (deg [1 _ _ _ _ _ 2 _ 1 _ _ _ :b5 _ _ _] 1)
                :step "16n" :dur "8n" :vel 0.65}
      :drone   {:inst :pad-cinema
                :notes (prog [[1 :dark-m9]
                              [6 :maj7]
                              [2 :maj7]
                              [7 :min7]] 2)
                :step "2m" :dur "2m" :vel 0.28}
      :echo    {:inst :pad-glass
                :notes (deg [nil nil 1 nil nil :b2 nil nil nil :b5 nil nil nil 4 nil nil] 3)
                :step "16n" :dur "16n" :vel 0.24}}}]

   [:neuro-tech
    {:name   "Neuro-Dystopia"
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
              :notes (deg [1 1 _ 2 _ 1 _ :b5 1 _ :b7 1 _ 3 2 _] 1)
              :step "16n" :dur "16n" :vel 0.74}
      :sub   {:inst :sub-pure
              :notes (deg [1 1 _ 2 _ 1 _ :b5 1 _ :b7 1 _ 3 2 _] 1)
              :step "16n" :dur "16n" :vel 0.72}
      :stab  {:inst :lead-fm
              :notes (deg [nil nil nil 1 nil nil :b5 nil nil nil 2 nil nil :b7 nil nil] 2)
              :step "16n" :dur "32n" :vel 0.4}}}]

   [:glow-dub
    {:name   "Glowing Cavern"
     :mod    :analog
     :bpm    130
     :scale  [:c :minor 1]
     :geom   :icosahedron
     :colors ["#05080c" "#00e5a3"]
     :cutoff 2600
     :tracks
     {:kick   {:notes (pattern "k . . .  . . . .  k . . .  . . . .") :step "16n"}
      :snare  {:notes (pattern ". . . .  rs . . .  . . . .  rs . . .") :step "16n"}
      :hats   {:notes (pattern "h . hc .  h . o .  h . sp .  h . o .") :step "16n"}
      :bass   {:inst :sub-808
               :notes (deg [1 _ _ _ 1 _ _ _ 1 _ _ _ :b7 _ 5 _] 1)
               :step "16n" :dur "8n" :vel 0.78}
      :chords {:inst :pad-cinema
               :notes (prog [[1 :min9] nil nil nil
                             [7 :sus4] nil nil nil] 3)
               :step "4n" :dur "2n" :vel 0.15}
      :drone  {:inst :pad-drone
               :notes (prog [[1 :min9]] 2)
               :step "1m" :dur "1m" :vel 0.35}}}]

   [:laser-turret
    {:name   "Laser Turret"
     :mod    :analog
     :bpm    126
     :scale  [:d :dorian 1]
     :geom   :octahedron
     :colors ["#0a0b0d" "#e2e8f0"]
     :cutoff 3000
     :tracks
     {:kick  {:notes (pattern "k . . .  k . . .  k . . .  k . . .") :step "16n"}
      :hats  {:notes (pattern ". . o! .  . . o! .  . . o! .  . . o! .") :step "16n"}
      :bass  {:inst :bass-analog
              :notes (deg [_ 1 _ 1  _ 1 _ 1  _ 1 _ 1  _ :b7 _ 1] 1)
              :step "16n" :dur "16n" :vel 0.82}
      :synth {:inst :lead-pluck
              :notes (deg [1 _ _ 3  _ 5 _ 4  _ _ 7 _  5 _ 4 _] 3)
              :step "16n" :dur "16n" :vel 0.36}}}]

   [:synthwave-run
    {:name   "Neon Highway Pursuit"
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
      :bass    {:inst :bass-moog
                :notes (deg [1 1 1 1 1 1 1 1 7 7 7 7 6 6 6 6] 1)
                :step "16n" :dur "16n" :vel 0.9}
      :strings {:inst :lead-blade
                :notes (prog [[1 :min7]
                              [6 :maj7]
                              [3 :maj7]
                              [7 :dom7]] 3)
                :step "1m" :dur "1m" :vel 0.15}
      :arp     {:inst :lead-supersaw
                :notes (deg [1 3 5 8 5 3 1 3] 3)
                :step "16n" :dur "8n" :vel 0.19}}}]

   [:electro-break
    {:name   "Cyborg Factory"
     :mod    :analog
     :bpm    134
     :scale  [:e :phrygian 1]
     :geom   :dodecahedron
     :colors ["#040a18" "#00aaff"]
     :cutoff 4000
     :tracks
     {:kick  {:notes (pattern "k! . . .  . . k_ .  . . k! .  k . . .") :step "16n"}
      :snare {:notes (pattern ". . . .  cp! . . .  . . . .  cp! . clk_ .") :step "16n"}
      :hats  {:notes (pattern "h . cb .  . . o! .  h . cb .  . hc o! .") :step "16n"}
      :bass  {:inst :bass-slap
              :notes (deg [1 _ 1 _ 2 _ 1 _ _ 1 _ 3 2 _ 1 _] 1)
              :step "16n" :dur "16n" :vel 0.76}
      :sub   {:inst :sub-808
              :notes (deg [1 _ _ _ _ _ _ _ 2 _ _ _ 1 _ _ _] 1)
              :step "16n" :dur "4n" :vel 0.75}
      :lead  {:inst :lead-pluck
              :notes (deg [1 _ _ 2 _ 1 _ _ _ 1 3 _ 2 _ 1 _] 2)
              :step "16n" :dur "16n" :vel 0.4}}}]

   [:hardcore-rave
    {:name   "Never Sleep Rave"
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
      :bass  {:inst :liquid-reese
              :notes (deg [1 _ 1 _ 3 _ 1 _ :b7 _ 1 _ 5 _ 4 _] 1)
              :step "16n" :dur "16n" :vel 0.58}
      :lead  {:inst :lead-hoover
              :notes (deg [1 _ _ 3 _ 5 _ 8 _ 7 _ 5 _ 4 _ 3] 2)
              :step "16n" :dur "16n" :vel 0.45}}}]

   [:dubstep-wobble
    {:name   "Seismic Aftershock"
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
                :notes (deg [1 _ _ _ 1 _ _ _ 7 _ _ _ 5 _ 4 _] 1)
                :step "16n" :dur "8n" :vel 0.74}
      :bass    {:inst :liquid-reese
                :notes (deg [1 _ _ _ 1 _ _ _ 7 _ _ _ 5 _ 4 _] 1)
                :step "16n" :dur "8n" :vel 0.74}
      :strings {:inst :pad-cinema
                :notes (prog [[1 :min9]
                              [6 :maj7]] 3)
                :step "1m" :dur "1m" :vel 0.35}}}]

   [:ambient-temple
    {:name   "Sanctuary of Order"
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
                :notes (deg [1 _ 2 _ 3 _ 5 _ 4 _ 3 _ 2 _ 1 _] 3)
                :step "16n" :dur "8n" :vel 0.25}
      :drone   {:inst :pad-drone
                :notes (prog [[1 :min9]] 2)
                :step "1m" :dur "1m" :vel 0.3}
      :strings {:inst :lead-string
                :notes (deg [1 _ _ _ 2 _ _ _ 4 _ _ _ 5 _ 4 _] 3)
                :step "16n" :dur "4n" :vel 0.28}}}]

   [:glitch-hop
    {:name   "Fractured Display"
     :mod    :idm
     :bpm    115
     :scale  [:g :blues 1]
     :geom   :torus-knot
     :colors ["#120814" "#00ffb7"]
     :cutoff 4200
     :tracks
     {:drums   {:notes (pattern "k! hc_ cb k_  s! hc_ h g_  sp k_ ck clk_  s! o_ roll clk!")
                :step "16n"}
      :bass    {:inst :bass-slap
                :notes (deg [1 _ 1 _ 3 _ 3 _ 4 _ 5 _ 5 _ 1 _] 1)
                :step "16n" :dur "16n" :vel 0.95}
      :lead    {:inst :lead-pluck
                :notes (deg [_ 1 _ 3 _ 4 _ 5 _ 7 _ _ _ 5 _ 3] 2)
                :step "16n" :dur "16n" :vel 0.4}
      :glitch  {:inst :lead-fm
                :notes (deg [nil nil 1 nil nil 5 nil nil nil 4 nil nil nil 8 nil nil] 3)
                :step "16n" :dur "32n" :vel 0.35}}}]

   [:chiptune-odyssey
    {:name   "Pixelated Joy"
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
              :notes (deg [1 1 1 1 5 5 5 5 6 6 6 6 4 4 4 4] 1)
              :step "16n" :dur "16n" :vel 0.5}
      :lead  {:inst :lead-8bit
              :notes (deg [1 3 5 8 5 3 1 3 4 6 8 6 5 3 2 1] 3)
              :step "16n" :dur "16n" :vel 0.3}}}]

   [:psy-trance
    {:name   "Warp Speed Vortex"
     :mod    :analog
     :bpm    145
     :scale  [:f :phrygian 1]
     :geom   :icosahedron
     :colors ["#050014" "#ff00d4"]
     :cutoff 4800
     :tracks
     {:kick  {:notes (pattern "k . . .  k . . .  k . . .  k . . .") :step "16n"}
      :hats  {:notes (pattern ". . o! .  . . o! .  . . o! .  . . o! ck") :step "16n"}
      :bass  {:inst :bass-analog
              :notes (deg [_ 1 1 1 _ 1 1 1 _ 1 1 1 _ 2 2 1] 1)
              :step "16n" :dur "32n" :vel 0.78}
      :lead  {:inst :lead-supersaw
              :notes (deg [nil 1 1 2 nil 1 3 1 nil 1 1 4 nil 3 2 1] 2)
              :step "16n" :dur "16n" :vel 0.40}}}]

   [:future-garage
    {:name   "Ease Out"
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
                :notes (deg [1 _ _ 1 _ _ :b7 _ _ 5 _ _ 4 _ _ _] 1)
                :step "16n" :dur "8n" :vel 0.9}
      :pad     {:inst :pad-strings
                :notes (prog [[1 :min9]
                              [6 :maj7]] 3)
                :step "1m" :dur "1m" :vel 0.35}
      :vocal   {:inst :pad-vocal
                :notes (prog [[1 :min9]
                              [6 :maj7]] 3)
                :step "1m" :dur "1m" :vel 0.32}}}]])

(def core-track-order
  (mapv first core-tracks-catalog))

(def core-tracks
  (into {} core-tracks-catalog))
