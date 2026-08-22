(ns app.ui.top-bar
  (:require [clojure.string :as str]
            [app.state :refer [state active-tracks global-key]]))

(defn top-bar-component [{:keys [toggle-stats!]}]
  (let [{:keys [active? bpm current-jam stats-visible? mesh-color]} @state
        jam-name (-> (or current-jam :roller) name str/upper-case)
        {:keys [root mode]} @global-key
        active-loops-map @active-tracks
        active-loops (keys active-loops-map)]
    [:div.hud-top
     [:div.neo-brand-container
      [:div.neo-brand
       [:span.neo-prompt "> "]
       [:span.neo-title "TRITONCHA"]]

      [:div.neo-badge-group
       [:span.neo-badge (str bpm " BPM")]
       [:span.neo-badge.badge-cyan
        (str (str/upper-case (name (or root :e))) " " (str/upper-case (name (or mode :dorian))))]
       [:span.neo-badge.badge-jam {:style {:border-color (or mesh-color "#ffffff")}} jam-name]
       [:button.neo-btn-stats {:on-click toggle-stats!
                               :class (when stats-visible? "active")}
        (str "STATS " (if stats-visible? "[-]" "[+]"))]]]

     [:div.hud-info
      [:div.hud-tracks
       (when (seq active-loops)
         [:div.loop-tags
          (for [lk active-loops
                :let [tr (get active-loops-map lk)
                      muted? (boolean @(:muted? tr))]]
            ^{:key lk}
            [:span.loop-tag {:class (if muted? "muted" "active")}
             (str (if muted? "[M] " "") (name lk))])])
       [:span.status-hint (if active?
                            "ENGINE: ACTIVE"
                            "ENGINE: READY [SPACE]")]]]]))
