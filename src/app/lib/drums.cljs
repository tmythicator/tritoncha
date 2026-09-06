(ns app.lib.drums
  "Core built-in drum kit synthesizer instruments and composite drum voice catalog.")

(def core-drum-instruments
  {:kick
   {:category    :drums
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
   {:category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :base-freq   185.0
    :tone-decay  0.9985
    :noise-decay 0.9991
    :cutoff      2400.0
    :snappy      0.85}

   :hat
   {:category     :drums
    :type         :hat
    :bus          :bus/drums
    :mod          :analog
    :cutoff       7200.0
    :decay-closed 0.04
    :decay-open   0.24}

   :snare-body
   {:category   :drums
    :type       :snare
    :bus        :bus/drums
    :mod        :analog
    :base-freq  160.0
    :tone-decay 0.9985}

   :snare-wire
   {:category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :noise-decay 0.9993
    :cutoff      5200.0}

   :snare-rim
   {:category   :drums
    :type       :snare
    :bus        :bus/drums
    :mod        :analog
    :base-freq  420.0
    :tone-decay 0.9975
    :cutoff     4200.0}

   :snare-ghost
   {:category    :drums
    :type        :snare
    :bus         :bus/drums
    :mod         :analog
    :base-freq   190.0
    :noise-decay 0.9980}

   :hat-closed
   {:category     :drums
    :type         :hat
    :bus          :bus/drums
    :mod          :analog
    :cutoff       7200.0
    :decay-closed 0.04}

   :hat-open
   {:category   :drums
    :type       :hat
    :bus        :bus/drums
    :mod        :analog
    :cutoff     7200.0
    :decay-open 0.24}

   :ride
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    950.0
    :resonance 0.40
    :decay     2.20
    :drive     1.15}

   :tom
   {:category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 180.0
    :min-pitch   105.0
    :pitch-decay 0.015
    :decay       0.40
    :drive       1.10}

   :sn-crack
   {:category   :drums
    :type       :snare
    :bus        :bus/drums
    :mod        :analog
    :base-freq  220.0
    :cutoff     3800.0
    :tone-decay 0.9988}

   :ride-bell
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1650.0
    :resonance 0.35
    :decay     2.00
    :bandpass  false
    :drive     1.25}

   :tom-high
   {:category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 240.0
    :min-pitch   170.0
    :pitch-decay 0.018
    :decay       0.35
    :drive       1.15}

   :tom-mid
   {:category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 180.0
    :min-pitch   120.0
    :pitch-decay 0.016
    :decay       0.42
    :drive       1.15}

   :tom-low
   {:category    :drums
    :type        :membrane
    :bus         :bus/drums
    :mod         :analog
    :start-pitch 120.0
    :min-pitch   75.0
    :pitch-decay 0.014
    :decay       0.55
    :drive       1.15}

   :crash-16
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1900.0
    :resonance 0.20
    :decay     2.00
    :bandpass  false
    :drive     1.05}

   :crash-17
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1650.0
    :resonance 0.20
    :decay     2.40
    :bandpass  false
    :drive     1.05}

   :crash-18
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1400.0
    :resonance 0.20
    :decay     2.80
    :bandpass  false
    :drive     1.05}

   :splash
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    3200.0
    :resonance 0.25
    :decay     0.38
    :bandpass  false
    :drive     0.95}

   :china
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1800.0
    :resonance 0.45
    :decay     1.60
    :bandpass  false
    :drive     1.35}

   :cowbell
   {:category  :drums
    :type      :metallic
    :bus       :bus/drums
    :mod       :analog
    :cutoff    820.0
    :resonance 0.85
    :decay     0.38
    :bandpass  true
    :drive     1.40}

   :clap
   {:category  :drums
    :type      :clap
    :bus       :bus/drums
    :mod       :analog
    :cutoff    1200.0
    :resonance 0.70
    :decay     0.28
    :drive     1.00}})

