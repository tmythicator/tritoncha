(ns app.lib.synth
  "Core built-in synthesizer voice library and sound design catalog for Tritoncha.")

;; Sound Design Specification:
;;   :title     - Human-readable display title for UI and Studio
;;   :osc       - {:type :saw|:pulse|:tri|:sine|:supersaw|:karplus|:organ|:chiptune|:fm|:reese|:blade|:hoover
;;                 :sub-level 0.0..1.0 :pulse-width 0.05..0.95 :noise 0.0..1.0 :drift 0.0..1.0}
;;   :filter    - {:type :lowpass|:highpass|:bandpass|:notch|:ladder :cutoff 20..20000 :q 0.0..0.98
;;                 :drive 0.0..1.0 :env-amount -10000..10000 :key-track 0.0..4.0}
;;   :amp-env   - {:attack 0.001..2.0 :decay 0.005..4.0 :sustain 0.0..1.0 :release 0.005..4.0}
;;   :mod-env   - {:attack 0.001..2.0 :decay 0.005..4.0}
;;   :pitch-env - {:amount 0..48 :decay 0.005..0.100}
;;   :bus       - :bus/bass, :bus/space, :bus/lead, :bus/drums, :bus/direct
;;   :type      - :mono or :poly
;;   :glide     - portamento glide in seconds

