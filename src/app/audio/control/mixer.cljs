(ns app.audio.control.mixer
  "Audio bus mixer, levels, track mutes, solo, and performance undrum/redrum."
  (:require [app.audio.dsp.busses :as busses :refer [set-bus-gain!]]
            [app.config :as cfg]
            [app.state :refer [audio-state]]
            [app.utils.audio :refer [is-drum-track?]]))

(defn set-volume!
  "Sets the gain volume of a specific audio bus in decibels.
  Examples: (set-volume! :bus/drums -3), (set-volume! :bus/bass 0)."
  ([bus-key db-val] (set-volume! bus-key db-val cfg/default-ramp-time))
  ([bus-key db-val ramp-time]
   (let [b-key (busses/normalize-bus-key bus-key)]
     (when (busses/valid-bus? b-key)
       (swap! audio-state assoc-in [:bus-levels b-key] db-val)
       (set-bus-gain! b-key db-val ramp-time)))))

(defn mute-bus!
  "Mutes an audio bus."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-mutes b-key] true)
      (set-bus-gain! b-key cfg/mute-db cfg/default-ramp-time))))

(defn unmute-bus!
  "Unmutes an audio bus."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-mutes b-key] false)
      (let [target-db (get-in @audio-state [:bus-levels b-key] 0.0)]
        (set-bus-gain! b-key target-db cfg/default-ramp-time)))))

(defn toggle-bus!
  "Toggles the mute state of an audio bus."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)
        muted? (get-in @audio-state [:bus-mutes b-key] false)]
    (if muted?
      (unmute-bus! b-key)
      (mute-bus! b-key))))

(defn mute!
  "Mutes one or more active tracks by keyword.
  Examples: (mute! :kick :snare), (mute! :pad)."
  [& track-keys]
  (doseq [k track-keys]
    (when-let [tr (get (:active-tracks @audio-state) (keyword k))]
      (swap! (:pattern tr) assoc :muted? true)))
  (keys (:active-tracks @audio-state)))

(defn unmute!
  "Unmutes one or more active tracks by keyword.
  Examples: (unmute! :kick :snare), (unmute! :all)."
  [& track-keys]
  (if (or (empty? track-keys) (= (first track-keys) :all))
    (doseq [[_ tr] (:active-tracks @audio-state)]
      (swap! (:pattern tr) assoc :muted? false))
    (doseq [k track-keys]
      (when-let [tr (get (:active-tracks @audio-state) (keyword k))]
        (swap! (:pattern tr) assoc :muted? false))))
  (keys (:active-tracks @audio-state)))

(defn solo!
  "Solos one or more tracks, muting everything else.
  Examples: (solo! :bass :sub), (solo! :kick)."
  [& track-keys]
  (let [solo-set (set (map keyword track-keys))]
    (swap! audio-state assoc :solo-mode? true)
    (doseq [[k tr] (:active-tracks @audio-state)]
      (swap! (:pattern tr) assoc :solo? (contains? solo-set k))))
  track-keys)

(defn unsolo!
  "Clears solo mode, unmuting all audible tracks."
  []
  (swap! audio-state assoc :solo-mode? false)
  (doseq [[_ tr] (:active-tracks @audio-state)]
    (swap! (:pattern tr) assoc :solo? false))
  :unsoloed)

(defn undrum!
  "Mutes all drum/percussion tracks, keeping bass, pads, leads and click intact."
  []
  (doseq [[k tr] (:active-tracks @audio-state)]
    (when (is-drum-track? k)
      (swap! (:pattern tr) assoc :muted? true)))
  (mute-bus! :bus/drums)
  (swap! audio-state assoc :drums-muted? true)
  :undrummed)

(defn redrum!
  "Unmutes all drum tracks and restores full drum bus volume for the drop."
  []
  (doseq [[k tr] (:active-tracks @audio-state)]
    (when (is-drum-track? k)
      (swap! (:pattern tr) assoc :muted? false)))
  (unmute-bus! :bus/drums)
  (swap! audio-state assoc :drums-muted? false)
  :redrummed)

(defn toggle-drums!
  "Toggles all drum tracks between muted (undrum) and active (redrum) states."
  []
  (if (:drums-muted? @audio-state)
    (redrum!)
    (undrum!)))
