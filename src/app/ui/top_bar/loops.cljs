(ns app.ui.top-bar.loops
  "Top bar active loops monitor and interactive mute control tags subcomponent.")

(defn- loop-tag-button [lk tr toggle-track-mute!]
  (let [pat    @(:pattern tr)
        muted? (boolean (:muted? pat))]
    [:button.loop-tag {:on-click #(toggle-track-mute! lk)
                       :class    (if muted? "muted" "active")
                       :title    (str "Click to " (if muted? "unmute " "mute ") (name lk))}
     (str (if muted? "[M] " "") (name lk))]))

(defn loops-component
  ([active-tracks-map toggle-track-mute!]
   (loops-component active-tracks-map toggle-track-mute! 0))
  ([active-tracks-map toggle-track-mute! _tracks-ver]
   (let [active-loops (keys active-tracks-map)]
     (when (seq active-loops)
       [:div.top-bar-loops
        (into [:div.loop-tags]
              (for [lk active-loops
                    :let [tr     (get active-tracks-map lk)
                          muted? (boolean (:muted? @(:pattern tr)))]]
                ^{:key (str (name lk) "-" muted?)}
                [loop-tag-button lk tr toggle-track-mute!]))]))))
