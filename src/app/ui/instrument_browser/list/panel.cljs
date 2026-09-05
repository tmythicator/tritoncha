(ns app.ui.instrument-browser.list.panel
  "Scrollable left instrument catalog panel and empty state."
  (:require
   [app.ui.instrument-browser.list.card :refer [instrument-card]]))

(defn- empty-query-view
  "Render empty state when search or category filter yields no results.
  Examples: [empty-query-view]."
  []
  [:div.inst-empty-view {:key "empty-query"}
   "No instruments match query"])

(defn instrument-list-panel
  "Render scrollable left instrument catalog panel.
  Examples: [instrument-list-panel filtered-instruments selected-key cat-key]."
  [filtered-insts cur-sel-key cat-key]
  (into
   [:div.inst-catalog-panel {:key (str "inst-list-" cat-key)}]
   (if (empty? filtered-insts)
     [[empty-query-view]]
     (for [[inst-key spec] filtered-insts]
       ^{:key (str inst-key)}
       [instrument-card inst-key spec (= inst-key cur-sel-key)]))))
