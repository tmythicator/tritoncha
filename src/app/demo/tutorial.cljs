(ns app.demo.tutorial
  "Interactive masterclass tutorial for Tritoncha: live-coding audio + WebGL 3D visuals, sound design, and track building."
  (:require [app.api :as a :refer [_ arp b! c! chord click! d definst! defrouting! deftrack! demo!
                                   demo-stop! drop! euc! f! fb! g! jam! l! m! mod-all! oct-shift pat!
                                   play! progression redrum! refresh! s! scale set-key! so! stat stop!
                                   sw! tr-all! transpose u! undrum! unso! w! wet!]]))

(comment
  ;; Scale Degrees, Basslines + Live Drums
  ;; `l!` (or `loop!`) creates or hot-swaps an audio loop on the fly.
  ;; `d` converts 1-based scale degrees (1, 2, 3...) to real note pitches in the active key.
  ;; `_` represents a rest (silence).

  ;; Basic bassline
  (l! :bass
    {:inst  :bass                                        ;; Instrument: :bass, :sub, :acid-bass, :pad...
     :notes (d [1 _ 4 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])       ;; Scale degrees (_ = rest)
     :step  "16n"                                        ;; Quantized step: "16n", "8n", "4n", "1m"
     :dur   "16n"                                        ;; Note gate: "32n" (staccato) to "1m" (legato)
     :vel   0.45})                                       ;; Velocity accent: 0.0 to 1.0

  ;; Basic live drum loops (kick, snare, hi-hats):
  (l! :kick  {:inst :kick  :pattern (pat! "k . . .  k . . .  . . k .  . . . .") :step "16n"})
  (l! :snare {:inst :snare :pattern (pat! ". . . .  s . . .  . . . .  s . . g") :step "16n"})
  (l! :hat   {:inst :hh-c  :pattern (pat! "h h h o  h h h o  h h h o  h hc h o") :step "16n" :dur "32n"})

  ;; Dynamic velocity accents
  (l! :bass
    {:inst  :bass
     :notes (d [1 1 1 1  2 2 2 2])
     :vel   [1.0 0.4 0.8 0.4]})

  ;; Generative probability
  (l! :arp
    {:inst  :pad
     :notes (d [1 2 3 5 7 8 5 3] 3)
     :prob  0.75
     :step  "16n"})

  ;; To stop the specific loop
  (stop! :arp)

  ;; Euclidean rhythm (7 hits over 16 steps)
  (l! :lead
    {:inst  :pad
     :notes (d [1 3 4 5 7] 3)
     :mask  (euc! 7 16)
     :step  "16n"})

  (stat)
  (stop! :lead)
  (stop!)

  ;; Sound Design & Live Auditioning (definst! -> demo!)
  ;; Workflow:
  ;;   1. Define a sound synthesis preset in REPL with `definst!`.
  ;;   2. Audition the new instrument in the current key with `(demo! :inst-key)`.
  ;;   3. Stop preview with `(demo-stop!)`.
  ;;   4. Integrate the instrument into any loop with `(l! :loop-name {:inst :inst-key ...})`.

  ;; Built-in core instruments catalog:
  ;;   :saw-bass    - Multi-oscillator detuned saw bass with 24dB lowpass filter (:bass)
  ;;   :sub-sine    - Pure sub-bass sine wave with pitch glide portamento (:sub)
  ;;   :acid-bass   - Resonant high-resonance filter bass synthesizer
  ;;   :fm-growl    - Frequency-modulated bass growl
  ;;   :dark-pad    - Atmospheric polyphonic synthesizer for chords + pads (:pad)
  ;;   :ambient-glass - Ethereal shimmering polyphonic glass pad
  ;;   :pluck-lead  - Fast transient square-wave pluck lead
  ;;   :kick        - Punchy sub-kick drum with fast pitch sweep
  ;;   :snare       - Full layered snare (body + wire)
  ;;   :sn-rs       - Snare rimshot with high-pitched resonant crack
  ;;   :sn-clk      - Snare cross-stick / rim click
  ;;   :sn-gh       - Quiet ghost snare note
  ;;   :sn-roll     - Fast snare roll / fill
  ;;   :hh-c        - Closed crisp metallic hi-hat
  ;;   :hh-o        - Open splashy acoustic hi-hat
  ;;   :hh-clk      - Pedal chick hi-hat click
  ;;   :siren       - Classic pitch-sweeping laser siren
  ;;   :click       - Metronome click (C6 downbeat / G5 beats)

  ;; Auditioning built-in instruments (Live Previews):
  (demo! :saw-bass)
  (demo! :acid-bass)
  (demo! :fm-growl)
  (demo! :dark-pad)
  (demo-stop!)

  ;; Custom instrument in REPL
  (definst! :supersaw
    {:type :mono
     :bus :space                                         ;; Mixer bus: :bass, :drums, :space, :direct
     :options {:oscillator {:type "fatsawtooth" :count 5 :spread 30}
               :filter {:Q 4 :type "lowpass" :rolloff -24}
               :filterEnvelope {:attack 0.01 :decay 0.2 :sustain 0.4 :release 0.2 :baseFrequency 300 :octaves 3}
               :envelope {:attack 0.01 :decay 0.2 :sustain 0.7 :release 0.25}
               :portamento 0.03}})

  (definst! :fm-bell
    {:type :fm
     :bus :space
     :options {:harmonicity 3.5
               :modulationIndex 18
               :oscillator {:type "sine"}
               :envelope {:attack 0.001 :decay 0.8 :sustain 0.1 :release 0.8}
               :modulation {:type "triangle"}
               :modulationEnvelope {:attack 0.001 :decay 0.4 :sustain 0.2 :release 0.4}}})

  ;; Auditioning REPL instruments
  (demo! :supersaw)
  (demo! :fm-bell)
  (demo-stop!)

  ;; Using REPL-defined instruments in live loops
  (l! :lead {:inst :supersaw :notes (d [1 3 4 5 7 8 5 3] 3) :step "16n"})
  (l! :bell {:inst :fm-bell  :notes (d [1 _ 3 _ 5 _ 7 _] 4) :step "16n"})
  (stop!)

  ;; Custom Audio Routing (defrouting!)
  (defrouting! :dub-space
    {:busses
     {:drum-bus   {:type :volume :volume 0}
      :bass-bus   {:type :volume :volume 0}
      :space-bus  {:type :volume :volume -2}
      :direct-bus {:type :volume :volume 0}}
     :processors
     {:distort       {:type :distortion :distortion 0.45}
      :master-filter {:type :filter :frequency 4200 :filter-type "lowpass"}
      :delay         {:type :delay :time "8n." :feedback 0.55}
      :reverb        {:type :reverb :decay 5.5 :wet 0.42}
      :limiter       {:type :limiter :threshold -2.0}}
     :routes
     [[:drum-bus :master-filter]
      [:bass-bus :distort :master-filter]
      [:space-bus :delay :reverb :limiter]
      [:direct-bus :limiter]
      [:master-filter :limiter]
      [:limiter :destination]]})

  ;; Music Theory, Modal Harmony, Chords, Arps
  (set-key! :e :phrygian 1)

  ;; Live modulation
  (mod-all! :b :arabic 3)
  (mod-all! :d :dorian 2)
  (tr-all! 3)
  (tr-all! -3)

  ;; Scale degrees
  (d [1 _ 1 2 3 5])
  (d [1 2 3 5] 3)

  ;; Modal scales
  (scale :e :phrygian)
  (scale :f# :hirajoshi 2)
  (scale :a :hungarian-minor)
  (scale :d :dorian)
  (scale :c :blues)

  ;; Chords & inversions
  (chord :e :min9)
  (chord :f :dark-m9)
  (chord :c :maj7 {:inversion 1})

  ;; Progressions + Arps
  (progression :e :dorian [1 4 5 1] :type :min7)
  (arp (chord :e :min9) :up-down)
  (arp (chord :f :dark-m9) :converge)
  (arp (scale :e :hirajoshi {:octave 3}) :random)

  (transpose "C4" 7)
  (oct-shift "E1" 2)

  ;; Drum Programming + Breakbeats
  (l! :kick  {:inst :kick  :pattern (pat! "k . . .  k . . .  . . k .  . . . .") :step "16n"})
  (l! :snare {:inst :snare :pattern (pat! ". . . .  s . . .  . . . .  rs . . g") :step "16n"})
  (l! :hat   {:inst :hh-c  :pattern (pat! "h h h o  h h h o  h h h o  h hc h o") :step "16n" :dur "32n"})

  ;; Snare roll into drop
  (l! :snare {:inst :snare :pattern (pat! ". . . .  s . . .  . . s .  r r r r") :step "16n"})

  ;; Multi-hit drum sequence
  (l! :drums
    {:hits [[:kick 1.0 "D1"] [:hh-c 0.4] [:sn-gh 0.35] [:hh-c 0.5]
            [[:snare 1.0 "G3"] [:hh-c 0.45]]
            [:hh-c 0.4] [:sn-gh 0.4] [:kick 0.9 "D1"]
            [:hh-o 0.75] [:kick 0.8 "D1"]
            [[:snare 1.0 "G3"] [:hh-c 0.45]]
            [:hh-c 0.4]
            [[:sn-rs 0.95 "G3"] [:hh-o 0.8]]
            [:sn-gh 0.35] [:sn-roll 0.8 "A3"] [:sn-roll 0.95 "A3"]]
     :step "16n"})
  (stop!)

  ;; Track Architecture + Loop Integration into Tracks (deftrack! -> jam!)
  ;; A Track bundles together:
  ;;   - Metadata (name, BPM, scale)
  ;;   - 3D WebGL scene (geometry, shader colors, master filter cutoff)
  ;;   - A map of loops in `:tracks` (each entry is a standard loop data map)

  (deftrack! :cyber-roller
    {:name   "Cyber Roller In D Dorian (174 BPM)"
     :bpm    174
     :scale  [:d :dorian 1]
     :geom   :torus-knot
     :colors ["#080412" "#00ffaa"]
     :cutoff 3800
     :tracks
     {;; Loop 1: Drum breakbeat
      :drums {:pattern (pat! "k . . .  s . . .  . . k .  s . . g")
              :step "16n"}
      ;; Loop 2: Euclidean Hi-Hats
      :hat   {:inst :hh-c
              :mask (euc! 11 16)
              :step "16n" :dur "32n" :vel [0.3 0.7 0.4 0.9]}
      ;; Loop 3: Phrygian/Dorian Saw Bass
      :bass  {:inst :bass
              :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _])
              :step "16n" :dur "16n" :vel 0.95}
      ;; Loop 4: Sub-bass reinforcement
      :sub   {:inst :sub
              :notes (d [1 _ _ _ 1 _ _ _  4 _ _ _ 3 _ _ _])
              :step "16n" :dur "8n" :vel 1.0}
      ;; Loop 5: Atmospheric chord pads
      :pad   {:inst :pad
              :notes [(chord :d :min9 3) (chord :g :dom7 3) (chord :c :maj7 3)]
              :step "1m" :dur "1m" :vel 0.35}}})

  ;; Launch built-in tracks
  (play!)
  (jam! :sub-roller)
  (jam! :acid-roller)
  (jam! :ambient-drift)

  ;; Launch REPL-defined track
  (jam! :cyber-roller)

  ;; Refresh updated synths and track definitions
  (refresh!)

  ;; Live Improvisation, Loop Mutation + Mixer Controls
  ;; While track is playing, live-mutate any loop or add new ones:

  ;; Hot-swap bassline pattern live without stopping beat:
  (l! :bass {:inst :acid-bass :notes (d [1 1 _ 2 3 _ 1 5  1 _ 4 3 2 _ 1 _] 1) :step "16n" :vel 1.0})

  ;; Inject additional arpeggiator loop into active mix:
  (l! :arp  {:inst :pad :notes (arp (chord :d :min9 3) :up-down) :mask (euc! 7 16) :step "16n" :vel 0.35})

  ;; Breakdown + drop FX
  (m! :drums :hat)
  (f! 450)
  (sw! 400 5500 4)
  (s!)
  (u! :drums :hat)
  (drop!)

  ;; Solo & space
  (so! :bass :sub)
  (fb! 0.65)
  (wet! 0.4)
  (unso!)

  ;; Live modulations
  (mod-all! :f :phrygian 1)
  (mod-all! :a :lydian 3)
  (tr-all! 2)
  (tr-all! -2)

  ;; Drummer sync + Metronome
  (undrum!)
  (click!)
  (b! 176)
  (redrum!)

  ;; 3D visuals & diagnostics
  (g! :icosahedron)
  (w!)
  (c! "#020b14" "#00ffcc")
  (stat)

  (stop!))
