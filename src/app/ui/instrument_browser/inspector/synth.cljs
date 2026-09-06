(ns app.ui.instrument-browser.inspector.synth
  "Modular synthesizer sound design studio inspector sections and live parameter tuning."
  (:require
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.inspector.shared :as shared]
   [app.ui.instrument-browser.state :as state]))

(defn osc-section
  "Render oscillator waveform, sub-level, pulse width, and voicing controls.
  Examples: [osc-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "OSCILLATOR + VOICING (:osc)"]
   [comps/pill-selector
    {:label    "WAVEFORM ALGORITHM:"
     :items    [:saw :supersaw :pulse :tri :sine :karplus :organ :chiptune :fm :reese :blade :hoover :click]
     :current  (or (get-in cur-spec [:osc :type]) :saw)
     :on-select #(state/patch-param! cur-sel-key :osc :type % cur-spec)}]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Sub-Osc Level:"
      :val-str   (.toFixed (or (get-in cur-spec [:osc :sub-level]) 0.0) 2)
      :min       "0.0" :max "1.0" :step "0.05"
      :value     (or (get-in cur-spec [:osc :sub-level]) 0.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :osc :sub-level v cur-spec)))}]

    [comps/param-slider
     {:label     "Pulse Width:"
      :val-str   (.toFixed (or (get-in cur-spec [:osc :pulse-width]) 0.5) 2)
      :min       "0.05" :max "0.95" :step "0.02"
      :value     (or (get-in cur-spec [:osc :pulse-width]) 0.5)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :osc :pulse-width v cur-spec)))}]]

   [:div.inst-grid-2col-bottom
    [comps/pill-selector
     {:label    "VOICING MODE:"
      :items    [:mono :poly]
      :current  (if (= (:type cur-spec) :poly) :poly :mono)
      :on-select (fn [mode]
                   (if (= mode :poly)
                     (do
                       (state/patch-param! cur-sel-key :type :poly cur-spec)
                       (state/patch-param! cur-sel-key :polyphony 16 cur-spec)
                       (state/patch-param! cur-sel-key :maxPolyphony 16 cur-spec))
                     (do
                       (state/patch-param! cur-sel-key :type :mono cur-spec)
                       (state/patch-param! cur-sel-key :polyphony 1 cur-spec)
                       (state/patch-param! cur-sel-key :maxPolyphony 1 cur-spec))))}]

    [comps/param-slider
     {:label     "Portamento Glide:"
      :val-str   (str (.toFixed (or (:glide cur-spec) 0.0) 3) " s")
      :min       "0.0" :max "0.5" :step "0.005"
      :value     (or (:glide cur-spec) 0.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :glide v cur-spec)))}]]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Voice White Noise:"
      :val-str   (str (Math/round (* (or (get-in cur-spec [:osc :noise]) 0.0) 100)) "%")
      :min       "0.0" :max "1.0" :step "0.02"
      :value     (or (get-in cur-spec [:osc :noise]) 0.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :osc :noise v cur-spec)))}]

    [comps/param-slider
     {:label     "VCO Analog Drift:"
      :val-str   (str (Math/round (* (or (get-in cur-spec [:osc :drift]) 0.0) 100)) "%")
      :min       "0.0" :max "1.0" :step "0.05"
      :value     (or (get-in cur-spec [:osc :drift]) 0.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :osc :drift v cur-spec)))}]]])

(defn filter-section
  "Render filter topology, cutoff, resonance, env amount, and key tracking controls.
  Examples: [filter-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "STATE-VARIABLE FILTER (:filter)"]
   [comps/pill-selector
    {:label    "FILTER TOPOLOGY:"
     :items    [:lowpass :highpass :bandpass :notch]
     :current  (or (get-in cur-spec [:filter :type]) :lowpass)
     :on-select #(state/patch-param! cur-sel-key :filter :type % cur-spec)}]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Cutoff Frequency:"
      :val-str   (str (Math/round (or (get-in cur-spec [:filter :cutoff]) 2400)) " Hz")
      :min       "40" :max "14000" :step "50"
      :value     (or (get-in cur-spec [:filter :cutoff]) 2400)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :filter :cutoff v cur-spec)))}]

    [comps/param-slider
     {:label     "Resonance (Q):"
      :val-str   (.toFixed (or (get-in cur-spec [:filter :q]) 0.2) 2)
      :min       "0.0" :max "0.95" :step "0.02"
      :value     (or (get-in cur-spec [:filter :q]) 0.2)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :filter :q v cur-spec)))}]]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Filter Env Amount:"
      :val-str   (str (Math/round (or (get-in cur-spec [:filter :env-amount]) 0)) " Hz")
      :min       "-8000" :max "8000" :step "100"
      :value     (or (get-in cur-spec [:filter :env-amount]) 0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :filter :env-amount v cur-spec)))}]

    [comps/param-slider
     {:label     "Keyboard Tracking:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:filter :key-track]) 1.0) 1) "x")
      :min       "0.0" :max "3.0" :step "0.1"
      :value     (or (get-in cur-spec [:filter :key-track]) 1.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :filter :key-track v cur-spec)))}]]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Filter Drive (Saturation):"
      :val-str   (str (Math/round (* (or (get-in cur-spec [:filter :drive]) 0.0) 100)) "%")
      :min       "0.0" :max "1.0" :step "0.05"
      :value     (or (get-in cur-spec [:filter :drive]) 0.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :filter :drive v cur-spec)))}]
    [:div]]])

