(ns app.ui.hud
  (:require
   [app.audio.control.looper :refer [toggle-click!]]
   [app.audio.control.mixer :refer [mute! toggle-drums! unmute!]]
   [app.audio.control.tracker :refer [cycle-jam! toggle-play!]]
   [app.state :refer [audio-state engine-ctx ui-state]]
   [app.ui.bottom-bar :refer [bottom-bar-component]]
   [app.ui.instrument-browser :refer [instrument-browser-component]]
   [app.ui.mobile-notice :refer [mobile-notice-component]]
   [app.ui.stats-panel :refer [stats-panel-component]]
   [app.ui.top-bar :refer [top-bar-component]]
   [app.ui.track-browser :refer [track-browser-component]]
   [app.ui.tutorial-modal :refer [tutorial-modal-component]]
   [app.utils.dom :refer [mobile?]]
   [app.visuals.engine :refer [cycle-scene! toggle-wireframe!]]
   [reagent.dom.client :as rdom]))

(declare render-ui!)

(defn toggle-hud! []
  (swap! ui-state update :hud-visible? not))

(defn toggle-stats! []
  (swap! ui-state update :stats-visible? not))

(defn toggle-tutorial! []
  (swap! ui-state update :tutorial-visible? not))

(defn toggle-track-browser! []
  (swap! ui-state update :track-browser-open? not))

(defn toggle-instrument-browser! []
  (swap! ui-state update :instrument-browser-open? not))

(defn- toggle-track-mute! [track-key]
  (let [kw (keyword track-key)]
    (when-let [tr (get (:active-tracks @audio-state) kw)]
      (if (:muted? @(:pattern tr))
        (unmute! kw)
        (mute! kw)))))

(defn hud-component []
  (let [{:keys [hud-visible? stats-visible? tutorial-visible? track-browser-open? instrument-browser-open? mobile-notice-dismissed?]} @ui-state]
    [:div
     (when-not hud-visible?
       [:button.hud-restore-btn {:on-click toggle-hud!
                                 :aria-label "Restore HUD"
                                 :title "Click or press [H] to show HUD"}
        "[+] HUD"])

     [:div.minimal-hud {:class (when-not hud-visible? "hidden")}
      [top-bar-component {:toggle-play!               toggle-play!
                          :cycle-jam!                 cycle-jam!
                          :toggle-track-browser!      toggle-track-browser!
                          :toggle-instrument-browser! toggle-instrument-browser!
                          :cycle-scene!               cycle-scene!
                          :toggle-track-mute!         toggle-track-mute!
                          :toggle-stats!              toggle-stats!
                          :toggle-tutorial!           toggle-tutorial!}]

      (when stats-visible?
        [stats-panel-component {:on-close toggle-stats!}])

      (when tutorial-visible?
        [tutorial-modal-component {:on-close toggle-tutorial!}])

      (when track-browser-open?
        [track-browser-component {:on-close toggle-track-browser!}])

      (when instrument-browser-open?
        [instrument-browser-component {:on-close toggle-instrument-browser!}])

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
    (if-let [root (:root @engine-ctx)]
      (rdom/render root [hud-component])
      (let [root (rdom/create-root el)]
        (swap! engine-ctx assoc :root root)
        (rdom/render root [hud-component])))))
