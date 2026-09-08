(ns app.audio.control.looper
  "Live looper, scheduler and master transport engine driving the Rust WASM audio core."
  (:require [app.audio.control.scheduler :as sched]
            [app.audio.dsp.busses :as busses]
            [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.instruments :as inst]
            [app.audio.dsp.worklet :as worklet]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx pulse!]]
            [app.utils.audio :as audio-utils]
            [app.utils.math :refer [clamp]]
            [clojure.string :as str]
            [reagent.core :as r]))

(defonce click-config
  (atom {:pattern [1 0 0 0]
         :step    "4n"
         :dur     "32n"
         :accent  {:note "C6" :vel 1.0}
         :beat    {:note "G5" :vel 0.55}}))

(defn- compile-click-hits [pat-seq {:keys [accent beat]}]
  (mapv (fn [hit]
          (cond
            (or (nil? hit) (= hit :_) (= hit :-) (= hit "."))
            [:click 0.0 -1]

            (or (= hit 1) (= hit true) (= hit :acc) (= hit :accent) (= hit "X") (= hit "!"))
            [:click (float (:vel accent 1.0)) (:note accent "C6")]

            (or (= hit 0) (= hit false) (= hit :beat) (= hit "x") (= hit "o"))
            [:click (float (:vel beat 0.55)) (:note beat "G5")]

            (vector? hit)
            hit

            (string? hit)
            [:click (float (:vel beat 0.55)) hit]

            :else
            [:click (float (:vel beat 0.55)) (:note beat "G5")]))
        pat-seq))

