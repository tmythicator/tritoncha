(ns app.ui.top-bar
  (:require [clojure.string :as str]
            [app.state :refer [ui-state audio-state visual-state]]))

(defn top-bar-component [{:keys [toggle-stats! toggle-tutorial! cycle-scene!]}]
  (let [{:keys [stats-visible? tutorial-visible?]} @ui-state
        {:keys [active? bpm current-jam key active-tracks]} @audio-state
        {:keys [current-scene colors]} @visual-state
        mesh-color (:mesh colors "#00ffcc")
        {:keys [root mode]} (or key {:root :e :mode :phrygian})
        jam-name   (-> (or current-jam :roller) name str/upper-case)
        scene-name (-> (or current-scene :cyber-torus) name str/upper-case)
        active-loops-map (or active-tracks {})
        active-loops (keys active-loops-map)]
    [:header.hud-top {:role "banner" :aria-label "Studio Status and Controls"}
     [:div.neo-brand-container
      [:div.neo-brand
       [:span.neo-prompt "> "]
       [:span.neo-title "TRITONCHA"]]

      [:div.neo-badge-group
       [:span.neo-badge (str bpm " BPM")]
       [:span.neo-badge.badge-cyan
        (str (str/upper-case (name (or root :e))) " " (str/upper-case (name (or mode :dorian))))]
       [:span.neo-badge.badge-jam {:style {:border-color (or mesh-color "#ffffff")}} jam-name]
       [:button.neo-btn-stats {:on-click cycle-scene!
                               :aria-label "Cycle 3D Scene Preset"
                               :title "Click or press [G] to cycle 3D Scene"}
        (str "SCENE: " scene-name)]
       [:button.neo-btn-stats {:on-click toggle-tutorial!
                               :aria-label (if tutorial-visible? "Close interactive tutorial" "Open interactive tutorial")
                               :class (when tutorial-visible? "active")}
        "TUTORIAL [T]"]
       [:button.neo-btn-stats {:on-click toggle-stats!
                               :aria-label (if stats-visible? "Close statistics modal" "Open statistics modal")
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