(defn pitch-snap-section
  "Render physical pitch transient attack envelope controls.
  Examples: [pitch-snap-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "PITCH TRANSIENT SNAP (:pitch-env)"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Snap Amount:"
      :val-str   (str (Math/round (or (get-in cur-spec [:pitch-env :amount]) 0)) " st")
      :min       "0" :max "48" :step "1"
      :value     (or (get-in cur-spec [:pitch-env :amount]) 0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :pitch-env :amount v cur-spec)))}]

    [comps/param-slider
     {:label     "Snap Decay:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:pitch-env :decay]) 0.015) 3) " s")
      :min       "0.005" :max "0.100" :step "0.005"
      :value     (or (get-in cur-spec [:pitch-env :decay]) 0.015)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :pitch-env :decay v cur-spec)))}]]])

(defn amp-section
  "Render 4-column amplifier ADSR envelope controls.
  Examples: [amp-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "AMPLIFIER ENVELOPE (:amp-env)"]
   [:div.inst-grid-4col
    [comps/param-slider
     {:label     "Attack:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:amp-env :attack]) 0.01) 3) "s")
      :min       "0.001" :max "1.5" :step "0.005"
      :value     (or (get-in cur-spec [:amp-env :attack]) 0.01)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :amp-env :attack v cur-spec)))}]

    [comps/param-slider
     {:label     "Decay:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:amp-env :decay]) 0.25) 2) "s")
      :min       "0.01" :max "3.0" :step "0.02"
      :value     (or (get-in cur-spec [:amp-env :decay]) 0.25)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :amp-env :decay v cur-spec)))}]

    [comps/param-slider
     {:label     "Sustain:"
      :val-str   (.toFixed (or (get-in cur-spec [:amp-env :sustain]) 0.5) 2)
      :min       "0.0" :max "1.0" :step "0.02"
      :value     (or (get-in cur-spec [:amp-env :sustain]) 0.5)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :amp-env :sustain v cur-spec)))}]

    [comps/param-slider
     {:label     "Release:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:amp-env :release]) 0.4) 2) "s")
      :min       "0.01" :max "4.0" :step "0.02"
      :value     (or (get-in cur-spec [:amp-env :release]) 0.4)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :amp-env :release v cur-spec)))}]]])

(defn mod-section
  "Render filter modulation envelope controls.
  Examples: [mod-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "FILTER MOD ENVELOPE (:mod-env)"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Mod Attack:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:mod-env :attack])
                                    (get-in cur-spec [:amp-env :attack]) 0.01) 3) "s")
      :min       "0.001" :max "1.5" :step "0.005"
      :value     (or (get-in cur-spec [:mod-env :attack])
                     (get-in cur-spec [:amp-env :attack]) 0.01)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :mod-env :attack v cur-spec)))}]

    [comps/param-slider
     {:label     "Mod Decay:"
      :val-str   (str (.toFixed (or (get-in cur-spec [:mod-env :decay])
                                    (get-in cur-spec [:amp-env :decay]) 0.25) 2) "s")
      :min       "0.01" :max "3.0" :step "0.02"
      :value     (or (get-in cur-spec [:mod-env :decay])
                     (get-in cur-spec [:amp-env :decay]) 0.25)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :mod-env :decay v cur-spec)))}]]])

(defn synth-inspector
  "Render complete modular sound design suite for synth voices.
  Examples: [synth-inspector :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-section
   [:div.inst-spec-header
    [:div.inst-section-label "LIVE PARAMETER FINE-TUNING"]
    [:button.neo-btn-stats.inst-reset-btn
     {:on-click #(state/reset-param! cur-sel-key)
      :title    "Reset parameters to original catalog definition"}
     "↺ RESET"]]

   [osc-section cur-sel-key cur-spec]
   [filter-section cur-sel-key cur-spec]
   [pitch-snap-section cur-sel-key cur-spec]
   [amp-section cur-sel-key cur-spec]
   [mod-section cur-sel-key cur-spec]
   [shared/bus-section cur-sel-key cur-spec]
   [shared/code-spec-section cur-sel-key cur-spec]])
