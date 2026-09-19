(ns app.lib.drums
  "Core built-in drum kit synthesizer instruments and sound design catalog for Tritoncha.")

;; Drum Sound Design Specification:
;;   :title        - Human-readable display title for UI and Studio
;;   :category     - :drums
;;   :type         - :kick, :snare, :hat, :membrane, :metallic, :clap
;;   :bus          - :bus/drums (default) or :bus/direct
;;   :mod          - Character synthesis mode (:analog, :natural, :idm, :industrial)
;;
;; Parameters by drum type:
;;   :kick     - {:base-pitch 40..80 :pitch-drop 50..400 :pitch-decay 0.01..0.10 :decay 0.05..1.5 :click 0.0..1.0 :drive 0.5..4.0}
;;   :snare    - {:base-freq 120..350 :tone-decay 0.990..0.9999 :noise-decay 0.990..0.9999 :cutoff 1000..12000 :snappy 0.0..2.5}
;;   :hat      - {:cutoff 3000..14000 :decay-closed 0.01..0.15 :decay-open 0.05..0.80}
;;   :membrane - {:start-pitch 80..400 :min-pitch 40..250 :pitch-decay 0.005..0.05 :decay 0.1..1.2 :drive 0.5..3.0}
;;   :metallic - {:cutoff 500..8000 :resonance 0.05..0.95 :decay 0.1..4.0 :drive 0.5..3.0}
;;   :clap     - {:cutoff 500..4000 :resonance 0.10..0.95 :decay 0.05..0.80 :drive 0.5..3.0}

(def core-drums
  {:kick
   {:title       "Kick Drum"
    :category    :drums
    :type        :kick
    :bus         :bus/drums
    :mod         :analog
    :base-pitch  48.0
    :pitch-drop  180.0
    :pitch-decay 0.040
    :decay       0.28
    :click       0.35
    :drive       1.6}

   :snare
   {:title       "Snare Drum"
    :category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :base-freq   185.0
    :tone-decay  0.9985
    :noise-decay 0.9991
    :cutoff      2400.0
    :snappy      0.85}

   :hat-closed
   {:title        "Closed Hi-Hat"
    :category     :drums
    :type         :hat
    :bus          :bus/drums
    :mod          :analog
    :cutoff       7200.0
    :decay-closed 0.04}

   :hat-open
   {:title       "Open Hi-Hat"
    :category    :drums
    :type        :hat
    :bus         :bus/drums
    :mod         :analog
    :cutoff      7200.0
    :decay-open  0.24}

   :snare-body
   {:title      "Snare Body Component"
    :category   :drums
    :type       :snare
    :bus        :bus/drums
    :mod        :analog
    :base-freq  160.0
    :tone-decay 0.9985}

   :snare-wire
   {:title       "Snare Wire Noise"
    :category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :noise-decay 0.9993
    :cutoff      5200.0}

   :snare-rim
   {:title       "Snare Rimshot"
    :category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :natural
    :base-freq   215.0
    :tone-decay  0.9985
    :noise-decay 0.9989
    :cutoff      5200.0
    :snappy      1.35}

   :snare-ghost
   {:title       "Snare Ghost Hit"
    :category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :base-freq   190.0
    :noise-decay 0.9980}

   :ride
   {:title     "Ride Cymbal"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    950.0
    :resonance 0.40
    :decay     2.20
    :drive     1.15}

   :tom
   {:title       "Tom Drum"
    :category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 180.0
    :min-pitch   105.0
    :pitch-decay 0.015
    :decay       0.40
    :drive       1.10}

   :sn-crack
   {:title      "Snare Crack"
    :category   :drums
    :type       :snare
    :bus        :bus/drums
    :mod        :analog
    :base-freq  220.0
    :cutoff     3800.0
    :tone-decay 0.9988}

   :ride-bell
   {:title     "Ride Cymbal Bell"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1650.0
    :resonance 0.35
    :decay     2.00
    :bandpass  false
    :drive     1.25}

   :tom-high
   {:title       "High Tom"
    :category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 240.0
    :min-pitch   170.0
    :pitch-decay 0.018
    :decay       0.35
    :drive       1.15}

   :tom-mid
   {:title       "Mid Tom"
    :category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 180.0
    :min-pitch   120.0
    :pitch-decay 0.016
    :decay       0.42
    :drive       1.15}

   :tom-low
   {:title       "Floor Tom"
    :category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 120.0
    :min-pitch   75.0
    :pitch-decay 0.014
    :decay       0.55
    :drive       1.15}

   :crash-16
   {:title     "16\" Crash"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1900.0
    :resonance 0.20
    :decay     2.00
    :bandpass  false
    :drive     1.05}

   :crash-17
   {:title     "17\" Crash"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1650.0
    :resonance 0.20
    :decay     2.40
    :bandpass  false
    :drive     1.05}

   :crash-18
   {:title     "18\" Crash"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1400.0
    :resonance 0.20
    :decay     2.80
    :bandpass  false
    :drive     1.05}

   :splash
   {:title     "Splash Cymbal"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    3200.0
    :resonance 0.25
    :decay     0.38
    :bandpass  false
    :drive     0.95}

   :china
   {:title     "China Cymbal"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1800.0
    :resonance 0.45
    :decay     1.60
    :bandpass  false
    :drive     1.35}

   :cowbell
   {:title     "Cowbell"
    :category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    820.0
    :resonance 0.85
    :decay     0.38
    :bandpass  true
    :drive     1.40}

   :clap
   {:title     "Handclap"
    :category  :drums
    :type      :clap
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1200.0
    :resonance 0.70
    :decay     0.28
    :drive     1.00}})
