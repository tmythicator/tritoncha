(ns app.audio.dsp.worklet.protocol
  "Protocol codecs, enum mappings, and numeric identifier encoders for Rust WASM DSP."
  (:require [app.lib.drums :refer [drum-keywords]]))

(def canonical-inst-ids
  {;; Analog Drum Voices (fixed DSP algorithms in Rust)
   :kick          0 :bd 0
   :snare         1 :sn-rs 1 :sn-roll 1
   :hh-c          2 :hh 2 :hh-clk 2 :hat-closed 2 :hat 2
   :hh-o          3 :hat-open 3
   :util-click    11 :click 11
   :clap          18
   :ride          20
   :tom           21
   :sn-crack      22
   :snare-wire    23 :sn-wire 23
   :snare-body    24 :sn-body 24
   :snare-ghost   25 :sn-gh 25
   :snare-rim     26 :sn-clk 26
   :ride-bell     64 :rb 64
   :tom-high      65 :th 65 :tom-h 65
   :tom-mid       66 :tm 66 :tom-m 66
   :tom-low       67 :tl 67 :tom-l 67
   :crash-16      68 :cr16 68 :crash 68 :cr 68
   :crash-17      69 :cr17 69
   :crash-18      70 :cr18 70
   :splash        71 :sp 71
   :china         72 :ch 72
   :cowbell       73 :cb 73

   ;; Melodic and Harmonic Synthesizer Patches (Rust modular voice slots 4..41)
   :bass-analog   4  :bass 4
   :bass-303      5  :acid 5
   :sub-pure      6  :sub 6
   :pad-cinema    7  :pad 7
   :lead-pluck    8  :lead 8 :pluck 8 :arp 8
   :lead-fm       9  :fm 9
   :bass-reese    10 :reese 10
   :lead-supersaw 12 :supersaw 12
   :lead-blade    13
   :lead-hoover   14 :hoover 14
   :lead-string   15 :karplus 15
   :lead-organ    16 :organ 16
   :lead-8bit     17 :8bit 17
   :pad-glass     19 :glass 19
   :pad-shimmer   27 :shimmer 27
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
  (into #{} (keep canonical-inst-ids drum-keywords)))

(defonce custom-synth-patch-ids
  (atom {}))

(defn register-custom-patch-id!
  "Resolves or allocates a patch ID for an instrument.
  Built-in synths preserve their canonical patch ID.
  Truly custom user synths are allocated from the safe range 42..63.
  Examples: (register-custom-patch-id! :my-synth) -> 42."
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
              free (first (filter #(not (contains? used %)) (range 42 64)))]
          (swap! custom-synth-patch-ids assoc sk (or free 42))
          (or free 42))))))

(defn inst-keyword->id
  "Resolves an instrument keyword to its numeric ID for the Rust DSP engine.
  Examples: (inst-keyword->id :kick) -> 0, (inst-keyword->id :bass-analog) -> 4."
  [inst-key]
  (let [k (keyword inst-key)]
    (or (get canonical-inst-ids k)
        (get @custom-synth-patch-ids k)
        (register-custom-patch-id! k))))

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
  "Maps filter type keyword to numeric identifier for the TPT state-variable filter.
  Examples: (filter-type->id :lowpass) -> 0, (filter-type->id :highpass) -> 1."
  [ft]
  (case (keyword ft)
    (:lp :lowpass) 0
    (:hp :highpass) 1
    (:bp :bandpass) 2
    (:notch) 3
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
