(ns app.ui.top-bar.branding
  "Top bar branding and musical status badge subcomponent."
  (:require
   [app.state :refer [audio-state]]
   [clojure.string :as str]))

(defn branding-component []
  (let [{:keys [bpm key]} @audio-state
        {:keys [root mode]} (or key {:root :e :mode :phrygian})]
    [:div.top-bar-branding
     [:div.neo-brand
      [:span.neo-prompt "> "]
      [:span.neo-title "TRITONCHA"]]

     [:div.top-bar-status
      [:span.neo-badge (str bpm " BPM")]
      [:span.neo-badge.badge-cyan
       (str (str/upper-case (name (or root :e))) " " (str/upper-case (name (or mode :dorian))))]]]))
