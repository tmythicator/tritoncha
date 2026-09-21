(ns app.custom.drums
  "User custom drum models, physical percussion presets, and kit sound design.")

;; Custom Drum Models Catalog (Same format as app.lib.drums)
;; For complete parameter schema and baseline DSP drum specifications, see: app.lib.drums

(def user-drums
  {:fat-kick
   {:title       "Fat Kick"
    :category    :drums
    :type        :kick
    :bus         :bus/drums
    :mod         :natural
    :base-pitch  42.0
    :pitch-drop  220.0
    :pitch-decay 0.035
    :decay       0.36
    :click       0.45
    :drive       2.2}

   :lofi-snare
   {:title       "Lo-Fi Snare"
    :category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :base-freq   210.0
    :tone-decay  0.9980
    :noise-decay 0.9988
    :cutoff      2100.0
    :snappy      0.75}})
