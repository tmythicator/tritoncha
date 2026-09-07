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
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:osc :sub-level]
                          :label "Sub-Osc Level:" :min 0.0 :max 1.0 :step 0.05 :default 0.0 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:osc :pulse-width]
                          :label "Pulse Width:" :min 0.05 :max 0.95 :step 0.02 :default 0.5 :decimals 2}]]

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

    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :glide
                          :label "Portamento Glide:" :min 0.0 :max 0.5 :step 0.005 :default 0.0 :unit "s" :decimals 3}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:osc :noise]
                          :label "Voice White Noise:" :min 0.0 :max 1.0 :step 0.02 :default 0.0 :pct? true}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:osc :drift]
                          :label "VCO Analog Drift:" :min 0.0 :max 1.0 :step 0.05 :default 0.0 :pct? true}]]])

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
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:filter :cutoff]
                          :label "Cutoff Frequency:" :min 40 :max 14000 :step 50 :default 2400 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:filter :q]
                          :label "Resonance (Q):" :min 0.0 :max 0.95 :step 0.02 :default 0.2 :decimals 2}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:filter :env-amount]
                          :label "Filter Env Amount:" :min -8000 :max 8000 :step 100 :default 0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:filter :key-track]
                          :label "Keyboard Tracking:" :min 0.0 :max 3.0 :step 0.1 :default 1.0 :unit "x" :decimals 1}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:filter :drive]
                          :label "Filter Drive (Saturation):" :min 0.0 :max 1.0 :step 0.05 :default 0.0 :pct? true}]
    [:div]]])

(defn pitch-snap-section
  "Render physical pitch transient attack envelope controls.
  Examples: [pitch-snap-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "PITCH TRANSIENT SNAP (:pitch-env)"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:pitch-env :amount]
                          :label "Snap Amount:" :min 0 :max 48 :step 1 :default 0 :unit "st" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:pitch-env :decay]
                          :label "Snap Decay:" :min 0.005 :max 0.100 :step 0.005 :default 0.015 :unit "s" :decimals 3}]]])

(defn amp-section
  "Render 4-column amplifier ADSR envelope controls.
  Examples: [amp-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "AMPLIFIER ENVELOPE (:amp-env)"]
   [:div.inst-grid-4col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:amp-env :attack]
                          :label "Attack:" :min 0.001 :max 1.5 :step 0.005 :default 0.01 :unit "s" :decimals 3}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:amp-env :decay]
                          :label "Decay:" :min 0.01 :max 3.0 :step 0.02 :default 0.25 :unit "s" :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:amp-env :sustain]
                          :label "Sustain:" :min 0.0 :max 1.0 :step 0.02 :default 0.5 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:amp-env :release]
                          :label "Release:" :min 0.01 :max 4.0 :step 0.02 :default 0.4 :unit "s" :decimals 2}]]])

(defn mod-section
  "Render filter modulation envelope controls.
  Examples: [mod-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  (let [default-atk (or (get-in cur-spec [:amp-env :attack]) 0.01)
        default-dec (or (get-in cur-spec [:amp-env :decay]) 0.25)]
    [:div.inst-box
     [:div.inst-section-label "FILTER MOD ENVELOPE (:mod-env)"]
     [:div.inst-grid-2col
      [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:mod-env :attack]
                            :label "Mod Attack:" :min 0.001 :max 1.5 :step 0.005 :default default-atk :unit "s" :decimals 3}]
      [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key [:mod-env :decay]
                            :label "Mod Decay:" :min 0.01 :max 3.0 :step 0.02 :default default-dec :unit "s" :decimals 2}]]]))

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
