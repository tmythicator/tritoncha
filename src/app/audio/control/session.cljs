(ns app.audio.control.session
  "Session key context, scale degree resolution, and real-time modal transposition."
  (:require [app.audio.dsp.instruments.catalog :as catalog]
            [app.audio.dsp.worklet :as worklet]
            [app.audio.theory.harmony :as harmony]
            [app.audio.theory.patterns :as patterns]
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
  (if-let [notes (:notes pat)]
    (assoc pat :notes (harmony/transpose notes delta-st))
    pat))

(defn- update-track-melody
  "Pure transform modulating pattern notes to new key context or applying chromatic pitch shift."
  [pat track-key delta-st {:keys [root mode oct-shift]}]
  (let [notes     (:notes pat)
        degs      (patterns/extract-pattern-degs pat notes)
        prog      (patterns/extract-pattern-prog pat notes)
        def-oct   (if (catalog/bass? track-key) cfg/default-bass-octave cfg/default-lead-octave)
        base-oct  (patterns/extract-pattern-octave pat notes degs prog (if prog 3 def-oct))
        track-oct (+ base-oct oct-shift)]
    (cond
      prog
      (let [updated-prog (if (and (meta prog) (:progression (meta prog)))
                           (vary-meta prog assoc :octave track-oct)
                           prog)
            new-notes    (harmony/resolve-track-notes updated-prog [root mode track-oct] track-oct)]
        (assoc pat
               :notes new-notes
               :oct track-oct
               :progression updated-prog))

      degs
      (assoc pat
             :notes (harmony/deg root mode degs {:octave track-oct})
             :deg degs
             :oct track-oct)

      notes
      (transpose-track-melody pat delta-st)

      :else pat)))

(defn- update-active-melodies!
  "Applies an update function to all active non-drum track patterns and re-syncs them to the worklet."
  [update-fn]
  (doseq [[kw tr] (:active-tracks @audio-state)
          :when (not (catalog/drum? kw))]
    (let [updated-pat (swap! (:pattern tr) update-fn kw)]
      (worklet/sync-track-to-worklet! kw updated-pat))))

(defn transpose-all!
  "Transposes all active melodic loops by N semitones live.
  Examples: (transpose-all! 2), (transpose-all! -1)."
  [semitones]
  (let [delta (or semitones 0)]
    (when-not (zero? delta)
      (update-active-melodies! (fn [pat _] (transpose-track-melody pat delta))))
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
     (update-active-melodies! (fn [pat kw] (update-track-melody pat kw delta-st key-info)))
     new-k)))
