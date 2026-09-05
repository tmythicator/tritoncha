(ns app.audio.control.mixer
  "Audio bus mixer, levels, track mutes, solo, and performance undrum/redrum."
  (:require [app.audio.control.looper :as looper]
            [app.audio.dsp.busses :as busses]
            [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-state]]))

(def default-bus-sends
  {:bus/drums  {:delay 0.02 :reverb 0.06}
   :bus/bass   {:delay 0.0  :reverb 0.0}
   :bus/space  {:delay 0.20 :reverb 0.35}
   :bus/lead   {:delay 0.15 :reverb 0.10}
   :bus/direct {:delay 0.0  :reverb 0.0}})

(defn- sync-bus-to-worklet! [b-key]
  (let [db     (get-in @audio-state [:bus-levels b-key] 0.0)
        muted? (get-in @audio-state [:bus-mutes b-key] false)
        del    (get-in @audio-state [:bus-sends b-key :delay] (get-in default-bus-sends [b-key :delay] 0.15))
        rev    (get-in @audio-state [:bus-sends b-key :reverb] (get-in default-bus-sends [b-key :reverb] 0.20))]
    (worklet/set-bus-params! b-key db muted? del rev)))

(defn set-volume!
  "Sets the gain volume of a specific audio bus in decibels.
  Examples: (set-volume! :bus/drums -3), (set-volume! :bus/bass 0)."
  ([bus-key db-val] (set-volume! bus-key db-val cfg/default-ramp-time))
  ([bus-key db-val _ramp-time]
   (let [b-key (busses/normalize-bus-key bus-key)]
     (when (busses/valid-bus? b-key)
       (swap! audio-state assoc-in [:bus-levels b-key] db-val)
       (sync-bus-to-worklet! b-key)))))

(defn mute-bus!
  "Mutes an audio bus."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-mutes b-key] true)
      (sync-bus-to-worklet! b-key))))

(defn unmute-bus!
  "Unmutes an audio bus."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-mutes b-key] false)
      (sync-bus-to-worklet! b-key))))

(defn set-send!
  "Sets the FX send amount for a bus (:delay or :reverb, 0.0 to 1.0).
  Examples: (set-send! :bus/space :reverb 0.6), (set-send! :bus/drums :delay 0.2)."
  [bus-key send-type amount]
  (let [b-key (busses/normalize-bus-key bus-key)
        stype (keyword send-type)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-sends b-key stype] amount)
      (sync-bus-to-worklet! b-key))))

(defn toggle-bus!
  "Toggles the mute state of an audio bus."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)
        muted? (get-in @audio-state [:bus-mutes b-key] false)]
    (if muted?
      (unmute-bus! b-key)
      (mute-bus! b-key))))

(defn- set-track-mute! [k muted?]
  (let [kw (keyword k)]
    (when-let [tr (get (:active-tracks @audio-state) kw)]
      (swap! (:pattern tr) assoc :muted? muted?))
    (when-let [slot (looper/track-slot kw)]
      (worklet/mute-track! slot muted?))))

(defn- set-track-solo! [k solo?]
  (let [kw (keyword k)]
    (when-let [tr (get (:active-tracks @audio-state) kw)]
      (swap! (:pattern tr) assoc :solo? solo?))
    (when-let [slot (looper/track-slot kw)]
      (worklet/solo-track! slot solo?))))

(defn mute!
  "Mutes one or more active tracks by keyword.
  Examples: (mute! :kick :snare), (mute! :pad)."
  [& track-keys]
  (doseq [k track-keys]
    (set-track-mute! k true))
  (swap! audio-state update :tracks-ver (fnil inc 0))
  (keys (:active-tracks @audio-state)))

(defn unmute!
  "Unmutes one or more active tracks by keyword.
  Examples: (unmute! :kick :snare), (unmute! :all)."
  [& track-keys]
  (let [ks (if (or (empty? track-keys) (= (first track-keys) :all))
             (keys (:active-tracks @audio-state))
             track-keys)]
    (doseq [k ks]
      (set-track-mute! k false)))
  (swap! audio-state update :tracks-ver (fnil inc 0))
  (keys (:active-tracks @audio-state)))

(defn solo!
  "Solos one or more tracks, muting everything else.
  Examples: (solo! :bass :sub), (solo! :kick)."
  [& track-keys]
  (let [solo-set (set (map keyword track-keys))]
    (swap! audio-state (fn [st] (-> st (assoc :solo-mode? true) (update :tracks-ver (fnil inc 0)))))
    (doseq [k (keys (:active-tracks @audio-state))]
      (set-track-solo! k (contains? solo-set k))))
  track-keys)

(defn unsolo!
  "Clears solo mode, unmuting all audible tracks."
  []
  (swap! audio-state (fn [st] (-> st (assoc :solo-mode? false) (update :tracks-ver (fnil inc 0)))))
  (doseq [k (keys (:active-tracks @audio-state))]
    (set-track-solo! k false))
  :unsoloed)

(defn- mute-category-tracks! [cat-pred bus-key state-flag mute?]
  (doseq [[k _] (:active-tracks @audio-state)
          :when (cat-pred k)]
    (set-track-mute! k mute?))
  (if mute? (mute-bus! bus-key) (unmute-bus! bus-key))
  (swap! audio-state (fn [st] (-> st (assoc state-flag mute?) (update :tracks-ver (fnil inc 0))))))

(defn undrum!
  "Mutes all drum/percussion tracks, keeping bass, pads, leads and click intact."
  []
  (mute-category-tracks! busses/drum? :bus/drums :drums-muted? true)
  :undrummed)

(defn redrum!
  "Unmutes all drum tracks and restores full drum bus volume for the drop."
  []
  (mute-category-tracks! busses/drum? :bus/drums :drums-muted? false)
  :redrummed)

(defn toggle-drums!
  "Toggles all drum tracks between muted (undrum) and active (redrum) states."
  []
  (if (:drums-muted? @audio-state) (redrum!) (undrum!)))

(defn unbass!
  "Mutes all bass and sub tracks, isolating groove, drums, and pads."
  []
  (mute-category-tracks! busses/bass? :bus/bass :bass-muted? true)
  :unbassed)

(defn rebass!
  "Unmutes all bass and sub tracks, restoring low-end punch for the bass drop."
  []
  (mute-category-tracks! busses/bass? :bus/bass :bass-muted? false)
  :rebassed)

(defn toggle-bass!
  "Toggles all bass and sub tracks between muted and active states."
  []
  (if (:bass-muted? @audio-state) (rebass!) (unbass!)))

(defn unlead!
  "Mutes all lead and arpeggiator tracks."
  []
  (mute-category-tracks! busses/lead? :bus/lead :leads-muted? true)
  :unleaded)

(defn relead!
  "Unmutes all lead and arpeggiator tracks."
  []
  (mute-category-tracks! busses/lead? :bus/lead :leads-muted? false)
  :releaded)

(defn toggle-leads!
  "Toggles all lead tracks between muted and active states."
  []
  (if (:leads-muted? @audio-state) (relead!) (unlead!)))

(defn unpad!
  "Mutes all pad, string, and atmospheric soundscape tracks."
  []
  (mute-category-tracks! busses/pad? :bus/space :pads-muted? true)
  :unpadded)

(defn repad!
  "Unmutes all pad, string, and atmospheric soundscape tracks."
  []
  (mute-category-tracks! busses/pad? :bus/space :pads-muted? false)
  :repadded)

(defn toggle-pads!
  "Toggles all pad and atmospheric tracks between muted and active states."
  []
  (if (:pads-muted? @audio-state) (repad!) (unpad!)))
