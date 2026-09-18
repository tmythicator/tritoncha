(ns app.custom.instruments
  "User custom synthesizers, drum models, and sound design presets.")

;; Custom Instruments Catalog (Same format as app.lib.instruments)
;; For complete parameter schema and baseline DSP voice patches, see: app.lib.instruments

(def user-instruments
  {:tokyo-drift
   {:category  :leads
    :type      :poly
    :osc       {:type :fm :noise 0.015}
    :pitch-env {:amount 14 :decay 0.012}
    :filter    {:type :lowpass :cutoff 2800 :q 0.45 :drive 0.32 :env-amount 3500 :key-track 2.0}
    :amp-env   {:attack 0.003 :decay 0.22 :sustain 0.25 :release 0.18}
    :mod-env   {:attack 0.003 :decay 0.22}
    :bus       :bus/lead}

   :acid-beast
   {:category :bass
    :type     :mono
    :osc      {:type :saw :sub-level 0.20}
    :filter   {:type :lowpass :cutoff 420 :q 0.86 :drive 0.44 :env-amount 6000 :key-track 1.5}
    :amp-env  {:attack 0.002 :decay 0.16 :sustain 0.0 :release 0.10}
    :mod-env  {:attack 0.002 :decay 0.15}
    :glide    0.045
    :bus      :bus/bass}

   :juno-chorus
   {:category      :pads
    :type          :poly
    :maxPolyphony  16
    :osc           {:type :supersaw :drift 0.32 :noise 0.03}
    :filter        {:type :lowpass :cutoff 1400 :q 0.18 :drive 0.12 :env-amount 800 :key-track 0.9}
    :amp-env       {:attack 0.08 :decay 0.45 :sustain 0.8 :release 0.85}
    :mod-env       {:attack 0.08 :decay 0.45}
    :bus           :bus/space}

   :liquid-reese
   {:category :bass
    :type     :mono
    :osc      {:type :reese :sub-level 0.45 :drift 0.22}
    :filter   {:type :lowpass :cutoff 950 :q 0.52 :drive 0.26 :env-amount 2600 :key-track 2.0}
    :amp-env  {:attack 0.008 :decay 0.35 :sustain 0.55 :release 0.25}
    :mod-env  {:attack 0.008 :decay 0.35}
    :glide    0.025
    :bus      :bus/bass}

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

   :blade-runner
   {:category      :pads
    :type          :poly
    :maxPolyphony  8
    :osc           {:type :blade :drift 0.35 :noise 0.025}
    :filter        {:type :lowpass :cutoff 1700 :q 0.30 :drive 0.22 :env-amount 2200 :key-track 1.2}
    :amp-env       {:attack 0.07 :decay 0.60 :sustain 0.75 :release 1.2}
    :mod-env       {:attack 0.07 :decay 0.60}
    :bus           :bus/space}

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

   :rave-hoover
   {:category      :leads
    :type          :poly
    :maxPolyphony  6
    :osc           {:type :hoover :sub-level 0.35 :drift 0.22}
    :filter        {:type :lowpass :cutoff 1350 :q 0.68 :drive 0.38 :env-amount 4200 :key-track 2.0}
    :amp-env       {:attack 0.004 :decay 0.25 :sustain 0.5 :release 0.30}
    :mod-env       {:attack 0.004 :decay 0.25}
    :bus           :bus/lead}})