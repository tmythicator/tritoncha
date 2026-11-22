(ns app.ui.instrument-browser.inspector.drum
  "Data-driven drum synthesis parameter studio, body/style selectors, and mini-notation generator."
  (:require
   [app.audio.dsp.instruments :as instruments]
   [app.audio.dsp.styles :as styles]
   [app.state :refer [audio-state repl-registry]]
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.inspector.shared :as shared]
   [app.ui.instrument-browser.state :as state]))

(def ^:private drum-param-sections
  {:kick
   {:title "KICK DRUM SYNTHESIS PARAMETERS"
    :sliders
    [{:param-key :pitch       :label "Base Sub Pitch (Hz):"    :min 32.0  :max 75.0   :step 0.5   :default 48.0  :unit "Hz" :decimals 1}
     {:param-key :pitch-drop  :label "Pitch Drop (+Hz):"       :min 0.0   :max 350.0  :step 5.0   :default 160.0 :unit "Hz" :decimals 0}
     {:param-key :pitch-decay :label "Pitch Decay (s):"        :min 0.010 :max 0.200  :step 0.005 :default 0.085 :unit "s"  :decimals 3}
     {:param-key :decay       :label "Sub Decay Time (s):"     :min 0.06  :max 0.90   :step 0.01  :default 0.45  :unit "s"  :decimals 2}
     {:param-key :click       :label "Beater Click Punch:"     :min 0.0   :max 1.0    :step 0.02  :default 0.28  :decimals 2}
     {:param-key :drive       :label "Analog Saturation Drive:" :min 0.5  :max 4.0    :step 0.1   :default 1.55  :decimals 2}]}

   :snare
   {:title "SNARE DRUM SYNTHESIS PARAMETERS"
    :sliders
    [{:param-key :pitch        :label "Shell Base Frequency (Hz):" :min 110.0 :max 320.0  :step 1.0  :default 185.0  :unit "Hz" :decimals 1}
     {:param-key :decay        :label "Shell Tone Decay (s):"      :min 0.03  :max 0.50   :step 0.01 :default 0.14   :unit "s"  :decimals 2}
     {:param-key :noise-decay  :label "Wire Noise Decay (s):"      :min 0.04  :max 0.60   :step 0.01 :default 0.22   :unit "s"  :decimals 2}
     {:param-key :noise-cutoff :label "Wire Bandpass Cutoff (Hz):" :min 800.0 :max 7000.0 :step 50.0 :default 2600.0 :unit "Hz" :decimals 0}
     {:param-key :noise-mix    :label "Snappy Wire Mix:"           :min 0.0   :max 1.0    :step 0.02 :default 0.85   :decimals 2}
     {:param-key :drive        :label "Saturation Drive:"          :min 0.5   :max 4.0    :step 0.1  :default 1.35   :decimals 2}]}

   :hat
   {:title "HI-HAT SYNTHESIS PARAMETERS"
    :sliders
    [{:param-key :noise-cutoff :label "Highpass Cutoff (Hz):" :min 3000.0 :max 12000.0 :step 100.0 :default 7200.0 :unit "Hz" :decimals 0}
     {:param-key :decay        :label "Metallic Decay (s):"   :min 0.01   :max 0.80    :step 0.005 :default 0.045  :unit "s"  :decimals 3}
     {:param-key :noise-decay  :label "Noise Tail Decay (s):" :min 0.01   :max 0.80    :step 0.005 :default 0.048  :unit "s"  :decimals 3}
     {:param-key :noise-mix    :label "Noise Sizzle Mix:"     :min 0.0    :max 1.0     :step 0.02  :default 0.72   :decimals 2}]}

   :membrane
   {:title "TOM / MEMBRANE SYNTHESIS PARAMETERS"
    :sliders
    [{:param-key :pitch       :label "Base Pitch (Hz):"       :min 50.0  :max 350.0 :step 1.0   :default 138.0 :unit "Hz" :decimals 1}
     {:param-key :pitch-drop  :label "Pitch Drop (+Hz):"      :min 0.0   :max 180.0 :step 2.0   :default 56.0  :unit "Hz" :decimals 0}
     {:param-key :pitch-decay :label "Pitch Glide Decay (s):" :min 0.010 :max 0.250 :step 0.005 :default 0.105 :unit "s"  :decimals 3}
     {:param-key :decay       :label "Amplitude Decay (s):"   :min 0.08  :max 1.20  :step 0.02  :default 0.38  :unit "s"  :decimals 2}
     {:param-key :fm-depth    :label "Membrane Overtone:"     :min 0.0   :max 1.0   :step 0.02  :default 0.18  :decimals 2}
     {:param-key :drive       :label "Saturation Drive:"      :min 0.5   :max 3.0   :step 0.05  :default 1.15  :decimals 2}]}

   :metallic
   {:title "METALLIC CYMBAL + PERCUSSION PARAMETERS"
    :noise-filter-pills? true
    :sliders
    [{:param-key :pitch        :label "Base Metallic Pitch (Hz):"      :min 180.0 :max 1600.0  :step 10.0  :default 400.0  :unit "Hz" :decimals 0}
     {:param-key :modal-spread :label "Modal Spread (Inharmonicity):"  :min 0.50  :max 2.60    :step 0.05  :default 1.55   :decimals 2}
     {:param-key :decay        :label "Bronze Ring Decay (s):"         :min 0.08  :max 3.00    :step 0.05  :default 1.05   :unit "s"  :decimals 2}
     {:param-key :noise-decay  :label "Wash Tail Decay (s):"           :min 0.08  :max 3.00    :step 0.05  :default 1.15   :unit "s"  :decimals 2}
     {:param-key :noise-mix    :label "Wash / Noise Mix:"              :min 0.0   :max 1.0     :step 0.02  :default 0.66   :pct? true}
     {:param-key :noise-cutoff :label "Filter Cutoff (Hz):"            :min 500.0 :max 10000.0 :step 100.0 :default 3200.0 :unit "Hz" :decimals 0}
     {:param-key :noise-q      :label "Filter Resonance (Q):"          :min 0.05  :max 0.95    :step 0.02  :default 0.16   :decimals 2}
     {:param-key :drive        :label "Saturation Drive:"              :min 0.5   :max 4.0     :step 0.05  :default 1.15   :decimals 2}
     {:param-key :click        :label "Stick Attack Level:"            :min 0.0   :max 1.0     :step 0.02  :default 0.0    :pct? true}]}

   :clap
   {:title "HAND CLAP SYNTHESIS PARAMETERS"
    :sliders
    [{:param-key :noise-cutoff :label "Bandpass Cutoff (Hz):" :min 400.0 :max 3000.0 :step 50.0 :default 1250.0 :unit "Hz" :decimals 0}
     {:param-key :noise-q      :label "Filter Resonance:"     :min 0.10  :max 0.95   :step 0.05 :default 0.68   :decimals 2}
     {:param-key :noise-decay  :label "Decay Time (s):"       :min 0.08  :max 0.80   :step 0.02 :default 0.24   :unit "s"  :decimals 2}
     {:param-key :drive        :label "Saturation Drive:"     :min 0.5   :max 3.0    :step 0.1  :default 1.45   :decimals 2}]}})

