(ns app.ui.stats-panel
  "Audio engine statistics panel coordinating telemetry, DSP graph and loops monitors."
  (:require
   [app.audio.dsp.telemetry :refer [telemetry-snapshot]]
   [app.config :as cfg]
   [app.state :refer [audio-state visual-state]]
   [app.ui.stats.loops :refer [active-loops-component]]
   [app.ui.stats.routing-graph :refer [routing-graph-component]]
   [app.ui.stats.telemetry :refer [telemetry-component]]
   [app.utils.audio :refer [format-key]]
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

(defn stats-panel-component [_props]
  (let [live-snap (atom (telemetry-snapshot))
        timer-id  (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (reset! timer-id
                (js/setInterval (fn []
                                  (reset! live-snap (telemetry-snapshot))
                                  (r/force-update this))
                                cfg/stats-refresh-interval-ms)))
      :component-will-unmount
      (fn [_this]
        (when-let [id @timer-id]
          (js/clearInterval id)
          (reset! timer-id nil)))
      :reagent-render
      (fn [{:keys [on-close]}]
        (let [snap         @live-snap
              tracks-map   (or (:active-tracks @audio-state) {})
              key-data     (:key @audio-state)
              key-str      (format-key key-data)
              scene-name   (name (:current-scene @visual-state :cyber-torus))
              telemetry    (merge snap {:key-str key-str :scene-name scene-name})]
          [:aside.neo-stats-card {:aria-label "System Audio Status"}
           [stats-header snap on-close]
           [:div.neo-body
            [telemetry-component telemetry]
            [routing-graph-component]
            [active-loops-component tracks-map]]
           [stats-footer]]))})))
