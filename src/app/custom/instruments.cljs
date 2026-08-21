(ns app.custom.instruments
  "User custom synthesizers, drum models, and sound design presets.")

;; Custom Instruments Catalog (Same format as app.lib.instruments)
;;
;; Synthesizer types:
;;   :mono     - Tone.MonoSynth (fat basslines, acid leads, resonant synths)
;;   :poly     - Tone.PolySynth (lush chords, pads, ambient textures)
;;   :fm       - Tone.FMSynth   (metallic bells, morphing growls)
;;   :synth    - Tone.Synth     (sub-bass sine, pitch glides)
;;   :membrane - Tone.MembraneSynth (punchy kicks, 808 subs)
;;   :noise    - Tone.NoiseSynth (white/pink noise snares, hats)
;;
;; Mixer Busses: :bass, :space, :drums, :direct

(def user-instruments
  {:supersaw
   {:type :mono
    :bus  :space
    :options {:oscillator {:type "fatsawtooth" :count 5 :spread 30}
              :filter {:Q 4 :type "lowpass" :rolloff -24}
              :envelope {:attack 0.01 :decay 0.2 :sustain 0.7 :release 0.25}
              :portamento 0.03}}

   :acid-lead
   {:type :mono
    :bus  :bass
    :options {:oscillator {:type "sawtooth"}
              :filter {:Q 12 :type "lowpass" :rolloff -24}
              :filterEnvelope {:attack 0.005 :decay 0.15 :sustain 0.15 :release 0.08 :baseFrequency 120 :octaves 4.5}
              :envelope {:attack 0.005 :decay 0.12 :sustain 0.25 :release 0.1}
              :portamento 0.04}}

   :fm-bell
   {:type :fm
    :bus  :space
    :options {:harmonicity 3.5
              :modulationIndex 18
              :oscillator {:type "sine"}
              :envelope {:attack 0.001 :decay 0.8 :sustain 0.1 :release 0.8}
              :modulation {:type "triangle"}
              :modulationEnvelope {:attack 0.001 :decay 0.4 :sustain 0.2 :release 0.4}}}})