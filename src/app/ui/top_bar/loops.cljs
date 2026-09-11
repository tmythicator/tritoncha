(ns app.ui.top-bar.loops
  "Top bar active loops monitor and interactive mute control tags subcomponent."
  (:require [clojure.string :as str]))

(defn- loop-tag-button [lk tr toggle-track-mute!]
  (let [pat    @(:pattern tr)
        muted? (boolean (:muted? pat))
        name-str (-> lk name str/upper-case)]
    [:button.loop-tag {:on-click #(toggle-track-mute! lk)
                       :class    (if muted? "muted" "active")
                       :aria-label (str (if muted? "Unmute " "Mute ") name-str " channel")}
     name-str]))

(defn loops-component
  ([active-tracks-map toggle-track-mute!]
   (loops-component active-tracks-map toggle-track-mute! 0))
  ([active-tracks-map toggle-track-mute! _tracks-ver]
   (let [active-loops (keys active-tracks-map)]
     (when (seq active-loops)
       [:div.top-bar-loops
        [:span.loop-tags-label "LOOPS:"]
        (into [:div.loop-tags]
              (for [lk active-loops
                    :let [tr     (get active-tracks-map lk)
                          muted? (boolean (:muted? @(:pattern tr)))]]
                ^{:key (str (name lk) "-" muted?)}
                [loop-tag-button lk tr toggle-track-mute!]))]))))

