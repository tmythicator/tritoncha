(ns app.lib.drums
  "Core built-in layered parametric percussion catalog for Tritoncha.")

(def core-drums
  {:kick
   {:title        "Kick Drum"
    :category     :drums
    :role         :kick
    :type         :kick
    :bus          :bus/drums
    :body         :membrane
    :pitch        45.0
    :pitch-drop   145.0
    :pitch-decay  0.055
    :decay        0.58
    :click        0.25
    :click-freq   1600.0
    :drive        2.15
    :vel->pitch   0.08
    :vel->tone    0.25
    :variation    0.03}

   :fat-kick
   {:title        "Fat 909 Kick"
    :category     :drums
    :role         :kick
    :type         :kick
    :bus          :bus/drums
    :body         :membrane
    :pitch        41.0
    :pitch-drop   175.0
    :pitch-decay  0.068
    :decay        0.82
    :click        0.34
    :click-freq   1900.0
    :drive        2.90
    :vel->pitch   0.10
    :vel->tone    0.28
    :variation    0.03}

   :snare
   {:title        "Snare Drum"
    :category     :drums
    :role         :snare
    :type         :snare
    :bus          :bus/drums
    :body         :membrane
    :pitch        185.0
    :pitch-drop   75.0
    :pitch-decay  0.015
    :decay        0.14
    :click        0.22
    :click-freq   3600.0
    :noise-mix    0.85
    :noise-decay  0.22
    :noise-cutoff 2600.0
    :noise-q      0.42
    :noise-filter :bp
    :drive        1.35
    :vel->pitch   0.15
    :vel->tone    0.45
    :variation    0.10}

   :lofi-snare
   {:title        "Lo-Fi Crunchy Snare"
    :category     :drums
    :role         :snare
    :type         :snare
    :bus          :bus/drums
    :body         :membrane
    :pitch        165.0
    :pitch-drop   55.0
    :pitch-decay  0.018
    :decay        0.16
    :click        0.18
    :noise-mix    0.80
    :noise-decay  0.20
    :noise-cutoff 1900.0
    :noise-q      0.50
    :noise-filter :bp
    :drive        1.8
    :crush        0.25
    :vel->tone    0.40
    :variation    0.14}

   :hat-closed
   {:title        "Closed Hi-Hat"
    :category     :drums
    :role         :hat
    :type         :hat
    :bus          :bus/drums
    :body         :cymbal
    :pitch        340.0
    :decay        0.042
    :click        0.08
    :click-freq   7800.0
    :modal-spread 1.35
    :noise-mix    0.46
    :noise-decay  0.045
    :noise-cutoff 7200.0
    :noise-q      0.38
    :noise-filter :hp
    :drive        1.05
    :vel->tone    0.45
    :variation    0.12
    :choke-group  1}

   :hat-open
   {:title        "Open Hi-Hat"
    :category     :drums
    :role         :hat
    :type         :hat
    :bus          :bus/drums
    :body         :cymbal
    :pitch        340.0
    :decay        0.38
    :click        0.08
    :click-freq   7400.0
    :modal-spread 1.35
    :noise-mix    0.48
    :noise-decay  0.42
    :noise-cutoff 6800.0
    :noise-q      0.35
    :noise-filter :hp
    :drive        1.05
    :vel->tone    0.45
    :variation    0.10
    :choke-group  1}

   :clap
   {:title        "Hand Clap"
    :category     :drums
    :role         :clap
    :type         :clap
    :bus          :bus/drums
    :body         :burst
    :pitch        220.0
    :decay        0.22
    :click        0.08
    :burst-count  4
    :burst-spacing 0.011
    :noise-mix    1.0
    :noise-decay  0.24
    :noise-cutoff 1250.0
    :noise-q      0.68
    :noise-filter :bp
    :drive        1.45
    :vel->tone    0.40
    :variation    0.14}

   :tom-high
   {:title        "High Tom"
    :category     :drums
    :role         :tom
    :type         :membrane
    :bus          :bus/drums
    :body         :membrane
    :pitch        195.0
    :pitch-drop   72.0
    :pitch-decay  0.100
    :decay        0.54
    :click        0.18
    :click-freq   2800.0
    :fm-depth     0.20
    :fm-ratio     1.594
    :drive        1.15
    :vel->pitch   0.18
    :variation    0.08}

   :tom-mid
   {:title        "Mid Tom"
    :category     :drums
    :role         :tom
    :type         :membrane
    :bus          :bus/drums
    :body         :membrane
    :pitch        138.0
    :pitch-drop   56.0
    :pitch-decay  0.115
    :decay        0.72
    :click        0.16
    :click-freq   2400.0
    :fm-depth     0.18
    :fm-ratio     1.594
    :drive        1.15
    :vel->pitch   0.18
    :variation    0.08}

   :tom-low
   {:title        "Low Floor Tom"
    :category     :drums
    :role         :tom
    :type         :membrane
    :bus          :bus/drums
    :body         :membrane
    :pitch        92.0
    :pitch-drop   42.0
    :pitch-decay  0.135
    :decay        0.96
    :click        0.14
    :click-freq   1900.0
    :fm-depth     0.16
    :fm-ratio     1.594
    :drive        1.20
    :vel->pitch   0.16
    :variation    0.08}

   :ride
   {:title        "Ride Cymbal"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        580.0
    :decay        1.70
    :click        0.34
    :click-freq   6200.0
    :modal-spread 1.75
    :noise-mix    0.42
    :noise-decay  1.40
    :noise-cutoff 5200.0
    :noise-q      0.26
    :noise-filter :hp
    :drive        1.10
    :vel->tone    0.55
    :variation    0.10}

   :ride-bell
   {:title        "Ride Bell"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        1080.0
    :decay        1.15
    :click        0.46
    :click-freq   7400.0
    :modal-spread 1.60
    :noise-mix    0.14
    :noise-decay  0.55
    :noise-cutoff 6500.0
    :noise-q      0.32
    :noise-filter :hp
    :drive        1.15
    :vel->tone    0.50
    :variation    0.08}

   :crash-16
   {:title        "16\" Bright Crash"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        460.0
    :decay        0.18
    :click        0.0
    :click-freq   6200.0
    :modal-spread 1.65
    :noise-mix    0.90
    :noise-decay  1.70
    :noise-cutoff 3600.0
    :noise-q      0.12
    :noise-filter :hp
    :drive        1.15
    :vel->tone    0.50
    :variation    0.10}

   :crash-17
   {:title        "17\" Medium Crash"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        400.0
    :decay        0.20
    :click        0.0
    :click-freq   5600.0
    :modal-spread 1.68
    :noise-mix    0.90
    :noise-decay  2.05
    :noise-cutoff 3000.0
    :noise-q      0.12
    :noise-filter :hp
    :drive        1.15
    :vel->tone    0.50
    :variation    0.10}

   :crash-18
   {:title        "18\" Dark Crash"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        340.0
    :decay        0.22
    :click        0.0
    :click-freq   4900.0
    :modal-spread 1.72
    :noise-mix    0.92
    :noise-decay  2.40
    :noise-cutoff 2400.0
    :noise-q      0.12
    :noise-filter :hp
    :drive        1.15
    :vel->tone    0.50
    :variation    0.10}

   :splash
   {:title        "Splash Cymbal"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        580.0
    :decay        0.12
    :click        0.0
    :click-freq   7400.0
    :modal-spread 1.55
    :noise-mix    0.88
    :noise-decay  0.65
    :noise-cutoff 5000.0
    :noise-q      0.14
    :noise-filter :hp
    :drive        1.08
    :vel->tone    0.50
    :variation    0.10}

   :china
   {:title        "China Cymbal"
    :category     :drums
    :role         :cymbal
    :type         :metallic
    :bus          :bus/drums
    :body         :cymbal
    :pitch        380.0
    :decay        0.24
    :click        0.0
    :click-freq   5200.0
    :modal-spread 2.10
    :noise-mix    0.86
    :noise-decay  1.45
    :noise-cutoff 2500.0
    :noise-q      0.22
    :noise-filter :hp
    :drive        1.35
    :fold         0.16
    :vel->tone    0.50
    :variation    0.12}

   :cowbell
   {:title        "808 Cowbell"
    :category     :drums
    :role         :perc
    :type         :metallic
    :bus          :bus/drums
    :body         :idiophone
    :pitch        540.0
    :decay        0.36
    :click        0.12
    :click-freq   3200.0
    :modal-spread 0.50
    :noise-mix    0.0
    :noise-cutoff 820.0
    :noise-q      0.72
    :noise-filter :bp
    :drive        1.35
    :vel->tone    0.25
    :variation    0.03}})
