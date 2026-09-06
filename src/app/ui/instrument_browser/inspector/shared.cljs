(ns app.ui.instrument-browser.inspector.shared
  "Shared inspector UI sections for mixer bus routing and code specification generation."
  (:require
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.state :as state]))

(defn bus-section
  "Render target bus selector and dynamic FX send chain summary.
  Examples: [bus-section :saw-bass cur-spec]."
  [cur-sel-key cur-spec]
  [:div.inst-box
   [:div.inst-section-label "MIXER BUS + FX ROUTING (:bus)"]
   [comps/pill-selector
    {:label    "TARGET BUS:"
     :items    [:bus/drums :bus/bass :bus/lead :bus/space :bus/direct]
     :current  (or (:bus cur-spec) (if (= (:category cur-spec) :drums) :bus/drums :bus/lead))
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
