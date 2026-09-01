(ns app.ui.bottom-bar
  (:require [app.state :refer [audio-state ui-state visual-state]]))

(defn bottom-bar-component [{:keys [toggle-play! cycle-jam! cycle-scene! toggle-drums! toggle-wireframe! toggle-click! toggle-stats! toggle-tutorial! toggle-hud!]}]
  (let [{:keys [active? drums-muted? active-tracks]} @audio-state
        {:keys [stats-visible? tutorial-visible? hud-visible?]} @ui-state
        {:keys [wireframe?]} @visual-state
        click-active? (contains? active-tracks :click)]
    [:footer.hud-bottom {:role "contentinfo" :aria-label "Performance shortcuts and author link"}
     [:div.neo-links-group
      [:span.hud-by "by "]
      [:a.neo-link-btn {:href "https://timcha.dev" :target "_blank" :rel "noreferrer"}
       "timcha.dev"]]

     [:div.hotkey-hints
      [:button.neo-action-btn {:on-click toggle-play!
                               :class (when active? "active-success")
                               :title "Play or Stop Engine (Space)"}
       (if active? "[■ Stop]" "[▶ Jam]")]
      [:button.neo-action-btn {:on-click cycle-jam! :title "Switch Track Preset (1-4)"} "[1-4] Jam"]
      [:button.neo-action-btn {:on-click toggle-drums!
                               :class (when drums-muted? "active-danger")
                               :title "Toggle Drums Undrum / Redrum (D)"}
       (if drums-muted? "[D] Drumless [ON]" "[D] Drumless")]
      [:button.neo-action-btn {:on-click cycle-scene! :title "Cycle 3D Scene (G)"} "[G] Scene"]
      [:button.neo-action-btn {:on-click toggle-wireframe!
                               :class (when wireframe? "active")
                               :title "Toggle Wireframe (W)"}
       "[W] Wire"]
      [:button.neo-action-btn {:on-click toggle-click!
                               :class (when click-active? "active")
                               :title "Toggle Metronome Click (C)"}
       "[C] Click"]
      [:button.neo-action-btn {:on-click toggle-tutorial!
                               :class (when tutorial-visible? "active")
                               :title "Toggle Tutorial (T)"}
       "[T] Code"]
      [:button.neo-action-btn {:on-click toggle-stats!
                               :class (when stats-visible? "active")
                               :title "Toggle Stats (I)"}
       "[I] Stats"]
      [:button.neo-action-btn {:on-click toggle-hud!
                               :class (when hud-visible? "active")
                               :title "Toggle HUD (H)"}
       "[H] HUD"]]]))
