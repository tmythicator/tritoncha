(ns app.ui.bottom-bar
  (:require [app.state :refer [audio-state]]))

(defn bottom-bar-component [{:keys [toggle-play! cycle-jam! cycle-scene! undrum! redrum! toggle-wireframe! toggle-click! toggle-stats! toggle-tutorial! toggle-hud!]}]
  (let [{:keys [active?]} @audio-state]
    [:footer.hud-bottom {:role "contentinfo" :aria-label "Performance shortcuts and author link"}
     [:div.neo-links-group
      [:span.hud-by "by "]
      [:a.neo-link-btn {:href "https://timcha.dev" :target "_blank" :rel "noreferrer"}
       "timcha.dev"]]

     [:div.hotkey-hints
      [:button.neo-action-btn {:on-click toggle-play!
                              :class (when active? "active")
                              :title "Play or Stop Engine (Space)"}
       (if active? "[■ Stop]" "[▶ Jam]")]
      [:button.neo-action-btn {:on-click cycle-jam! :title "Switch Track Preset (1-4)"} "[1-4] Jam"]
      [:button.neo-action-btn {:on-click undrum! :title "Mute Drums (U)"} "[U] Undrum"]
      [:button.neo-action-btn {:on-click redrum! :title "Unmute Drums (R)"} "[R] Redrum"]
      [:button.neo-action-btn {:on-click cycle-scene! :title "Cycle 3D Scene (G)"} "[G] Scene"]
      [:button.neo-action-btn {:on-click toggle-wireframe! :title "Toggle Wireframe (W)"} "[W] Wire"]
      [:button.neo-action-btn {:on-click toggle-click! :title "Toggle Metronome Click (C)"} "[C] Click"]
      [:button.neo-action-btn {:on-click toggle-tutorial! :title "Toggle Tutorial (T)"} "[T] Code"]
      [:button.neo-action-btn {:on-click toggle-stats! :title "Toggle Stats (I)"} "[I] Stats"]
      [:button.neo-action-btn {:on-click toggle-hud! :title "Toggle HUD (H)"} "[H] HUD"]]]))
