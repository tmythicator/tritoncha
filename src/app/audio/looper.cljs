(ns app.audio.looper
  "Live looper, scheduler and modulation engine."
  (:require ["tone" :as tone]
            [app.state :refer [audio-state engine-ctx]]
            [app.audio.engine :refer [init-audio!]]
            [app.audio.voices :as voices]
            [app.audio.theory :as theory]))

(defn- track-audible? [track-info]
  (let [muted? @(:muted? track-info)
        solo?  @(:solo? track-info)
        global-solo? (:solo-mode? @audio-state)]
    (and (not muted?)
         (if global-solo? solo? true))))

(defn- ensure-transport-running! []
  (when-let [^js t (.-Transport tone)]
    (when (not= (.-state t) "started")
      (.start t)
      (swap! audio-state assoc :active? true))))

(defn- trigger-event!
  "Plays a single note/drum event or simultaneous multi-instrument hit."
  [event inst-key dur time vel ctx]
  (cond
    ;; Simultaneous set of hits: #{:snare :hh-c} or #{[:snare 1.0 "G3"] [:hh-c 0.4]}
    (set? event)
    (doseq [sub-ev event]
      (trigger-event! sub-ev inst-key dur time vel ctx))

    ;; Simultaneous vector of tuple hits on one step: [[:snare 1.0 "G3"] [:hh-c 0.4]]
    (and (vector? event) (sequential? (first event)))
    (doseq [sub-ev event]
      (trigger-event! sub-ev inst-key dur time vel ctx))

    ;; Single tuple hit: [:kick 1.0 "D1"] or [:snare 1.0]
    (and (vector? event) (keyword? (first event)))
    (let [[h-type v-hit pitch-hit] event]
      (voices/play-instrument! h-type pitch-hit dur time (or v-hit vel) ctx))

    ;; Keyword direct hit: :kick or :hh-c or :snare
    (keyword? event)
    (voices/play-instrument! event nil dur time vel ctx)

    ;; Melodic synth note/chord: "E1" or ["E3" "G3" "B3"]
    :else
    (voices/play-instrument! inst-key event dur time vel ctx)))

