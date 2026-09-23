(ns app.audio.control.mixer
  "Audio bus mixer, levels, track mutes, solo, and performance drop controls."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.instruments.catalog :as catalog]
            [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-state]]))

;; Default Bus Configuration

(def default-bus-sends
  "Baseline hardware FX send routing into master delay and reverb in Rust WASM."
  cfg/default-bus-sends)

(def default-bus-levels
  "Default gain levels in decibels across all audio buses."
  cfg/default-bus-levels)

;; Bus Parameter Synchronization

(defn- sync-bus-to-worklet!
  "Sends gain, mute, FX send levels, and master FX bypass state of a bus into the Rust WASM engine."
  [b-key]
  (let [def-sends  (get cfg/default-bus-sends b-key {:delay 0.0 :reverb 0.0})
        db         (get-in @audio-state [:bus-levels b-key] (get cfg/default-bus-levels b-key 0.0))
        muted?     (get-in @audio-state [:bus-mutes b-key] false)
        del        (get-in @audio-state [:bus-sends b-key :delay] (:delay def-sends))
        rev        (get-in @audio-state [:bus-sends b-key :reverb] (:reverb def-sends))
        bypass-fx? (get-in @audio-state [:bus-bypass-master-fx b-key] false)]
    (if (= b-key :bus/master)
      (worklet/set-worklet-master-volume! (if muted? -60.0 db))
      (worklet/set-bus-params! b-key db muted? del rev bypass-fx?))))

(defn sync-all-busses!
  "Synchronizes all bus volumes, mutes, and sends into the Rust WASM DSP engine."
  []
  (doseq [b-key (keys cfg/default-bus-levels)]
    (sync-bus-to-worklet! b-key)))

;; Bus Gain and Channel Fader Controls

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
  "Mutes an audio bus.
  Examples: (mute-bus! :bus/drums)."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-mutes b-key] true)
      (sync-bus-to-worklet! b-key))))

(defn unmute-bus!
  "Unmutes an audio bus.
  Examples: (unmute-bus! :bus/drums)."
  [bus-key]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-mutes b-key] false)
      (sync-bus-to-worklet! b-key))))

(defn toggle-bus!
  "Toggles the mute state of an audio bus.
  Examples: (toggle-bus! :bus/drums)."
  [bus-key]
  (let [b-key  (busses/normalize-bus-key bus-key)
        muted? (get-in @audio-state [:bus-mutes b-key] false)]
    (if muted?
      (unmute-bus! b-key)
      (mute-bus! b-key))))

(defn set-bus-bypass-master-fx!
  "Configures whether an audio bus bypasses master inserts (filter and compressor) directly to output.
  Still affected by master volume fader.
  Examples: (set-bus-bypass-master-fx! :bus/drums true)."
  [bus-key bypass?]
  (let [b-key (busses/normalize-bus-key bus-key)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-bypass-master-fx b-key] (boolean bypass?))
      (sync-bus-to-worklet! b-key))))

(defn set-send!
  "Sets the hardware FX send level for a bus (:delay or :reverb, 0.0 to 1.0) into the Rust WASM engine.
  Examples: (set-send! :bus/space :reverb 0.6), (set-send! :bus/drums :delay 0.2)."
  [bus-key send-type amount]
  (let [b-key (busses/normalize-bus-key bus-key)
        stype (keyword send-type)]
    (when (busses/valid-bus? b-key)
      (swap! audio-state assoc-in [:bus-sends b-key stype] amount)
      (sync-bus-to-worklet! b-key))))

;; Track Mute and Solo State Controls

(defn- set-track-mute! [k muted?]
  (let [kw (keyword k)]
    (when-let [tr (get (:active-tracks @audio-state) kw)]
      (swap! (:pattern tr) assoc :muted? muted?))
    (doseq [slot (worklet/track-slots-for kw)]
      (worklet/mute-track! slot muted?))))

(defn- set-track-solo! [k solo?]
  (let [kw (keyword k)]
    (when-let [tr (get (:active-tracks @audio-state) kw)]
      (swap! (:pattern tr) assoc :solo? solo?))
    (doseq [slot (worklet/track-slots-for kw)]
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

;; Section Controllers

(def ^:private section-controllers
  {:drums {:pred catalog/drum? :bus :bus/drums :flag :drums-muted? :un :undrummed :re :redrummed}
   :bass  {:pred catalog/bass? :bus :bus/bass  :flag :bass-muted?  :un :unbassed  :re :rebassed}
   :lead  {:pred catalog/lead? :bus :bus/lead  :flag :leads-muted? :un :unleaded  :re :releaded}
   :pad   {:pred catalog/pad?  :bus :bus/space :flag :pads-muted?  :un :unpadded  :re :repadded}})

(defn- track-in-category? [cat-pred k tr]
  (let [pat    (when-let [p (:pattern tr)] (if (satisfies? IDeref p) @p p))
        inst-k (or (:inst pat) (:inst-key tr))]
    (boolean
     (or (cat-pred k)
         (when inst-k (cat-pred inst-k))
         (when pat (cat-pred pat))))))

(defn- mute-category-tracks! [cat-pred bus-key state-flag mute?]
  (doseq [[k tr] (:active-tracks @audio-state)
          :when (track-in-category? cat-pred k tr)]
    (set-track-mute! k mute?))
  (if mute? (mute-bus! bus-key) (unmute-bus! bus-key))
  (swap! audio-state (fn [st] (-> st (assoc state-flag mute?) (update :tracks-ver (fnil inc 0))))))

(defn- set-section-mute! [section mute?]
  (let [{:keys [pred bus flag un re]} (get section-controllers section)]
    (mute-category-tracks! pred bus flag mute?)
    (if mute? un re)))

(defn- toggle-section! [section]
  (let [{:keys [flag]} (get section-controllers section)]
    (set-section-mute! section (not (get @audio-state flag false)))))

(defn undrum!
  "Mutes all drum and percussion tracks, keeping bass, pads, leads and click intact."
  []
  (set-section-mute! :drums true))

(defn redrum!
  "Unmutes all drum tracks and restores full drum bus volume for the drop."
  []
  (set-section-mute! :drums false))

(defn toggle-drums!
  "Toggles all drum tracks between muted (undrum) and active (redrum) states."
  []
  (toggle-section! :drums))

(defn unbass!
  "Mutes all bass and sub tracks, isolating groove, drums, and pads."
  []
  (set-section-mute! :bass true))

(defn rebass!
  "Unmutes all bass and sub tracks, restoring low-end punch for the bass drop."
  []
  (set-section-mute! :bass false))

(defn toggle-bass!
  "Toggles all bass and sub tracks between muted and active states."
  []
  (toggle-section! :bass))

(defn unlead!
  "Mutes all lead and arpeggiator tracks."
  []
  (set-section-mute! :lead true))

(defn relead!
  "Unmutes all lead and arpeggiator tracks."
  []
  (set-section-mute! :lead false))

(defn toggle-leads!
  "Toggles all lead tracks between muted and active states."
  []
  (toggle-section! :lead))

(defn unpad!
  "Mutes all pad, string, and atmospheric soundscape tracks."
  []
  (set-section-mute! :pad true))

(defn repad!
  "Unmutes all pad, string, and atmospheric soundscape tracks."
  []
  (set-section-mute! :pad false))

(defn toggle-pads!
  "Toggles all pad and atmospheric tracks between muted and active states."
  []
  (toggle-section! :pad))
