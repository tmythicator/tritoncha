(ns app.ui.instrument-browser.list
  "Left scrollable instrument catalog list and category filters facade."
  (:require
   [app.ui.instrument-browser.list.buttons :as buttons]
   [app.ui.instrument-browser.list.card :as card]
   [app.ui.instrument-browser.list.filters :as filters]
   [app.ui.instrument-browser.list.panel :as panel]))

(defn category-tabs
  "Render category filter tabs bar.
  Examples: [category-tabs]."
  []
  [filters/category-tabs])

(defn search-input
  "Render search input box.
  Examples: [search-input]."
  []
  [filters/search-input])

(defn card-audition-buttons
  "Render unified quick audition buttons on instrument catalog cards.
  Examples: [card-audition-buttons :saw-bass :bass false]."
  [inst-key cat poly?]
  [buttons/card-audition-buttons inst-key cat poly?])

(defn instrument-card
  "Render card for a single instrument in catalog list.
  Examples: [instrument-card :saw-bass spec selected?]."
  [inst-key spec selected?]
  [card/instrument-card inst-key spec selected?])

(defn instrument-list-panel
  "Render scrollable left instrument catalog panel.
  Examples: [instrument-list-panel filtered-instruments selected-key cat-key]."
  [filtered-insts cur-sel-key cat-key]
  [panel/instrument-list-panel filtered-insts cur-sel-key cat-key])