(def ^:private shaper-section-spec
  {:title "SHAPER + VELOCITY DYNAMICS"
   :sliders
   [{:param-key :fold      :label "Wavefolder:"    :min 0.0 :max 1.0 :step 0.02 :default 0.0  :pct? true}
    {:param-key :crush     :label "Bitcrush:"      :min 0.0 :max 1.0 :step 0.02 :default 0.0  :pct? true}
    {:param-key :vel->tone :label "Vel -> Tone:"   :min 0.0 :max 1.0 :step 0.05 :default 0.35 :pct? true}
    {:param-key :variation :label "Hit Variation:" :min 0.0 :max 1.0 :step 0.02 :default 0.08 :pct? true}]})

(defn- slider-section
  [cur-sel-key cur-spec {:keys [title sliders noise-filter-pills?]}]
  (let [rows (vec (partition-all 2 sliders))
        last-idx (dec (count rows))]
    [:div.inst-box
     [:div.inst-section-label title]
     (for [[idx row] (map-indexed vector rows)]
       ^{:key idx}
       [:div {:class (if (= idx last-idx) "inst-grid-2col-bottom" "inst-grid-2col")}
        (for [s row]
          ^{:key (:param-key s)}
          [shared/patch-slider (assoc s :inst-key cur-sel-key :spec cur-spec)])
        (when (and (= idx last-idx) noise-filter-pills? (= 1 (count row)))
          [comps/pill-selector
           {:label     "NOISE FILTER MODE:"
            :items     [:hp :bp :lp]
            :current   (or (:noise-filter cur-spec) :hp)
            :on-select #(state/patch-param! cur-sel-key :noise-filter % cur-spec)}])])]))

(defn- audition-patch! [inst-key param-k val]
  (instruments/patch! inst-key param-k val)
  (when-not @audition/audition-loop-active?
    (audition/play-test-note! inst-key)))

(defn drum-inspector
  "Render drum voice parameter studio, physical body and style selectors, reset controls, and mini-notation generator."
  [cur-sel-key cur-spec]
  (let [drum-type   (or (:type cur-spec) cur-sel-key)
        style-items (vec (distinct (concat [:natural :idm :industrial] (keys (:styles @repl-registry)))))
        global-mod  (:drum-mode @audio-state)
        active-mod  (or (:mod cur-spec)
                        (when (keyword? global-mod) (styles/normalize-style-key global-mod))
                        :natural)]
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
       {:label     "PHYSICAL BODY:"
        :items     [:membrane :shell :cymbal :idiophone :burst]
        :current   (or (:body cur-spec) :membrane)
        :on-select #(audition-patch! cur-sel-key :body %)}]
      [comps/pill-selector
       {:label     "GENRE STYLE:"
        :items     style-items
        :current   active-mod
        :on-select #(audition-patch! cur-sel-key :mod %)}]]

     (if-let [sec (get drum-param-sections drum-type)]
       [slider-section cur-sel-key cur-spec sec]
       [:div.inst-drum-notice
        "Layered percussion voice runs on 5 physical body models in Rust WASM with sample-accurate transient punch."])

     [slider-section cur-sel-key cur-spec shaper-section-spec]
     [shared/bus-section cur-sel-key cur-spec]
     [shared/code-spec-section cur-sel-key cur-spec]
     [:div.inst-spec-container
      [:div.inst-section-label "LIVE-CODING RHYTHM SNIPPET"]
      [:pre.inst-spec-pre
       (str "(loop! " cur-sel-key "\n  {:inst " cur-sel-key "\n   :notes (euclid 5 16 :hit)\n   :step \"16n\"})")]]]))