(defn loop!
  "Creates or live-updates an active quantized audio loop.

   Parameters:
     track-name - keyword identifier (e.g. :bass, :drums, :arp)
     pattern-map:
       :inst    - instrument keyword (e.g. :bass, :sub, :pad, :kick, :snare, :hh-c)
       :notes   - vector of notes/pitch names, chords, degrees, drum hits, or simultaneous hits (or :hits / :pattern)
       :step    - quantized step grid (default \"16n\")
       :dur     - note gate duration (default same as step)
       :vel     - velocity number (0.0 to 1.0) or vector of velocities
       :mask    - optional boolean/euc pattern vector filtering note triggers
       :prob    - probability of triggering note (0.0 to 1.0, default 1.0)

   Examples:
     (loop! :bass {:inst :bass :notes (deg :e :phrygian [1 _ 1 2 _ 1 4 3]) :step \"16n\" :vel 0.9})
     (loop! :drums {:hits [[:kick 1.0] [:hh-c 0.4] [[:snare 1.0] [:hh-c 0.4]] [:hh-c 0.5]] :step \"16n\"})
     (loop! :arp  {:inst :pad  :notes (arp (chord :e :min9) :up-down) :mask (euclid 7 16) :step \"16n\"})"
  [track-name pattern-map]
  (init-audio!)
  (let [track-kw     (keyword track-name)
        raw-data     (if (vector? pattern-map) {:notes pattern-map} pattern-map)
        pattern-data (if-let [degs (or (:deg raw-data) (:degrees raw-data))]
                       (let [oct (or (:oct raw-data) (:octave raw-data) 2)]
                         (assoc raw-data :notes (theory/d degs oct)))
                       raw-data)
        step         (or (:step pattern-data) "16n")
        existing-tr  (get (:active-tracks @audio-state) track-kw)]

    (if existing-tr
      ;; Live hot-swap pattern state without rebuilding Tone.Sequence
      (reset! (:pattern existing-tr) pattern-data)

      ;; First-time track instantiation
      (let [pattern-atom (atom pattern-data)
            muted-atom   (atom false)
            solo-atom    (atom false)
            tr-info      {:pattern pattern-atom
                          :muted?  muted-atom
                          :solo?   solo-atom}
            max-steps    64
            indices-js   (clj->js (vec (range max-steps)))
            seq-instance
            (tone/Sequence.
             (fn [time idx]
               (when-let [ctx (:tone @engine-ctx)]
                 (let [cur-pat  @pattern-atom
                       events   (or (:notes cur-pat) (:hits cur-pat) (:pattern cur-pat))
                       inst-key (or (:inst cur-pat) (:synth cur-pat) track-kw)
                       dur      (or (:dur cur-pat) (:step cur-pat) "16n")
                       mask     (:mask cur-pat)
                       prob     (or (:prob cur-pat) 1.0)
                       vel      (or (:vel cur-pat) 0.85)]
                   (when (track-audible? tr-info)
                     (let [actual-idx (mod idx (max 1 (count events)))
                           event (get events actual-idx)
                           mask-val (when mask (get mask (mod idx (count mask))))
                           should-play? (and (some? event)
                                             (if (some? mask-val) (boolean mask-val) true)
                                             (or (= prob 1.0) (< (js/Math.random) prob)))
                           v (if (sequential? vel) (get vel (mod idx (count vel)) 0.8) vel)]
                       (when should-play?
                         (trigger-event! event inst-key dur time v ctx)))))))
             indices-js
             step)]

        (.start ^js seq-instance 0)
        (swap! audio-state assoc-in [:active-tracks track-kw]
               {:seq seq-instance
                :pattern pattern-atom
                :muted? muted-atom
                :solo? solo-atom})
        (ensure-transport-running!)))))

(defn stop-loop!
  "Stops and deletes one or more active loops from memory.
  Examples: (stop-loop! :arp :lead)."
  [& track-names]
  (doseq [tn track-names]
    (let [kw (keyword tn)]
      (when-let [{:keys [^js seq]} (get (:active-tracks @audio-state) kw)]
        (try (.stop seq) (catch js/Object _))
        (try (.dispose seq) (catch js/Object _))
        (swap! audio-state update :active-tracks dissoc kw)))))

(defn clear-loops!
  "Stops all active loops, releases synth voices, and stops the transport."
  []
  (doseq [[_ {:keys [^js seq]}] (:active-tracks @audio-state)]
    (try (.stop seq) (catch js/Object _))
    (try (.dispose seq) (catch js/Object _)))
  (when-let [{:keys [^js pad ^js bass ^js sub]} (:tone @engine-ctx)]
    (try (.releaseAll pad) (catch js/Object _))
    (try (.triggerRelease bass) (catch js/Object _))
    (try (.triggerRelease sub) (catch js/Object _)))
  (try
    (.. tone -Transport cancel)
    (.. tone -Transport stop)
    (catch js/Object _))
  (swap! audio-state assoc :active-tracks {} :solo-mode? false :active? false))

(defn toggle-click!
  "Toggles the metronome click track (C6 downbeat / G5 beats) for playing in sync."
  []
  (if (get (:active-tracks @audio-state) :click)
    (stop-loop! :click)
    (loop! :click
      {:inst :click
       :notes ["C6" "G5" "G5" "G5"]
       :step "4n"
       :dur "32n"
       :vel 0.8})))

(defn- drum-track?
  "Determines if an active track or pattern map belongs to drums or percussion."
  [kw pat]
  (let [inst (or (:inst pat) (:synth pat) kw)]
    (or (= (keyword kw) :drums)
        (= (keyword kw) :drum)
        (contains? (voices/all-drum-keys) (keyword kw))
        (contains? (voices/all-drum-keys) (keyword inst))
        (= (voices/instrument-bus inst) :drums)
        (some? (:pattern pat))
        (some? (:hits pat)))))

(defn transpose-all!
  "Transposes all active melodic loops by N semitones live.
  Examples: (transpose-all! 2), (transpose-all! -1)."
  [semitones]
  (doseq [[kw tr] (:active-tracks @audio-state)]
    (let [cur-pat @(:pattern tr)]
      (when-not (drum-track? kw cur-pat)
        (when-let [notes (:notes cur-pat)]
          (let [tr-notes (mapv (fn [n]
                                 (cond
                                   (nil? n) nil
                                   (sequential? n) (mapv #(theory/transpose % semitones) n)
                                   :else (theory/transpose n semitones)))
                               notes)]
            (swap! (:pattern tr) assoc :notes tr-notes)))))))

(defn modulate-all!
  "Changes session key/mode and modulates all active melodic loops to the new scale on the fly.
  Examples: (modulate-all! :f :phrygian), (modulate-all! :d :dorian 1)."
  ([root mode] (modulate-all! root mode 2))
  ([root mode octave]
   (let [old-root (get-in @audio-state [:key :root] :e)
         old-midi (or (theory/note->midi (str (name old-root) "3")) 60)
         new-midi (or (theory/note->midi (str (name root) "3")) 60)
         delta-st (- new-midi old-midi)]
     (theory/set-key! root mode (or octave 2))
     (doseq [[kw tr] (:active-tracks @audio-state)]
       (let [cur-pat @(:pattern tr)]
         (when-not (drum-track? kw cur-pat)
           (cond
             (or (:deg cur-pat) (:degrees cur-pat))
             (let [degs (or (:deg cur-pat) (:degrees cur-pat))
                   oct  (or (:oct cur-pat) (:octave cur-pat) 2)
                   new-notes (theory/deg root mode degs {:octave oct})]
               (swap! (:pattern tr) assoc :notes new-notes))

             (:notes cur-pat)
             (let [notes (:notes cur-pat)
                   tr-notes (mapv (fn [n]
                                    (cond
                                       (nil? n) nil
                                       (sequential? n) (mapv #(theory/transpose % delta-st) n)
                                       :else (theory/transpose n delta-st)))
                                   notes)]
               (swap! (:pattern tr) assoc :notes tr-notes)))))))))

(def mod-all! modulate-all!)
(def tr-all! transpose-all!)
