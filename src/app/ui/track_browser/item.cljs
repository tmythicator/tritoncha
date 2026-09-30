(ns app.ui.track-browser.item
  "Track card item row subcomponent for preset catalog list."
  (:require
   [app.ui.track-browser.formatters :as fmt]
   [clojure.string :as str]))

(defn- track-index-str
  "Format 0-based index into padded two-digit track number string."
  [idx]
  (let [n (inc idx)]
    (if (< n 10) (str "0" n) (str n))))

(defn track-item
  "Render a single track row in preset browser catalog."
  [{:keys [idx item active? on-select]}]
  (let [{:keys [id name bpm scale geom]} item
        num-str   (track-index-str idx)
        scale-str (fmt/format-scale scale)
        geom-str  (clojure.core/name (or geom :torus))]
    [:div.track-browser-item
     {:class    (when active? "active")
      :on-click #(on-select id)}
     [:div.track-item-info
      [:div.track-item-title-row
       [:span.track-item-num num-str]
       [:span.track-item-title {:class (when active? "active")} name]]
      [:div.track-item-meta
       [:span.track-meta-bpm (str bpm " BPM")]
       [:span.track-meta-dot "·"]
       [:span.track-meta-scale scale-str]
       [:span.track-meta-dot "·"]
       [:span.track-meta-geom (str/capitalize geom-str)]]]
     [:button.neo-btn-stats.track-item-btn {:class (when active? "active")}
      (if active? "PLAYING" "LOAD ▶")]]))
