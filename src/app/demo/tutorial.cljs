(ns app.demo.tutorial
  "Live-coding audio + visuals tutorial for Tritoncha."
  (:require [app.api :refer [_ arp b! c! chord d definst! deftrack! demo! demo-stop!
                             drive-mode! drop! euc every-n f! fast fb! hud! inst! jam! jams!
                             l! mod! mod-all! pat redrum! rev reverb-mode! s! scale scene!
                             shift slow sometimes sometimes-by stack! stats! stop! sw!
                             take-steps tr-all! undrum! v! w! wet!]]))

(comment
  ;; =============================================================================
  ;; TRITONCHA: Live-Coding Electronic Music + 3D WebGL Studio
  ;;
  ;; Web Browser Shortcuts:
  ;;   [Ctrl+Enter]       -> Evaluate form under cursor / current line
  ;;   [Ctrl+Shift+Enter] -> Evaluate entire script buffer
  ;;
  ;; Emacs + CIDER Live Performance:
  ;;   M-x cider-connect-cljs -> select shadow -> :app -> (in-ns 'app.core)
  ;; =============================================================================

  ;; Built-in Jams
  (jam! :orbital-roller)
  (jam! :street-roller)
  (jam! :acid-roller)
  (jam! :ambient-drift)

  ;; Live tempo control
  (b! 174)
  (b! 160)

  ;; Full audio stop
  (stop!)

  ;; Live Stack: Launch and hot-swap all tracks in a single form
  (stack!
   [:kick  (pat "k! . . k_  k! . . .  . . k! .  . k_ . .")]
   [:snare (pat ". . s_ .  s! . s_ .  . s_ . s_  s! . s_ s!")]
   [:hat   {:inst :hh-c :mask (euc 11 16) :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]}]
   [:bass  {:notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95}]
   [:sub   {:notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _]) :step "16n" :dur "8n" :vel 1.0}]
   [:arp   {:inst :pad :notes (arp (chord :e :min9 3) :up-down) :mask (euc 7 16) :step "16n" :vel 0.8}])

  ;; Live Breakbeat Masterclass: Articulations and Ghost Rudiments
  ;;
  ;; Mini-notation modifiers:
  ;;   ! -> Accent (vel 1.15)
  ;;   _ -> Ghost note (vel 0.35)
  ;;   (no suffix) -> Normal hit (vel 0.90)
  ;;   . or _ (standalone) -> Musical rest (nil)
  ;;
  ;; Expanded drum palette:
  ;;   k (kick)           s (snare)          rb (ride bell)      ride (ride body)
  ;;   th (tom high)      tm (tom mid)       tl (tom low)
  ;;   cr16 / cr17 / cr18 (crashes)          sp (splash)         ch (china)
  ;;   cb (cowbell)       h (hat-closed)     o (hat-open)

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

  ;; Time Manipulation on the Fly (fast, slow, rev)

  ;; Double-time bass roll (fast 2x):
  (l! :bass {:notes (fast 2 (d [1 _ 1 2 _ 1 4 3])) :step "16n" :vel 0.95})

  ;; Halftime bass breakdown (slow 2x):
  (l! :bass {:notes (slow 2 (d [1 _ 1 2 _ 1 4 3])) :step "16n" :vel 0.95})

  ;; Reverse the bass melody:
  (l! :bass {:notes (rev (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])) :step "16n"})

  ;; Back to standard bass:
  (l! :bass {:notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95})

  ;; Double-time kick drum buildup before the drop:
  (l! :kick {:notes (fast 2 (pat "k . . .  k . . .")) :step "16n"})

  ;; Restore the syncopated breakbeat:
  (l! :kick {:notes (pat "k . . .  k . . .  . . k .  . . . .") :step "16n"})

  ;; Threading Pipelines (->>)
  ;; 1. Shifting and doubling an arpeggio on the fly
  (l! :arp
      {:inst :pad
       :notes (->> (chord :e :min9 3)
                   (arp :up-down)
                   (fast 2)
                   (shift 2))
       :step "16n"
       :vel 0.45})

  ;; 2. Probabilistic reverse: 50% chance to flip each bar
  (l! :arp
      {:inst :pad
       :notes (->> (chord :e :min9 3)
                   (fast 8)
                   (arp :up-down)
                   (sometimes rev))
       :step "16n"
       :vel 0.9})

  ;; 3. Morphing degree melody pipeline
  (l! :bass
      {:inst :bass
       :notes (->> [1 _ 1 2 _ 1 4 3]
                   (fast 2)
                   (shift 1)
                   (d 1))
       :step "16n"
       :dur "16n"
       :vel 0.95})

  ;; 4. Breakbeat transformation pipeline:
  (l! :kick
      {:pattern (->> "k . . .  k . . .  . . k .  . . . ."
                     (pat)
                     (fast 2)
                     (shift 4))
       :step "64n"})

  ;; 5. Polyrhythmic truncation with take-steps:
  (l! :hat
      {:inst :hh-c
       :mask (->> (euc 7 16)
                  (shift 2)
                  (take-steps 12))
       :step "16n"
       :dur "32n"
       :vel 0.6})

  ;; 6. Probabilistic pitch mutations with sometimes-by and every-n:
  (l! :lead
      {:inst :pad
       :notes (->> (chord :e :min9 3)
                   (arp :random)
                   (sometimes-by 0.3 rev)
                   (every-n 4 (partial fast 2)))
       :step "16n"
       :vel 0.4})

  ;; Harmonic Modulation (mod-all!, tr-all!)

  ;; Shift entire jam to <key> and <scale> (check them out in theory.cljs)
  (mod-all! :d :dorian)
  (mod-all! :f# :hirajoshi)
  (mod-all! :a :hungarian-minor)
  (mod-all! :b :arabic)
  (mod-all! :c :blues)
  (mod-all! :e :blues)
  (mod-all! :e :arabic)

  ;; Return back to default E phrygian
  (mod-all! :e :phrygian)

  ;; Live transposition (by X semitones):
  (tr-all! 3)
  (tr-all! -3)
  (tr-all! -1)
  (tr-all! 1)

  ;; Live FX and Mixer Control

  ;; Mute all drums
  (undrum!)
  ;; 4-second opening filter sweep
  (sw! 300 7000 4)

  ;; Fire Dub Laser Siren
  (s!)

  ;; Sub-bass drop
  (drop!)

  ;; Unmute all drums back
  (redrum!)

  ;; Real-time mixer tweaks

  ;; Smooth lowpass cutoff
  (f! 3200)
  ;; Delay feedback (0 <-> 1)
  (fb! 0.6)
  ;; Wet reverb (0 <-> 1)
  (wet! 0.45)

  ;; Volume control
  (v! :bus/drums +2)
  (v! :bus/space -3)

  ;; Rust WASM Audio Core sound modes
  (mod! :idm)          ;; Drum synthesis mode (:natural, :analog, :idm, :industrial)
  (drive-mode! :adaa)  ;; ADAA anti-aliased saturation (:tan-h, :soft, :adaa)
  (reverb-mode! :fdn)   ;; Feedback Delay Network reverb (:fdn, :freeverb)

  ;; UI Overlays and REPL Introspection Modals
  (stats!)             ;; Toggle telemetry HUD modal (or press [I])
  (hud!)               ;; Toggle live active track loop HUD
  (inst!)              ;; Toggle instrument browser overlay
  (jams!)              ;; Toggle track browser modal

  ;; 3D WebGL Scenes
  (scene! :synthwave-grid)
  (scene! :star-tunnel)
  (scene! :orbital-matrix)
  (scene! :dna-nexus)
  (scene! :cyber-torus)

  ;; Toggle wireframe
  (w!)

  ;; Color scheme change (bg, mesh)
  (c! "#04000c" "#00ffff")
  (c! "#100404" "#ff0055")

  ;; Music Theory Inspection
  (scale :d :dorian)
  (scale :e :hirajoshi 2)
  (chord :e :min9 3)
  (chord :f :dark-m9 3)
  (arp (chord :e :min9 3) :up-down)

  ;; Live Sound Design in REPL (definst! -> demo!)
  (definst! :supersaw-cus
    {:category :leads
     :type     :mono
     :bus      :bus/space
     :osc      {:type :supersaw :sub-level 0.4 :drift 0.2}
     :filter   {:type :lowpass :cutoff 1400 :q 0.5 :drive 0.25 :env-amount 3000 :key-track 2.0}
     :amp-env  {:attack 0.01 :decay 0.2 :sustain 0.7 :release 0.25}
     :mod-env  {:attack 0.01 :decay 0.2}
     :glide    0.03})

  ;; Preview instrument
  (demo! :supersaw-cus)
  (demo-stop!)

  ;; Plug the new synth directly into a live loop
  (l! :lead {:inst :supersaw-cus :notes (d [1 3 4 5 7 8 5 3] 3) :step "16n" :vel 0.5})

  ;; Custom Track Architecture (deftrack! -> jam!)
  (deftrack! :cyber-roller-cus
    {:name   "Cyber Roller In D Dorian (174 BPM)"
     :bpm    174
     :scale  [:d :dorian 1]
     :geom   :torus-knot
     :colors ["#080412" "#00ffaa"]
     :cutoff 3800
     :tracks
     {:drums {:pattern (pat "k . . .  s . . .  . . k .  s . . g") :step "16n"}
      :hat   {:inst :hh-c :mask (euc 11 16) :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]}
      :bass  {:inst :bass :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95}
      :sub   {:inst :sub  :notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _]) :step "16n" :dur "8n" :vel 1.0}
      :pad   {:inst :pad  :notes [(chord :d :min9 3) (chord :g :dom7 3) (chord :c :maj7 3)] :step "1m" :dur "1m" :vel 0.4}}})

  (jam! :cyber-roller-cus)

  ;; Audio Stop
  (stop!))
