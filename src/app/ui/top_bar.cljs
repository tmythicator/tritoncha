(ns app.ui.top-bar
  "Top bar UI orchestrator from HUD."
  (:require
   [app.state :refer [audio-state]]
   [app.ui.top-bar.branding :refer [branding-component]]
   [app.ui.top-bar.controls :refer [master-deck-component tweaks-deck-component]]
   [app.ui.top-bar.loops :refer [loops-component]]))

(defn top-bar-component
  "Render structured studio console deck with Master row, Harmonic row, and separate Loops row."
  [{:keys [toggle-track-mute!] :as props}]
  (let [{:keys [active?]} @audio-state
        active-tracks-map (or (:active-tracks @audio-state) {})
        tracks-ver        (:tracks-ver @audio-state 0)]
    [:header.hud-top {:role "banner" :aria-label "Studio Status and Controls"}
     [branding-component]
     [:div.hud-deck
      [:div.hud-deck-row.hud-master-row
       [master-deck-component props]]
      (when active?
        [:div.hud-deck-row.hud-tweaks-row
         [tweaks-deck-component props]])]
     (when (and active? (seq active-tracks-map))
       [loops-component active-tracks-map toggle-track-mute! tracks-ver])]))
