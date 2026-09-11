(ns app.ui.stats.loops
  "Active audio loops monitor and track card subcomponent with volume control."
  (:require [app.audio.control.looper :as looper]
            [app.audio.control.mixer :as mixer]
            [app.audio.dsp.busses :as busses]))

(defn- bus-badge-class [bus-kw]
  (case (busses/normalize-bus-key bus-kw)
    :bus/drums  "bus-drums"
    :bus/bass   "bus-bass"
    :bus/lead   "bus-lead"
    :bus/space  "bus-space"
    :bus/direct "bus-direct"
    "bus-master"))

(defn- format-vel [vel]
  (cond
    (number? vel)
    (.toFixed (js/parseFloat vel) 1)

    (sequential? vel)
    (if (seq vel)
      (str (.toFixed (js/parseFloat (first vel)) 1) "..")
      "0.9")

    :else "0.9"))

(defn- track-card [kw info]
  (let [pat      @(:pattern info)
        muted?   (boolean (:muted? pat))
        solo?    (boolean (:solo? pat))
        step     (or (:step pat) "16n")
        dur      (or (:dur pat) step)
        inst     (or (:inst pat) kw)
        bus      (busses/instrument-bus inst)
        events   (or (:notes pat) (:hits pat) (:pattern pat))
        len      (if (sequential? events) (count events) 1)
        raw-vel  (:vel pat 0.9)
        num-vel  (if (number? raw-vel) (js/parseFloat raw-vel) 0.9)
        vel-str  (format-vel raw-vel)]
    [:div.neo-track-box {:class (cond solo? "box-solo" muted? "box-muted" :else "box-live")}
     [:div.track-main-line
      [:div.track-left
       [:span.track-name (name kw)]
       [:span.track-sep "/"]
       [:span.track-inst (name inst)]]
      [:div.track-right
       [:span.neo-bus-tag {:class (bus-badge-class bus)} (name bus)]
       [:button.neo-track-btn.btn-mute
        {:class (when muted? "active")
         :title (if muted? "Unmute loop" "Mute loop")
         :on-click #(if muted? (mixer/unmute! kw) (mixer/mute! kw))}
        "M"]
       [:button.neo-track-btn.btn-solo
        {:class (when solo? "active")
         :title (if solo? "Unsolo loop" "Solo loop")
         :on-click #(if solo? (mixer/unsolo!) (mixer/solo! kw))}
        "S"]
       [:span.track-state-badge {:class (cond solo? "badge-solo" muted? "badge-muted" :else "badge-live")}
        (cond solo? "SOLO" muted? "MUTED" :else "LIVE")]]]

     ;; Interactive volume / gain control line
     [:div.track-vol-line
      [:div.track-vol-left
       [:span.vol-label "VOL "]
       [:span.vol-val (str vel-str "x")]]
      [:input.track-vol-slider
       {:type "range"
        :min "0.0"
        :max "2.0"
        :step "0.05"
        :value num-vel
        :on-change #(looper/set-track-vel! kw (.. % -target -value))}]
      [:div.track-vol-presets
       [:button.vol-preset-btn {:on-click #(looper/set-track-vel! kw 0.5)} "0.5x"]
       [:button.vol-preset-btn {:on-click #(looper/set-track-vel! kw 1.0)} "1.0x"]
       [:button.vol-preset-btn {:on-click #(looper/set-track-vel! kw 1.5)} "1.5x"]
       [:button.vol-preset-btn {:on-click #(looper/set-track-vel! kw 2.0)} "2.0x"]]]

     [:div.track-sub-line
      [:span.track-metric [:span.metric-label "grid "] [:span.metric-val step]]
      [:span.metric-dot "•"]
      [:span.track-metric [:span.metric-label "gate "] [:span.metric-val dur]]
      [:span.metric-dot "•"]
      [:span.track-metric [:span.metric-label "steps "] [:span.metric-val len]]]]))

(defn active-loops-component [tracks-map]
  (let [active-count (count tracks-map)]
    [:div.neo-section.active-loops-section
     [:div.neo-section-label (str "# ACTIVE LOOPS (" active-count ")")]
     (if (empty? tracks-map)
       [:div.neo-empty "no active loops (use (play!) or (l! :track-name {...}))"]
       (into [:div.neo-tracks-grid {:tab-index 0
                                    :role "region"
                                    :aria-label "Active audio loops list"}]
             (for [[kw info] tracks-map]
               ^{:key kw}
               [track-card kw info])))]))
