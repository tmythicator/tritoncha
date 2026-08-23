(ns app.ui.stats.loops
  "Active audio loops monitor and track card subcomponent."
  (:require [app.audio.voices :as voices]))

(defn- bus-badge-class [bus-kw]
  (case bus-kw
    :drums  "bus-drums"
    :bass   "bus-bass"
    :space  "bus-space"
    :direct "bus-direct"
    "bus-master"))

(defn- track-card [kw info]
  (let [muted?   (boolean @(:muted? info))
        solo?    (boolean @(:solo? info))
        pat      @(:pattern info)
        step     (or (:step pat) "16n")
        dur      (or (:dur pat) step)
        inst     (or (:inst pat) kw)
        bus      (voices/instrument-bus inst)
        events   (or (:notes pat) (:hits pat) (:pattern pat))
        len      (if (sequential? events) (count events) 1)]
    [:div.neo-track-box {:class (cond solo? "box-solo" muted? "box-muted" :else "box-live")}
     [:div.track-main-line
      [:div.track-left
       [:span.track-name (name kw)]
       [:span.track-sep "/"]
       [:span.track-inst (name inst)]]
      [:div.track-right
       [:span.neo-bus-tag {:class (bus-badge-class bus)} (name bus)]
       [:span.track-state-badge {:class (cond solo? "badge-solo" muted? "badge-muted" :else "badge-live")}
        (cond solo? "SOLO" muted? "MUTED" :else "LIVE")]]]
     [:div.track-sub-line
      [:span.track-metric [:span.metric-label "grid "] [:span.metric-val step]]
      [:span.metric-dot "•"]
      [:span.track-metric [:span.metric-label "gate "] [:span.metric-val dur]]
      [:span.metric-dot "•"]
      [:span.track-metric [:span.metric-label "steps "] [:span.metric-val len]]]]))

(defn active-loops-component [tracks-map]
  (let [active-count (count tracks-map)]
    [:div.neo-section
     [:div.neo-section-label (str "# ACTIVE LOOPS (" active-count ")")]
     (if (empty? tracks-map)
       [:div.neo-empty "no active loops (use (play!) or (l! :track-name {...}))"]
       [:div.neo-tracks-grid {:tab-index 0
                              :role "region"
                              :aria-label "Active audio loops list"}
        (for [[kw info] tracks-map]
          ^{:key kw}
          [track-card kw info])])]))
