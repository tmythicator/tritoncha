(ns app.audio.mixer
  "Audio mixer for tempo, bus routing, track mutes, soloing and volume leveling."
  (:require ["tone" :as tone]
            [app.state :refer [state tone-ctx]]
            [app.audio.engine :refer [init-audio!]]
            [app.audio.voices :as voices]
            [app.audio.looper :refer [active-tracks solo-mode?]]))

(defn set-bpm!
  "Changes the global tempo (BPM) in real time.
  Examples: (set-bpm! 174), (set-bpm! 130)."
  [bpm]
  (swap! state assoc :bpm bpm)
  (set! (.. tone -Transport -bpm -value) bpm))

(defn- get-bus [bus-key]
  (when-let [ctx @tone-ctx]
    (case (keyword bus-key)
      (:drums :drum :drum-bus)       (:drum-bus ctx)
      (:bass :bass-bus)              (:bass-bus ctx)
      (:space :space-bus :pads :pad) (:space-bus ctx)
      (:direct :direct-bus :click)   (:direct-bus ctx)
      (:master :master-bus :filter)  (:master-filter ctx)
      nil)))

(defn set-bus-volume!
  "Sets mixer bus volume in decibels with smooth 50ms ramp.
  Examples: (set-bus-volume! :drums -6), (set-bus-volume! :bass 0)."
  [bus-key db]
  (init-audio!)
  (when-let [^js bus (get-bus bus-key)]
    (let [val (or db 0)]
      (try
        (.rampTo (.-volume bus) val 0.05)
        (catch js/Object _ (set! (.. bus -volume -value) val))))))

(defn mute-bus!
  "Mutes one or more audio mixer busses instantly at the DSP node level.
  Examples: (mute-bus! :drums), (mute-bus! :bass :space)."
  [& bus-keys]
  (init-audio!)
  (doseq [bk bus-keys]
    (when-let [^js bus (get-bus bk)]
      (try
        (.rampTo (.-volume bus) -100 0.04)
        (catch js/Object _ (set! (.. bus -volume -value) -100))))))

(defn unmute-bus!
  "Unmutes one or more audio mixer busses to 0 dB.
  Examples: (unmute-bus! :drums), (unmute-bus! :bass :space)."
  [& bus-keys]
  (init-audio!)
  (doseq [bk bus-keys]
    (when-let [^js bus (get-bus bk)]
      (try
        (.rampTo (.-volume bus) 0 0.04)
        (catch js/Object _ (set! (.. bus -volume -value) 0))))))

(defn toggle-bus!
  "Toggles mute state of an audio mixer bus.
  Examples: (toggle-bus! :drums)."
  [bus-key]
  (when-let [^js bus (get-bus bus-key)]
    (if (< (.. bus -volume -value) -60)
      (unmute-bus! bus-key)
      (mute-bus! bus-key))))

;; Mixer Loop Controls

(defn mute!
  "Mutes one or more active tracks by name.
  Examples: (mute! :kick :snare :hat)."
  [& track-names]
  (doseq [tn track-names]
    (when-let [t (get @active-tracks (keyword tn))]
      (reset! (:muted? t) true))))

(defn unmute!
  "Unmutes one or more active tracks by name.
  Examples: (unmute! :bass :sub)."
  [& track-names]
  (doseq [tn track-names]
    (when-let [t (get @active-tracks (keyword tn))]
      (reset! (:muted? t) false))))

(defn muted?
  "Returns true if the specified track is currently muted.
  Examples: (muted? :kick)."
  [track-name]
  (if-let [t (get @active-tracks (keyword track-name))]
    (true? @(:muted? t))
    false))

(defn toggle-mute!
  "Toggles the mute state of one or more active tracks.
  Examples: (toggle-mute! :bass), (toggle-mute! :kick :snare)."
  [track-name]
  (if (muted? track-name)
    (unmute! track-name)
    (mute! track-name)))

(defn all-mute!
  "Mutes all currently active tracks."
  []
  (doseq [[_ t] @active-tracks]
    (reset! (:muted? t) true)))

(defn all-unmute!
  "Unmutes all currently active tracks and audio busses."
  []
  (unmute-bus! :drums :bass :space)
  (doseq [[_ t] @active-tracks]
    (reset! (:muted? t) false)))

