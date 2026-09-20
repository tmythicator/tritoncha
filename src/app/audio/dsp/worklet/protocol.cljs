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
   "rb"   :ride-bell
   "cr16" :crash-16
   "cr17" :crash-17
   "cr18" :crash-18
   "sp"   :splash
   "ch"   :china
   "cb"   :cowbell})

(def canonical-inst-ids
  {;; Analog Drum Voices (fixed DSP algorithms in Rust)
   :kick          0
   :snare         1
   :sn-roll       1
   :hat-closed    2 :hh-c 2 :hh-clk 2
   :hat-open      3 :hh-o 3
   :util-click    11
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
   :cowbell       73

   ;; Melodic and Harmonic Synthesizer Patches (Rust modular voice slots 4..41)
   :bass-analog   4  :bass 4
   :bass-303      5  :acid 5
   :sub-pure      6  :sub 6
   :pad-cinema    7  :pad 7
   :lead-pluck    8  :lead 8 :pluck 8 :arp 8
   :lead-fm       9  :fm 9
   :bass-reese    10 :reese 10 :liquid-reese 10
   :lead-supersaw 12 :supersaw 12
   :lead-blade    13
   :lead-hoover   14 :hoover 14
   :lead-string   15 :karplus 15
   :lead-organ    16 :organ 16
   :lead-8bit     17 :8bit 17
   :pad-glass     19 :glass 19
   :sub-moog      42 :moog-sub 42
   :bass-moog     43 :moog-bass 43
   :pad-strings   29 :strings 29
   :pad-vocal     30 :choir 30
   :pad-drone     31 :drone 31
   :bass-neuro    33 :neuro 33
   :sub-808       34 :808 34
   :bass-slap     35 :slap 35
   :bass-organ    36
   :bass-liquid   37 :liquid 37
   :lead-bell     38 :bell 38
   :fx-siren      40 :siren 40
   :fx-laser      41 :laser 41})

(def drum-id-set
  #{0 1 2 3 11 18 20 21 22 23 24 25 26 64 65 66 67 68 69 70 71 72 73})

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
  "Maps a custom drum keyword to its underlying Rust drum voice ID.
  Examples: (register-custom-drum-id! :fat-kick 0)."
  [drum-key drum-id]
  (swap! custom-drum-ids assoc (keyword drum-key) (int drum-id)))

(defonce custom-synth-patch-ids
  (atom {}))

(defn register-custom-patch-id!
  "Resolves or allocates a patch ID for an instrument.
  Built-in synths preserve their canonical patch ID.
  Truly custom user synths are allocated from the safe range 44..63.
  Examples: (register-custom-patch-id! :my-synth) -> 44."
  [synth-key]
  (let [sk (keyword synth-key)]
    (if-let [cid (get canonical-inst-ids sk)]
      (if (not (contains? drum-id-set cid))
        (do
          (swap! custom-synth-patch-ids assoc sk cid)
          cid)
        cid)
      (if-let [id (get @custom-synth-patch-ids sk)]
        id
        (let [used (set (vals @custom-synth-patch-ids))
              free (first (filter #(not (contains? used %)) (range 44 64)))]
          (swap! custom-synth-patch-ids assoc sk (or free 44))
          (or free 44))))))

(defn inst-keyword->id
  "Resolves an instrument keyword to its numeric ID for the Rust DSP engine.
  Examples: (inst-keyword->id :kick) -> 0, (inst-keyword->id :bass-analog) -> 4."
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
         (get @custom-synth-patch-ids k)
         (register-custom-patch-id! k)))))

(defn bus-key->id
  "Maps symbolic audio bus keyword to numeric index for the Rust multi-bus DSP mixer.
  Examples: (bus-key->id :bus/drums) -> 0, (bus-key->id :bus/bass) -> 1."
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
  "Maps oscillator type keyword to numeric identifier for the Rust voice architecture.
  Examples: (osc-type->id :saw) -> 0, (osc-type->id :pulse) -> 1."
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
  Supported types: :lp (:lowpass), :hp (:highpass), :bp (:bandpass), :notch, :ladder (:ladder24).
  Examples: (filter-type->id :lowpass) -> 0, (filter-type->id :ladder) -> 4."
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
  Supported modes: :analog (0.0), :natural (1.0), :idm (2.0), :industrial (3.0).
  Examples: (drum-mod->id :natural) -> 1.0, (drum-mod->id :analog) -> 0.0."
  [m]
  (case (keyword (or m :analog))
    (:analog :classic :808 :909) 0.0
    (:natural :acoustic :organic :wood) 1.0
    (:idm :glitch :laser :chirp) 2.0
    (:industrial :distort :hard :crush) 3.0
    0.0))
