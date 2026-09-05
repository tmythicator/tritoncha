(ns app.audio.dsp.fx
  "Audio effects automations, drive, chorus, bitcrush, sidechain, smooth filter sweeps, dub sirens, and sub-bass drops via Rust WASM DSP engine."
  (:require [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.worklet :as worklet]
            [app.state :refer [pulse!]]
            [app.utils.math :refer [clamp]]))

(defonce ^:private filter-state
  (atom {:cutoff 18000.0 :resonance 0.0}))

(defonce ^:private delay-state
  (atom {:time 0.35 :feedback 0.40 :wet 0.30}))

(defonce ^:private reverb-state
  (atom {:room-size 0.70 :wet 0.35}))

(defn get-filter-state
  "Returns the current master filter state map {:cutoff hz :resonance q}."
  []
  @filter-state)

(defn set-filter-cutoff!
  "Sets the master lowpass filter cutoff frequency in Hz (50 to 18000 Hz).
  Examples: (set-filter-cutoff! 3000), (f! 12000)."
  [hz-or-norm]
  (let [hz (if (<= hz-or-norm 1.0) (* hz-or-norm 18000.0) hz-or-norm)
        clamped-hz (clamp hz 50.0 18000.0)]
    (swap! filter-state assoc :cutoff clamped-hz)
    (worklet/set-worklet-master-filter! clamped-hz (:resonance @filter-state))))

(defn set-filter-q!
  "Sets filter resonance (0.0 to 0.95).
  Examples: (set-filter-q! 0.8), (q! 0.5)."
  [q]
  (let [res (if (<= q 1.0) q (/ q 10.0))
        clamped-res (clamp res 0.0 0.95)]
    (swap! filter-state assoc :resonance clamped-res)
    (worklet/set-worklet-master-filter! (:cutoff @filter-state) clamped-res)))

(defn sweep-filter!
  "Smoothly sweeps master filter cutoff from `from-hz` to `to-hz` over `duration-secs` via Rust WASM DSP.
  Examples: (sweep-filter! 400 8000 4), (sw! 300 12000 2)."
  ([to-hz] (sweep-filter! (:cutoff @filter-state) to-hz 4.0))
  ([from-hz to-hz] (sweep-filter! from-hz to-hz 4.0))
  ([from-hz to-hz duration-secs]
   (let [f-hz (clamp (if (<= from-hz 1.0) (* from-hz 18000.0) from-hz) 50.0 18000.0)
         t-hz (clamp (if (<= to-hz 1.0) (* to-hz 18000.0) to-hz) 50.0 18000.0)
         dur  (or duration-secs 4.0)]
     (swap! filter-state assoc :cutoff t-hz)
     (worklet/sweep-worklet-master-filter! f-hz t-hz dur))))

(defn set-distortion!
  "Sets overdrive/distortion amount (0.0 to 1.0).
  Examples: (set-distortion! 0.6), (dist! 0.8)."
  [amt]
  (worklet/set-worklet-drive-bitcrush! (clamp amt 0.0 1.0) 16.0 1.0))

(defn set-bitcrush!
  "Sets bitcrusher resolution (bits 1 to 16, sample-hold downsampling 1 to 16).
  Examples: (set-bitcrush! 4), (bitcrush! 8 2.0)."
  ([bits] (set-bitcrush! bits 1.0))
  ([bits sample-hold]
   (worklet/set-worklet-drive-bitcrush! 0.0 (clamp bits 1.0 16.0) (clamp sample-hold 1.0 16.0))))

(defn set-chorus!
  "Sets stereo chorus / flanger mix (0.0 to 1.0) and modulation rate.
  Examples: (set-chorus! 0.5), (chorus! 1.2 0.6 0.4)."
  ([mix] (set-chorus! 0.8 mix))
  ([rate-hz mix] (set-chorus! rate-hz 0.4 mix))
  ([rate-hz depth mix]
   (worklet/set-worklet-chorus! (clamp rate-hz 0.1 10.0) (clamp depth 0.0 1.0) (clamp mix 0.0 1.0))))

(defn set-sidechain!
  "Sets kick sidechain ducking pump amount (0.0 to 1.0).
  Examples: (set-sidechain! 0.8), (sidechain! 0.5)."
  [amount]
  (worklet/set-worklet-sidechain! (clamp amount 0.0 1.0)))

(defn set-delay-feedback!
  "Sets delay feedback amount (0.0 to 0.9).
  Examples: (set-delay-feedback! 0.6), (fb! 0.7)."
  [fb]
  (let [clamped-fb (clamp fb 0.0 0.92)]
    (swap! delay-state assoc :feedback clamped-fb)
    (worklet/set-worklet-delay! (:time @delay-state) clamped-fb (:wet @delay-state))))

(defn set-delay-time!
  "Sets delay time in seconds (e.g. 0.25).
  Examples: (set-delay-time! 0.25), (dt! 0.35)."
  [time-val]
  (let [time-s (if (number? time-val) time-val 0.35)]
    (swap! delay-state assoc :time time-s)
    (worklet/set-worklet-delay! time-s (:feedback @delay-state) (:wet @delay-state))))

(defn set-reverb-wet!
  "Sets reverb wet mix (0.0 to 1.0).
  Examples: (set-reverb-wet! 0.5), (wet! 0.6)."
  [w]
  (let [clamped-w (clamp w 0.0 1.0)]
    (swap! reverb-state assoc :wet clamped-w)
    (worklet/set-worklet-reverb! (:room-size @reverb-state) clamped-w)))

(defn trigger-dub-siren!
  "Triggers a classic one-shot dub laser siren FX."
  []
  (init-audio!)
  (worklet/trigger-worklet-note! :lead "E5" 0.9)
  (pulse! 2.8))

(defn trigger-sub-drop!
  "Triggers a seismic sub-bass drop."
  []
  (init-audio!)
  (worklet/trigger-worklet-note! :sub-sine "F1" 1.0)
  (pulse! 3.0))

(defn trigger-dark-chord!
  "Triggers a dark minor 9th pad chord stab."
  ([] (trigger-dark-chord! ["E3" "G3" "B3" "D4" "F#4"]))
  ([chord]
   (init-audio!)
   (doseq [n chord]
     (worklet/trigger-worklet-note! :pad n 0.6))
   (pulse! 1.8)))
