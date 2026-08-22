(ns app.ui.hud
  (:require [reagent.dom.client :as rdom]
            [app.state :refer [state root-container]]
            [app.ui.top-bar :refer [top-bar-component]]
            [app.ui.bottom-bar :refer [bottom-bar-component]]
            [app.ui.stats-panel :refer [stats-panel-component]]))

(defn toggle-hud! []
  (swap! state update :hud-visible? not))

(defn toggle-stats! []
  (swap! state update :stats-visible? not))

(defn hud-component []
  (let [{:keys [hud-visible? stats-visible?]} @state]
    [:div.minimal-hud {:class (when-not hud-visible? "hidden")}
     [top-bar-component {:toggle-stats! toggle-stats!}]

     (when stats-visible?
       [stats-panel-component])

     [bottom-bar-component]]))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (when-not @root-container
      (reset! root-container (rdom/create-root el)))
    (rdom/render @root-container [hud-component])))
