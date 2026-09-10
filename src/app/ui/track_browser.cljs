(ns app.ui.track-browser
  "Modal track browser and preset preview selector for catalog tracks."
  (:require
   [app.audio.control.tracker :as tracker]
   [app.state :refer [audio-state]]
   [clojure.string :as str]))

(defn- format-scale [scale]
  (if (vector? scale)
    (let [[root mode] scale]
      (str (str/upper-case (name (or root :e))) " " (str/capitalize (name (or mode :minor)))))
    "Custom"))

(defn- track-header [on-close track-count]
  [:div.neo-header
   [:div.neo-title
    [:span.neo-prompt "> "]
    [:span (str "TRACK PRESETS LIBRARY (" track-count " TRACKS)")]]
   [:button.neo-btn-close {:on-click on-close
                           :aria-label "Close track browser"}
    "[X]"]])

(defn- track-item [{:keys [idx item active? on-select]}]
  (let [{:keys [id name bpm scale geom]} item
        num-str   (if (< (inc idx) 10) (str "0" (inc idx)) (str (inc idx)))
        scale-str (format-scale scale)]
    [:div.track-browser-item
     {:class    (when active? "active")
      :on-click #(on-select id)
      :title    (str "Click to play " name)}
     [:div.track-item-left
      [:span.track-item-num num-str]
      [:span.track-item-title {:class (when active? "active")} name]]

     [:div.track-item-right
      [:span.neo-badge (str bpm " BPM")]
      [:span.neo-badge scale-str]
      [:span.neo-badge (str/upper-case (clojure.core/name (or geom :torus)))]
      [:button.neo-btn-stats.track-item-btn {:class (when active? "active")}
       (if active? "PLAYING" "LOAD ▶")]]]))

(defn- track-footer []
  [:div.neo-footer.track-browser-footer
   [:span.neo-foot-cmd "> (jam! :preset-id)"]
   [:span.neo-foot-hint "[Click any track or use ◀ ▶ in Top Bar]"]])

(defn track-browser-component [{:keys [on-close]}]
  (let [cur-jam (:current-jam @audio-state)
        jams    (tracker/jam-list)
        select! (fn [id]
                  (tracker/play-preset! id)
                  (when on-close (on-close)))]
    [:div.track-browser-modal
     {:role       "dialog"
      :aria-modal true
      :aria-label (str "Track Presets Library (" (count jams) " Tracks)")}
     [track-header on-close (count jams)]

     [:div.track-browser-list
      (doall
       (map-indexed
        (fn [idx item]
          ^{:key (:id item)}
          [track-item {:idx       idx
                       :item      item
                       :active?   (= cur-jam (:id item))
                       :on-select select!}])
        jams))]

     [track-footer]]))
