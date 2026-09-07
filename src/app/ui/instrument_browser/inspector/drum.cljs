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
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :base-pitch
                          :label "Base Sub Pitch (Hz):" :min 32.0 :max 75.0 :step 0.5 :default 48.0 :unit "Hz" :decimals 1}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-drop
                          :label "Pitch Drop (+Hz):" :min 0.0 :max 350.0 :step 5.0 :default 180.0 :unit "Hz" :decimals 0}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-decay
                          :label "Pitch Decay Rate:" :min 0.010 :max 0.120 :step 0.005 :default 0.040 :decimals 3}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Sub Decay Time (s):" :min 0.06 :max 0.80 :step 0.01 :default 0.28 :unit "s" :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :click
                          :label "Beater Click Punch:" :min 0.0 :max 1.0 :step 0.02 :default 0.35 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Analog Saturation Drive:" :min 0.0 :max 4.0 :step 0.1 :default 1.6 :decimals 2}]]])

(defn- snare-params-section
  "Render snare drum physical model tuning parameters.
  Examples: [snare-params-section :snare cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "SNARE DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :base-freq
                          :label "Shell Base Frequency (Hz):" :min 110.0 :max 320.0 :step 1.0 :default 185.0 :unit "Hz" :decimals 1}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :tone-decay
                          :label "Shell Tone Decay:" :min 0.9900 :max 0.9999 :step 0.0001 :default 0.9985 :decimals 4}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-decay
                          :label "Wire Noise Decay:" :min 0.9900 :max 0.9999 :step 0.0001 :default 0.9991 :decimals 4}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :cutoff
                          :label "Wire Bandpass Cutoff (Hz):" :min 800.0 :max 7000.0 :step 50.0 :default 2400.0 :unit "Hz" :decimals 0}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :snappy
                          :label "Snappy Balance:" :min 0.0 :max 1.0 :step 0.02 :default 0.85 :decimals 2}]]])

(defn- hat-params-section
  "Render hi-hat metallic noise and decay parameters.
  Examples: [hat-params-section :hat cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "HI-HAT SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :cutoff
                          :label "Highpass Cutoff (Hz):" :min 3000.0 :max 12000.0 :step 100.0 :default 7200.0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay-closed
                          :label "Closed Decay (s):" :min 0.01 :max 0.15 :step 0.005 :default 0.04 :unit "s" :decimals 3}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay-open
                          :label "Open Decay (s):" :min 0.05 :max 0.80 :step 0.01 :default 0.24 :unit "s" :decimals 2}]]])

(defn- membrane-params-section
  "Render membrane drum voice tuning parameters for toms and tuned percussions.
  Examples: [membrane-params-section :tom cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "MEMBRANE DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :start-pitch
                          :label "Start Strike Pitch (Hz):" :min 50.0 :max 450.0 :step 2.0 :default 180.0 :unit "Hz" :decimals 1}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :min-pitch
                          :label "Resting Sub Pitch (Hz):" :min 40.0 :max 300.0 :step 1.0 :default 105.0 :unit "Hz" :decimals 1}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-decay
                          :label "Pitch Glide Decay:" :min 0.005 :max 0.060 :step 0.002 :default 0.015 :decimals 3}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Amplitude Decay (s):" :min 0.08 :max 1.20 :step 0.02 :default 0.40 :unit "s" :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Membrane Saturation Drive:" :min 0.5 :max 3.0 :step 0.05 :default 1.10 :decimals 2}]]])

(defn- metallic-params-section
  "Render metallic voice tuning parameters for cymbals, rides, and bells.
  Examples: [metallic-params-section :ride-bell cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "METALLIC CYMBAL + PERCUSSION PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :cutoff
                          :label "Filter Cutoff (Hz):" :min 500.0 :max 10000.0 :step 100.0 :default 3500.0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :resonance
                          :label "Filter Resonance:" :min 0.05 :max 0.95 :step 0.05 :default 0.35 :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Decay Ring Time (s):" :min 0.10 :max 3.00 :step 0.05 :default 0.85 :unit "s" :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Inharmonic Drive:" :min 0.2 :max 3.0 :step 0.1 :default 1.0 :decimals 2}]]])

(defn- clap-params-section
  "Render clap synthesis parameters.
  Examples: [clap-params-section :clap cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "HAND CLAP SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :cutoff
                          :label "Bandpass Cutoff (Hz):" :min 400.0 :max 3000.0 :step 50.0 :default 1200.0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Decay Time (s):" :min 0.08 :max 0.80 :step 0.02 :default 0.28 :unit "s" :decimals 2}]]])

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
