(ns app.ui.stats-panel
  "Audio engine statistics panel coordinating telemetry, DSP graph, and loops monitors."
  (:require
   [app.audio.dsp.telemetry :refer [telemetry-snapshot]]
   [app.config :as cfg]
   [app.state :refer [audio-state]]
   [app.ui.common :as common]
   [app.ui.stats.bus-mixer :refer [bus-mixer-component]]
   [app.ui.stats.loops :refer [active-loops-component]]
   [app.ui.stats.routing-graph :refer [routing-graph-component]]
   [app.ui.stats.telemetry :refer [telemetry-component]]
   [reagent.core :as r]))

(defn stats-panel-component
  "Render system audio status and telemetry diagnostic panel."
  [_props]
  (let [live-snap (r/atom (telemetry-snapshot))
        timer-id  (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [_this]
        (reset! timer-id
                (js/setInterval (fn []
                                  (reset! live-snap (telemetry-snapshot)))
                                cfg/stats-refresh-interval-ms)))
      :component-will-unmount
      (fn [_this]
        (when-let [id @timer-id]
          (js/clearInterval id)
          (reset! timer-id nil)))
      :reagent-render
      (fn [{:keys [on-close]}]
        (let [snap         @live-snap
              st           (or (:ctx-state snap) "uninitialized")
              tracks-map   (or (:active-tracks @audio-state) {})]
          [:aside.neo-stats-card {:aria-label "System Audio Status"}
           [common/modal-header
            {:title         "SYSTEM AUDIO STATUS"
             :on-close      on-close
             :close-label   "Close stats modal"
             :right-content [common/status-badge {:online? (= st "running")}]}]
           [:div.neo-body
            [telemetry-component snap]
            [bus-mixer-component]
            [routing-graph-component]
            [active-loops-component tracks-map]]
           [common/modal-footer
            {:cmd  "> ./tritoncha --stats"
             :hint "[Press I or click [X] to close]"}]]))})))
