(ns app.audio.dsp.fx
  "Audio effects automations, drive, chorus, bitcrush, sidechain, smooth filter sweeps, dub sirens, and sub-bass drops via Rust WASM DSP engine."
  (:require [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.worklet :as worklet]
            [app.state :refer [audio-state pulse!]]
            [app.utils.math :refer [clamp]]))

(defonce ^:private filter-state
  (atom {:cutoff 18000.0 :resonance 0.0}))

(defonce ^:private drive-state
  (atom {:drive 0.0 :bits 16.0 :sample-hold 1.0}))

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
    (when (= (:current-routing @audio-state :default) :default)
      (swap! audio-state assoc :track-cutoff clamped-hz))
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
  "Sets overdrive/distortion amount (0.0 to 1.0) preserving bitcrusher settings.
  Examples: (set-distortion! 0.6), (dist! 0.8)."
  [amt]
  (let [d (clamp (or amt 0.0) 0.0 1.0)]
    (swap! drive-state assoc :drive d)
    (worklet/set-worklet-drive-bitcrush! d (:bits @drive-state) (:sample-hold @drive-state))))

(defn set-bitcrush!
  "Sets bitcrusher resolution preserving current overdrive setting.
  Examples: (set-bitcrush! 4), (bitcrush! 8 2.0)."
  ([bits] (set-bitcrush! bits 1.0))
  ([bits sample-hold]
   (let [b  (clamp (or bits 16.0) 1.0 16.0)
         sh (clamp (or sample-hold 1.0) 1.0 16.0)]
     (swap! drive-state assoc :bits b :sample-hold sh)
     (worklet/set-worklet-drive-bitcrush! (:drive @drive-state) b sh))))

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

(defn- parse-delay-time-s [t]
  (cond
    (number? t)  (float t)
    (= t "16n")  0.09
    (= t "8n")   0.18
    (= t "8n.")  0.27
    (= t "4n")   0.36
    (= t "4n.")  0.54
    (= t "2n")   0.72
    (= t "2n.")  1.08
    (= t "1m")   1.44
    :else        0.35))

(defn set-delay-time!
  "Sets delay time in seconds (e.g. 0.25).
  Examples: (set-delay-time! 0.25), (dt! 0.35)."
  [time-val]
  (let [time-s (parse-delay-time-s time-val)]
    (swap! delay-state assoc :time time-s)
    (worklet/set-worklet-delay! time-s (:feedback @delay-state) (:wet @delay-state))))

(defn set-delay-wet!
  "Sets delay wet mix (0.0 to 1.0).
  Examples: (set-delay-wet! 0.4)."
  [w]
  (let [clamped-w (clamp w 0.0 1.0)]
    (swap! delay-state assoc :wet clamped-w)
    (worklet/set-worklet-delay! (:time @delay-state) (:feedback @delay-state) clamped-w)))

(defn set-delay!
  "Sets delay parameters: time (seconds or note string), feedback, and wet mix.
  Examples: (set-delay! \"8n.\" 0.40 0.35)."
  [time-val feedback wet]
  (let [time-s     (parse-delay-time-s time-val)
        clamped-fb (clamp (or feedback 0.38) 0.0 0.92)
        clamped-w  (clamp (or wet 0.35) 0.0 1.0)]
    (swap! delay-state assoc :time time-s :feedback clamped-fb :wet clamped-w)
    (worklet/set-worklet-delay! time-s clamped-fb clamped-w)))

(defn set-reverb-wet!
  "Sets reverb wet mix (0.0 to 1.0).
  Examples: (set-reverb-wet! 0.5), (wet! 0.6)."
  [w]
  (let [clamped-w (clamp w 0.0 1.0)]
    (swap! reverb-state assoc :wet clamped-w)
    (worklet/set-worklet-reverb! (:room-size @reverb-state) clamped-w)))

(defn set-reverb!
  "Sets reverb room size (0.0 to 1.0) and wet mix (0.0 to 1.0).
  Examples: (set-reverb! 0.8 0.35)."
  [room-size wet]
  (let [clamped-s (clamp (or room-size 0.75) 0.0 0.98)
        clamped-w (clamp (or wet 0.35) 0.0 1.0)]
    (swap! reverb-state assoc :room-size clamped-s :wet clamped-w)
    (worklet/set-worklet-reverb! clamped-s clamped-w)))

(defn set-drive-mode!
  "Selects the master overdrive saturation algorithm mode (:adaa or :classic).
  Examples: (set-drive-mode! :adaa), (set-drive-mode! :classic)."
  [mode-kw]
  (let [m (if (= (keyword mode-kw) :classic) :classic :adaa)]
    (swap! audio-state assoc :drive-mode m)
    (worklet/set-worklet-drive-mode! m)
    m))

(defn set-reverb-mode!
  "Selects the reverb engine algorithm mode (:fdn or :freeverb).
  Examples: (set-reverb-mode! :fdn), (set-reverb-mode! :freeverb)."
  [mode-kw]
  (let [m (if (= (keyword mode-kw) :freeverb) :freeverb :fdn)]
    (swap! audio-state assoc :reverb-mode m)
    (worklet/set-worklet-reverb-mode! m)
    m))

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
