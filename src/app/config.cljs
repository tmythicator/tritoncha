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
(def stats-refresh-interval-ms 500)

(def lookahead-desktop 0.10)
(def lookahead-mobile  0.10)
(def lookahead-bg      0.25)

(def max-dpr-desktop 2.0)
(def max-dpr-mobile  1.5)
(def default-camera-speed 0.005)
(def default-sensitivity 1.6)
(def default-camera-distance 7.0)
(def default-pulse-decay 0.06)
(def default-scale-lerp 0.18)
(def default-pulse-scale-factor 0.4)

(def default-figure-scale-factor 0.12)
(def default-figure-lerp 0.10)
(def default-figure-decay 0.035)

(def default-scene :cyber-torus)
(def default-geometry :torus-knot)
(def default-scene-colors
  {:bg    "#050510"
   :mesh  "#00ffcc"
   :wire  "#ff007f"
   :outer "#331144"})

(def default-ambient-light-intensity 0.6)
(def default-directional-light-intensity 1.2)

(def default-jam :metro-roller)

;; AudioWorklet and Rust WASM DSP configuration
(def wasm-processor-name "tritoncha-dsp-processor")
(def worklet-script-path "worklets/tritoncha_dsp.js")
(def wasm-binary-path    "wasm/tritoncha_dsp.wasm")
