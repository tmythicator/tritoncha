(ns app.ui.top-bar.loops
  "Top bar active loops monitor and interactive mute control tags subcomponent.")

(defn- loop-tag-button [lk tr toggle-track-mute!]
  (let [muted? (boolean (:muted? @(:pattern tr)))]
    [:button.loop-tag {:on-click #(toggle-track-mute! lk)
                       :class (if muted? "muted" "active")
                       :title (str "Click to " (if muted? "unmute " "mute ") (name lk))}
     (str (if muted? "[M] " "") (name lk))]))

(defn loops-component [active-tracks-map toggle-track-mute!]
  (let [active-loops (keys active-tracks-map)]
    (when (seq active-loops)
      [:div.top-bar-loops
       (into [:div.loop-tags]
             (for [lk active-loops
                   :let [tr (get active-tracks-map lk)]]
               ^{:key lk}
               [loop-tag-button lk tr toggle-track-mute!]))])))
