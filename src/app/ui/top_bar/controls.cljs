(ns app.ui.top-bar.controls
  "Top bar engine playback and preset controls."
  (:require
   [app.state :refer [audio-state ui-state visual-state]]
   [clojure.string :as str]))

(defn controls-component [{:keys [toggle-play! cycle-jam! cycle-scene! toggle-stats! toggle-tutorial!]}]
  (let [{:keys [stats-visible? tutorial-visible?]} @ui-state
        {:keys [active? current-jam]} @audio-state
        {:keys [current-scene colors]} @visual-state
        mesh-color (:mesh colors "#00ffcc")
        jam-name   (-> (or current-jam :roller) name str/upper-case)
        scene-name (-> (or current-scene :cyber-torus) name str/upper-case)]
    [:div.top-bar-controls
     [:button.neo-btn-stats.btn-engine {:on-click toggle-play!
                                        :class (when active? "active")
                                        :title "Click or press [Space] to Start / Stop Engine"}
      (if active? "ACTIVE [■]" "PLAY [▶]")]

     [:button.neo-btn-stats.badge-jam {:on-click cycle-jam!
                                       :style {:border-color (or mesh-color "#ffffff")}
                                       :aria-label "Cycle track preset"
                                       :title "Click or press [1-4] to cycle Track Preset"}
      (str "JAM: " jam-name)]

     [:button.neo-btn-stats {:on-click cycle-scene!
                             :aria-label "Cycle 3D Scene Preset"
                             :title "Click or press [G] to cycle 3D Scene"}
      (str "SCENE: " scene-name)]

     [:button.neo-btn-stats {:on-click toggle-tutorial!
                             :aria-label (if tutorial-visible? "Close interactive tutorial" "Open interactive tutorial")
                             :class (when tutorial-visible? "active")}
      "CODE [T]"]

     [:button.neo-btn-stats {:on-click toggle-stats!
                             :aria-label (if stats-visible? "Close statistics modal" "Open statistics modal")
                             :class (when stats-visible? "active")}
      (str "STATS " (if stats-visible? "[-]" "[+]"))]]))