(def core-drum-voices
  {:kick      {:node :kick        :default-note "D1" :dur "16n" :pulse 1.5}
   :snare     {:layers [{:node :snare-body :default-note "G3" :dur "16n"}
                        {:node :snare-wire :dur "16n"}]
               :pulse 1.3}
   :sn-rs     {:layers [{:node :snare-body :default-note "B3" :dur "16n" :vel-scale 1.1}
                        {:node :snare-wire :dur "16n" :vel-scale 1.15}
                        {:node :snare-rim  :default-note "E5" :dur "32n" :vel-scale 0.9}]
               :pulse 1.6}
   :sn-clk    {:node :snare-rim   :default-note "D5" :dur "32n" :vel-scale 0.8 :pulse 0.6}
   :sn-gh     {:node :snare-ghost :dur "32n" :vel-scale 0.45 :pulse 0.3}
   :sn-roll   {:layers [{:node :snare-body :default-note "A3" :dur "32n" :vel-scale 0.85}
                        {:node :snare-wire :dur "32n" :vel-scale 0.72}]
               :pulse 0.9}
   :sn-crack  {:node :sn-crack    :default-note "G3" :dur "16n" :pulse 1.5}
   :hh-c      {:node :hat-closed  :dur "32n" :vel-scale 0.6 :pulse 0.5}
   :hh-o      {:node :hat-open    :dur "16n" :vel-scale 0.75 :pulse 0.7}
   :hh-clk    {:node :hat-closed  :dur "64n" :vel-scale 0.45 :pulse 0.35}
   :ride      {:node :ride        :dur "16n" :vel-scale 0.7 :pulse 0.6}
   :ride-bell {:node :ride-bell   :dur "16n" :vel-scale 0.8 :pulse 0.7}
   :tom       {:node :tom         :default-note "A2" :dur "16n" :pulse 1.0}
   :tom-high  {:node :tom-high    :default-note "D3" :dur "16n" :pulse 1.0}
   :tom-mid   {:node :tom-mid     :default-note "A2" :dur "16n" :pulse 1.1}
   :tom-low   {:node :tom-low     :default-note "D2" :dur "16n" :pulse 1.2}
   :crash-16  {:node :crash-16    :dur "8n"  :vel-scale 0.9 :pulse 1.8}
   :crash-17  {:node :crash-17    :dur "8n"  :vel-scale 0.9 :pulse 1.9}
   :crash-18  {:node :crash-18    :dur "4n"  :vel-scale 0.95 :pulse 2.0}
   :splash    {:node :splash      :dur "16n" :vel-scale 0.85 :pulse 1.3}
   :china     {:node :china       :dur "8n"  :vel-scale 0.95 :pulse 1.7}
   :cowbell   {:node :cowbell     :dur "16n" :vel-scale 0.85 :pulse 0.9}
   :click     {:node :click       :default-note "C6" :dur "32n" :pulse 0.4}})

(def drum-keywords
  "Unified set of all drum instrument keywords and aliases."
  (into #{:drums :clap :handclap :crack :rimshot :hh :hihat :closed-hh :open-hh :bd :bassdrum
          :rb :th :tm :tl :cr :crash :cr16 :cr17 :cr18 :sp :ch :cb
          :hats :perc :percussion :break}
        (concat (keys core-drum-voices)
                (keys core-drum-instruments))))

(defn drum-keyword?
  "Checks if a keyword represents a drum instrument or drum hit.
  Examples: (drum-keyword? :kick) -> true, (drum-keyword? :bass-analog) -> false."
  [k]
  (contains? drum-keywords (keyword k)))

(def mini-notation-aliases
  "Lookup map for mini-notation drum tokens and aliases to canonical keywords."
  {"k"         :kick
   "bd"        :kick
   "s"         :snare
   "rs"        :sn-rs
   "sn-rs"     :sn-rs
   "c"         :sn-clk
   "clk"       :sn-clk
   "sn-clk"    :sn-clk
   "g"         :sn-gh
   "gh"        :sn-gh
   "sn-gh"     :sn-gh
   "roll"      :sn-roll
   "sn-roll"   :sn-roll
   "h"         :hh-c
   "hh"        :hh-c
   "hh-c"      :hh-c
   "o"         :hh-o
   "oh"        :hh-o
   "hh-o"      :hh-o
   "hc"        :hh-clk
   "hh-clk"    :hh-clk
   "crack"     :sn-crack
   "sn-crack"  :sn-crack
   "rim"       :snare-rim
   "wire"      :snare-wire
   "body"      :snare-body
   "ghost"     :snare-ghost
   "ride"      :ride
   "rb"        :ride-bell
   "ride-bell" :ride-bell
   "bell"      :ride-bell
   "tom"       :tom
   "th"        :tom-high
   "tom-high"  :tom-high
   "tm"        :tom-mid
   "tom-mid"   :tom-mid
   "tl"        :tom-low
   "tom-low"   :tom-low
   "cr"        :crash-16
   "crash"     :crash-16
   "cr16"      :crash-16
   "crash-16"  :crash-16
   "cr17"      :crash-17
   "crash-17"  :crash-17
   "cr18"      :crash-18
   "crash-18"  :crash-18
   "sp"        :splash
   "splash"    :splash
   "ch"        :china
   "china"     :china
   "cb"        :cowbell
   "cowbell"   :cowbell
   "clap"      :clap
   "cp"        :clap})
