(ns app.ui.hud
  (:require [reagent.dom.client :as rdom]
            [app.state :refer [ui-state audio-state visual-state engine-ctx]]
            [app.visuals.engine :as engine :refer [toggle-wireframe!]]
            [app.audio.mixer :refer [stop! toggle-mute! undrum! redrum!]]
            [app.audio.looper :refer [toggle-click!]]
            [app.audio.tracker :as tracker]
            [app.ui.top-bar :refer [top-bar-component]]
            [app.ui.bottom-bar :refer [bottom-bar-component]]
            [app.ui.stats-panel :refer [stats-panel-component]]
            [app.ui.tutorial-modal :refer [tutorial-modal-component]]))

(def jam-presets [:roller :sub-roller :acid-roller :ambient-drift])

(defn toggle-hud! []
  (swap! ui-state update :hud-visible? not))

(defn toggle-stats! []
  (swap! ui-state update :stats-visible? not))

(defn toggle-tutorial! []
  (swap! ui-state update :tutorial-visible? not))

(defn toggle-play! []
  (if (:active? @audio-state)
    (stop!)
    (tracker/play-preset! (:current-jam @audio-state :roller))))

(defn cycle-jam! []
  (let [cur (:current-jam @audio-state :roller)
        idx (or (first (keep-indexed (fn [i k] (when (= k cur) i)) jam-presets)) 0)
        next-jam (get jam-presets (mod (inc idx) (count jam-presets)) :roller)]
    (tracker/play-preset! next-jam)))

(defn cycle-scene! []
  (let [all-keys (vec (keys (engine/all-scenes)))
        cur      (:current-scene @visual-state :cyber-torus)
        cur-name (name cur)
        idx      (or (first (keep-indexed (fn [i k] (when (= (name k) cur-name) i)) all-keys)) 0)
        next-s   (get all-keys (mod (inc idx) (count all-keys)) :cyber-torus)]
    (engine/scene! next-s)))

(defn hud-component []
  (let [{:keys [hud-visible? stats-visible? tutorial-visible?]} @ui-state]
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
                          :toggle-track-mute! toggle-mute!
                          :toggle-stats!      toggle-stats!
                          :toggle-tutorial!   toggle-tutorial!}]

      (when stats-visible?
        [stats-panel-component])

      (when tutorial-visible?
        [tutorial-modal-component {:on-close toggle-tutorial!}])

      [bottom-bar-component {:toggle-play!      toggle-play!
                             :cycle-jam!        cycle-jam!
                             :cycle-scene!      cycle-scene!
                             :undrum!           undrum!
                             :redrum!           redrum!
                             :toggle-wireframe! toggle-wireframe!
                             :toggle-click!     toggle-click!
                             :toggle-stats!     toggle-stats!
                             :toggle-tutorial!  toggle-tutorial!
                             :toggle-hud!       toggle-hud!}]]]))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (when-not (:root @engine-ctx)
      (swap! engine-ctx assoc :root (rdom/create-root el)))
    (rdom/render (:root @engine-ctx) [hud-component])))
