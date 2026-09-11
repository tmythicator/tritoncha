(ns app.ui.top-bar.branding
  "Top bar branding component.")

(defn branding-component []
  [:div.top-bar-branding
   [:div.neo-brand
    [:span.neo-prompt "> "]
    [:span.neo-title "TRITONCHA"]]])

