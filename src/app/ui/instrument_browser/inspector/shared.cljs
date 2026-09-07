(ns app.ui.instrument-browser.inspector.shared
  "Shared inspector UI sections for mixer bus routing, code specification generation, and parameter sliders."
  (:require
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.formatters :as fmt]
   [app.ui.instrument-browser.state :as state]))

(defn patch-slider
  "Render a parameter slider declaratively bound to an instrument patch property.
  Supports both root keys (:decay) and nested section paths ([:filter :cutoff]).
  Examples: [patch-slider {:inst-key cur-sel-key :spec cur-spec :param-key :decay :label \"Decay:\" :min 0.05 :max 1.0 :step 0.01 :default 0.28 :unit \"s\"}]."
  [{:keys [inst-key spec param-key label min max step default unit decimals pct?]
    :or   {decimals 2 default 0.0 unit "" pct? false}}]
  (let [nested?  (vector? param-key)
        curr-val (or (if nested? (get-in spec param-key) (get spec param-key)) default)
        num-val  (js/parseFloat curr-val)
        val-str  (cond
                   pct?
                   (str (Math/round (* num-val 100)) "%")

                   (zero? decimals)
                   (str (Math/round num-val))

                   :else
                   (.toFixed num-val decimals))
        display  (if (and (seq unit) (not pct?)) (str val-str " " unit) val-str)]
    [comps/param-slider
     {:label     label
      :val-str   display
      :min       (str min)
      :max       (str max)
      :step      (str step)
      :value     curr-val
      :on-down   #(audition/touch-preview-note! inst-key)
      :on-change (fn [e]
                   (let [v (js/parseFloat (.. e -target -value))]
                     (if nested?
                       (let [[sec param] param-key]
                         (state/patch-param! inst-key sec param v spec))
                       (state/patch-param! inst-key param-key v spec))))}]))

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
    (fmt/format-spec-map cur-sel-key cur-spec)]])
