(ns app.ui.instrument-browser.inspector.drum
  "Drum synthesis parameter studio, character mode selector, and mini-notation generator."
  (:require
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.inspector.shared :as shared]
   [app.ui.instrument-browser.state :as state]))

(defn- kick-params-section
  "Render kick drum physical model tuning parameters.
  Examples: [kick-params-section :kick cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "KICK DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Base Sub Pitch (Hz):"
      :val-str   (str (.toFixed (or (:base-pitch cur-spec) 48.0) 1) " Hz")
      :min       "32.0" :max "75.0" :step "0.5"
      :value     (or (:base-pitch cur-spec) 48.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :base-pitch v cur-spec)))}]

    [comps/param-slider
     {:label     "Pitch Drop (+Hz):"
      :val-str   (str (.toFixed (or (:pitch-drop cur-spec) 180.0) 0) " Hz")
      :min       "0.0" :max "350.0" :step "5.0"
      :value     (or (:pitch-drop cur-spec) 180.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :pitch-drop v cur-spec)))}]]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Pitch Decay Rate:"
      :val-str   (.toFixed (or (:pitch-decay cur-spec) 0.040) 3)
      :min       "0.010" :max "0.120" :step "0.005"
      :value     (or (:pitch-decay cur-spec) 0.040)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :pitch-decay v cur-spec)))}]

    [comps/param-slider
     {:label     "Sub Decay Time (s):"
      :val-str   (str (.toFixed (or (:decay cur-spec) 0.28) 2) " s")
      :min       "0.06" :max "0.80" :step "0.01"
      :value     (or (:decay cur-spec) 0.28)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :decay v cur-spec)))}]]

   [:div.inst-grid-2col-bottom
    [comps/param-slider
     {:label     "Beater Click Punch:"
      :val-str   (.toFixed (or (:click cur-spec) 0.35) 2)
      :min       "0.0" :max "1.0" :step "0.02"
      :value     (or (:click cur-spec) 0.35)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :click v cur-spec)))}]

    [comps/param-slider
     {:label     "Analog Saturation Drive:"
      :val-str   (.toFixed (or (:drive cur-spec) 1.6) 2)
      :min       "0.0" :max "4.0" :step "0.1"
      :value     (or (:drive cur-spec) 1.6)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :drive v cur-spec)))}]]])

(defn- snare-params-section
  "Render snare drum physical model tuning parameters.
  Examples: [snare-params-section :snare cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "SNARE DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Shell Base Frequency (Hz):"
      :val-str   (str (.toFixed (or (:base-freq cur-spec) 185.0) 1) " Hz")
      :min       "110.0" :max "320.0" :step "1.0"
      :value     (or (:base-freq cur-spec) 185.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :base-freq v cur-spec)))}]

    [comps/param-slider
     {:label     "Shell Tone Decay:"
      :val-str   (.toFixed (or (:tone-decay cur-spec) 0.9985) 4)
      :min       "0.9900" :max "0.9999" :step "0.0001"
      :value     (or (:tone-decay cur-spec) 0.9985)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :tone-decay v cur-spec)))}]]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Wire Noise Decay:"
      :val-str   (.toFixed (or (:noise-decay cur-spec) 0.9991) 4)
      :min       "0.9900" :max "0.9999" :step "0.0001"
      :value     (or (:noise-decay cur-spec) 0.9991)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :noise-decay v cur-spec)))}]

    [comps/param-slider
     {:label     "Wire Bandpass Cutoff (Hz):"
      :val-str   (str (Math/round (or (:cutoff cur-spec) 2400.0)) " Hz")
      :min       "800.0" :max "7000.0" :step "50.0"
      :value     (or (:cutoff cur-spec) 2400.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :cutoff v cur-spec)))}]]

   [:div.inst-grid-2col-bottom
    [comps/param-slider
     {:label     "Snappy Balance:"
      :val-str   (.toFixed (or (:snappy cur-spec) 0.85) 2)
      :min       "0.0" :max "1.0" :step "0.02"
      :value     (or (:snappy cur-spec) 0.85)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :snappy v cur-spec)))}]]])

(defn- hat-params-section
  "Render hi-hat metallic noise and decay parameters.
  Examples: [hat-params-section :hat cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "HI-HAT SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Highpass Cutoff (Hz):"
      :val-str   (str (Math/round (or (:cutoff cur-spec) 7200.0)) " Hz")
      :min       "3000.0" :max "12000.0" :step "100.0"
      :value     (or (:cutoff cur-spec) 7200.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :cutoff v cur-spec)))}]

    [comps/param-slider
     {:label     "Closed Decay (s):"
      :val-str   (str (.toFixed (or (:decay-closed cur-spec) 0.04) 3) " s")
      :min       "0.01" :max "0.15" :step "0.005"
      :value     (or (:decay-closed cur-spec) 0.04)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :decay-closed v cur-spec)))}]]

   [:div.inst-grid-2col-bottom
    [comps/param-slider
     {:label     "Open Decay (s):"
      :val-str   (str (.toFixed (or (:decay-open cur-spec) 0.24) 2) " s")
      :min       "0.05" :max "0.80" :step "0.01"
      :value     (or (:decay-open cur-spec) 0.24)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :decay-open v cur-spec)))}]]])

