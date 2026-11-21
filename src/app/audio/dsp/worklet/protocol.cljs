(ns app.audio.dsp.worklet.protocol
  "Protocol codecs, enum mappings, and mini-notation drum token remaps for Rust WASM DSP.")

(def synth-step-inst-id
  "Sentinel instrument ID instructing the Rust sequencer to trigger the track's own bound synth patch."
  -1)

(def drum-remaps
  "Lookup map of short drum notation tokens to canonical drum keywords for live mini-notation."
  {"k"    :kick
   "s"    :snare
   "g"    :snare_
   "h"    :hat-closed
   "o"    :hat-open
   "cp"   :clap
   "th"   :tom-high
   "tm"   :tom-mid
   "tl"   :tom-low
   "rd"   :ride
   "rb"   :ride-bell
   "cr16" :crash-16
   "cr17" :crash-17
   "cr18" :crash-18
   "sp"   :splash
   "ch"   :china
   "cb"   :cowbell})

(defn bus-key->id
  "Maps symbolic audio bus keyword to numeric index for the Rust multi-bus DSP mixer."
  [bus-key]
  (case (keyword bus-key)
    (:bus/drums :drums) 0
    (:bus/bass :bass) 1
    (:bus/space :space) 2
    (:bus/lead :lead) 3
    (:bus/direct :direct :click) 4
    (:bus/master :master) 5
    4))

(defn osc-type->id
  "Maps oscillator type keyword to numeric identifier for the Rust voice architecture."
  [osc]
  (case (keyword osc)
    (:saw :sawtooth) 0
    (:pulse :square) 1
    (:tri :triangle) 2
    (:sine) 3
    (:supersaw :prophet) 4
    (:karplus :pluck) 5
    (:organ :drawbar) 6
    (:chiptune :8bit :nes) 7
    (:fm :fm-synth) 8
    (:reese) 9
    (:blade :cs80) 10
    (:hoover :mentasm) 11
    (:click) 12
    0))

(defn filter-type->id
  "Maps filter type keyword to numeric identifier for voice filters (SVF and 4-Pole 24dB Ladder).
  Supported types: :lp (:lowpass), :hp (:highpass), :bp (:bandpass), :notch, :ladder (:ladder24)."
  [ft]
  (case (keyword ft)
    (:lp :lowpass) 0
    (:hp :highpass) 1
    (:bp :bandpass) 2
    :notch 3
    (:ladder :ladder24) 4
    0))

(defn perc-body->id
  "Maps physical percussion body family keyword to Rust PercBodyType discriminant (0..4):
  0 = :membrane (stretched drumhead + air column),
  1 = :shell (rigid wooden drum shell / rim hoop),
  2 = :cymbal (bronze cymbal disc: bell <-> bow <-> hat <-> edge crash),
  3 = :idiophone (hollow folded metal/wood cavity: cowbell, woodblock, agogo),
  4 = :burst (multi-tap air clap / snare wire rattle)."
  [body]
  (case (keyword (or body :membrane))
    :membrane 0
    :shell 1
    :cymbal 2
    :idiophone 3
    :burst 4
    0))

(defn noise-filter->id
  "Maps percussion noise layer filter mode keyword to Rust NoiseFilterMode discriminant (0..2)."
  [mode]
  (case (keyword (or mode :hp))
    (:hp :highpass) 0
    (:bp :bandpass) 1
    (:lp :lowpass) 2
    0))
