(ns app.ui.top-bar.controls
  "Top bar engine playback and preset controls."
  (:require
   [app.audio.control.tracker :refer [next-jam! prev-jam!]]
   [app.state :refer [audio-state ui-state visual-state]]
   [clojure.string :as str]))

(defn controls-component [{:keys [toggle-play! cycle-jam! toggle-track-browser! toggle-instrument-browser! cycle-scene! toggle-stats! toggle-tutorial!]}]
  (let [{:keys [stats-visible? tutorial-visible? instrument-browser-open?]} @ui-state
        {:keys [active? current-jam]} @audio-state
        {:keys [current-scene]}        @visual-state
        jam-name   (-> (or current-jam :roller) name str/upper-case)
        scene-name (-> (or current-scene :cyber-torus) name str/upper-case)]
    [:div.top-bar-controls
     [:button.neo-btn-stats.btn-engine {:on-click toggle-play!
                                        :class (when active? "active")
                                        :title "Click or press [Space] to Start / Stop Engine"}
      (if active? "ACTIVE [■]" "PLAY [▶]")]

     [:div.jam-rotator
      [:button.neo-btn-stats.btn-arrow {:on-click prev-jam!
                                        :aria-label "Previous track preset"
                                        :title "Previous Track"}
       "◀"]
      [:button.neo-btn-stats.badge-jam {:on-click (or toggle-track-browser! cycle-jam!)
                                        :aria-label "Open track browser or cycle preset"
                                        :title "Click to open 20-track library browser"}
       (str "JAM: " jam-name " ▾")]
      [:button.neo-btn-stats.btn-arrow {:on-click next-jam!
                                        :aria-label "Next track preset"
                                        :title "Next Track"}
       "▶"]]

     [:button.neo-btn-stats {:on-click toggle-instrument-browser!
                             :aria-label (if instrument-browser-open? "Close instrument studio" "Open instrument studio")
                             :class (when instrument-browser-open? "active")
                             :title "Click or press [K] to open Instrument Studio & Audition Lab"}
      "SYNTHS [K]"]

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
