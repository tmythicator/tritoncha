(ns app.ui.top-bar
  "Top bar UI orchestrator from HUD."
  (:require
   [app.state :refer [audio-state]]
   [app.ui.top-bar.branding :refer [branding-component]]
   [app.ui.top-bar.controls :refer [master-deck-component tweaks-deck-component]]
   [app.ui.top-bar.loops :refer [loops-component]]))

(defn top-bar-component
  "Render structured studio console deck with Master row and Harmonic/Loop row.
  Examples: [top-bar-component props]."
  [{:keys [toggle-track-mute!] :as props}]
  (let [active-tracks-map (or (:active-tracks @audio-state) {})
        tracks-ver        (:tracks-ver @audio-state 0)]
    [:header.hud-top {:role "banner" :aria-label "Studio Status and Controls"}
     [:div.hud-deck
      ;; Strip 1: Master Engine, Track Preset Scrubber, and 3D Visual Scene
      [:div.hud-deck-row.hud-master-row
       [branding-component]
       [:div.hud-deck-divider]
       [master-deck-component props]]

      ;; Strip 2: Harmonic Tuning, Tempo Slider, Preset Reset, and Loop Channels
      [:div.hud-deck-row.hud-tweaks-row
       [tweaks-deck-component props]
       (when (seq active-tracks-map)
         [:div.hud-deck-divider])
       [loops-component active-tracks-map toggle-track-mute! tracks-ver]]]]))
