(ns app.custom.synth
  "User custom synthesizers and sound design presets.")

;; Custom Synthesizers Catalog (Same format as app.lib.synth)
;; For complete parameter schema and baseline DSP voice patches, see: app.lib.synth

(def user-synths
  {:phonk-bell
   {:title         "Phonk Bell"
    :category      :leads
    :type          :poly
    :maxPolyphony  8
    :osc           {:type :pulse :pulse-width 0.25 :noise 0.02}
    :pitch-env     {:amount 24 :decay 0.014}
    :filter        {:type :bandpass :cutoff 2400 :q 0.60 :drive 0.35 :env-amount 1800 :key-track 2.0}
    :amp-env       {:attack 0.002 :decay 0.20 :sustain 0.0 :release 0.15}
    :mod-env       {:attack 0.002 :decay 0.20}
    :bus           :bus/lead}})

(def user-instruments user-synths)