(defn flip-mute!
  "Inverts mute state of all active tracks."
  []
  (doseq [[_ t] @active-tracks]
    (swap! (:muted? t) not)))

(defn solo!
  "Solos the specified active tracks and silences all other tracks.
  Examples: (solo! :bass :sub)."
  [track-name & more-tracks]
  (let [solos (set (map keyword (cons track-name more-tracks)))]
    (reset! solo-mode? true)
    (doseq [[kw t] @active-tracks]
      (if (contains? solos kw)
        (reset! (:muted? t) false)
        (reset! (:muted? t) true)))))

(defn unsolo!
  "Clears solo mode and restores all active tracks to unmuted state.
  Examples: (unsolo!)."
  []
  (reset! solo-mode? false)
  (doseq [[_ t] @active-tracks]
    (reset! (:muted? t) false)))

(defn toggle-solo!
  "Toggles solo mode for the given tracks.
  Examples: (toggle-solo! :bass)."
  [track-name & more-tracks]
  (if @solo-mode?
    (unsolo!)
    (apply solo! track-name more-tracks)))

(defn- track-type [inst-kw]
  (let [kw (keyword inst-kw)]
    (cond
      (contains? (voices/all-drum-keys) kw)                                  :drums
      (contains? #{:bass :saw-bass :sub :sub-sine :acid-bass :fm-growl} kw)  :bass
      (contains? #{:pad :dark-pad :ambient-glass :pluck-lead :siren} kw)     :space
      :else (let [spec (get (voices/all-instruments) kw)]
              (or (:bus spec) :master)))))

(defn mute-type!
  "Mutes all active tracks belonging to specific instrument categories (:drums, :bass, :space).
  Examples: (mute-type! :drums)."
  [& bus-types]
  (let [targets (set bus-types)]
    (doseq [[_ t] @active-tracks]
      (when (contains? targets (track-type (:inst t)))
        (reset! (:muted? t) true)))))

(defn unmute-type!
  "Unmutes all active tracks belonging to specific instrument categories.
  Examples: (unmute-type! :drums :bass)."
  [& bus-types]
  (let [targets (set bus-types)]
    (doseq [[_ t] @active-tracks]
      (when (contains? targets (track-type (:inst t)))
        (reset! (:muted? t) false)))))

(defn undrum!
  "Mutes all drum tracks and drum busses while leaving bass and synths active."
  []
  (mute-bus! :drums)
  (mute-type! :drums))

(defn redrum!
  "Unmutes all drum tracks and synth loops."
  []
  (unmute-bus! :drums)
  (unmute-type! :drums :bass :space)
  (doseq [[_ t] @active-tracks]
    (reset! (:muted? t) false)))

;; Track Stop + Cleanup

(defn stop-loop!
  "Stops and deletes one or more active loops from memory.
  Examples: (stop-loop! :arp), (stop-loop! :bass :lead)."
  [& track-names]
  (doseq [tn track-names]
    (let [kw (keyword tn)]
      (when-let [{:keys [^js seq]} (get @active-tracks kw)]
        (try (.stop seq) (catch js/Object _))
        (try (.dispose seq) (catch js/Object _))
        (swap! active-tracks dissoc kw)))))

(defn clear-loops!
  "Stops all active loops, releases synth voices, and stops the transport."
  []
  (doseq [[_ {:keys [^js seq]}] @active-tracks]
    (try (.stop seq) (catch js/Object _))
    (try (.dispose seq) (catch js/Object _)))
  (reset! active-tracks {})
  (reset! solo-mode? false)
  (when-let [{:keys [^js pad ^js bass ^js sub]} @tone-ctx]
    (try (.releaseAll pad) (catch js/Object _))
    (try (.triggerRelease bass) (catch js/Object _))
    (try (.triggerRelease sub) (catch js/Object _)))
  (try
    (.. tone -Transport cancel)
    (.. tone -Transport stop)
    (catch js/Object _))
  (swap! state assoc :active? false :pulse 0.0))

(defn stop!
  "Stops all playing audio, or specific loops if names are provided.
  Examples: (stop!), (stop! :arp), (stop! :bass :lead)."
  ([]
   (clear-loops!))
  ([track-name & more-tracks]
   (apply stop-loop! (cons track-name more-tracks))))
