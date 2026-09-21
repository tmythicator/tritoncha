(ns app.demo.tutorial
  "Live-coding audio + visuals tutorial for Tritoncha."
  (:require [app.api :refer [_ arp b! c! chord clear-loops! comp! d definst!
                             defrouting! defscene! deftrack! demo! demo-stop!
                             drive-mode! drop! euc every-n f! fast fb! g! hud!
                             inst! jam! jams! l! m! mod! mod-all! next-jam!
                             pat patch! prev-jam! prog pulse! q! rebass!
                             redrum! relead! repad! rev reverb-mode! rfx! rot
                             route! s! scale scene! set-key! shift slow
                             so! sometimes sometimes-by stack! stats! stop!
                             stop-loop! sw! take-steps toggle-bass!
                             toggle-click! toggle-drums! toggle-leads!
                             toggle-pads! tr-all! u! unbass! undrum! unlead!
                             unpad! unso! unstack! v! w! wet!]]))

(comment
  ;; Tritoncha: Live-Coding Electronic Music + 3D WebGL Studio
  ;;
  ;; Browser Shortcuts:
  ;;   [Ctrl+Enter]       -> Evaluate form under cursor or current line
  ;;   [Ctrl+Shift+Enter] -> Evaluate entire script buffer
  ;;
  ;; Emacs + CIDER Live Performance:
  ;;   M-x cider-connect-cljs -> select shadow -> :app -> (in-ns 'app.core)

  ;; Built-in Jams + Transport
  (jam! :orbital-roller)
  (jam! :street-roller)
  (jam! :orbital-matrix)
  (jam! :acid-roller)

  ;; Cycle through track library
  (next-jam!)
  (prev-jam!)

  ;; Live tempo control and metronome click
  (b! 174)
  (b! 160)
  (toggle-click!)

  ;; Full audio stop
  (stop!)

  ;; Live Stacking + Multi-Track Sequencing
  ;; Launch and hot-swap all tracks in a single form
  (stack!
   [:kick  (pat "k! . . k_  k! . . .  . . k! .  . k_ . .")]
   [:snare (pat ". . s_ .  s! . s_ .  . s_ . s_  s! . s_ s!")]
   [:hat   {:inst :hat-closed :mask (euc 11 16) :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]}]
   [:bass  {:inst :lead-8bit :bus :bus/bass :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95}]
   [:sub   {:inst :sub-pure  :notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _]) :step "16n" :dur "8n" :vel 1.0}]
   [:arp   {:inst :pad-cinema :notes (arp (chord :e :min9 3) :up-down) :mask (euc 7 16) :step "16n" :vel 0.35}])

  ;; Remove tracks from stack
  (unstack! :arp)
  (stop-loop! :kick)
  (clear-loops!)

  ;; Breakbeat Masterclass: Articulations and Ghost Rudiments
  ;;
  ;; Mini-notation modifiers:
  ;;   ! -> Accent (vel 1.15)
  ;;   _ -> Ghost note (vel 0.35)
  ;;   (no suffix) -> Normal hit (vel 0.90)
  ;;   . or _ (standalone) -> Musical rest (nil)
  ;;
  ;; Expanded drum palette tokens:
  ;;   k (kick)           s (snare)          rs (rimshot)        clk (snare click)
  ;;   ck (snare crack)   g (snare ghost)    roll (snare roll)
  ;;   h (hat closed)     o (hat open)       hc (hat click)      cp (clap)
  ;;   th (tom high)      tm (tom mid)       tl (tom low)
  ;;   rd (ride cymbal)   rb (ride bell)
  ;;   cr16 / cr17 / cr18 (crashes)          sp (splash)         ch (china)
  ;;   cb (cowbell)

  ;; Interlocking syncopated kick, backbeat rim accents, and rolling ghost taps
  (b! 168)
  (l! :drums
      {:inst :drums
       :notes (pat "k! . s_ .  s_ k_ s! s_  . s_ k! .  s! s_ s_ s!")
       :step "16n"})

  ;; Reverse Paradiddle Snare Rudiment
  ;; Pure snare phrasing with alternating accents and ghost rolls
  (l! :snare
      {:inst :snare
       :notes (pat "s! s_ s_ s!  s_ s! s! s_  s_ s_ s! s_  s! s_ s_ s!")
       :step "16n"})

  ;; Polyrhythmic Ride Bell and Cymbal Matrix
  ;; Off-beat ride bell, splash cuts, and trashy china accents
  (l! :cymb
      {:inst :drums
       :notes (pat "rb! . rb_ .  rb! . sp! .  rb_ rb! . rb_  cr16! . ch! .")
       :step "16n"})

  ;; Ride cymbal body groove with ride bell accents
  (l! :ride
      {:inst :drums
       :notes (pat "rd . rd_ rd  rb! . rd_ .  rd . rd_ rd  rb! . sp! .")
       :step "16n"})

  ;; 3-Tom Linear Chops (High, Mid, Low Floor Tom)
  (l! :toms
      {:inst :drums
       :notes (pat "th! tm_ tl! k!  th_ tm! tl_ k_  th! tm_ tl! s!  cr18! . . .")
       :step "16n"})

  ;; 32nd-Note Linear Fill (using fast 2x into the drop)
  (l! :fill
      {:inst :drums
       :notes (fast 2 (pat "s! s_ s_ k!  th! tm_ tl! k!  s! s_ s_ s!  cr16! . . ."))
       :step "16n"})

  ;; Full Breakbeat Stack
  (stack!
   [:kick  (pat "k! . . k_  . k_ k! .  k! . . k_  . k! . .")]
   [:snare (pat ". . s_ .  s! s_ . s_  . s_ s_ s!  s_ . s! s_")]
   [:ride  (pat "rb! . rb_ .  rb! . rb_ rb_  rb! . sp! .  rb_ rb! ch! .")]
   [:toms  (pat ". . . .  . . . .  . . . .  th! tm_ tl! .")]
   [:bass  {:notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _] 1) :step "16n" :dur "16n" :vel 0.95}]
   [:sub   {:notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _] 1) :step "16n" :dur "8n" :vel 1.0}]
   [:pad   {:notes (arp (chord :e :min9 3) :up-down) :mask (euc 7 16) :step "16n" :vel 0.35}])

  ;; Algorithmic Time Transforms + Probability
  ;; Double-time bass roll (fast 2x):
  (l! :bass {:notes (fast 2 (d [1 _ 1 2 _ 1 4 3])) :step "16n" :vel 0.95})

  ;; Halftime bass breakdown (slow 2x):
  (l! :bass {:notes (slow 2 (d [1 _ 1 2 _ 1 4 3])) :step "16n" :vel 0.95})

  ;; Reverse the bass melody:
  (l! :bass {:notes (rev (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])) :step "16n"})

  ;; Rotate notes by offset steps:
  (l! :bass {:notes (rot 2 (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])) :step "16n"})

  ;; Restore standard bass:
  (l! :bass {:notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95})

  ;; Double-time kick drum buildup before the drop:
  (l! :kick {:notes (fast 2 (pat "k . . .  k . . .")) :step "16n"})

  ;; Restore syncopated breakbeat:
  (l! :kick {:notes (pat "k . . .  k . . .  . . k .  . . . .") :step "16n"})

  ;; Threading Pipelines (->>)
  ;; Shifting and doubling an arpeggio on the fly
  (l! :arp
      {:inst :pad-cinema
       :notes (->> (chord :e :min9 3)
                   (arp :up-down)
                   (fast 2)
                   (shift 2))
       :step "16n"
       :vel 0.45})

  ;; Probabilistic reverse: 50% chance to flip each bar
  (l! :arp
      {:inst :pad-cinema
       :notes (->> (chord :e :min9 3)
                   (fast 8)
                   (arp :up-down)
                   (sometimes rev))
       :step "16n"
       :vel 0.9})

  ;; Morphing degree melody pipeline
  (l! :bass
      {:inst :bass-analog
       :notes (->> [1 _ 1 2 _ 1 4 3]
                   (fast 2)
                   (shift 1)
                   (d 1))
       :step "16n"
       :dur "16n"
       :vel 0.95})

  ;; Breakbeat transformation pipeline
  (l! :kick
      {:pattern (->> "k . . .  k . . .  . . k .  . . . ."
                     (pat)
                     (fast 2)
                     (shift 4))
       :step "64n"})

  ;; Polyrhythmic truncation with take-steps
  (l! :hat
      {:inst :hat-closed
       :mask (->> (euc 7 16)
                  (shift 2)
                  (take-steps 12))
       :step "16n"
       :dur "32n"
       :vel 0.6})

  ;; Probabilistic pitch mutations with sometimes-by and every-n
  (l! :lead
      {:inst :lead-pluck
       :notes (->> (chord :e :min9 3)
                   (arp :random)
                   (sometimes-by 0.3 rev)
                   (every-n 4 (partial fast 2)))
       :step "16n"
       :vel 0.4})

  ;; Music Theory, Scales, Degrees and Progressions
  ;; Global Key Context
  (set-key! :e :phrygian 1)

  ;; Scale Degree Sequences (rests nil or _)
  (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])

  ;; Chords and Voicings
  (chord :e :min9 3)
  (chord :f :dark-m9 3)
  (chord :d :dom7 3)

  ;; Chord Progression DSL
  (prog [[1 :min9]
         [6 :maj9]
         [4 :min7]
         [7 :dom7]] 3)

  ;; Arpeggiators (:up, :down, :up-down, :down-up, :random)
  (arp (chord :e :min9 3) :up-down)
  (arp (chord :e :min9 3) :random)

  ;; Scales and Modes
  (scale :d :dorian)
  (scale :e :hirajoshi 2)
  (scale :a :hungarian-minor)
  (scale :c :blues)

  ;; Live Harmonic Modulation (mod-all!, tr-all!)
  ;; Shift entire jam key and scale
  (mod-all! :d :dorian)
  (mod-all! :f# :hirajoshi)
  (mod-all! :a :hungarian-minor)
  (mod-all! :b :arabic)
  (mod-all! :c :blues)

  ;; Return back to default E phrygian
  (mod-all! :e :phrygian)

  ;; Live pitch transposition (by +/- semitones)
  (tr-all! 3)
  (tr-all! -3)
  (tr-all! -1)
  (tr-all! 1)

  ;; Modular DSP Routing Graph DSL + FX Chains
  ;; Switch built-in routing topologies
  (route! :cyber-dub)          ;; Sub-heavy dub delay and distorted bass
  (route! :ambient-prism)      ;; Wide FDN reverb diffusion on space bus
  (route! :industrial-crush)   ;; Distorted drums and bitcrushed lead
  (route! :crematorium)        ;; Resonant bandpass filter sweeps
  (route! :studio-master)      ;; Clean mastering compression and delay
  (route! :default)            ;; Transparent reference routing

  ;; Reset active processor states and clear delay or reverb tails
  (rfx!)

  ;; Define custom routing topology live from REPL
  ;; :out bypasses master bus processors (straight to DAC)
  (defrouting! :space-dub-custom
    {:graph {:drums  [:crusher :compressor :out]
             :bass   [:distort :filter]
             :lead   [:delay :reverb]
             :space  [:reverb :out]
             :master [:compressor :limiter]}
     :processors {:filter     [2800 0.85]
                  :crusher    {:bits 8.0 :sample-hold 1.4 :drive 0.25}
                  :distort    {:drive 0.55 :algo :adaa :wet 0.85}
                  :delay      ["8n." 0.45 0.35]
                  :reverb     {:algo :fdn :size 0.92 :wet 0.48}
                  :compressor {:threshold -14.0 :ratio 3.5 :attack 0.010 :release 0.090 :makeup 2.0 :mix 1.0}}})

  (route! :space-dub-custom)

  ;; Live FX, Mixer Bus Controls and Mutes
  ;; Instrument family mutes
  (undrum!)
  (redrum!)
  (toggle-drums!)

  (unbass!)
  (rebass!)
  (toggle-bass!)

  (unlead!)
  (relead!)
  (toggle-leads!)

  (unpad!)
  (repad!)
  (toggle-pads!)

  ;; Track mutes and solos
  (m! :kick :snare)
  (u! :kick :snare)
  (so! :bass)
  (unso!)

  ;; 4-second opening filter sweep into the drop
  (sw! 300 7000 4)

  ;; Master lowpass cutoff and resonance
  (f! 3200)
  (q! 0.7)

  ;; Master delay feedback and reverb wet mix
  (fb! 0.6)
  (wet! 0.45)

  ;; Master compressor adjustment
  (comp! -14.0 4.0 0.01 0.09 2.0 1.0)

  ;; Bus volume control (in dB)
  (v! :bus/drums +2)
  (v! :bus/space -3)

  ;; Dub FX One-Shots
  (s!)      ;; Dub laser siren
  (drop!)   ;; Seismic sub-bass drop

  ;; Rust WASM Audio Core sound modes
  (mod! :idm)          ;; Drum synthesis mode (:natural, :analog, :idm, :industrial)
  (drive-mode! :adaa)  ;; ADAA anti-aliased saturation (:tan-h, :soft, :adaa)
  (reverb-mode! :fdn)   ;; Feedback Delay Network reverb (:fdn, :freeverb)

  ;; Live Sound Design: Patching + Custom Instruments
  ;; Live patch tweak without redefining
  (patch! :bass-analog :filter {:cutoff 1400 :drive 0.6 :q 0.5})
  (patch! :lead-8bit   :osc {:pulse-width 0.4})

  ;; Define custom synthesizer voice
  (definst! :supersaw-custom
    {:category :leads
     :type     :poly
     :bus      :bus/lead
     :osc      {:type :supersaw :sub-level 0.35 :drift 0.25}
     :filter   {:type :ladder :cutoff 2400 :q 0.55 :drive 0.35 :env-amount 4500 :key-track 2.0}
     :amp-env  {:attack 0.005 :decay 0.22 :sustain 0.35 :release 0.20}
     :mod-env  {:attack 0.005 :decay 0.22}
     :glide    0.035})

  ;; Preview instrument
  (demo! :supersaw-custom)
  (demo-stop!)

  ;; Plug the new synth directly into a live loop
  (l! :lead {:inst :supersaw-custom :notes (d [1 3 4 5 7 8 5 3] 3) :step "16n" :vel 0.5})

  ;; Three.js WebGL Visual Controls + 3D Scenes
  (scene! :synthwave-grid)
  (scene! :star-tunnel)
  (scene! :orbital-matrix)
  (scene! :dna-nexus)
  (scene! :cyber-torus)

  ;; Geometry morphing: :icosahedron | :torus-knot | :octahedron | :box | :sphere | :dodecahedron
  (g! :torus-knot)

  ;; Toggle wireframe view
  (w!)

  ;; Color scheme adjustment (Hex strings for background and mesh)
  (c! "#04000c" "#00ffff")
  (c! "#100404" "#ff0055")

  ;; Audio-reactive pulse trigger
  (pulse! 2.5)

  ;; Custom 3D Scene definition
  (defscene! :neon-grid-custom
    {:geom :torus-knot
     :colors {:bg "#000000" :mesh "#00ffff" :wire "#ff007f" :outer "#220033"}
     :material {:wireframe true :roughness 0.1 :metalness 0.9}})

  (scene! :neon-grid-custom)

  ;; Defining Complete Tracks (deftrack! -> jam!)
  (deftrack! :cyber-roller-custom
    {:name    "Custom Cyber Roller"
     :bpm     174
     :scale   [:d :dorian 1]
     :geom    :torus-knot
     :colors  ["#080412" "#00ffaa"]
     :routing :ambient-prism
     :tracks
     {:drums {:pattern (pat "k . . .  s . . .  . . k .  s . . g") :step "16n"}
      :hat   {:inst :hat-closed :mask (euc 11 16) :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]}
      :bass  {:inst :bass-analog :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95}
      :sub   {:inst :sub-pure    :notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _]) :step "16n" :dur "8n" :vel 1.0}
      :pad   {:inst :pad-cinema  :notes (prog [[1 :min9] [6 :maj9] [4 :min7] [7 :dom7]] 3) :step "1m" :dur "1m" :vel 0.4}}})

  (jam! :cyber-roller-custom)

  ;; UI Overlays and Realtime Diagnostics
  (stats!)             ;; Toggle telemetry HUD modal (or press [I])
  (hud!)               ;; Toggle live active track loop HUD
  (inst!)              ;; Toggle instrument browser overlay
  (jams!)              ;; Toggle track browser modal

  ;; Full Audio Stop
  (stop!))
