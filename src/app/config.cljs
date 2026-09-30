(ns app.config
  "System configuration, audio/visual buffer parameters and runtime constants.")

(def app-version "1.7.0")

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

(def default-bus-levels
  {:bus/drums   0.0
   :bus/bass    0.0
   :bus/space   0.0
   :bus/lead    0.0
   :bus/direct  0.0
   :bus/master  0.0})

(def default-bus-sends
  {:bus/drums  {:delay 0.02 :reverb 0.06}
   :bus/bass   {:delay 0.0  :reverb 0.0}
   :bus/space  {:delay 0.20 :reverb 0.35}
   :bus/lead   {:delay 0.15 :reverb 0.10}
   :bus/direct {:delay 0.0  :reverb 0.0}})

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
(def default-kick-pulse 0.7)
(def default-trigger-velocity 0.85)

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

;; AudioWorklet and Rust WASM DSP configuration
(def wasm-processor-name "tritoncha-dsp-processor")
(def worklet-script-path "worklets/tritoncha_dsp.js")
(def wasm-binary-path    "wasm/tritoncha_dsp.wasm")
