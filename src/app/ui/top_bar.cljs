(ns app.ui.top-bar
  "Top bar UI orchestrator from HUD."
  (:require
   [app.state :refer [audio-state]]
   [app.ui.top-bar.branding :refer [branding-component]]
   [app.ui.top-bar.controls :refer [controls-component]]
   [app.ui.top-bar.loops :refer [loops-component]]))

(defn top-bar-component [{:keys [toggle-track-mute!] :as props}]
  (let [active-tracks-map (or (:active-tracks @audio-state) {})
        tracks-ver        (:tracks-ver @audio-state 0)]
    [:header.hud-top {:role "banner" :aria-label "Studio Status and Controls"}
     [branding-component]
     [controls-component props]
     [loops-component active-tracks-map toggle-track-mute! tracks-ver]]))
