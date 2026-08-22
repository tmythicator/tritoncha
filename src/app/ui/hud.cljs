(ns app.ui.hud
  (:require [reagent.dom.client :as rdom]
            [app.state :refer [state root-container]]
            [app.visuals.engine :as engine]
            [app.ui.top-bar :refer [top-bar-component]]
            [app.ui.bottom-bar :refer [bottom-bar-component]]
            [app.ui.stats-panel :refer [stats-panel-component]]))

(defn toggle-hud! []
  (swap! state update :hud-visible? not))

(defn toggle-stats! []
  (swap! state update :stats-visible? not))

(defn cycle-scene! []
  (let [all-keys (vec (keys (engine/all-scenes)))
        cur      (:current-scene @state :cyber-torus)
        cur-name (name cur)
        idx      (or (first (keep-indexed (fn [i k] (when (= (name k) cur-name) i)) all-keys)) 0)
        next-s   (get all-keys (mod (inc idx) (count all-keys)) :cyber-torus)]
    (engine/scene! next-s)))

(defn hud-component []
  (let [{:keys [hud-visible? stats-visible?]} @state]
    [:div.minimal-hud {:class (when-not hud-visible? "hidden")}
     [top-bar-component {:toggle-stats! toggle-stats!
                         :cycle-scene!  cycle-scene!}]

     (when stats-visible?
       [stats-panel-component])

     [bottom-bar-component]]))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (when-not @root-container
      (reset! root-container (rdom/create-root el)))
    (rdom/render @root-container [hud-component])))
