(ns app.ui.stats-panel
  "Audio engine statistics panel coordinating telemetry, DSP graph and loops monitors."
  (:require
   [app.audio.engine :refer [telemetry-snapshot]]
   [app.state :refer [audio-state visual-state]]
   [app.ui.stats.loops :refer [active-loops-component]]
   [app.ui.stats.routing-graph :refer [routing-graph-component]]
   [app.ui.stats.telemetry :refer [telemetry-component]]
   [clojure.string :as str]
   [reagent.core :as r]))

(defn- stats-header [{:keys [ctx-state]} on-close]
  (let [st (or ctx-state "uninitialized")]
    [:div.neo-header
     [:div.neo-title
      [:span.neo-prompt "> "]
      [:span "SYSTEM AUDIO STATUS"]]
     [:div.neo-header-right
      [:div.neo-status-badge
       [:span.neo-dot {:class (if (= st "running") "online" "offline")}]
       [:span (if (= st "running") "ONLINE" "OFFLINE")]]
      [:button.neo-btn-close {:on-click on-close
                              :aria-label "Close stats modal"
                              :title "Close"} "[X]"]]]))

(defn- stats-footer []
  [:div.neo-footer
   [:span.neo-foot-cmd "> ./tritoncha --stats"]
   [:span.neo-foot-hint "[Press I or click [X] to close]"]])

(defn stats-panel-component [_]
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
      (fn [{:keys [on-close]}]
        (let [_            @tick-atom
              snap         (telemetry-snapshot)
              tracks-map   (or (:active-tracks @audio-state) {})
              key-data     (:key @audio-state {:root :e :mode :phrygian :octave 1})
              key-str      (str (str/upper-case (name (:root key-data :e))) " " (name (:mode key-data :phrygian)))
              scene-name   (name (:current-scene @visual-state :cyber-torus))
              telemetry    (merge snap {:key-str key-str :scene-name scene-name})]
          [:aside.neo-stats-card {:aria-label "System Audio Status"}
           [stats-header snap on-close]
           [:div.neo-body
            [telemetry-component telemetry]
            [routing-graph-component]
            [active-loops-component tracks-map]]
           [stats-footer]]))})))
