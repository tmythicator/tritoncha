(ns app.ui.bottom-bar
  (:require [app.state :refer [audio-state ui-state]]))

(defn bottom-bar-component [{:keys [toggle-tutorial! toggle-drums! toggle-click! toggle-instrument-browser! toggle-stats! toggle-hud!]}]
  (let [{:keys [drums-muted? active-tracks]} @audio-state
        {:keys [tutorial-visible? instrument-browser-open? stats-visible? hud-visible?]} @ui-state
        click-active? (contains? active-tracks :click)]
    [:footer.hud-bottom {:role "contentinfo" :aria-label "Performance shortcuts and author link"}
     [:div.hotkey-hints
      [:button.neo-action-btn {:on-click toggle-tutorial!
                               :class (when tutorial-visible? "active")
                               :aria-label "Toggle tutorial"}
       "[T] TUTORIAL"]
      [:button.neo-action-btn {:on-click toggle-drums!
                               :class (when drums-muted? "active-danger")
                               :aria-label "Toggle drum tracks"}
       (if drums-muted? "[D] DRUMLESS [ON]" "[D] DRUMLESS")]
      [:button.neo-action-btn {:on-click toggle-click!
                               :class (when click-active? "active")
                               :aria-label "Toggle click metronome"}
       "[C] CLICK"]
      [:button.neo-action-btn {:on-click toggle-instrument-browser!
                               :class (when instrument-browser-open? "active")
                               :aria-label "Toggle synth studio"}
       "[S] SYNTH STUDIO"]
      [:button.neo-action-btn {:on-click toggle-stats!
                               :class (when stats-visible? "active")
                               :aria-label "Toggle telemetry info stats"}
       "[I] INFO"]
      [:button.neo-action-btn {:on-click toggle-hud!
                               :class (when hud-visible? "active")
                               :aria-label "Toggle HUD"}
       "[H] HUD"]]

     [:div.neo-links-group
      [:a.neo-link-btn {:href "https://timcha.dev" :target "_blank" :rel "noreferrer"}
       [:span.hud-by "by "]
       "timcha.dev"]]]))



