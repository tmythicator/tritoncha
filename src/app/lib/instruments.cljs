(ns app.lib.instruments
  "Core built-in instrument library and synthesizer voice catalog for Tritoncha.")

;; Sound Design in ClojureScript:
;; Instruments are configured as pure, neutral declarative maps.
;; The Rust WASM DSP provides clean, uncolored building blocks:
;;   :osc       - {:type :saw|:pulse|:tri|:sine|:supersaw|:karplus|:organ|:chiptune|:fm|:reese|:blade|:hoover
;;                 :sub-level 0.0..1.0 :pulse-width 0.05..0.95
;;                 :noise 0.0..1.0 :drift 0.0..1.0}
;;   :filter    - {:type :lowpass|:highpass|:bandpass|:notch :cutoff 20..20000 :q 0.0..0.98
;;                 :drive 0.0..1.0 :env-amount -10000..10000 :key-track 0.0..4.0}
;;   :amp-env   - {:attack 0.001..2.0 :decay 0.005..4.0 :sustain 0.0..1.0 :release 0.005..4.0}
;;   :mod-env   - {:attack 0.001..2.0 :decay 0.005..4.0}
;;   :pitch-env - {:amount 0..48 :decay 0.005..0.100}
;;   :bus       - :bus/bass, :bus/space, :bus/lead, :bus/drums, :bus/direct
;;   :type      - :mono or :poly
;;   :glide     - portamento glide in seconds