(defn- membrane-params-section
  "Render membrane drum voice tuning parameters for toms and tuned percussions.
  Examples: [membrane-params-section :tom cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "MEMBRANE DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Start Strike Pitch (Hz):"
      :val-str   (str (.toFixed (or (:start-pitch cur-spec) 180.0) 1) " Hz")
      :min       "50.0" :max "450.0" :step "2.0"
      :value     (or (:start-pitch cur-spec) 180.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :start-pitch v cur-spec)))}]

    [comps/param-slider
     {:label     "Resting Sub Pitch (Hz):"
      :val-str   (str (.toFixed (or (:min-pitch cur-spec) 105.0) 1) " Hz")
      :min       "40.0" :max "300.0" :step "1.0"
      :value     (or (:min-pitch cur-spec) 105.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :min-pitch v cur-spec)))}]]

   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Pitch Glide Decay:"
      :val-str   (.toFixed (or (:pitch-decay cur-spec) 0.015) 3)
      :min       "0.005" :max "0.060" :step "0.002"
      :value     (or (:pitch-decay cur-spec) 0.015)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :pitch-decay v cur-spec)))}]

    [comps/param-slider
     {:label     "Amplitude Decay (s):"
      :val-str   (str (.toFixed (or (:decay cur-spec) 0.40) 2) " s")
      :min       "0.08" :max "1.20" :step "0.02"
      :value     (or (:decay cur-spec) 0.40)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :decay v cur-spec)))}]]

   [:div.inst-grid-2col-bottom
    [comps/param-slider
     {:label     "Membrane Saturation Drive:"
      :val-str   (.toFixed (or (:drive cur-spec) 1.10) 2)
      :min       "0.5" :max "3.0" :step "0.05"
      :value     (or (:drive cur-spec) 1.10)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :drive v cur-spec)))}]]])

(defn- metallic-params-section
  "Render metallic voice tuning parameters for cymbals, rides, and bells.
  Examples: [metallic-params-section :ride-bell cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "METALLIC CYMBAL + PERCUSSION PARAMETERS"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Filter Cutoff (Hz):"
      :val-str   (str (Math/round (or (:cutoff cur-spec) 3500.0)) " Hz")
      :min       "500.0" :max "10000.0" :step "100.0"
      :value     (or (:cutoff cur-spec) 3500.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :cutoff v cur-spec)))}]

    [comps/param-slider
     {:label     "Filter Resonance:"
      :val-str   (.toFixed (or (:resonance cur-spec) 0.35) 2)
      :min       "0.05" :max "0.95" :step "0.05"
      :value     (or (:resonance cur-spec) 0.35)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :resonance v cur-spec)))}]]

   [:div.inst-grid-2col-bottom
    [comps/param-slider
     {:label     "Decay Ring Time (s):"
      :val-str   (str (.toFixed (or (:decay cur-spec) 0.85) 2) " s")
      :min       "0.10" :max "3.00" :step "0.05"
      :value     (or (:decay cur-spec) 0.85)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :decay v cur-spec)))}]

    [comps/param-slider
     {:label     "Inharmonic Drive:"
      :val-str   (.toFixed (or (:drive cur-spec) 1.0) 2)
      :min       "0.2" :max "3.0" :step "0.1"
      :value     (or (:drive cur-spec) 1.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :drive v cur-spec)))}]]])

(defn- clap-params-section
  "Render clap synthesis parameters.
  Examples: [clap-params-section :clap cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "HAND CLAP SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [comps/param-slider
     {:label     "Bandpass Cutoff (Hz):"
      :val-str   (str (Math/round (or (:cutoff cur-spec) 1200.0)) " Hz")
      :min       "400.0" :max "3000.0" :step "50.0"
      :value     (or (:cutoff cur-spec) 1200.0)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :cutoff v cur-spec)))}]

    [comps/param-slider
     {:label     "Decay Time (s):"
      :val-str   (str (.toFixed (or (:decay cur-spec) 0.28) 2) " s")
      :min       "0.08" :max "0.80" :step "0.02"
      :value     (or (:decay cur-spec) 0.28)
      :on-down   #(state/touch-preview-note! cur-sel-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (state/patch-param! cur-sel-key :decay v cur-spec)))}]]])

(defn drum-inspector
  "Render drum voice parameter studio, reset controls, and mini-notation generator.
  Examples: [drum-inspector :kick cur-spec]."
  [cur-sel-key cur-spec]
  (let [drum-type (or (:type cur-spec) cur-sel-key)]
    [:div.inst-section
     [:div.inst-spec-header
      [:div.inst-section-label "DRUM SYNTHESIS PARAMETERS"]
      [:button.neo-btn-stats.inst-reset-btn
       {:on-click #(state/reset-param! cur-sel-key)
        :title    "Reset drum parameters to baseline catalog definition"}
       "↺ RESET"]]

     [:div.inst-box
      [:div.inst-section-label "DRUM SYNTHESIS MODEL (:mod)"]
      [comps/pill-selector
       {:label    "CHARACTER MODE:"
        :items    [:analog :natural :idm :industrial]
        :current  (or (:mod cur-spec) :natural)
        :on-select #(state/patch-param! cur-sel-key :mod % cur-spec)}]]

     (case drum-type
       :kick     [kick-params-section cur-sel-key cur-spec]
       :snare    [snare-params-section cur-sel-key cur-spec]
       :hat      [hat-params-section cur-sel-key cur-spec]
       :membrane [membrane-params-section cur-sel-key cur-spec]
       :metallic [metallic-params-section cur-sel-key cur-spec]
       :clap     [clap-params-section cur-sel-key cur-spec]
       [:div.inst-drum-notice
        "Analog Drum voice runs on dedicated physical models in Rust WASM with sample-accurate transient punch."])

     [shared/bus-section cur-sel-key cur-spec]
     [shared/code-spec-section cur-sel-key cur-spec]
     [:div.inst-spec-container
      [:div.inst-section-label "LIVE-CODING RHYTHM SNIPPET"]
      [:pre.inst-spec-pre
       (str "(loop! " cur-sel-key "\n  {:inst " cur-sel-key "\n   :notes (euclid 5 16 :hit)\n   :step \"16n\"})")]]]))
