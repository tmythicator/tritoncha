(ns app.audio.control.session
  "Session key context, scale degree resolution, and real-time modal transposition."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.worklet :as worklet]
            [app.audio.theory.harmony :as harmony]
            [app.config :as cfg]
            [app.state :refer [audio-state]]
            [app.utils.audio :as audio-utils :refer [normalize-opts note->midi]]))

(defn current-key
  "Returns the active musical key context map {:root :e, :mode :phrygian, :octave 1}."
  []
  (get @audio-state :key cfg/default-key))

(defn set-key!
  "Updates the global session key context.
  Examples: (set-key! :d :dorian), (set-key! :e :phrygian 2)."
  ([root mode]
   (set-key! root mode (get-in @audio-state [:key :octave] (:octave cfg/default-key 1))))
  ([root mode octave]
   (let [key-map {:root (keyword root)
                  :mode (keyword mode)
                  :octave (or octave 1)}]
     (swap! audio-state assoc :key key-map)
     key-map)))

(defn d
  "Resolves scale degrees against the active global session key.
  Rests (nil or :_) become rests. Single numbers or vectors of scale degrees.
  Options: nil, number for octave, or {:octave 1 :octaves 2}.
  Supports thread-last (->> coll (d 1)) pipeline ordering.
  Examples: (d [1 3 5]), (d [1 _ 1 2 _ 1 4 3] 2), (d 1 {:octave 2}), (->> [1 2 3] (d 1))."
  ([degrees] (d degrees nil))
  ([a b]
   (let [[degrees opts] (if (and (sequential? b) (not (sequential? a)))
                          [b a]
                          [a b])
         {:keys [root mode octave]} (current-key)
         o-map (normalize-opts opts octave)]
     (harmony/deg root mode degrees o-map))))

(defn deg
  "Resolves scale degrees with explicit root and mode, with fallback to global context.
  Examples: (deg :e :phrygian [1 3 5]), (deg :d :dorian [:i :iii :v])."
  ([root mode degrees] (deg root mode degrees nil))
  ([root mode degrees opts]
   (let [octave (get-in @audio-state [:key :octave] 1)
         o-map  (normalize-opts opts octave)]
     (harmony/deg root mode degrees o-map))))

(defn scale
  "Returns notes of the active session scale across octaves.
  Examples: (scale), (scale 2)."
  ([] (scale nil))
  ([opts]
   (let [{:keys [root mode octave]} (current-key)
         o-map (normalize-opts opts octave)]
     (harmony/scale root mode o-map))))

(def sc scale)

(defn- transpose-track-melody
  "Pure helper transposing a single track pattern map by semitones."
  [pat delta-st]
  (let [notes (or (:notes pat) (:pattern pat))]
    (cond
      (vector? notes)
      (let [shifted (mapv (fn [n]
                            (cond
                              (nil? n) nil
                              (= n :_) nil
                              (string? n) (harmony/transpose n delta-st)
                              (vector? n) (mapv #(if (or (nil? %) (= % :_)) nil (harmony/transpose % delta-st)) n)
                              :else n))
                          notes)]
        (assoc pat :notes shifted :hits-vec shifted))

      (string? notes)
      (let [shifted (harmony/transpose notes delta-st)]
        (assoc pat :notes [shifted] :hits-vec [shifted]))

      :else pat)))

(defn- update-track-melody
  "Pure transform modulating pattern notes to new key context or applying chromatic pitch shift."
  [pat track-key delta-st {:keys [root mode oct-shift]}]
  (let [notes (or (:notes pat) (:pattern pat))
        degs  (or (:deg pat) (:degrees pat) (when (vector? notes) (:degrees (meta notes))))]
    (cond
      degs
      (let [base-oct  (or (:oct pat) (:octave pat)
                          (when (vector? notes) (:octave (meta notes)))
                          (if (busses/bass? track-key) cfg/default-bass-octave cfg/default-lead-octave))
            track-oct (+ base-oct oct-shift)
            new-notes (harmony/deg root mode degs {:octave track-oct})]
        (assoc pat
               :notes new-notes
               :hits-vec new-notes
               :deg degs
               :oct track-oct))

      notes
      (transpose-track-melody pat delta-st)

      :else pat)))

(defn- worklet-sync-track! [tk pat-data]
  (when-let [slot (worklet/track-slot tk)]
    (let [inst-k  (or (:inst pat-data) (:synth pat-data) tk)
          hits    (or (:notes pat-data) (:hits-vec pat-data) [true])
          notes   (if (sequential? hits) hits [hits])
          step-m  (audio-utils/step->mult (:step pat-data))
          bpm     (:bpm @audio-state 168)
          dur-raw (or (:dur pat-data) (:duration pat-data) (:step pat-data) "16n")
          dur-s   (audio-utils/dur->seconds dur-raw bpm)]
      (worklet/set-track! slot inst-k (vec notes) step-m dur-s))))

(defn transpose-all!
  "Transposes all active melodic loops by N semitones live.
  Examples: (transpose-all! 2), (transpose-all! -1)."
  [semitones]
  (let [delta (or semitones 0)]
    (when-not (zero? delta)
      (doseq [[kw tr] (:active-tracks @audio-state)
              :when (not (busses/drum? kw))]
        (let [updated-pat (swap! (:pattern tr) transpose-track-melody delta)]
          (worklet-sync-track! kw updated-pat))))
    delta))

(defn modulate-all!
  "Changes session key/mode and modulates all active melodic loops on the fly.
  Examples: (modulate-all! :d :dorian), (modulate-all! :b :arabic 1)."
  ([root mode] (modulate-all! root mode nil))
  ([root mode octave]
   (let [old-root   (get-in @audio-state [:key :root] (:root cfg/default-key :e))
         old-oct    (get-in @audio-state [:key :octave] (:octave cfg/default-key 1))
         target-oct (or octave old-oct)
         oct-shift  (- target-oct old-oct)
         old-midi   (note->midi (str (name old-root) "3"))
         new-midi   (note->midi (str (name root) "3"))
         delta-st   (+ (- new-midi old-midi) (* 12 oct-shift))
         key-info   {:root root :mode mode :oct-shift oct-shift}
         new-k      (set-key! root mode target-oct)]
     (doseq [[kw tr] (:active-tracks @audio-state)
             :when (not (busses/drum? kw))]
       (let [updated-pat (swap! (:pattern tr) update-track-melody kw delta-st key-info)]
         (worklet-sync-track! kw updated-pat)))
     new-k)))
