(ns app.config
  "System configuration, audio/visual buffer parameters and runtime constants.")

(def default-bpm 168)
(def min-bpm 40)
(def max-bpm 300)
(def default-step "16n")
(def default-velocity 0.9)
(def default-ramp-time 0.05)
(def mute-db -96.0)
(def default-key {:root :e :mode :phrygian :octave 1})
(def default-bass-octave 1)
(def default-lead-octave 2)
(def default-max-polyphony 32)
(def sequence-length 384)
(def max-looper-steps 64)

(def default-filter-frequency 8000)
(def default-filter-type "lowpass")
(def default-filter-rolloff -24)
(def default-delay-time "8n.")
(def default-delay-feedback 0.35)
(def default-reverb-room-size 0.75)
(def default-reverb-dampening 3000)
(def default-distortion 0.25)
(def default-limiter-threshold -0.5)
(def default-bitcrusher-bits 8)

(def lookahead-desktop 0.25)
(def lookahead-mobile  0.55)
(def lookahead-bg      0.65)

(def max-dpr-desktop 2.0)
(def max-dpr-mobile  1.5)
(def default-camera-speed 0.005)
(def default-sensitivity 1.6)
(def default-camera-distance 7.0)
(def default-pulse-decay 0.06)
(def default-scale-lerp 0.18)
(def default-pulse-scale-factor 0.4)

(def default-scene :cyber-torus)
(def default-geometry :torus-knot)
(def default-scene-colors
  {:bg    "#050510"
   :mesh  "#00ffcc"
   :wire  "#ff007f"
   :outer "#331144"})

(def default-ambient-light-intensity 0.6)
(def default-directional-light-intensity 1.2)

(def jam-presets
  [:roller :sub-roller :acid-roller :ambient-drift])
