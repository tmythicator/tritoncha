(ns app.ui.instrument-browser.list.panel
  "Scrollable left instrument catalog panel and empty state."
  (:require
   [app.ui.instrument-browser.list.card :refer [instrument-card]]))

(defn- empty-query-view
  "Render empty state when search or category filter yields no results."
  []
  [:div.inst-empty-view {:key "empty-query"}
   "No instruments match query"])

(defn instrument-list-panel
  "Render scrollable left instrument catalog panel with section header."
  [filtered-insts cur-sel-key cat-key]
  [:div.inst-catalog-section
   [:div.inst-section-bar
    [:div.inst-section-title-wrap
     [:span.inst-section-prompt "$ "]
     [:span.inst-section-title "SOUND CATALOG"]]
    [:span.inst-section-count (str (count filtered-insts) " sounds")]]
   (into
    [:div.inst-catalog-panel {:key (str "inst-list-" cat-key)}]
    (if (empty? filtered-insts)
      [[empty-query-view]]
      (for [[inst-key spec] filtered-insts]
        ^{:key (str inst-key)}
        [instrument-card inst-key spec (= inst-key cur-sel-key)])))])
