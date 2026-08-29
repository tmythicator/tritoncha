(ns app.ui.hud
  (:require
   [app.audio.control.looper :refer [toggle-click!]]
   [app.audio.control.mixer :refer [toggle-drums!]]
   [app.audio.control.tracker :refer [cycle-jam! toggle-play!]]
   [app.state :refer [audio-state engine-ctx ui-state]]
   [app.ui.bottom-bar :refer [bottom-bar-component]]
   [app.ui.mobile-notice :refer [mobile-notice-component]]
   [app.ui.stats-panel :refer [stats-panel-component]]
   [app.ui.top-bar :refer [top-bar-component]]
   [app.ui.tutorial-modal :refer [tutorial-modal-component]]
   [app.utils.dom :refer [mobile?]]
   [app.visuals.engine :refer [cycle-scene! toggle-wireframe!]]
   [reagent.dom.client :as rdom]))

(defn toggle-hud! []
  (swap! ui-state update :hud-visible? not))

(defn toggle-stats! []
  (swap! ui-state update :stats-visible? not))

(defn toggle-tutorial! []
  (swap! ui-state update :tutorial-visible? not))

(defn- toggle-track-mute! [track-key]
  (when-let [tr (get (:active-tracks @audio-state) (keyword track-key))]
    (swap! (:pattern tr) update :muted? not)))

(defn hud-component []
  (let [{:keys [hud-visible? stats-visible? tutorial-visible? mobile-notice-dismissed?]} @ui-state]
    [:div
     (when-not hud-visible?
       [:button.hud-restore-btn {:on-click toggle-hud!
                                 :aria-label "Restore HUD"
                                 :title "Click or press [H] to show HUD"}
        "[+] HUD"])

     [:div.minimal-hud {:class (when-not hud-visible? "hidden")}
      [top-bar-component {:toggle-play!       toggle-play!
                          :cycle-jam!         cycle-jam!
                          :cycle-scene!       cycle-scene!
                          :toggle-track-mute! toggle-track-mute!
                          :toggle-stats!      toggle-stats!
                          :toggle-tutorial!   toggle-tutorial!}]

      (when stats-visible?
        [stats-panel-component {:on-close toggle-stats!}])

      (when tutorial-visible?
        [tutorial-modal-component {:on-close toggle-tutorial!}])

      [:div.hud-bottom-area
       (when (and (mobile?) (not mobile-notice-dismissed?))
         [mobile-notice-component])
       [bottom-bar-component {:toggle-play!      toggle-play!
                              :cycle-jam!        cycle-jam!
                              :cycle-scene!      cycle-scene!
                              :toggle-drums!     toggle-drums!
                              :toggle-wireframe! toggle-wireframe!
                              :toggle-click!     toggle-click!
                              :toggle-stats!     toggle-stats!
                              :toggle-tutorial!  toggle-tutorial!
                              :toggle-hud!       toggle-hud!}]]]]))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (when-not (:root @engine-ctx)
      (swap! engine-ctx assoc :root (rdom/create-root el)))
    (rdom/render (:root @engine-ctx) [hud-component])))
