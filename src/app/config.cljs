(ns app.config
  "System configuration, audio/visual buffer parameters and runtime constants.")

(def default-bpm 168)
(def default-step "16n")
(def default-key {:root :e :mode :phrygian :octave 1})
(def max-looper-steps 64)

(def lookahead-desktop 0.25)
(def lookahead-mobile  0.55)
(def lookahead-bg      0.65)

(def max-dpr-desktop 2.0)
(def max-dpr-mobile  1.5)
(def default-camera-speed 0.005)
(def default-sensitivity 1.6)

(def jam-presets
  [:roller :sub-roller :acid-roller :ambient-drift])
