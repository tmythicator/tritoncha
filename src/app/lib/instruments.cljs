(ns app.lib.instruments
  "Core built-in instrument library and drum voice catalog for Tritoncha.")

;; Core Synthesizers & Sound Presets

(def core-instruments
  {:saw-bass
   {:type :mono
    :bus :bass
    :options {:oscillator {:type "fatsawtooth" :count 3 :spread 20}
              :filter {:Q 3 :type "lowpass" :rolloff -24}
              :filterEnvelope {:attack 0.02 :decay 0.3 :sustain 0.7 :release 0.2 :baseFrequency 120 :octaves 3}
              :envelope {:attack 0.01 :decay 0.2 :sustain 0.85 :release 0.25}
              :portamento 0.05}}

   :acid-bass
   {:type :mono
    :bus :bass
    :options {:oscillator {:type "sawtooth"}
              :filter {:Q 8 :type "lowpass" :rolloff -24}
              :filterEnvelope {:attack 0.01 :decay 0.18 :sustain 0.2 :release 0.1 :baseFrequency 180 :octaves 4}
              :envelope {:attack 0.005 :decay 0.15 :sustain 0.3 :release 0.1}
              :portamento 0.04}}

   :sub-sine
   {:type :synth
    :bus :bass
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.01 :decay 0.2 :sustain 0.9 :release 0.2}
              :portamento 0.05}}

   :fm-growl
   {:type :fm
    :bus :bass
    :options {:harmonicity 2.0
              :modulationIndex 12
              :oscillator {:type "sine"}
              :envelope {:attack 0.01 :decay 0.25 :sustain 0.4 :release 0.2}
              :modulation {:type "triangle"}
              :modulationEnvelope {:attack 0.02 :decay 0.3 :sustain 0.6 :release 0.2}}}

   :dark-pad
   {:type :poly
    :bus :space
    :maxPolyphony 6
    :options {:oscillator {:type "sawtooth"}
              :envelope {:attack 0.15 :decay 0.6 :sustain 0.4 :release 1.2}}}

   :ambient-glass
   {:type :poly
    :bus :space
    :maxPolyphony 6
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.4 :decay 1.2 :sustain 0.7 :release 2.0}}}

   :pluck-lead
   {:type :poly
    :bus :space
    :maxPolyphony 6
    :options {:oscillator {:type "square"}
              :envelope {:attack 0.002 :decay 0.14 :sustain 0.05 :release 0.12}}}

   :kick
   {:type :membrane
    :bus :drums
    :options {:pitchDecay 0.035 :octaves 7 :oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.22 :sustain 0 :release 0.08}}}

   :snare-body
   {:type :synth
    :bus :drums
    :options {:oscillator {:type "triangle"}
              :envelope {:attack 0.001 :decay 0.12 :sustain 0 :release 0.05}}}

   :snare-wire
   {:type :noise
    :bus :drums
    :options {:noise {:type "white"}
              :envelope {:attack 0.001 :decay 0.14 :sustain 0 :release 0.06}}}

   :snare-rim
   {:type :synth
    :bus :drums
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.03 :sustain 0 :release 0.01}}}

   :snare-ghost
   {:type :noise
    :bus :drums
    :options {:noise {:type "pink"}
              :envelope {:attack 0.001 :decay 0.08 :sustain 0 :release 0.03}}}

   :hat-closed
   {:type :noise
    :bus :drums
    :options {:noise {:type "white"}
              :envelope {:attack 0.001 :decay 0.035 :sustain 0 :release 0.01}}}

   :hat-open
   {:type :noise
    :bus :drums
    :options {:noise {:type "white"}
              :envelope {:attack 0.001 :decay 0.18 :sustain 0 :release 0.05}}}

   :siren
   {:type :synth
    :bus :space
    :options {:oscillator {:type "sawtooth"}
              :envelope {:attack 0.05 :decay 0.4 :sustain 0.3 :release 0.8}}}

   :click
   {:type :synth
    :bus :direct
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.04 :sustain 0 :release 0.02}}}})

;; Drum Kit Voice Specifications

(def drum-voices
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