(def core-synths
  {;; Basses
   :bass-analog
   {:title     "Analog Saw"
    :category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :saw :sub-level 0.45}
    :pitch-env {:amount 7 :decay 0.012}
    :filter    {:type :lowpass :cutoff 800 :q 0.45 :drive 0.25 :env-amount 3500 :key-track 2.0}
    :amp-env   {:attack 0.005 :decay 0.18 :sustain 0.2 :release 0.15}
    :mod-env   {:attack 0.005 :decay 0.18}}

   :bass-303
   {:title    "Acid 303"
    :category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :saw :sub-level 0.3}
    :filter   {:type :ladder :cutoff 2240 :q 0.74 :drive 0.80 :env-amount 2500 :key-track 1.2}
    :amp-env  {:attack 0.003 :decay 0.15 :sustain 0.0 :release 0.1}
    :mod-env  {:attack 0.003 :decay 0.15}
    :glide    0.04}

   :sub-moog
   {:title     "Moog Sub"
    :category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :tri :sub-level 0.70 :drift 0.10}
    :filter    {:type :ladder :cutoff 220 :q 0.35 :drive 0.20 :env-amount 300 :key-track 1.0}
    :amp-env   {:attack 0.006 :decay 0.30 :sustain 0.7 :release 0.20}
    :mod-env   {:attack 0.006 :decay 0.30}
    :glide     0.04}

   :bass-moog
   {:title     "Moog Bass"
    :category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :saw :sub-level 0.40 :drift 0.15}
    :pitch-env {:amount 9 :decay 0.012}
    :filter    {:type :ladder :cutoff 950 :q 0.65 :drive 0.40 :env-amount 4800 :key-track 2.0}
    :amp-env   {:attack 0.003 :decay 0.18 :sustain 0.3 :release 0.15}
    :mod-env   {:attack 0.003 :decay 0.18}
    :glide     0.035}

   :sub-pure
   {:title    "Sine Sub"
    :category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :sine}
    :filter   {:type :lowpass :cutoff 400 :q 0.0 :drive 0.12 :env-amount 0.0 :key-track 2.0}
    :amp-env  {:attack 0.006 :decay 0.24 :sustain 0.6 :release 0.2}
    :mod-env  {:attack 0.006 :decay 0.24}}

   :sub-808
   {:title     "808 Sub"
    :category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :sine :sub-level 0.2}
    :pitch-env {:amount 24 :decay 0.018}
    :filter    {:type :lowpass :cutoff 350 :q 0.15 :drive 0.20 :env-amount 800 :key-track 1.0}
    :amp-env   {:attack 0.002 :decay 0.5 :sustain 0.5 :release 0.4}
    :mod-env   {:attack 0.002 :decay 0.5}
    :glide     0.06}

   :bass-liquid
   {:title     "Liquid Organ"
    :category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :organ :sub-level 0.35 :drift 0.12}
    :filter    {:type :lowpass :cutoff 1790 :q 0.06 :drive 0.65 :env-amount 1800 :key-track 0.2}
    :amp-env   {:attack 0.031 :decay 1.790 :sustain 0.65 :release 0.200}
    :mod-env   {:attack 0.001 :decay 0.280}
    :pitch-env {:amount 15 :decay 0.065}
    :glide     0.045}

   :bass-slap
   {:title     "Slap Pulse"
    :category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :pulse :pulse-width 0.35 :sub-level 0.3}
    :pitch-env {:amount 12 :decay 0.010}
    :filter    {:type :lowpass :cutoff 1600 :q 0.55 :drive 0.22 :env-amount 4800 :key-track 2.5}
    :amp-env   {:attack 0.002 :decay 0.14 :sustain 0.1 :release 0.1}
    :mod-env   {:attack 0.002 :decay 0.14}}

   :bass-neuro
   {:title    "Neuro Notch"
    :category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :reese :sub-level 0.35 :drift 0.15}
    :filter   {:type :notch :cutoff 1200 :q 0.72 :drive 0.42 :env-amount 3800 :key-track 2.0}
    :amp-env  {:attack 0.005 :decay 0.22 :sustain 0.4 :release 0.2}
    :mod-env  {:attack 0.005 :decay 0.22}}

   :bass-organ
   {:title    "Slap Organ"
    :category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :organ :sub-level 0.4}
    :filter   {:type :lowpass :cutoff 1200 :q 0.35 :drive 0.18 :env-amount 2500 :key-track 2.0}
    :amp-env  {:attack 0.002 :decay 0.18 :sustain 0.25 :release 0.12}
    :mod-env  {:attack 0.002 :decay 0.18}}

   :acid-beast
   {:title    "Acid 303 Beast"
    :category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :saw :sub-level 0.20}
    :filter   {:type :lowpass :cutoff 420 :q 0.86 :drive 0.44 :env-amount 6000 :key-track 1.5}
    :amp-env  {:attack 0.002 :decay 0.16 :sustain 0.0 :release 0.10}
    :mod-env  {:attack 0.002 :decay 0.15}
    :glide    0.045}

   :liquid-reese
   {:title    "Liquid Reese"
    :category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :reese :sub-level 0.45 :drift 0.22}
    :filter   {:type :lowpass :cutoff 950 :q 0.52 :drive 0.26 :env-amount 2600 :key-track 2.0}
    :amp-env  {:attack 0.008 :decay 0.35 :sustain 0.55 :release 0.25}
    :mod-env  {:attack 0.008 :decay 0.35}
    :glide    0.025}

   ;; Pads and Atmospheres
   :pad-cinema
   {:title         "Cinema"
    :category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :saw :sub-level 0.4 :drift 0.30 :noise 0.035}
    :filter        {:type :lowpass :cutoff 1100 :q 0.28 :drive 0.15 :env-amount 500 :key-track 0.7}
    :amp-env       {:attack 0.08 :decay 0.45 :sustain 0.75 :release 0.65}
    :mod-env       {:attack 0.08 :decay 0.45}}

   :pad-strings
   {:title         "Strings Supersaw"
    :category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :supersaw :sub-level 0.0 :drift 0.32 :noise 0.065}
    :filter        {:type :lowpass :cutoff 1900 :q 0.12 :drive 0.12 :env-amount 800 :key-track 0.85}
    :amp-env       {:attack 0.07 :decay 0.45 :sustain 0.8 :release 0.7}
    :mod-env       {:attack 0.07 :decay 0.45}}

   :pad-vocal
   {:title         "Vocal Choir"
    :category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :organ :sub-level 0.20 :noise 0.02 :drift 0.35}
    :filter        {:type :ladder :cutoff 3740 :q 0.56 :drive 0.15 :env-amount -2800 :key-track 1.7}
    :amp-env       {:attack 0.071 :decay 1.030 :sustain 0.36 :release 0.230}
    :mod-env       {:attack 0.156 :decay 1.090}
    :pitch-env     {:amount 12 :decay 0.010}
    :glide         0.275}

   :pad-glass
   {:title         "Glass Sine"
    :category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :sine :sub-level 0.0 :drift 0.18 :noise 0.045}
    :filter        {:type :lowpass :cutoff 3200 :q 0.15 :drive 0.08 :env-amount 1200 :key-track 1.0}
    :amp-env       {:attack 0.01 :decay 0.25 :sustain 0.0 :release 0.2}
    :mod-env       {:attack 0.01 :decay 0.25}}

   :pad-drone
   {:title         "Deep Drone"
    :category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :saw :sub-level 0.55 :drift 0.32 :noise 0.03}
    :filter        {:type :lowpass :cutoff 750 :q 0.38 :drive 0.25 :env-amount 400 :key-track 0.6}
    :amp-env       {:attack 0.12 :decay 0.6 :sustain 0.85 :release 0.85}
    :mod-env       {:attack 0.12 :decay 0.6}}

   :pad-dreamy
   {:title        "Dreamy Noisy Chorus"
    :category     :pads
    :type         :poly
    :bus          :bus/space
    :maxPolyphony 16
    :osc          {:type :supersaw :sub-level 0.15 :noise 0.18 :drift 0.95}
    :filter       {:type :lowpass :cutoff 3690 :q 0.18 :drive 0.12 :env-amount 800 :key-track 0.9}
    :amp-env      {:attack 0.080 :decay 1.370 :sustain 0.52 :release 0.850}
    :mod-env      {:attack 0.676 :decay 0.450}
    :pitch-env    {:amount 19 :decay 0.030}
    :glide        0.315}

   :blade-runner
   {:title        "Blade Runner"
    :category     :pads
    :type         :poly
    :bus          :bus/space
    :maxPolyphony 8
    :osc          {:type :blade :drift 0.35 :noise 0.025}
    :filter       {:type :lowpass :cutoff 1700 :q 0.30 :drive 0.22 :env-amount 2200 :key-track 1.2}
    :amp-env      {:attack 0.07 :decay 0.60 :sustain 0.75 :release 1.2}
    :mod-env      {:attack 0.07 :decay 0.60}}

   ;; Leads and Arps
   :lead-pluck
   {:title     "Pulse Pluck Lead"
    :category  :leads
    :type      :poly
    :bus       :bus/lead
    :osc       {:type :pulse :pulse-width 0.5}
    :pitch-env {:amount 14 :decay 0.014}
    :filter    {:type :lowpass :cutoff 1200 :q 0.5 :drive 0.22 :env-amount 4500 :key-track 3.0}
    :amp-env   {:attack 0.003 :decay 0.18 :sustain 0.1 :release 0.15}
    :mod-env   {:attack 0.003 :decay 0.18}}

   :lead-supersaw
   {:title    "Trance Supersaw Lead"
    :category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :supersaw :drift 0.22}
    :filter   {:type :lowpass :cutoff 2000 :q 0.4 :drive 0.25 :env-amount 5000 :key-track 3.0}
    :amp-env  {:attack 0.004 :decay 0.22 :sustain 0.4 :release 0.3}
    :mod-env  {:attack 0.004 :decay 0.22}}

   :lead-fm
   {:title     "FM Metallic Pluck"
    :category  :leads
    :type      :poly
    :bus       :bus/lead
    :osc       {:type :fm}
    :pitch-env {:amount 12 :decay 0.015}
    :filter    {:type :lowpass :cutoff 1500 :q 0.3 :drive 0.32 :env-amount 3000 :key-track 2.5}
    :amp-env   {:attack 0.003 :decay 0.16 :sustain 0.2 :release 0.15}
    :mod-env   {:attack 0.003 :decay 0.16}}

   :lead-blade
   {:title    "CS-80 Blade Lead"
    :category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :blade :drift 0.32 :noise 0.02}
    :filter   {:type :lowpass :cutoff 1800 :q 0.25 :drive 0.20 :env-amount 1800 :key-track 1.0}
    :amp-env  {:attack 0.08 :decay 0.6 :sustain 0.7 :release 1.4}
    :mod-env  {:attack 0.08 :decay 0.6}}

   :lead-hoover
   {:title    "Mentasm Hoover Lead"
    :category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :hoover :sub-level 0.3 :drift 0.20}
    :filter   {:type :lowpass :cutoff 1200 :q 0.65 :drive 0.35 :env-amount 4000 :key-track 2.0}
    :amp-env  {:attack 0.004 :decay 0.25 :sustain 0.5 :release 0.3}
    :mod-env  {:attack 0.004 :decay 0.25}}

   :lead-string
   {:title    "Karplus Acoustic Pluck"
    :category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :karplus}
    :filter   {:type :lowpass :cutoff 4000 :q 0.1 :drive 0.10 :env-amount 0.0 :key-track 8.0}
    :amp-env  {:attack 0.001 :decay 0.8 :sustain 0.0 :release 0.4}
    :mod-env  {:attack 0.001 :decay 0.8}}

   :lead-8bit
   {:title    "Chiptune 8-Bit Lead"
    :category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :chiptune :pulse-width 0.25}
    :filter   {:type :lowpass :cutoff 4000 :q 0.2 :drive 0.15 :env-amount 0.0 :key-track 4.0}
    :amp-env  {:attack 0.002 :decay 0.18 :sustain 0.3 :release 0.15}
    :mod-env  {:attack 0.002 :decay 0.18}}

   :lead-organ
   {:title    "Drawbar Tonewheel Organ"
    :category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :organ}
    :filter   {:type :lowpass :cutoff 14000 :q 0.0 :drive 0.05 :env-amount 0.0 :key-track 0.0}
    :amp-env  {:attack 0.002 :decay 0.20 :sustain 0.8 :release 0.2}
    :mod-env  {:attack 0.002 :decay 0.20}}

   :lead-bell
   {:title        "Pure Sine Bell"
    :category     :leads
    :type         :poly
    :bus          :bus/lead
    :maxPolyphony 8
    :osc          {:type :sine :sub-level 0.2 :drift 0.15}
    :filter       {:type :lowpass :cutoff 3600 :q 0.3 :drive 0.10 :env-amount 2000 :key-track 2.0}
    :amp-env      {:attack 0.002 :decay 0.3 :sustain 0.0 :release 0.2}
    :mod-env      {:attack 0.002 :decay 0.3}}

   :tokyo-drift
   {:title     "Tokyo Drift Lead"
    :category  :leads
    :type      :poly
    :bus       :bus/lead
    :osc       {:type :fm :noise 0.015}
    :pitch-env {:amount 14 :decay 0.012}
    :filter    {:type :lowpass :cutoff 2800 :q 0.45 :drive 0.32 :env-amount 3500 :key-track 2.0}
    :amp-env   {:attack 0.003 :decay 0.22 :sustain 0.25 :release 0.18}
    :mod-env   {:attack 0.003 :decay 0.22}}

   :glass-mallet
   {:title        "Glass Mallet Lead"
    :category     :leads
    :type         :poly
    :bus          :bus/lead
    :maxPolyphony 12
    :osc          {:type :sine :sub-level 0.15 :drift 0.12}
    :pitch-env    {:amount 18 :decay 0.008}
    :filter       {:type :lowpass :cutoff 3400 :q 0.30 :drive 0.10 :env-amount 2400 :key-track 2.0}
    :amp-env      {:attack 0.002 :decay 0.28 :sustain 0.05 :release 0.35}
    :mod-env      {:attack 0.002 :decay 0.28}}

   ;; Sound Effects and Utilities
   :click
   {:title    "Metronome Click"
    :category :fx
    :type     :mono
    :bus      :bus/direct
    :osc      {:type :click}
    :filter   {:type :lowpass :cutoff 2800 :q 0.4 :drive 0.0 :env-amount 0.0 :key-track 0.0}
    :amp-env  {:attack 0.001 :decay 0.015 :sustain 0.0 :release 0.01}
    :mod-env  {:attack 0.001 :decay 0.015}}

   :fx-siren
   {:title    "Dub Laser Siren"
    :category :fx
    :type     :synth
    :bus      :bus/direct
    :osc      {:type :saw :drift 0.25}
    :filter   {:type :lowpass :cutoff 3000 :q 0.55 :drive 0.35 :env-amount 4000 :key-track 1.0}
    :amp-env  {:attack 0.05 :decay 0.4 :sustain 0.3 :release 0.8}
    :mod-env  {:attack 0.05 :decay 0.4}}

   :fx-laser
   {:title     "Pitch Laser Drop"
    :category  :fx
    :type      :mono
    :bus       :bus/direct
    :osc       {:type :pulse :pulse-width 0.5}
    :pitch-env {:amount 36 :decay 0.025}
    :filter    {:type :lowpass :cutoff 8000 :q 0.75 :drive 0.40 :env-amount -6000 :key-track 1.0}
    :amp-env   {:attack 0.002 :decay 0.12 :sustain 0.0 :release 0.08}
    :mod-env   {:attack 0.002 :decay 0.12}}

   :fx-subdrop
   {:title     "Seismic Sub Drop"
    :category  :fx
    :type      :mono
    :bus       :bus/direct
    :osc       {:type :sine :sub-level 0.4}
    :pitch-env {:amount 48 :decay 0.35}
    :filter    {:type :lowpass :cutoff 1200 :q 0.2 :drive 0.30 :env-amount -800 :key-track 0.0}
    :amp-env   {:attack 0.005 :decay 1.2 :sustain 0.0 :release 0.4}
    :mod-env   {:attack 0.005 :decay 1.2}}

   :fx-zap
   {:title     "Analog Zap Shot"
    :category  :fx
    :type      :mono
    :bus       :bus/direct
    :osc       {:type :tri}
    :pitch-env {:amount 24 :decay 0.018}
    :filter    {:type :lowpass :cutoff 6000 :q 0.8 :drive 0.25 :env-amount -4000 :key-track 0.0}
    :amp-env   {:attack 0.001 :decay 0.08 :sustain 0.0 :release 0.04}
    :mod-env   {:attack 0.001 :decay 0.08}}})

(def core-instruments core-synths)
