(ns app.ui.instrument-browser.inspector
  "Right live parameter inspector and sound design studio facade for Instrument Studio."
  (:require
   [app.ui.instrument-browser.inspector.drum :as drum]
   [app.ui.instrument-browser.inspector.header :as header]
   [app.ui.instrument-browser.inspector.shared :as shared]
   [app.ui.instrument-browser.inspector.synth :as synth]))

(defn inspector-header
  "Render inspector header with title, badges and FX routing summary.
  Examples: [inspector-header :saw-bass spec drum?]."
  [cur-sel-key cur-spec drum?]
  [header/inspector-header cur-sel-key cur-spec drum?])

(defn audition-bar
  "Render quick test trigger buttons and continuous audition loop toggle.
  Examples: [audition-bar :saw-bass cur-spec drum? looping?]."
  [cur-sel-key cur-spec drum? looping?]
  [header/audition-bar cur-sel-key cur-spec drum? looping?])

(defn bus-section
  "Render target bus selector and dynamic FX send chain summary.
  Examples: [bus-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [shared/bus-section cur-sel-key cur-spec])

(defn code-spec-section
  "Render declarative code specification block ready for custom/instruments.cljs.
  Examples: [code-spec-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [shared/code-spec-section cur-sel-key cur-spec])

(defn drum-inspector
  "Render drum voice parameter studio, reset controls, and mini-notation generator.
  Examples: [drum-inspector :kick cur-spec]."
  [cur-sel-key cur-spec]
  [drum/drum-inspector cur-sel-key cur-spec])

(defn synth-inspector
  "Render complete modular sound design suite for synth voices.
  Examples: [synth-inspector :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [synth/synth-inspector cur-sel-key cur-spec])

(defn inspector-panel
  "Render right parameter inspector and fine-tuning studio panel.
  Examples: [inspector-panel :saw-bass cur-spec drum? looping?]."
  [cur-sel-key cur-spec drum? looping?]
  [:div.inst-inspector-panel
   [header/inspector-header cur-sel-key cur-spec drum?]
   [header/audition-bar cur-sel-key cur-spec drum? looping?]
   (if drum?
     [drum/drum-inspector cur-sel-key cur-spec]
     [synth/synth-inspector cur-sel-key cur-spec])])
