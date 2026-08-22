(ns app.ui.bottom-bar)

(defn bottom-bar-component []
  [:footer.hud-bottom {:role "contentinfo" :aria-label "Performance shortcuts and author link"}
   [:div.neo-links-group
    [:span.hud-by "by "]
    [:a.neo-link-btn {:href "https://timcha.dev" :target "_blank" :rel "noreferrer"}
     "timcha.dev"]]

   [:div.hotkey-hints
    [:span "[Space] Jam"]
    [:span "[1-4] Presets"]
    [:span "[I] Stats"]
    [:span "[U] Undrum"]
    [:span "[R] Redrum"]
    [:span "[G] Geom"]
    [:span "[W] Wire"]
    [:span "[H] HUD"]]])
