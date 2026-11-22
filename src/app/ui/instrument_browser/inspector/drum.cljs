(ns app.ui.instrument-browser.inspector.drum
  "Drum synthesis parameter studio, physical body selector, character style selector, and mini-notation generator."
  (:require
   [app.audio.dsp.instruments :as instruments]
   [app.audio.dsp.styles :as styles]
   [app.state :refer [audio-state repl-registry]]
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.inspector.shared :as shared]
   [app.ui.instrument-browser.state :as state]))

(defn- kick-params-section
  "Render kick drum physical model tuning parameters."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "KICK DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch
                          :label "Base Sub Pitch (Hz):" :min 32.0 :max 75.0 :step 0.5 :default 48.0 :unit "Hz" :decimals 1}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-drop
                          :label "Pitch Drop (+Hz):" :min 0.0 :max 350.0 :step 5.0 :default 160.0 :unit "Hz" :decimals 0}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-decay
                          :label "Pitch Decay (s):" :min 0.010 :max 0.200 :step 0.005 :default 0.085 :unit "s" :decimals 3}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Sub Decay Time (s):" :min 0.06 :max 0.90 :step 0.01 :default 0.45 :unit "s" :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :click
                          :label "Beater Click Punch:" :min 0.0 :max 1.0 :step 0.02 :default 0.28 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Analog Saturation Drive:" :min 0.5 :max 4.0 :step 0.1 :default 1.55 :decimals 2}]]])

(defn- snare-params-section
  "Render snare drum physical model tuning parameters."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "SNARE DRUM SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch
                          :label "Shell Base Frequency (Hz):" :min 110.0 :max 320.0 :step 1.0 :default 185.0 :unit "Hz" :decimals 1}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Shell Tone Decay (s):" :min 0.03 :max 0.50 :step 0.01 :default 0.14 :unit "s" :decimals 2}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-decay
                          :label "Wire Noise Decay (s):" :min 0.04 :max 0.60 :step 0.01 :default 0.22 :unit "s" :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-cutoff
                          :label "Wire Bandpass Cutoff (Hz):" :min 800.0 :max 7000.0 :step 50.0 :default 2600.0 :unit "Hz" :decimals 0}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-mix
                          :label "Snappy Wire Mix:" :min 0.0 :max 1.0 :step 0.02 :default 0.85 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Saturation Drive:" :min 0.5 :max 4.0 :step 0.1 :default 1.35 :decimals 2}]]])

(defn- hat-params-section
  "Render hi-hat metallic noise and decay parameters."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "HI-HAT SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-cutoff
                          :label "Highpass Cutoff (Hz):" :min 3000.0 :max 12000.0 :step 100.0 :default 7200.0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Metallic Decay (s):" :min 0.01 :max 0.80 :step 0.005 :default 0.045 :unit "s" :decimals 3}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-decay
                          :label "Noise Tail Decay (s):" :min 0.01 :max 0.80 :step 0.005 :default 0.048 :unit "s" :decimals 3}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-mix
                          :label "Noise Sizzle Mix:" :min 0.0 :max 1.0 :step 0.02 :default 0.72 :decimals 2}]]])

(defn- membrane-params-section
  "Render membrane drum voice tuning parameters for toms and tuned percussions."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "TOM / MEMBRANE SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch
                          :label "Base Pitch (Hz):" :min 50.0 :max 350.0 :step 1.0 :default 138.0 :unit "Hz" :decimals 1}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-drop
                          :label "Pitch Drop (+Hz):" :min 0.0 :max 180.0 :step 2.0 :default 56.0 :unit "Hz" :decimals 0}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch-decay
                          :label "Pitch Glide Decay (s):" :min 0.010 :max 0.250 :step 0.005 :default 0.105 :unit "s" :decimals 3}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Amplitude Decay (s):" :min 0.08 :max 1.20 :step 0.02 :default 0.38 :unit "s" :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :fm-depth
                          :label "Membrane Overtone:" :min 0.0 :max 1.0 :step 0.02 :default 0.18 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Saturation Drive:" :min 0.5 :max 3.0 :step 0.05 :default 1.15 :decimals 2}]]])

