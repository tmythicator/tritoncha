(ns app.audio.dsp.worklet.protocol
  "Protocol codecs, enum mappings, and numeric identifier encoders for Rust WASM DSP."
  (:require [app.custom.drums :refer [user-drums]]))

(def drum-remaps
  "Lookup map of short drum notation tokens to canonical drum keywords for live mini-notation."
  {"k"    :kick
   "s"    :snare
   "rs"   :sn-rs
   "clk"  :sn-clk
   "ck"   :sn-crack
   "g"    :sn-gh
   "roll" :sn-roll
   "h"    :hat-closed
   "o"    :hat-open
   "hc"   :hh-clk
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

(def synth-step-inst-id
  "Sentinel instrument ID instructing the Rust sequencer to trigger the track's own bound synth patch."
  -1)

(def canonical-inst-ids
  {;; Analog Drum Voices (fixed DSP algorithms in Rust, replaced in Step 4)
   :kick          0  :fat-kick 0
   :snare         1  :sn-roll 1 :lofi-snare 1
   :hat-closed    2  :hat 2 :hh-c 2 :hh-clk 2
   :hat-open      3  :hh-o 3
   :clap          18
   :ride          20
   :tom           21
   :sn-crack      22
   :snare-wire    23
   :snare-body    24
   :snare-ghost   25 :sn-gh 25
   :snare-rim     26 :sn-rs 26 :sn-clk 26
   :ride-bell     64
   :tom-high      65
   :tom-mid       66
   :tom-low       67
   :crash-16      68
   :crash-17      69
   :crash-18      70
   :splash        71
   :china         72
   :cowbell       73})

(def drum-id-set
  #{0 1 2 3 18 20 21 22 23 24 25 26 64 65 66 67 68 69 70 71 72 73})

(def drum-type->id
  {:kick       0
   :snare      1
   :hat        2
   :hat-closed 2
   :hat-open   3
   :clap       18
   :ride       20
   :metallic   20
   :tom        21
   :membrane   21
   :cowbell    73})

(defonce custom-drum-ids
  (atom (into {}
              (for [[k spec] user-drums]
                [k (get drum-type->id (:type spec) 0)]))))

(defn register-custom-drum-id!
  "Maps a custom drum keyword to its underlying Rust drum voice ID."
  [drum-key drum-id]
  (swap! custom-drum-ids assoc (keyword drum-key) (int drum-id)))

(defn inst-keyword->id
  "Resolves an instrument keyword to its numeric ID for the Rust DSP engine.
  Drums resolve to their Rust drum voice ID; all synths resolve to `synth-step-inst-id` (-1)
  so the sequencer dispatches directly to the track's own bound patch."
  ([inst-key]
   (inst-keyword->id inst-key nil))
  ([inst-key spec]
   (let [k (keyword inst-key)]
     (or (get canonical-inst-ids k)
         (get @custom-drum-ids k)
         (when spec
           (when (or (= (:category spec) :drums)
                     (contains? drum-type->id (:type spec)))
             (let [drum-id (get drum-type->id (:type spec) 0)]
               (register-custom-drum-id! k drum-id)
               drum-id)))
         synth-step-inst-id))))

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

(defn drum-mod->id
  "Maps symbolic drum character mode keyword to numeric float ID for Rust WASM DSP.
  Supported modes: :analog (0.0), :natural (1.0), :idm (2.0), :industrial (3.0)."
  [m]
  (case (keyword (or m :analog))
    (:analog :classic :808 :909) 0.0
    (:natural :acoustic :organic :wood) 1.0
    (:idm :glitch :laser :chirp) 2.0
    (:industrial :distort :hard :crush) 3.0
    0.0))
