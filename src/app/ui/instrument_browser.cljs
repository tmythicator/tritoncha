(ns app.ui.instrument-browser
  "Interactive Instrument Studio and Audition Lab facade."
  (:require
   [app.audio.dsp.instruments :as inst]
   [app.ui.common :as common]
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.inspector :as inspector]
   [app.ui.instrument-browser.list :as list-view]
   [app.ui.instrument-browser.state :as state]
   [clojure.string :as str]))

(defn stop-audition-loop!
  "Halt active audition looper playback."
  []
  (audition/stop-audition-loop!))

(defn instrument-browser-component
  "Render interactive instrument studio modal overlay."
  [{:keys [on-close]}]
  (let [cat-filter    @state/active-category
        query       (str/trim (str/lower-case @state/search-query))
        all-insts     (inst/all-instruments)
        primary-insts all-insts
        cur-sel-key   @state/selected-inst
        cur-spec      (inst/resolve-instrument-spec cur-sel-key)
        drum?         (inst/drum? (or cur-spec cur-sel-key))
        looping?      @audition/audition-loop-active?
        filtered      (into []
                            (filter (fn [[k spec]]
                                      (let [nm    (str/lower-case (name k))
                                            title (str/lower-case (str (:title spec)))]
                                        (and (state/inst-matches-category? k spec cat-filter)
                                             (or (empty? query)
                                                 (str/includes? nm query)
                                                 (str/includes? title query))))))
                            primary-insts)]
    [:div.instrument-browser-modal
     {:role       "dialog"
      :aria-modal true
      :aria-label "Synth Studio"}

     ;; Modal Header
     [common/modal-header
      {:title       (str "SYNTH STUDIO (" (count primary-insts) " SOUNDS)")
       :on-close    #(do (audition/stop-audition-loop!) (when on-close (on-close)))
       :close-label "Close synth studio"}]

     ;; Search and Category Filters Bar
     [:div.inst-filter-bar
      [list-view/category-tabs]
      [list-view/search-input]]

     ;; Main Content: Left Catalog List + Right Parameter Inspector
     [:div.inst-content-grid
      [list-view/instrument-list-panel filtered cur-sel-key cat-filter]
      [inspector/inspector-panel cur-sel-key cur-spec drum? looping?]]

     ;; Modal Footer
     [common/modal-footer
      {:cmd        (str "> (patch! " cur-sel-key " :filter {:cutoff 2400})")
       :hint       "[♪ NOTE / ♫ RUN / ≋ ARP / ≈ CHORD to audition in real-time]"
       :class      "inst-footer"
       :cmd-class  "inst-foot-cmd"
       :hint-class "inst-foot-hint"}]]))
