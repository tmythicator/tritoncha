(ns app.ui.bottom-bar
  "HUD bottom bar navigation, performance shortcut triggers, and author info."
  (:require [app.state :refer [audio-state ui-state]]))

(defn- hotkey-button
  "Render hotkey shortcut action button with active and danger states.
  Examples: [hotkey-button {:label \"[T] TUTORIAL\" :on-click f :active? true :aria-label \"Toggle tutorial\"}]."
  [{:keys [label on-click active? danger? aria-label]}]
  [:button.neo-action-btn
   {:on-click   on-click
    :class      (cond danger? "active-danger" active? "active")
    :aria-label aria-label}
   label])

(defn- author-link
  "Render external author credit link.
  Examples: [author-link]."
  []
  [:div.neo-links-group
   [:a.neo-link-btn {:href "https://timcha.dev" :target "_blank" :rel "noreferrer"}
    [:span.hud-by "by "]
    "timcha.dev"]])

(defn bottom-bar-component
  "Render bottom HUD bar with quick performance keyboard shortcuts and developer info.
  Examples: [bottom-bar-component props]."
  [{:keys [toggle-tutorial! toggle-drums! toggle-click! toggle-instrument-browser! toggle-stats! toggle-hud!]}]
  (let [{:keys [drums-muted? active-tracks]} @audio-state
        {:keys [tutorial-visible? instrument-browser-open? stats-visible? hud-visible?]} @ui-state
        click-active? (contains? active-tracks :click)
        hotkeys       [{:label      "[T] TUTORIAL"
                        :on-click   toggle-tutorial!
                        :active?    tutorial-visible?
                        :aria-label "Toggle tutorial"}
                       {:label      (if drums-muted? "[D] DRUMLESS [ON]" "[D] DRUMLESS")
                        :on-click   toggle-drums!
                        :danger?    drums-muted?
                        :aria-label "Toggle drum tracks"}
                       {:label      "[C] CLICK"
                        :on-click   toggle-click!
                        :active?    click-active?
                        :aria-label "Toggle click metronome"}
                       {:label      "[S] SYNTH STUDIO"
                        :on-click   toggle-instrument-browser!
                        :active?    instrument-browser-open?
                        :aria-label "Toggle synth studio"}
                       {:label      "[I] INFO"
                        :on-click   toggle-stats!
                        :active?    stats-visible?
                        :aria-label "Toggle telemetry info stats"}
                       {:label      "[H] HUD"
                        :on-click   toggle-hud!
                        :active?    hud-visible?
                        :aria-label "Toggle HUD"}]]
    [:footer.hud-bottom {:role "contentinfo" :aria-label "Performance shortcuts and author link"}
     [:div.hotkey-hints
      (for [{:keys [label] :as btn} hotkeys]
        ^{:key label}
        [hotkey-button btn])]
     (author-link)]))
