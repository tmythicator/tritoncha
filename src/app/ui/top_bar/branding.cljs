(ns app.ui.top-bar.branding
  "Top bar branding component."
  (:require [app.state :refer [audio-state]]))

(defn branding-component
  "Render top bar brand title and real-time audio engine status dot indicator.
  Examples: [branding-component]."
  []
  (let [active? (boolean (:active? @audio-state))]
    [:div.top-bar-branding
     [:div.neo-brand
      [:span.neo-prompt "> "]
      [:span.neo-title "TRITONCHA"]
      [:span.neo-brand-dot
       {:class (if active? "active" "idle")
        :title (if active? "Audio Engine: Running" "Audio Engine: Stopped")}]]]))