(defn- metallic-params-section
  "Render metallic voice tuning parameters for cymbals, crashes, chinas, rides, and bells."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "METALLIC CYMBAL + PERCUSSION PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :pitch
                          :label "Base Metallic Pitch (Hz):" :min 180.0 :max 1600.0 :step 10.0 :default 400.0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :modal-spread
                          :label "Modal Spread (Inharmonicity):" :min 0.50 :max 2.60 :step 0.05 :default 1.55 :decimals 2}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay
                          :label "Bronze Ring Decay (s):" :min 0.08 :max 3.00 :step 0.05 :default 1.05 :unit "s" :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-decay
                          :label "Wash Tail Decay (s):" :min 0.08 :max 3.00 :step 0.05 :default 1.15 :unit "s" :decimals 2}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-mix
                          :label "Wash / Noise Mix:" :min 0.0 :max 1.0 :step 0.02 :default 0.66 :pct? true}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-cutoff
                          :label "Filter Cutoff (Hz):" :min 500.0 :max 10000.0 :step 100.0 :default 3200.0 :unit "Hz" :decimals 0}]]

   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-q
                          :label "Filter Resonance (Q):" :min 0.05 :max 0.95 :step 0.02 :default 0.16 :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Saturation Drive:" :min 0.5 :max 4.0 :step 0.05 :default 1.15 :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :click
                          :label "Stick Attack Level:" :min 0.0 :max 1.0 :step 0.02 :default 0.0 :pct? true}]
    [comps/pill-selector
     {:label     "NOISE FILTER MODE:"
      :items     [:hp :bp :lp]
      :current   (or (:noise-filter cur-spec) :hp)
      :on-select #(state/patch-param! cur-sel-key :noise-filter % cur-spec)}]]])

(defn- clap-params-section
  "Render clap synthesis parameters."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "HAND CLAP SYNTHESIS PARAMETERS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-cutoff
                          :label "Bandpass Cutoff (Hz):" :min 400.0 :max 3000.0 :step 50.0 :default 1250.0 :unit "Hz" :decimals 0}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-q
                          :label "Filter Resonance:" :min 0.10 :max 0.95 :step 0.05 :default 0.68 :decimals 2}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :noise-decay
                          :label "Decay Time (s):" :min 0.08 :max 0.80 :step 0.02 :default 0.24 :unit "s" :decimals 2}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :drive
                          :label "Saturation Drive:" :min 0.5 :max 3.0 :step 0.1 :default 1.45 :decimals 2}]]])

(defn- shaper-and-dynamics-section
  "Render wavefolder, bitcrusher, humanization variation, and velocity dynamics controls."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "SHAPER + VELOCITY DYNAMICS"]
   [:div.inst-grid-2col
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :fold
                          :label "Wavefolder:" :min 0.0 :max 1.0 :step 0.02 :default 0.0 :pct? true}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :crush
                          :label "Bitcrush:" :min 0.0 :max 1.0 :step 0.02 :default 0.0 :pct? true}]]

   [:div.inst-grid-2col-bottom
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :vel->tone
                          :label "Vel -> Tone:" :min 0.0 :max 1.0 :step 0.05 :default 0.35 :pct? true}]
    [shared/patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :variation
                          :label "Hit Variation:" :min 0.0 :max 1.0 :step 0.02 :default 0.08 :pct? true}]]])

(defn drum-inspector
  "Render drum voice parameter studio, physical body and style selectors, reset controls, and mini-notation generator."
  [cur-sel-key cur-spec]
  (let [drum-type   (or (:type cur-spec) cur-sel-key)
        user-styles (keys (:styles @repl-registry))
        style-items (vec (distinct (concat [:analog :natural :idm :industrial] user-styles)))
        global-mod  (:drum-mode @audio-state)
        active-mod  (or (:mod cur-spec)
                        (when (keyword? global-mod) (styles/normalize-style-key global-mod))
                        :analog)]
    [:div.inst-section
     [:div.inst-spec-header
      [:div.inst-section-label "DRUM SYNTHESIS PARAMETERS"]
      [:button.neo-btn-stats.inst-reset-btn
       {:on-click #(state/reset-param! cur-sel-key)
        :title    "Reset drum parameters to baseline catalog definition"}
       "↺ RESET"]]

     [:div.inst-box
      [:div.inst-section-label "PHYSICAL BODY + GENRE STYLE (:body / :mod)"]
      [comps/pill-selector
       {:label    "PHYSICAL BODY:"
        :items    [:membrane :shell :cymbal :idiophone :burst]
        :current  (or (:body cur-spec) :membrane)
        :on-select (fn [body]
                     (instruments/patch! cur-sel-key :body body)
                     (when-not @audition/audition-loop-active?
                       (audition/play-test-note! cur-sel-key)))}]
      [comps/pill-selector
       {:label    "GENRE STYLE:"
        :items    style-items
        :current  active-mod
        :on-select (fn [mode]
                     (instruments/patch! cur-sel-key :mod mode)
                     (when-not @audition/audition-loop-active?
                       (audition/play-test-note! cur-sel-key)))}]]

     (case drum-type
       :kick     [kick-params-section cur-sel-key cur-spec]
       :snare    [snare-params-section cur-sel-key cur-spec]
       :hat      [hat-params-section cur-sel-key cur-spec]
       :membrane [membrane-params-section cur-sel-key cur-spec]
       :metallic [metallic-params-section cur-sel-key cur-spec]
       :clap     [clap-params-section cur-sel-key cur-spec]
       [:div.inst-drum-notice
        "Layered percussion voice runs on 5 physical body models in Rust WASM with sample-accurate transient punch."])

     [shaper-and-dynamics-section cur-sel-key cur-spec]
     [shared/bus-section cur-sel-key cur-spec]
     [shared/code-spec-section cur-sel-key cur-spec]
     [:div.inst-spec-container
      [:div.inst-section-label "LIVE-CODING RHYTHM SNIPPET"]
      [:pre.inst-spec-pre
       (str "(loop! " cur-sel-key "\n  {:inst " cur-sel-key "\n   :notes (euclid 5 16 :hit)\n   :step \"16n\"})")]]]))
