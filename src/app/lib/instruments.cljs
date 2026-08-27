(ns app.lib.instruments
  "Core built-in instrument library and drum voice catalog for Tritoncha.")

(def core-instruments
  {:saw-bass
   {:type :mono
    :bus :bus/bass
    :options {:oscillator {:type "fatsawtooth" :count 3 :spread 20}
              :filter {:Q 3 :type "lowpass" :rolloff -24}
              :filterEnvelope {:attack 0.02 :decay 0.3 :sustain 0.7 :release 0.2 :baseFrequency 120 :octaves 3}
              :envelope {:attack 0.01 :decay 0.2 :sustain 0.85 :release 0.25}
              :portamento 0.05}}

   :acid-bass
   {:type :mono
    :bus :bus/bass
    :options {:oscillator {:type "sawtooth"}
              :filter {:Q 8 :type "lowpass" :rolloff -24}
              :filterEnvelope {:attack 0.01 :decay 0.18 :sustain 0.2 :release 0.1 :baseFrequency 180 :octaves 4}
              :envelope {:attack 0.005 :decay 0.15 :sustain 0.3 :release 0.1}
              :portamento 0.04}}

   :sub-sine
   {:type :mono
    :bus :bus/bass
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.01 :decay 0.2 :sustain 0.9 :release 0.2}
              :portamento 0.04}}

   :fm-growl
   {:type :fm
    :bus :bus/bass
    :options {:harmonicity 2.0
              :modulationIndex 12
              :oscillator {:type "sine"}
              :envelope {:attack 0.01 :decay 0.25 :sustain 0.4 :release 0.2}
              :modulation {:type "triangle"}
              :modulationEnvelope {:attack 0.02 :decay 0.3 :sustain 0.6 :release 0.2}}}

   :dark-pad
   {:type :poly
    :bus :bus/space
    :maxPolyphony 8
    :options {:volume -4
              :oscillator {:type "sawtooth"}
              :envelope {:attack 0.2 :decay 0.5 :sustain 0.4 :release 0.8}}}

   :ambient-glass
   {:type :poly
    :bus :bus/space
    :maxPolyphony 8
    :options {:oscillator {:type "sine"}
              :envelope {:attack 0.02 :decay 0.25 :sustain 0.15 :release 0.35}}}

   :pluck-lead
   {:type :poly
    :bus :bus/space
    :maxPolyphony 16
    :options {:oscillator {:type "square"}
              :envelope {:attack 0.002 :decay 0.14 :sustain 0.05 :release 0.12}}}

   :siren
   {:type :synth
    :bus :bus/space
    :options {:oscillator {:type "sawtooth"}
              :envelope {:attack 0.05 :decay 0.4 :sustain 0.3 :release 0.8}}}})
