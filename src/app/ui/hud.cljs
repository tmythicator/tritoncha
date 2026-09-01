(ns app.ui.hud
  (:require
   [app.audio.control.looper :refer [toggle-click!]]
   [app.audio.control.mixer :refer [toggle-drums!]]
   [app.audio.control.tracker :refer [cycle-jam! toggle-play!]]
   [app.state :refer [audio-state engine-ctx ui-state visual-state]]
   [app.ui.bottom-bar :refer [bottom-bar-component]]
   [app.ui.mobile-notice :refer [mobile-notice-component]]
   [app.ui.stats-panel :refer [stats-panel-component]]
   [app.ui.top-bar :refer [top-bar-component]]
   [app.ui.tutorial-modal :refer [tutorial-modal-component]]
   [app.utils.dom :refer [mobile?]]
   [app.visuals.engine :refer [cycle-scene! toggle-wireframe!]]
   [reagent.dom.client :as rdom]))

(declare request-ui-render!)

(defn toggle-hud! []
  (swap! ui-state update :hud-visible? not))

(defn toggle-stats! []
  (swap! ui-state update :stats-visible? not))

(defn toggle-tutorial! []
  (swap! ui-state update :tutorial-visible? not))

(defn- toggle-track-mute! [track-key]
  (when-let [tr (get (:active-tracks @audio-state) (keyword track-key))]
    (swap! (:pattern tr) update :muted? not)
    (swap! audio-state update :tracks-ver (fnil inc 0))))

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

(defonce ^:private render-scheduled? (atom false))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (if-let [root (:root @engine-ctx)]
      (rdom/render root [hud-component])
      (let [root (rdom/create-root el)]
        (swap! engine-ctx assoc :root root)
        (rdom/render root [hud-component])))))

(defn request-ui-render! []
  (when (and (exists? js/window) (not @render-scheduled?))
    (reset! render-scheduled? true)
    (js/requestAnimationFrame
     (fn []
       (reset! render-scheduled? false)
       (render-ui!)))))

(defonce ^:private _watches
  (do
    (add-watch ui-state :ui-render (fn [_ _ _ _] (request-ui-render!)))
    (add-watch audio-state :audio-ui-render (fn [_ _ _ _] (request-ui-render!)))
    (add-watch visual-state :visual-ui-render (fn [_ _ _ _] (request-ui-render!)))
    true))
