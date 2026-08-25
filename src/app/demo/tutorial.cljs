(ns app.demo.tutorial
  "Live-coding audio + visuals tutorial for Tritoncha."
  (:require [app.api :refer [_ arp b! c! chord d definst! deftrack! demo! demo-stop!
                             drop! euc! f! fb! jam! l! mod-all! pat! redrum! s!
                             scale scene! set-key! stop! sw! tr-all! undrum! w! wet!]]))

(comment
  ;; =============================================================================
  ;; Tritoncha: Live-Coding Electronic Music + 3D WebGL Studio
  ;;
  ;; NOTE: Tritoncha is fundamentally a REPL-FIRST live performance instrument!
  ;; This in-browser scratchpad is an interactive visual sandbox to play with
  ;; expressions directly on the web and get a taste of the algorithmic workflow.
  ;;
  ;; For the full Algorave & live jamming experience:
  ;;   1. Connect from your editor via nREPL (e.g. Emacs: M-x cider-connect-cljs -> :app)
  ;;   2. Jam directly in `src/app/live/jam.cljs` with full REPL keybindings!
  ;;
  ;; SHORTCUTS:
  ;;   [Ctrl+Enter]       -> Evaluate current line or form under cursor
  ;;   [Ctrl+Shift+Enter] -> Evaluate entire script buffer
  ;; =============================================================================

  ;; 1. Presets & Instant Jams
  (jam! :roller)           ;; Phrygian Drum & Bass Roller (168 BPM)
  (jam! :sub-roller)       ;; Deep Sub-Bass Halftime (172 BPM)
  (jam! :acid-roller)      ;; Resonant 303 Acid Roller (174 BPM)
  (jam! :ambient-drift)    ;; Floating Ambient Chill (160 BPM)
  (b! 174)                 ;; Live Tempo Change
  (stop!)                  ;; Full Audio Stop

  ;; 2. Live Modulations & Transpositions (Instant Harmonic Shifts)
  ;; While any track is playing, shift harmonic key or transpose on the fly:
  (mod-all! :b :arabic 3)          ;; Modulate all active loops to B Arabic (octave 3)
  (mod-all! :d :dorian 2)          ;; Modulate all active loops to D Dorian
  (mod-all! :f# :hirajoshi 2)      ;; Modulate to Japanese Hirajoshi pentatonic
  (mod-all! :a :hungarian-minor 2) ;; Modulate to dark Hungarian minor
  (mod-all! :c :blues 2)           ;; Modulate to C Blues scale
  (mod-all! :e :phrygian 1)        ;; Back to E Phrygian
  (tr-all! 3)                      ;; Transpose active loops UP by 3 semitones live
  (tr-all! -2)                     ;; Transpose active loops DOWN by 2 semitones live
  (tr-all! -1)                     ;; Transpose DOWN 1 semitone

  ;; Music Theory Inspect (Evaluate line to inspect notes in $ eval_output)
  (scale :d :dorian)               ;; => ["D3" "E3" "F3" "G3" "A3" "B3" "C4"]
  (scale :e :hirajoshi 2)          ;; => ["E2" "F#2" "G2" "B2" "C3"]
  (chord :e :min9 3)               ;; => ["E3" "G3" "B3" "D4" "F#4"]
  (chord :f :dark-m9 3)            ;; => Dark minor 9th spread

  ;; 3. 3D WebGL Scenes & Shaders
  (scene! :synthwave-grid) ;; Rolling 80s Cyberpunk Horizon Grid
  (scene! :star-tunnel)    ;; Hyperspace Warp Vortex
  (scene! :orbital-matrix) ;; Saturn Concentric Orbital Rings
  (scene! :dna-nexus)      ;; Helical Spine Inside Floating Dodecahedron
  (scene! :quantum-polyhedron)
  (w!)                     ;; Toggle Wireframe
  (c! "#04000c" "#00ffff") ;; Set Custom Colors (bg, mesh)

  ;; 4. Scale Degrees, Basslines & Live Loops
  (set-key! :e :phrygian 1)

  ;; Quantized Saw Bassline (Cursor inside block + Ctrl+Enter to eval)
  (l! :bass
      {:inst  :bass
       :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])
       :step  "16n"
       :dur   "16n"
       :vel   0.95})

  ;; Sub-bass Reinforcement
  (l! :sub
      {:inst  :sub
       :notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _])
       :step  "16n"
       :dur   "8n"
       :vel   1.0})

  ;; 5. Chords & Euclidean Arpeggiators
  (l! :arp
      {:inst  :pad
       :notes (arp (chord :e :min9 3) :up-down)
       :mask  (euc! 7 16)
       :step  "16n"
       :vel   0.35})

  ;; 6. Breakbeats & Mini-Notation
  (l! :kick
      {:inst    :kick
       :pattern (pat! "k . . .  k . . .  . . k .  . . . .")
       :step    "16n"})

  (l! :snare
      {:inst    :snare
       :pattern (pat! ". . . .  s . . .  . . . .  s . . g")
       :step    "16n"})

  (l! :hat
      {:inst :hh-c
       :mask (euc! 11 16)
       :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]})

  ;; 7. Live Sound Design in REPL (definst! -> demo!)
  (definst! :supersaw
    {:type :mono
     :bus :space
     :options {:oscillator {:type "fatsawtooth" :count 5 :spread 30}
               :filter {:Q 4 :type "lowpass" :rolloff -24}
               :filterEnvelope {:attack 0.01 :decay 0.2 :sustain 0.4 :release 0.2 :baseFrequency 300 :octaves 3}
               :envelope {:attack 0.01 :decay 0.2 :sustain 0.7 :release 0.25}
               :portamento 0.03}})

  (demo! :supersaw)        ;; Live preview instrument in current scale
  (demo-stop!)             ;; Stop preview

  ;; Use custom synth in a live loop:
  (l! :lead {:inst :supersaw :notes (d [1 3 4 5 7 8 5 3] 3) :step "16n"})

  ;; 8. Custom Track Architecture (deftrack! -> jam!)
  (deftrack! :cyber-roller
    {:name   "Cyber Roller In D Dorian (174 BPM)"
     :bpm    174
     :scale  [:d :dorian 1]
     :geom   :torus-knot
     :colors ["#080412" "#00ffaa"]
     :cutoff 3800
     :tracks
     {:drums {:pattern (pat! "k . . .  s . . .  . . k .  s . . g") :step "16n"}
      :hat   {:inst :hh-c :mask (euc! 11 16) :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]}
      :bass  {:inst :bass :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :dur "16n" :vel 0.95}
      :sub   {:inst :sub  :notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _]) :step "16n" :dur "8n" :vel 1.0}
      :pad   {:inst :pad  :notes [(chord :d :min9 3) (chord :g :dom7 3) (chord :c :maj7 3)] :step "1m" :dur "1m" :vel 0.35}}})

  (jam! :cyber-roller)

  ;; 9. Live Dub FX, Sweeps & Performance Controls
  (sw! 300 6000 4)         ;; 4-second opening filter sweep into the drop
  (s!)                     ;; Dub Laser Siren
  (drop!)                  ;; Seismic Sub-Bass Drop
  (fb! 0.65)               ;; Space delay feedback
  (wet! 0.4)               ;; Reverb mix
  (f! 3200)                ;; Master filter cutoff

  ;; Mixer controls
  (undrum!)                ;; Mute all drums (keep bass & pads)
  (redrum!)                ;; Drop drums back in
  (stop!))
