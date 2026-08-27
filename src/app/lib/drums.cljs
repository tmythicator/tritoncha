(ns app.lib.drums
  "Core built-in drum kit synthesizer instruments and composite drum voice catalog.")

(def core-drum-instruments
  {:kick
   {:type :membrane
    :bus :bus/drums
    :options {:pitchDecay 0.035 :octaves 7 :oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.22 :sustain 0 :release 0.08}}}

   :snare-body
   {:type :synth
    :bus :bus/drums
    :options {:oscillator {:type "triangle"}
              :envelope {:attack 0.001 :decay 0.12 :sustain 0 :release 0.05}}}

   :snare-wire
   {:type :noise
    :bus :bus/drums
    :options {:noise {:type "white"}
              :envelope {:attack 0.001 :decay 0.14 :sustain 0 :release 0.06}}}

   :snare-rim
   {:type :synth
    :bus :bus/drums
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.03 :sustain 0 :release 0.01}}}

   :snare-ghost
   {:type :noise
    :bus :bus/drums
    :options {:noise {:type "pink"}
              :envelope {:attack 0.001 :decay 0.08 :sustain 0 :release 0.03}}}

   :hat-closed
   {:type :noise
    :bus :bus/drums
    :options {:noise {:type "white"}
              :envelope {:attack 0.001 :decay 0.035 :sustain 0 :release 0.01}}}

   :hat-open
   {:type :noise
    :bus :bus/drums
    :options {:noise {:type "white"}
              :envelope {:attack 0.001 :decay 0.18 :sustain 0 :release 0.05}}}

   :click
   {:type :synth
    :bus :bus/direct
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.04 :sustain 0 :release 0.02}}}})

(def core-drum-voices
  {:kick    {:node :kick        :default-note "D1" :dur "16n" :pulse 1.5}
   :snare   {:layers [{:node :snare-body :default-note "G3" :dur "16n"}
                      {:node :snare-wire :dur "16n"}]
             :pulse 1.3}
   :sn-rs   {:layers [{:node :snare-body :default-note "B3" :dur "16n" :vel-scale 1.1}
                      {:node :snare-wire :dur "16n" :vel-scale 1.15}
                      {:node :snare-rim  :default-note "E5" :dur "32n" :vel-scale 0.9}]
             :pulse 1.6}
   :sn-clk  {:node :snare-rim   :default-note "D5" :dur "32n" :vel-scale 0.8 :pulse 0.6}
   :sn-gh   {:node :snare-ghost :dur "32n" :vel-scale 0.45 :pulse 0.3}
   :sn-roll {:layers [{:node :snare-body :default-note "A3" :dur "32n" :vel-scale 0.85}
                      {:node :snare-wire :dur "32n" :vel-scale 0.72}]
             :pulse 0.9}
   :hh-c    {:node :hat-closed  :dur "32n" :vel-scale 0.6 :pulse 0.5}
   :hh-o    {:node :hat-open    :dur "16n" :vel-scale 0.75 :pulse 0.7}
   :hh-clk  {:node :hat-closed  :dur "64n" :vel-scale 0.45 :pulse 0.35}
   :click   {:node :click       :default-note "C6" :dur "32n" :pulse 0.4}})
