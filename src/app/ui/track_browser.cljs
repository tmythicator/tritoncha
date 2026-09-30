(ns app.ui.track-browser
  "Modal track browser and preset preview selector for catalog tracks."
  (:require
   [app.audio.control.tracker :as tracker]
   [app.state :refer [audio-state]]
   [app.ui.common :as common]
   [app.ui.track-browser.list :refer [track-list]]))

(defn track-browser-component
  "Render track preset catalog browser modal dialog."
  [{:keys [on-close]}]
  (let [cur-jam (:current-jam @audio-state)
        jams    (tracker/jam-list)
        select! (fn [id]
                  (tracker/play-preset! id)
                  (when on-close (on-close)))]
    [:div.track-browser-modal
     {:role       "dialog"
      :aria-modal true
      :aria-label (str "Track Presets Library (" (count jams) " Tracks)")}
     [common/modal-header
      {:title       (str "TRACK PRESETS LIBRARY (" (count jams) " TRACKS)")
       :on-close    on-close
       :close-label "Close track browser"}]

     [track-list jams cur-jam select!]

     [common/modal-footer
      {:cmd   "> (jam! :preset-id)"
       :hint  "[Click any track or use ◀ ▶ in Top Bar]"
       :class "track-browser-footer"}]]))
