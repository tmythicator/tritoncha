(ns app.ui.top-bar.branding
  "Top bar branding and musical status badge subcomponent."
  (:require
   [app.state :refer [audio-state]]
   [app.utils.audio :refer [format-key]]))

(defn branding-component []
  (let [{:keys [bpm key]} @audio-state]
    [:div.top-bar-branding
     [:div.neo-brand
      [:span.neo-prompt "> "]
      [:span.neo-title "TRITONCHA"]]

     [:div.top-bar-status
      [:span.neo-badge (str bpm " BPM")]
      [:span.neo-badge.badge-cyan (format-key key)]]]))
