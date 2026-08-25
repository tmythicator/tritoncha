(ns app.ui.stats-panel
  "Audio engine statistics panel coordinating telemetry, DSP graph and loops monitors."
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [app.state :refer [audio-state visual-state]]
            [app.audio.engine :refer [telemetry-snapshot]]
            [app.ui.stats.telemetry :refer [telemetry-component]]
            [app.ui.stats.routing-graph :refer [routing-graph-component]]
            [app.ui.stats.loops :refer [active-loops-component]]))

(defn- stats-header [{:keys [ctx-state]}]
  (let [st (or ctx-state "uninitialized")]
    [:div.neo-header
     [:div.neo-title
      [:span.neo-prompt "> "]
      [:span "SYSTEM AUDIO STATUS"]]
     [:div.neo-status-badge
      [:span.neo-dot {:class (if (= st "running") "online" "offline")}]
      [:span (if (= st "running") "ONLINE" "OFFLINE")]]]))

(defn- stats-footer []
  [:div.neo-footer
   [:span.neo-foot-cmd "> ./tritoncha --stats"]
   [:span.neo-foot-hint "[Press I or click STATS to close]"]])

(defn stats-panel-component []
  (let [tick-atom   (r/atom 0)
        interval-id (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (reset! interval-id (js/setInterval #(swap! tick-atom inc) 1000)))

      :component-will-unmount
      (fn [_]
        (when-let [id @interval-id]
          (js/clearInterval id)))

      :reagent-render
      (fn []
        (let [_            @tick-atom
              snap         (telemetry-snapshot)
              tracks-map   (or (:active-tracks @audio-state) {})
              key-data     (:key @audio-state {:root :e :mode :phrygian :octave 1})
              key-str      (str (str/upper-case (name (:root key-data :e))) " " (name (:mode key-data :phrygian)))
              scene-name   (name (:current-scene @visual-state :cyber-torus))
              telemetry    (merge snap {:key-str key-str :scene-name scene-name})]
          [:aside.neo-stats-card {:aria-label "System Audio Status"}
           [stats-header snap]
           [:div.neo-body
            [telemetry-component telemetry]
            [routing-graph-component]
            [active-loops-component tracks-map]]
           [stats-footer]]))})))
