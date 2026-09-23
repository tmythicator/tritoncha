(ns app.ui.track-browser.list
  "Scrollable list container for catalog track items."
  (:require [app.ui.track-browser.item :refer [track-item]]))

(defn track-list
  "Render list of catalog tracks.
  Examples: [track-list jams cur-jam on-select]."
  [jams cur-jam on-select]
  (into [:div.track-browser-list]
        (map-indexed
         (fn [idx item]
           ^{:key (:id item)}
           [track-item {:idx       idx
                        :item      item
                        :active?   (= cur-jam (:id item))
                        :on-select on-select}])
         jams)))
