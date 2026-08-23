(ns app.ui.hud
  (:require [reagent.dom.client :as rdom]
            [app.state :refer [ui-state visual-state engine-ctx]]
            [app.visuals.engine :as engine]
            [app.ui.top-bar :refer [top-bar-component]]
            [app.ui.bottom-bar :refer [bottom-bar-component]]
            [app.ui.stats-panel :refer [stats-panel-component]]
            [app.ui.tutorial-modal :refer [tutorial-modal-component]]))

(defn toggle-hud! []
  (swap! ui-state update :hud-visible? not))

(defn toggle-stats! []
  (swap! ui-state update :stats-visible? not))

(defn toggle-tutorial! []
  (swap! ui-state update :tutorial-visible? not))

(defn cycle-scene! []
  (let [all-keys (vec (keys (engine/all-scenes)))
        cur      (:current-scene @visual-state :cyber-torus)
        cur-name (name cur)
        idx      (or (first (keep-indexed (fn [i k] (when (= (name k) cur-name) i)) all-keys)) 0)
        next-s   (get all-keys (mod (inc idx) (count all-keys)) :cyber-torus)]
    (engine/scene! next-s)))

(defn hud-component []
  (let [{:keys [hud-visible? stats-visible? tutorial-visible?]} @ui-state]
    [:div.minimal-hud {:class (when-not hud-visible? "hidden")}
     [top-bar-component {:toggle-stats!    toggle-stats!
                         :toggle-tutorial! toggle-tutorial!
                         :cycle-scene!     cycle-scene!}]

     (when stats-visible?
       [stats-panel-component])

     (when tutorial-visible?
       [tutorial-modal-component {:on-close toggle-tutorial!}])

     [bottom-bar-component]]))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (when-not (:root @engine-ctx)
      (swap! engine-ctx assoc :root (rdom/create-root el)))
    (rdom/render (:root @engine-ctx) [hud-component])))
