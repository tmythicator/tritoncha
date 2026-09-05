(ns app.lib.drums
  "Core built-in drum kit synthesizer instruments and composite drum voice catalog.")

(def core-drum-instruments
  {:kick
   {:category :drums
    :type     :membrane
    :bus      :bus/drums
    :options  {:pitchDecay 0.035 :octaves 7 :oscillator {:type "sine"}
               :envelope {:attack 0.001 :decay 0.22 :sustain 0 :release 0.08}}}

   :snare-body
   {:category :drums
    :type     :synth
    :bus      :bus/drums
    :options  {:oscillator {:type "triangle"}
               :envelope {:attack 0.001 :decay 0.12 :sustain 0 :release 0.05}}}

   :snare-wire
   {:category :drums
    :type     :noise
    :bus      :bus/drums
    :options  {:noise {:type "white"}
               :envelope {:attack 0.001 :decay 0.14 :sustain 0 :release 0.06}}}

   :snare-rim
   {:category :drums
    :type     :synth
    :bus      :bus/drums
    :options  {:oscillator {:type "sine"}
               :envelope {:attack 0.001 :decay 0.03 :sustain 0 :release 0.01}}}

   :snare-ghost
   {:category :drums
    :type     :noise
    :bus      :bus/drums
    :options  {:noise {:type "pink"}
               :envelope {:attack 0.001 :decay 0.08 :sustain 0 :release 0.03}}}

   :hat-closed
   {:category :drums
    :type     :noise
    :bus      :bus/drums
    :options  {:noise {:type "white"}
               :envelope {:attack 0.001 :decay 0.035 :sustain 0 :release 0.01}}}

   :hat-open
   {:category :drums
    :type     :noise
    :bus      :bus/drums
    :options  {:noise {:type "white"}
               :envelope {:attack 0.001 :decay 0.18 :sustain 0 :release 0.05}}}

   :ride
   {:category :drums
    :type     :synth
    :bus      :bus/drums
    :options  {:oscillator {:type "square"}
               :envelope {:attack 0.001 :decay 0.6 :sustain 0 :release 0.2}}}

   :tom
   {:category :drums
    :type     :synth
    :bus      :bus/drums
    :options  {:oscillator {:type "sine"}
               :envelope {:attack 0.001 :decay 0.25 :sustain 0 :release 0.1}}}

   :sn-crack
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :ride-bell
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :tom-high
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :tom-mid
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :tom-low
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :crash-16
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :crash-17
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :crash-18
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :splash
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :china
   {:category :drums
    :type     :synth
    :bus      :bus/drums}

   :cowbell
   {:category :drums
    :type     :synth
    :bus      :bus/drums}})

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
          :rb :th :tm :tl :cr :crash :cr16 :cr17 :cr18 :sp :ch :cb}
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
