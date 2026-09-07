(ns app.ui.instrument-browser
  "Interactive Instrument Studio and Audition Lab facade."
  (:require
   [app.audio.dsp.busses :as busses]
   [app.audio.dsp.instruments :as instruments]
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.inspector :as inspector]
   [app.ui.instrument-browser.list :as list-view]
   [app.ui.instrument-browser.state :as state]
   [clojure.string :as str]))

(defn stop-audition-loop!
  "Halt active audition looper playback.
  Examples: (stop-audition-loop!) -> nil."
  []
  (audition/stop-audition-loop!))

(defn instrument-browser-component
  "Render interactive instrument studio modal overlay.
  Examples: [instrument-browser-component {:on-close f}]."
  [{:keys [on-close]}]
  (let [_rev        @state/patch-revision
        cat-filter  @state/active-category
        query       (str/trim (str/lower-case @state/search-query))
        all-insts     (instruments/all-instruments)
        primary-insts (into {} (remove (fn [[_k spec]] (:legacy? spec))) all-insts)
        cur-sel-key   @state/selected-inst
        cur-spec      (instruments/resolve-instrument-spec cur-sel-key)
        drum?         (busses/drum? (or cur-spec cur-sel-key))
        looping?      @audition/audition-loop-active?
        filtered      (into []
                            (filter (fn [[k spec]]
                                      (let [nm (str/lower-case (name k))]
                                        (and (state/inst-matches-category? k spec cat-filter)
                                             (or (empty? query) (str/includes? nm query))))))
                            primary-insts)]
    [:div.instrument-browser-modal
     {:role       "dialog"
      :aria-modal true
      :aria-label "Instrument Studio and Audition Lab"}

     ;; Modal Header
     [:div.neo-header
      [:div.neo-title
       [:span.neo-prompt "> "]
       [:span "INSTRUMENT STUDIO + AUDITION LAB (" (count primary-insts) " SOUNDS)"]]
      [:button.neo-btn-close
       {:on-click   #(do (audition/stop-audition-loop!) (when on-close (on-close)))
        :aria-label "Close instrument lab"}
       "[X]"]]

     ;; Search and Category Filters Bar
     [:div.inst-filter-bar
      [list-view/category-tabs]
      [list-view/search-input]]

     ;; Main Content: Left Catalog List + Right Parameter Inspector
     [:div.inst-content-grid
      [list-view/instrument-list-panel filtered cur-sel-key cat-filter]
      [inspector/inspector-panel cur-sel-key cur-spec drum? looping?]]

     ;; Modal Footer
     [:div.neo-footer.inst-footer
      [:span.neo-foot-cmd.inst-foot-cmd
       "> (patch! " (str cur-sel-key) " :filter {:cutoff 2400})"]
      [:span.neo-foot-hint.inst-foot-hint
       "[♪ NOTE / ♫ RUN / ≋ ARP / ≈ CHORD to audition in real-time]"]]]))