(defn- parse-click-pattern [raw-pat]
  (cond
    (vector? raw-pat) raw-pat
    (string? raw-pat) (vec (remove #{" "} (str/split raw-pat #"\s+")))
    (sequential? raw-pat) (vec raw-pat)
    :else [1 0 0 0]))

(defn track-slot
  "Returns the hardware sequencer track slot index for a track keyword."
  [track-key]
  (worklet/track-slot track-key))

(defn- chord-progression?
  "Returns true if notes is a sequence of chords (vectors of pitch notes or frequencies)."
  [notes]
  (and (sequential? notes)
       (seq notes)
       (sequential? (first notes))
       (not (keyword? (first (first notes))))))

(defn sync-track-to-worklet!
  "Sends normalized pattern data to the Rust WASM sequencer, supporting velocity and polyphonic chords."
  [tk pat-data]
  (let [inst-k   (or (:inst pat-data) (:synth pat-data) tk)
        hits     (or (:notes pat-data) (:hits-vec pat-data) [true])
        notes    (if (sequential? hits) hits [hits])
        step-m   (audio-utils/step->mult (:step pat-data))
        bpm      (:bpm @audio-state 168)
        dur-raw  (or (:dur pat-data) (:duration pat-data) (:step pat-data) "16n")
        dur-s    (audio-utils/dur->seconds dur-raw bpm)
        base-vel (or (:vel pat-data) (:vel-vec pat-data) 0.9)]
    (if (chord-progression? notes)
      (let [max-voices   (min 4 (apply max 1 (map #(if (sequential? %) (count %) 1) notes)))
            scale-factor (if (> max-voices 1) (/ 1.0 (js/Math.sqrt max-voices)) 1.0)
            voice-vel    (if (number? base-vel)
                           (* (float base-vel) scale-factor)
                           (mapv #(* % scale-factor) (if (sequential? base-vel) base-vel [0.9])))]
        (doseq [v-idx (range max-voices)]
          (let [sub-tk      (keyword (str (name tk) "-v" (inc v-idx)))
                voice-notes (mapv #(if (sequential? %) (nth % v-idx nil) (when (zero? v-idx) %)) notes)
                slot        (worklet/get-or-assign-track-slot! sub-tk)]
            (worklet/set-track! slot inst-k voice-notes step-m dur-s voice-vel)))
        ;; Deactivate any remaining voices if chord density was reduced
        (doseq [v-idx (range max-voices 4)]
          (let [sub-tk (keyword (str (name tk) "-v" (inc v-idx)))]
            (when-let [sub-slot (get @worklet/track-slot-assignments sub-tk)]
              (worklet/deactivate-track! sub-slot)
              (swap! worklet/track-slot-assignments dissoc sub-tk)))))
      (let [slot (worklet/get-or-assign-track-slot! tk)]
        (worklet/set-track! slot inst-k (vec notes) step-m dur-s base-vel)
        ;; Deactivate any leftover polyphony sub-slots if switching to monophonic
        (doseq [v-idx (range 1 4)]
          (let [sub-tk (keyword (str (name tk) "-v" (inc v-idx)))]
            (when-let [sub-slot (get @worklet/track-slot-assignments sub-tk)]
              (worklet/deactivate-track! sub-slot)
              (swap! worklet/track-slot-assignments dissoc sub-tk))))))))

(defn sync-all-active-tracks!
  "Re-transmits all active session tracks to the Rust WASM sequencer."
  []
  (let [active (:active-tracks @audio-state)]
    (doseq [[tk tr] active]
      (let [pat-atom (:pattern tr)
            pat-data (if (satisfies? IDeref pat-atom) @pat-atom pat-atom)]
        (when pat-data
          (sync-track-to-worklet! tk pat-data))))
    (when (and (:active? @audio-state) (seq active))
      (worklet/set-playing! true))))

(worklet/on-worklet-ready! sync-all-active-tracks!)

(defn- handle-sequencer-triggers!
  "Dispatches hardware sequencer step triggers strictly to bound visual figures."
  [mask]
  (when (pos? mask)
    (let [assignments  @worklet/track-slot-assignments
          active       (:active-tracks @audio-state)
          has-figures? (boolean (seq (:figures (:three @engine-ctx))))]
      (doseq [[tk slot] assignments]
        (when (pos? (bit-and mask (bit-shift-left 1 slot)))
          (let [tr        (get active tk)
                pat       (when tr (let [p (:pattern tr)] (if (satisfies? IDeref p) @p p)))
                muted?    (boolean (:muted? pat false))
                fig       (or (:figure pat) (:fig pat))
                vel       (let [v (:vel pat)] (if (number? v) v 0.85))
                pulse-amt (or (:pulse pat) (* 1.0 vel))]
            ;; Strictly require an explicit figure assignment (no fallback to track-key)
            (when (and (not muted?) (some? fig) (not= fig :none) (not= fig false))
              (pulse! (keyword fig) pulse-amt)))))
      ;; Only pulse default scene background/mesh if no multi-figure setup is active
      (when-not has-figures?
        (let [kick-slot (get assignments :kick)
              kick-hit? (and kick-slot (pos? (bit-and mask (bit-shift-left 1 kick-slot))))]
          (when kick-hit?
            (pulse! :default 0.7)))))))

(worklet/on-trigger-event! handle-sequencer-triggers!)

(defn loop!
  "Schedules or hot-swaps an audio loop track in the live-coding session.
  Examples: (loop! :bass {:notes (d [1 2 3]) :step \"16n\"})."
  [track-name pattern-map]
  (init-audio!)
  (let [tk       (keyword track-name)
        pat-data (sched/normalize-pattern-data tk pattern-map)
        inst-k   (or (:inst pat-data) (:synth pat-data) tk)]
    (when-let [drum-m (:mod pat-data)]
      (if (or (= tk :drums) (not (busses/drum? tk)))
        (worklet/set-drum-mode! drum-m)
        (let [base (get (inst/all-instruments) tk {})]
          (worklet/set-worklet-drum-patch! tk (assoc base :mod drum-m)))))
    (if-let [tr (get (:active-tracks @audio-state) tk)]
      (let [old-pat @(:pattern tr)]
        (swap! (:pattern tr) merge (assoc pat-data :muted? (:muted? old-pat false) :solo? (:solo? old-pat false))))
      (let [pat-atom (r/atom (assoc pat-data :muted? false :solo? false))
            tr-info  {:pattern pat-atom :inst-key inst-k}]
        (swap! audio-state assoc-in [:active-tracks tk] tr-info)))
    (sync-track-to-worklet! tk pat-data)
    (worklet/set-playing! true)
    (let [hw-now (if-let [ctx (worklet/get-audio-context)] (.-currentTime ctx) 0.0)]
      (swap! audio-state (fn [st]
                           (cond-> (assoc st :active? true)
                             (nil? (:transport-start st)) (assoc :transport-start hw-now)))))
    tk))

(defn set-bpm!
  "Updates the master tempo in BPM.
  Examples: (set-bpm! 174)."
  [bpm]
  (let [clamped-bpm (clamp bpm cfg/min-bpm cfg/max-bpm)]
    (worklet/set-bpm! clamped-bpm)
    (swap! audio-state assoc :bpm clamped-bpm)
    clamped-bpm))

(defn stop-loop!
  "Stops and removes active tracks by keyword(s).
  Examples: (stop-loop! :arp), (stop-loop! :kick :snare :hat)."
  [& track-keys]
  (let [kw-set (set (map keyword (flatten track-keys)))]
    (doseq [tk kw-set]
      (doseq [slot (worklet/track-slots-for tk)]
        (worklet/deactivate-track! slot))
      (swap! worklet/track-slot-assignments
             (fn [slots]
               (apply dissoc slots (cons tk (map #(keyword (str (name tk) "-v" %)) (range 1 4)))))))
    (swap! audio-state update :active-tracks #(apply dissoc % kw-set))
    (when (empty? (:active-tracks @audio-state))
      (worklet/set-playing! false)
      (swap! audio-state assoc :active? false :transport-start nil))
    (vec kw-set)))

(def unstack! stop-loop!)

(defn clear-loops!
  "Stops and deletes all active loops."
  []
  (worklet/clear-tracks!)
  (swap! audio-state assoc :active-tracks {})
  :cleared)

(defn stop!
  "Stops playback and cancels all active loops."
  []
  (worklet/set-playing! false)
  (clear-loops!)
  (swap! audio-state assoc :active? false :solo-mode? false :transport-start nil)
  :stopped)

(defn set-click!
  "Configures the metronome click pattern, accents, and step subdivision.
  Accented hits (1, true, :acc, \"X\") play a high pitch (C6 at vel 1.0),
  while regular beats (0, false, :beat, \"x\") play a lower pitch (G5 at vel 0.55).
  Examples: (set-click! [1 0 0 0]), (set-click! \"X . x .\")."
  [arg]
  (if (map? arg)
    (swap! click-config merge arg)
    (swap! click-config assoc :pattern (parse-click-pattern arg)))
  (let [cfg  @click-config
        hits (compile-click-hits (:pattern cfg) cfg)
        pat  {:inst  :click
              :notes hits
              :step  (:step cfg "4n")
              :dur   (:dur cfg "32n")}]
    (when (contains? (:active-tracks @audio-state) :click)
      (loop! :click pat))
    pat))

(defn toggle-click!
  "Toggles a studio metronome click in headphones or master.
  Examples: (toggle-click!)."
  []
  (if (contains? (:active-tracks @audio-state) :click)
    (do
      (stop-loop! :click)
      :click-off)
    (let [cfg  @click-config
          hits (compile-click-hits (:pattern cfg) cfg)]
      (loop! :click {:inst :click :notes hits :step (:step cfg "4n") :dur (:dur cfg "32n")})
      :click-on)))

(defn click!
  "Toggles or sets the metronome click.
  Examples: (click!), (click! [1 0 0 0]), (click! \"X x x x\")."
  ([] (toggle-click!))
  ([pattern-or-config]
   (set-click! pattern-or-config)
   (when-not (contains? (:active-tracks @audio-state) :click)
     (let [cfg  @click-config
           hits (compile-click-hits (:pattern cfg) cfg)]
       (loop! :click {:inst :click :notes hits :step (:step cfg "4n") :dur (:dur cfg "32n")})))
   :click-on))

(defn set-drum-mode!
  "Configures the character synthesis mode across all drum voices in Rust WASM.
  Supported modes: :natural, :analog, :idm, :industrial.
  Examples: (set-drum-mode! :idm), (set-drum-mode! :natural)."
  [mode-kw]
  (worklet/set-drum-mode! mode-kw)
  mode-kw)

(def mod!
  "Shortcut for set-drum-mode!. Configures drum character mode.
  Examples: (mod! :idm), (mod! :analog), (mod! :natural)."
  set-drum-mode!)

(defn stack!
  "Launches multiple live loops simultaneously from variadic vectors, track pairs, or a track map.
  Supports setting drum mode via :mod keyword, {:mod :idm}, or track options.
  Examples:
    (stack!
      [:kick  (pat \"k . . .  k . . .\")]
      [:snare (pat \". . . .  s . . .\")])
    (stack! :idm
      [:kick  (pat \"k . . .\")]
      [:snare (pat \"s . . .\")])
    (stack! {:mod :idm}
      [:kick  (pat \"k . . .\")])
    (stack! {:kick (pat \"k . . .\") :snare (pat \"s . . .\")})"
  [& args]
  (let [first-arg (first args)
        [drum-mod rem-args]
        (cond
          ;; (stack! :mod :idm ...)
          (and (= first-arg :mod) (> (count args) 1))
          [(second args) (drop 2 args)]

          ;; (stack! :idm ...) where :idm is a known drum mode keyword
          (and (keyword? first-arg)
               (contains? #{:analog :natural :idm :industrial
                            :classic :808 :909 :acoustic :organic :wood
                            :glitch :laser :chirp :distort :hard :crush} first-arg))
          [first-arg (rest args)]

          ;; (stack! {:mod :idm} [:kick ...] ...)
          (and (map? first-arg) (contains? first-arg :mod) (> (count args) 1))
          [(:mod first-arg) (rest args)]

          ;; (stack! {:mod :idm :kick ...})
          (and (map? first-arg) (contains? first-arg :mod))
          [(:mod first-arg) [(dissoc first-arg :mod)]]

          :else
          [nil args])
        first-rem (first rem-args)
        pairs     (cond
                    (map? first-rem) first-rem
                    (and (= 1 (count rem-args)) (vector? (first first-rem))) first-rem
                    :else rem-args)
        tks       (into #{} (map first) pairs)]
    (when drum-mod
      (set-drum-mode! drum-mod))
    (when (some (fn [k] (and (busses/drum? k) (not= k :drums))) tks)
      (stop-loop! :drums))
    (when (contains? tks :drums)
      (doseq [k (filter (fn [k] (and (busses/drum? k) (not= k :drums)))
                        (keys (:active-tracks @audio-state)))]
        (stop-loop! k)))
    (doseq [[k spec] pairs]
      (when (and k spec) (loop! k spec)))
    (mapv first pairs)))
