(ns app.custom.instruments
  "User custom synthesizers, drum models, and sound design presets.")

;; Sound Design in ClojureScript:
;; Instruments are declared as pure, neutral maps.
;; Rust WASM DSP provides pure, uncolored building blocks:
;;   :osc       - {:type :saw|:pulse|:tri|:sine|:supersaw|:karplus|:organ|:chiptune|:fm|:reese|:blade|:hoover
;;                 :sub-level 0.0..1.0
;;                 :pulse-width 0.05..0.95
;;                 :noise 0.0..1.0 (white noise injection)
;;                 :drift 0.0..1.0 (organic VCO pitch drift)}
;;   :filter    - {:type :lowpass|:highpass|:bandpass|:notch
;;                 :cutoff 20..20000
;;                 :q 0.0..0.98
;;                 :drive 0.0..1.0 (analog saturation in SVF integrator feedback)
;;                 :env-amount -10000..10000
;;                 :key-track 0.0..4.0}
;;   :amp-env   - {:attack 0.001..2.0 :decay 0.005..4.0 :sustain 0.0..1.0 :release 0.005..4.0}
;;   :mod-env   - {:attack 0.001..2.0 :decay 0.005..4.0}
;;   :pitch-env - {:amount 0..48 (semitones transient punch) :decay 0.005..0.100 (seconds)}
;;   :bus       - :bus/bass, :bus/space, :bus/lead, :bus/drums, :bus/direct
;;   :type      - :mono or :poly
;;   :glide     - portamento glide in seconds (e.g. 0.04)

(def user-instruments
  {;; Cyberpunk FM Lead with fast punchy pitch transient
   :tokyo-drift
   {:category  :leads
    :type      :poly
    :osc       {:type :fm :noise 0.015}
    :pitch-env {:amount 14 :decay 0.012}
    :filter    {:type :lowpass :cutoff 2800 :q 0.45 :drive 0.32 :env-amount 3500 :key-track 2.0}
    :amp-env   {:attack 0.003 :decay 0.22 :sustain 0.25 :release 0.18}
    :mod-env   {:attack 0.003 :decay 0.22}
    :bus       :bus/lead}

   ;; Classic Minimoog analog mono bass: warm saw + deep sub + filter drive
   :moog-sub
   {:category  :bass
    :type      :mono
    :osc       {:type :saw :sub-level 0.50 :drift 0.18}
    :pitch-env {:amount 7 :decay 0.010}
    :filter    {:type :lowpass :cutoff 750 :q 0.50 :drive 0.30 :env-amount 3800 :key-track 2.0}
    :amp-env   {:attack 0.004 :decay 0.20 :sustain 0.2 :release 0.15}
    :mod-env   {:attack 0.004 :decay 0.20}
    :bus       :bus/bass}

   ;; Searing 303 acid bass: extreme resonance + saturation + portamento glide
   :acid-beast
   {:category :bass
    :type     :mono
    :osc      {:type :saw :sub-level 0.20}
    :filter   {:type :lowpass :cutoff 420 :q 0.86 :drive 0.44 :env-amount 6000 :key-track 1.5}
    :amp-env  {:attack 0.002 :decay 0.16 :sustain 0.0 :release 0.10}
    :mod-env  {:attack 0.002 :decay 0.15}
    :glide    0.045
    :bus      :bus/bass}

   ;; Vintage Roland Juno-106 analog pad: supersaw + organic VCO drift + tape air
   :juno-chorus
   {:category      :pads
    :type          :poly
    :maxPolyphony  16
    :osc           {:type :supersaw :drift 0.32 :noise 0.03}
    :filter        {:type :lowpass :cutoff 1400 :q 0.18 :drive 0.12 :env-amount 800 :key-track 0.9}
    :amp-env       {:attack 0.08 :decay 0.45 :sustain 0.8 :release 0.85}
    :mod-env       {:attack 0.08 :decay 0.45}
    :bus           :bus/space}

   ;; Liquid Drum and Bass Reese: detuned saws + sub foundation + filter saturation
   :liquid-reese
   {:category :bass
    :type     :mono
    :osc      {:type :reese :sub-level 0.45 :drift 0.22}
    :filter   {:type :lowpass :cutoff 950 :q 0.52 :drive 0.26 :env-amount 2600 :key-track 2.0}
    :amp-env  {:attack 0.008 :decay 0.35 :sustain 0.55 :release 0.25}
    :mod-env  {:attack 0.008 :decay 0.35}
    :glide    0.025
    :bus      :bus/bass}

   ;; Crystalline mallet / bell pluck with fast acoustic strike
   :glass-mallet
   {:category      :leads
    :type          :poly
    :maxPolyphony  12
    :osc           {:type :sine :sub-level 0.15 :drift 0.12}
    :pitch-env     {:amount 18 :decay 0.008}
    :filter        {:type :lowpass :cutoff 3400 :q 0.30 :drive 0.10 :env-amount 2400 :key-track 2.0}
    :amp-env       {:attack 0.002 :decay 0.28 :sustain 0.05 :release 0.35}
    :mod-env       {:attack 0.002 :decay 0.28}
    :bus           :bus/space}

   ;; Vangelis CS-80 cinematic brass lead with slow opening and rich drift
   :blade-runner
   {:category      :pads
    :type          :poly
    :maxPolyphony  8
    :osc           {:type :blade :drift 0.35 :noise 0.025}
    :filter        {:type :lowpass :cutoff 1700 :q 0.30 :drive 0.22 :env-amount 2200 :key-track 1.2}
    :amp-env       {:attack 0.07 :decay 0.60 :sustain 0.75 :release 1.2}
    :mod-env       {:attack 0.07 :decay 0.60}
    :bus           :bus/space}

   ;; Memphis Phonk cowbell / lead with aggressive harmonic bite
   :phonk-bell
   {:category      :leads
    :type          :poly
    :maxPolyphony  8
    :osc           {:type :pulse :pulse-width 0.25 :noise 0.02}
    :pitch-env     {:amount 24 :decay 0.014}
    :filter        {:type :bandpass :cutoff 2400 :q 0.60 :drive 0.35 :env-amount 1800 :key-track 2.0}
    :amp-env       {:attack 0.002 :decay 0.20 :sustain 0.0 :release 0.15}
    :mod-env       {:attack 0.002 :decay 0.20}
    :bus           :bus/lead}

   ;; Alpha Juno rave mentasm hoover with sub rumble and crunchy drive
   :rave-hoover
   {:category      :leads
    :type          :poly
    :maxPolyphony  6
    :osc           {:type :hoover :sub-level 0.35 :drift 0.22}
    :filter        {:type :lowpass :cutoff 1350 :q 0.68 :drive 0.38 :env-amount 4200 :key-track 2.0}
    :amp-env       {:attack 0.004 :decay 0.25 :sustain 0.5 :release 0.30}
    :mod-env       {:attack 0.004 :decay 0.25}
    :bus           :bus/lead}})