(def primary-instruments
  {;; Basses
   :bass-analog
   {:category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :saw :sub-level 0.45}
    :pitch-env {:amount 7 :decay 0.012}
    :filter    {:type :lowpass :cutoff 800 :q 0.45 :drive 0.25 :env-amount 3500 :key-track 2.0}
    :amp-env   {:attack 0.005 :decay 0.18 :sustain 0.2 :release 0.15}
    :mod-env   {:attack 0.005 :decay 0.18}}

   :bass-303
   {:category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :saw :sub-level 0.3}
    :filter   {:type :lowpass :cutoff 400 :q 0.82 :drive 0.38 :env-amount 5500 :key-track 1.5}
    :amp-env  {:attack 0.003 :decay 0.15 :sustain 0.0 :release 0.1}
    :mod-env  {:attack 0.003 :decay 0.15}
    :glide    0.04}

   :sub-pure
   {:category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :sine}
    :filter   {:type :lowpass :cutoff 400 :q 0.0 :drive 0.12 :env-amount 0.0 :key-track 2.0}
    :amp-env  {:attack 0.006 :decay 0.24 :sustain 0.6 :release 0.2}
    :mod-env  {:attack 0.006 :decay 0.24}}

   :sub-808
   {:category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :sine :sub-level 0.2}
    :pitch-env {:amount 24 :decay 0.018}
    :filter    {:type :lowpass :cutoff 350 :q 0.15 :drive 0.20 :env-amount 800 :key-track 1.0}
    :amp-env   {:attack 0.002 :decay 0.5 :sustain 0.5 :release 0.4}
    :mod-env   {:attack 0.002 :decay 0.5}
    :glide     0.06}

   :bass-reese
   {:category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :reese :sub-level 0.45 :drift 0.22}
    :filter   {:type :lowpass :cutoff 1100 :q 0.55 :drive 0.28 :env-amount 3200 :key-track 2.0}
    :amp-env  {:attack 0.008 :decay 0.35 :sustain 0.6 :release 0.25}
    :mod-env  {:attack 0.008 :decay 0.35}
    :glide    0.03}

   :bass-slap
   {:category  :bass
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :pulse :pulse-width 0.35 :sub-level 0.3}
    :pitch-env {:amount 12 :decay 0.010}
    :filter    {:type :lowpass :cutoff 1600 :q 0.55 :drive 0.22 :env-amount 4800 :key-track 2.5}
    :amp-env   {:attack 0.002 :decay 0.14 :sustain 0.1 :release 0.1}
    :mod-env   {:attack 0.002 :decay 0.14}}

   :bass-neuro
   {:category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :reese :sub-level 0.35 :drift 0.15}
    :filter   {:type :notch :cutoff 1200 :q 0.72 :drive 0.42 :env-amount 3800 :key-track 2.0}
    :amp-env  {:attack 0.005 :decay 0.22 :sustain 0.4 :release 0.2}
    :mod-env  {:attack 0.005 :decay 0.22}}

   :bass-organ
   {:category :bass
    :type     :mono
    :bus      :bus/bass
    :osc      {:type :organ :sub-level 0.4}
    :filter   {:type :lowpass :cutoff 1200 :q 0.35 :drive 0.18 :env-amount 2500 :key-track 2.0}
    :amp-env  {:attack 0.002 :decay 0.18 :sustain 0.25 :release 0.12}
    :mod-env  {:attack 0.002 :decay 0.18}}

   ;; Pads and Atmospheres
   :pad-cinema
   {:category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :saw :sub-level 0.4 :drift 0.30 :noise 0.035}
    :filter        {:type :lowpass :cutoff 1100 :q 0.28 :drive 0.15 :env-amount 500 :key-track 0.7}
    :amp-env       {:attack 0.08 :decay 0.45 :sustain 0.75 :release 0.65}
    :mod-env       {:attack 0.08 :decay 0.45}}

   :pad-strings
   {:category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :supersaw :sub-level 0.0 :drift 0.32}
    :filter        {:type :lowpass :cutoff 1900 :q 0.12 :drive 0.12 :env-amount 800 :key-track 0.85}
    :amp-env       {:attack 0.07 :decay 0.45 :sustain 0.8 :release 0.7}
    :mod-env       {:attack 0.07 :decay 0.45}}

   :pad-shimmer
   {:category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :supersaw :sub-level 0.0 :drift 0.24 :noise 0.02}
    :filter        {:type :lowpass :cutoff 1600 :q 0.15 :drive 0.10 :env-amount 600 :key-track 0.8}
    :amp-env       {:attack 0.08 :decay 0.4 :sustain 0.6 :release 0.6}
    :mod-env       {:attack 0.08 :decay 0.4}}

   :pad-vocal
   {:category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :pulse :pulse-width 0.32 :drift 0.28 :noise 0.04}
    :filter        {:type :bandpass :cutoff 1900 :q 0.62 :drive 0.15 :env-amount 800 :key-track 0.9}
    :amp-env       {:attack 0.06 :decay 0.4 :sustain 0.7 :release 0.5}
    :mod-env       {:attack 0.06 :decay 0.4}}

   :pad-glass
   {:category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :sine :sub-level 0.0 :drift 0.18 :noise 0.015}
    :filter        {:type :lowpass :cutoff 3200 :q 0.15 :drive 0.08 :env-amount 1200 :key-track 1.0}
    :amp-env       {:attack 0.01 :decay 0.25 :sustain 0.0 :release 0.2}
    :mod-env       {:attack 0.01 :decay 0.25}}

   :pad-drone
   {:category      :pads
    :type          :poly
    :bus           :bus/space
    :maxPolyphony  16
    :osc           {:type :saw :sub-level 0.55 :drift 0.32 :noise 0.03}
    :filter        {:type :lowpass :cutoff 750 :q 0.38 :drive 0.25 :env-amount 400 :key-track 0.6}
    :amp-env       {:attack 0.12 :decay 0.6 :sustain 0.85 :release 0.85}
    :mod-env       {:attack 0.12 :decay 0.6}}

   ;; Leads and Arps
   :lead-pluck
   {:category  :leads
    :type      :poly
    :bus       :bus/lead
    :osc       {:type :pulse :pulse-width 0.5}
    :pitch-env {:amount 14 :decay 0.014}
    :filter    {:type :lowpass :cutoff 1200 :q 0.5 :drive 0.22 :env-amount 4500 :key-track 3.0}
    :amp-env   {:attack 0.003 :decay 0.18 :sustain 0.1 :release 0.15}
    :mod-env   {:attack 0.003 :decay 0.18}}

   :lead-supersaw
   {:category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :supersaw :drift 0.22}
    :filter   {:type :lowpass :cutoff 2000 :q 0.4 :drive 0.25 :env-amount 5000 :key-track 3.0}
    :amp-env  {:attack 0.004 :decay 0.22 :sustain 0.4 :release 0.3}
    :mod-env  {:attack 0.004 :decay 0.22}}

   :lead-fm
   {:category  :leads
    :type      :poly
    :bus       :bus/lead
    :osc       {:type :fm}
    :pitch-env {:amount 12 :decay 0.015}
    :filter    {:type :lowpass :cutoff 1500 :q 0.3 :drive 0.32 :env-amount 3000 :key-track 2.5}
    :amp-env   {:attack 0.003 :decay 0.16 :sustain 0.2 :release 0.15}
    :mod-env   {:attack 0.003 :decay 0.16}}

   :lead-blade
   {:category :leads
    :type     :poly
    :bus      :bus/space
    :osc      {:type :blade :drift 0.32 :noise 0.02}
    :filter   {:type :lowpass :cutoff 1800 :q 0.25 :drive 0.20 :env-amount 1800 :key-track 1.0}
    :amp-env  {:attack 0.08 :decay 0.6 :sustain 0.7 :release 1.4}
    :mod-env  {:attack 0.08 :decay 0.6}}

   :lead-hoover
   {:category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :hoover :sub-level 0.3 :drift 0.20}
    :filter   {:type :lowpass :cutoff 1200 :q 0.65 :drive 0.35 :env-amount 4000 :key-track 2.0}
    :amp-env  {:attack 0.004 :decay 0.25 :sustain 0.5 :release 0.3}
    :mod-env  {:attack 0.004 :decay 0.25}}

   :lead-string
   {:category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :karplus}
    :filter   {:type :lowpass :cutoff 4000 :q 0.1 :drive 0.10 :env-amount 0.0 :key-track 8.0}
    :amp-env  {:attack 0.001 :decay 0.8 :sustain 0.0 :release 0.4}
    :mod-env  {:attack 0.001 :decay 0.8}}

   :lead-8bit
   {:category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :chiptune :pulse-width 0.25}
    :filter   {:type :lowpass :cutoff 4000 :q 0.2 :drive 0.15 :env-amount 0.0 :key-track 4.0}
    :amp-env  {:attack 0.002 :decay 0.18 :sustain 0.3 :release 0.15}
    :mod-env  {:attack 0.002 :decay 0.18}}

   :lead-organ
   {:category :leads
    :type     :poly
    :bus      :bus/lead
    :osc      {:type :organ}
    :filter   {:type :lowpass :cutoff 14000 :q 0.0 :drive 0.05 :env-amount 0.0 :key-track 0.0}
    :amp-env  {:attack 0.002 :decay 0.20 :sustain 0.8 :release 0.2}
    :mod-env  {:attack 0.002 :decay 0.20}}

   :lead-bell
   {:category     :leads
    :type         :poly
    :bus          :bus/lead
    :maxPolyphony 8
    :osc          {:type :sine :sub-level 0.2 :drift 0.15}
    :filter       {:type :lowpass :cutoff 3600 :q 0.3 :drive 0.10 :env-amount 2000 :key-track 2.0}
    :amp-env      {:attack 0.002 :decay 0.3 :sustain 0.0 :release 0.2}
    :mod-env      {:attack 0.002 :decay 0.3}}

   ;; Sound Effects and Utilities
   :click
   {:category :fx
    :type     :mono
    :bus      :bus/direct
    :osc      {:type :click}
    :filter   {:type :lowpass :cutoff 2800 :q 0.4 :drive 0.0 :env-amount 0.0 :key-track 0.0}
    :amp-env  {:attack 0.001 :decay 0.015 :sustain 0.0 :release 0.01}
    :mod-env  {:attack 0.001 :decay 0.015}}

   :fx-siren
   {:category :fx
    :type     :synth
    :bus      :bus/space
    :osc      {:type :saw :drift 0.25}
    :filter   {:type :lowpass :cutoff 3000 :q 0.55 :drive 0.35 :env-amount 4000 :key-track 1.0}
    :amp-env  {:attack 0.05 :decay 0.4 :sustain 0.3 :release 0.8}
    :mod-env  {:attack 0.05 :decay 0.4}}

   :fx-laser
   {:category  :fx
    :type      :mono
    :bus       :bus/lead
    :osc       {:type :pulse :pulse-width 0.5}
    :pitch-env {:amount 36 :decay 0.025}
    :filter    {:type :lowpass :cutoff 8000 :q 0.75 :drive 0.40 :env-amount -6000 :key-track 1.0}
    :amp-env   {:attack 0.002 :decay 0.12 :sustain 0.0 :release 0.08}
    :mod-env   {:attack 0.002 :decay 0.12}}

   :fx-subdrop
   {:category  :fx
    :type      :mono
    :bus       :bus/bass
    :osc       {:type :sine :sub-level 0.4}
    :pitch-env {:amount 48 :decay 0.35}
    :filter    {:type :lowpass :cutoff 1200 :q 0.2 :drive 0.30 :env-amount -800 :key-track 0.0}
    :amp-env   {:attack 0.005 :decay 1.2 :sustain 0.0 :release 0.4}
    :mod-env   {:attack 0.005 :decay 1.2}}

   :fx-zap
   {:category  :fx
    :type      :mono
    :bus       :bus/lead
    :osc       {:type :tri}
    :pitch-env {:amount 24 :decay 0.018}
    :filter    {:type :lowpass :cutoff 6000 :q 0.8 :drive 0.25 :env-amount -4000 :key-track 0.0}
    :amp-env   {:attack 0.001 :decay 0.08 :sustain 0.0 :release 0.04}
    :mod-env   {:attack 0.001 :decay 0.08}}})

(def core-instruments primary-instruments)
