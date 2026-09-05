(ns app.ui.instrument-browser.list.card
  "Instrument card component presenting taxonomy badges, sound specs, and audition triggers."
  (:require
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.list.buttons :as buttons]
   [app.ui.instrument-browser.state :as state]
   [clojure.string :as str]))

(defn- card-origin-badges
  "Render optional custom and session badges.
  Examples: [card-origin-badges true false]."
  [custom? session?]
  [:<>
   (when custom?
     [:span.neo-badge.badge-custom
      {:title "User custom instrument defined in custom/instruments.cljs (persisted on disk)"}
      "CUSTOM"])
   (when session?
     [:span.neo-badge.badge-session
      {:title "Session instrument / on-the-fly REPL modification (ephemeral, resets on F5)"}
      "SESSION"])])

(defn- card-routing-and-poly-badges
  "Render bus destination tag and polyphony/mono indicator.
  Examples: [card-routing-and-poly-badges spec :leads true]."
  [spec cat poly?]
  [:div.inst-card-badges
   [:span.neo-bus-tag.mini {:class (comps/bus-badge-class (:bus spec))}
    (str/upper-case (str/replace (name (or (:bus spec) :direct)) #"bus/" ""))]
   [:span.neo-badge.mini
    (cond
      (= cat :drums) "DRUM"
      (= cat :fx)    (if poly? (str "FX " (or (:polyphony spec) 16) "x") "FX")
      poly?
      (let [poly (or (:polyphony spec) (:maxPolyphony spec) (:max-polyphony spec) 16)]
        (str "POLY " poly "x"))
      :else "MONO")]])

(defn- card-header-row
  "Render title, origin tags, bus badge, and polyphony indicator.
  Examples: [card-header-row :bass-analog spec :bass true true false false]."
  [inst-key spec cat poly? selected? custom? session?]
  [:div.inst-card-header
   [:div.inst-card-title-group
    [:span.inst-card-title {:class (when selected? "selected")}
     (str inst-key)]
    [card-origin-badges custom? session?]]
   [card-routing-and-poly-badges spec cat poly?]])

(defn- card-subtitle-row
  "Render oscillator waveform summary and quick audition trigger buttons.
  Examples: [card-subtitle-row :lead-pluck spec :leads true]."
  [inst-key spec cat poly?]
  [:div.inst-card-subtitle
   [:span
    (cond
      (= cat :drums) "WASM Analog Drum"
      :else (str (name (get-in spec [:osc :type] :saw))
                 " • " (Math/round (or (get-in spec [:filter :cutoff]) 2000)) " Hz"))]
   [buttons/card-audition-buttons inst-key cat poly?]])

(defn instrument-card
  "Render interactive card for a single instrument in catalog list.
  Examples: [instrument-card :bass-analog spec selected?]."
  [inst-key spec selected?]
  (let [cat         (state/sound-family inst-key spec)
        poly?       (= (:type spec) :poly)
        custom?     (state/custom-file-inst? inst-key)
        session?    (state/session-inst? inst-key)]
    [:div.inst-catalog-card
     {:class    (when selected? "selected")
      :on-click #(state/select-instrument! inst-key)}
     [card-header-row inst-key spec cat poly? selected? custom? session?]
     [card-subtitle-row inst-key spec cat poly?]]))
