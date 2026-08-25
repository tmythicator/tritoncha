(ns app.ui.mobile-notice
  "Notice for mobile devices."
  (:require
   [app.state :refer [ui-state]]))

(defn dismiss-mobile-notice!
  "Dismisses the mobile performance notice banner."
  []
  (swap! ui-state assoc :mobile-notice-dismissed? true))

(defn mobile-notice-component []
  [:div.mobile-notice {:role "status" :aria-live "polite"}
   [:div.mobile-notice-body
    [:span.mobile-notice-tag "DESKTOP RECOMMENDED"]
    [:span.mobile-notice-msg "For the best audio and 3D visual experience, desktop browsers are recommended."]]
   [:button.mobile-notice-btn {:on-click dismiss-mobile-notice!
                               :aria-label "Dismiss mobile notice"
                               :title "Dismiss"} "✕"]])
