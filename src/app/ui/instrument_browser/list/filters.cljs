(ns app.ui.instrument-browser.list.filters
  "Search input and category filter tabs for instrument catalog list."
  (:require
   [app.audio.dsp.instruments :as instruments]
   [app.ui.instrument-browser.state :as state]))

(def ^:private category-tab-specs
  [[:all "ALL"]
   [:bass "BASS"]
   [:pads "PADS"]
   [:leads "LEADS"]
   [:drums "DRUMS"]
   [:fx "FX"]
   [:custom "CUSTOM"]
   [:session "SESSION"]])

(defn- select-category!
  "Update active category filter and synchronize selected instrument cursor.
  Examples: (select-category! :leads) -> nil."
  [cat-key]
  (reset! state/active-category cat-key)
  (let [all-insts (instruments/all-instruments)
        matches   (filter (fn [[k spec]]
                            (state/inst-matches-category? k spec cat-key))
                          all-insts)]
    (when (seq matches)
      (when-not (state/inst-matches-category? @state/selected-inst
                                              (instruments/resolve-instrument-spec @state/selected-inst)
                                              cat-key)
        (reset! state/selected-inst (first (first matches)))))))

(defn category-tabs
  "Render category filter tabs bar.
  Examples: [category-tabs]."
  []
  (let [cat-filter @state/active-category]
    [:div.inst-tabs-container
     (for [[cat-key label] category-tab-specs]
       (let [active? (= cat-filter cat-key)]
         ^{:key (str cat-key)}
         [:button.neo-btn-stats.inst-tab-btn
          {:class    (when active? "active")
           :on-click #(select-category! cat-key)}
          label]))]))

(defn search-input
  "Render sound search input box.
  Examples: [search-input]."
  []
  [:input.inst-search-input
   {:type        "text"
    :placeholder "Filter sounds..."
    :value       @state/search-query
    :on-change   #(reset! state/search-query (.. % -target -value))}])
