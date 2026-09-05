(ns app.ui.instrument-browser.inspector
  "Right live parameter inspector and sound design studio for Instrument Studio."
  (:require
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.state :as state]
   [clojure.string :as str]))

(defn inspector-header
  "Render inspector header with title, badges and FX routing summary.
  Examples: [inspector-header :saw-bass spec drum?]."
  [cur-sel-key cur-spec drum?]
  [:div.inst-inspector-header
   [:div.inst-inspector-title-row
    [:span.inst-inspector-title (str cur-sel-key)]
    [:div.inst-inspector-badges
     (when (state/custom-file-inst? cur-sel-key)
       [:span.neo-badge.badge-custom
        {:title "User custom instrument defined in custom/instruments.cljs (persisted on disk)"}
        "CUSTOM"])
     (when (state/session-inst? cur-sel-key)
       [:span.neo-badge.badge-session
        {:title "Session instrument / on-the-fly REPL modification (ephemeral, resets on F5)"}
        "SESSION"])
     [:span.neo-bus-tag.inspector-bus {:class (comps/bus-badge-class (:bus cur-spec))}
      (str "BUS: " (str/upper-case (str/replace (name (or (:bus cur-spec) :direct)) #"bus/" "")))]
     [:span.neo-badge.inspector-badge
      (cond
        drum? "ANALOG DRUM"
        (= (:type cur-spec) :poly)
        (let [poly (or (:polyphony cur-spec) (:maxPolyphony cur-spec) (:max-polyphony cur-spec) 16)]
          (str "POLY (" poly "x)"))
        :else "MONOPHONIC")]]]
   [:div.inst-inspector-subtitle
    (if drum?
      "Dedicated Rust WASM Analog Drum Synthesis Core"
      (str "Oscillator: " (str/upper-case (name (get-in cur-spec [:osc :type] :saw)))
           " | FX Chain: " (comps/bus-fx-summary (:bus cur-spec))))]])

(defn audition-bar
  "Render quick test trigger buttons and continuous audition loop toggle.
  Examples: [audition-bar :saw-bass cur-spec drum? looping?]."
  [cur-sel-key cur-spec drum? looping?]
  (let [cat (state/sound-family cur-sel-key cur-spec)
        fx? (= cat :fx)]
    [:div.inst-audition-bar
     [:button.neo-btn-stats
      {:on-click #(state/play-test-note! cur-sel-key)
       :title    (cond drum? "Audition single drum hit" fx? "Audition single one-shot FX" :else "Audition single sustained note")}
      (cond drum? "♪ HIT" fx? "♪ SHOT" :else "♪ NOTE")]
     [:button.neo-btn-stats
      {:on-click #(state/play-test-run! cur-sel-key)
       :title    (cond drum? "Audition drum roll" fx? "Audition descending pitch run" :else "Audition melodic 5-note scale run")}
      (if drum? "♫ ROLL" "♫ RUN")]
     (cond
       drum?
       [:button.neo-btn-stats
        {:on-click #(state/play-test-arp! cur-sel-key)
         :title    "Audition 8-step drum fill"}
        "≋ FILL"]

       fx?
       [:<>
        [:button.neo-btn-stats
         {:on-click #(state/play-test-arp! cur-sel-key)
          :title    "Audition rapid FX strobe"}
         "≋ ARP"]
        [:button.neo-btn-stats
         {:on-click #(state/play-test-chord! cur-sel-key)
          :title    "Audition FX burst"}
         "≈ CHORD"]]

       :else
       [:<>
        [:button.neo-btn-stats
         {:on-click #(state/play-test-arp! cur-sel-key)
          :title    "Audition 8-step rhythmic arpeggio"}
         "≋ ARP"]
        [:button.neo-btn-stats
         {:on-click #(state/play-test-chord! cur-sel-key)
          :title    (if (= (:type cur-spec) :poly)
                      "Audition polyphonic sustained chord"
                      "Audition fast broken chord strum")}
         "≈ CHORD"]])
     [:button.neo-btn-stats.audition-loop-btn
      {:class    (when looping? "active")
       :on-click #(state/toggle-audition-loop! cur-sel-key)
       :title    "Continuously loop the sound so you can tweak parameters live"}
      (if looping? "■ STOP" "⟳ LOOP")]]))

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

(defn bus-section
  "Render target bus selector and dynamic FX send chain summary.
  Examples: [bus-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "MIXER BUS + FX ROUTING (:bus)"]
   [comps/pill-selector
    {:label    "TARGET BUS:"
     :items    [:bus/bass :bus/lead :bus/space :bus/direct]
     :current  (or (:bus cur-spec) :bus/lead)
     :on-select #(state/patch-param! cur-sel-key :bus % cur-spec)}]
   [:div.inst-slider-label
    "Active FX Send: "
    [:span.neo-v.v-cyan (comps/bus-fx-summary (:bus cur-spec))]]])

(defn code-spec-section
  "Render declarative code specification block ready for custom/instruments.cljs.
  Examples: [code-spec-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-spec-container
   [:div.inst-spec-header
    [:div.inst-section-label "DECLARATIVE SPECIFICATION (CLOJURESCRIPT)"]
    [:span.inst-slider-label "Ready for custom/instruments.cljs"]]
   [:pre.inst-spec-pre
    (comps/format-spec-map cur-sel-key cur-spec)]])

(defn drum-inspector
  "Render drum voice details and mini-notation code generator.
  Examples: [drum-inspector :kick cur-spec]."
  [cur-sel-key _cur-spec]
  [:div.inst-section
   [:div.inst-section-label "ANALOG DRUM SYNTHESIS CORE"]
   [:div.inst-drum-notice
    "Analog Drum voices (Kick, Snares, Hi-Hats, Toms, Claps) run on dedicated 808/909 physical models in Rust WASM with sample-accurate transient punch. Use the audition buttons above (♪ HIT, ♫ ROLL, ≋ FILL, ⟳ LOOP) to test them."]
   [:div.inst-spec-container
    [:div.inst-section-label "LIVE-CODING RHYTHM SNIPPET"]
    [:pre.inst-spec-pre
     (str "(loop! " cur-sel-key "\n  {:inst " cur-sel-key "\n   :notes (euclid 5 16 :hit)\n   :step \"16n\"})")]]])

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
   [bus-section cur-sel-key cur-spec]
   [code-spec-section cur-sel-key cur-spec]])

(defn inspector-panel
  "Render right parameter inspector and fine-tuning studio panel.
  Examples: [inspector-panel :saw-bass cur-spec drum? looping?]."
  [cur-sel-key cur-spec drum? looping?]
  [:div.inst-inspector-panel
   [inspector-header cur-sel-key cur-spec drum?]
   [audition-bar cur-sel-key cur-spec drum? looping?]
   (if drum?
     [drum-inspector cur-sel-key cur-spec]
     [synth-inspector cur-sel-key cur-spec])])
