(ns app.ui.top-bar.controls
  "Top bar engine playback and preset controls."
  (:require
   [app.audio.control.tracker :refer [default-track-key next-jam! prev-jam!]]
   [app.state :refer [audio-state visual-state]]
   [app.utils.audio :refer [format-key]]
   [clojure.string :as str]))

(defn controls-component [{:keys [toggle-play! cycle-jam! toggle-track-browser! cycle-scene!]}]
  (let [{:keys [active? current-jam bpm key]} @audio-state
        {:keys [current-scene]}               @visual-state
        jam-name   (-> (or current-jam (default-track-key)) name str/upper-case)
        scene-name (-> (or current-scene :cyber-torus) name str/upper-case)]
    [:div.top-bar-controls
     [:button.player-play-btn {:on-click toggle-play!
                               :class (when active? "active")
                               :aria-label (if active? "Stop Audio Engine" "Start Audio Engine")}
      (if active? "■ STOP ENGINE" "▶ PLAY ENGINE")]

     [:div.player-track-deck
      [:button.player-btn-nav {:on-click prev-jam!
                               :aria-label "Previous track preset"}
       "◄"]
      [:button.player-track-badge {:on-click (or toggle-track-browser! cycle-jam!)
                                   :aria-label "Open track browser"}
       (str "TRACK: " jam-name " ▾")]
      [:button.player-btn-nav {:on-click next-jam!
                               :aria-label "Next track preset"}
       "►"]]

     [:div.top-bar-tools
      [:span.neo-badge (str bpm " BPM")]
      [:span.neo-badge.badge-cyan (format-key key)]
      [:button.neo-btn-stats {:on-click cycle-scene!
                              :aria-label "Cycle 3D Scene"}
       (str "SCENE: " scene-name)]]]))
