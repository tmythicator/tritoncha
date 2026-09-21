(ns app.audio.theory.harmony
  "Pure music theory mathematics: scales, modes, chords, degrees, arpeggiation and transposition."
  (:require [app.audio.theory.patterns :refer [map-notes]]
            [app.audio.theory.tables :as tables]
            [app.utils.audio :refer [midi->note note->midi parse-note]]))

(def ^:dynamic _
  "Rest placeholder symbol for note vectors.
  Examples: [1 _ 1 2]."
  nil)

(defn transpose
  "Transposes a single note or note vector by N semitones.
  Examples: (transpose 'C4' 7) -> 'G4', (transpose ['C4' 'E4'] 2) -> ['D4' 'F#4']."
  [note semitones]
  (map-notes (fn [n]
               (when-let [m (note->midi n)]
                 (midi->note (+ m semitones))))
             note))

(defn oct-shift
  "Shifts note or note vector by N octaves (+1, -1, etc.).
  Examples: (oct-shift 'E1' 2) -> 'E3', (oct-shift ['E1' 'G1'] 1) -> ['E2' 'G2']."
  [note octaves]
  (transpose note (* 12 octaves)))

(def scale-intervals tables/scale-intervals)
(def chord-intervals tables/chord-intervals)

(def ^:private memo-scale-raw
  (memoize
   (fn [root mode octave num-octaves]
     (let [{:keys [pitch octave]} (parse-note root octave)
           root-midi (note->midi (str pitch octave))
           intervals (get scale-intervals (keyword mode) (:minor scale-intervals))]
       (if root-midi
         (vec
          (for [o (range num-octaves)
                i intervals]
            (midi->note (+ root-midi (* o 12) i))))
         [])))))

(defn scale
  "Generates note names for a given root note and scale mode.
  Examples: (scale :d :dorian) -> ['D3' 'E3' 'F3' 'G3' 'A3' 'B3' 'C4'], (scale :e :phrygian 1) -> ['E1' 'F1' 'G1' 'A1' 'B1' 'C2' 'D2']."
  ([root mode] (scale root mode {}))
  ([root mode opts-or-oct]
   (let [octave   (if (map? opts-or-oct) (get opts-or-oct :octave 3) (or opts-or-oct 3))
         num-octs (if (map? opts-or-oct) (get opts-or-oct :octaves 1) 1)]
     (memo-scale-raw (keyword root) (keyword mode) octave num-octs))))

(defn- parse-degree
  "Parses a scale degree (e.g. 1, 3, :b5, :3, :#4) into {:idx ... :shift ...}."
  [d]
  (cond
    (number? d)
    {:idx (dec (int d)) :shift 0}

    (keyword? d)
    (when-let [[_ acc num-str] (re-matches #"^([b#]?)([0-9]+)$" (name d))]
      (let [n (js/parseInt num-str 10)
            shift (case acc "b" -1 "#" 1 0)]
        (when-not (js/isNaN n)
          {:idx (dec n) :shift shift})))

    :else nil))

(defn- resolve-degree-note
  "Resolves a single scale degree against a precomputed scale note vector."
  [sc d]
  (when-let [{:keys [idx shift]} (parse-degree d)]
    (when-let [base-note (get sc idx)]
      (if (zero? shift)
        base-note
        (transpose base-note shift)))))

(defn deg-pattern?
  "Returns true if x is a deferred degree pattern."
  [x]
  (and (vector? x) (true? (:deg-pattern (meta x)))))

(defn deg
  "Resolves 1-based scale degrees to note names in the given scale, or creates a deferred
  deg-pattern when root and mode are omitted.
  Examples:
    (deg [1 _ 1 2] {:octave 3})
    (deg [1 _ 1 2] 3)
    (deg :e :phrygian [1 _ 1 2] 1) -> ['E1' nil 'E1' 'F1']
    (deg :d :dorian [1 3 5 7]) -> ['D3' 'F3' 'A3' 'C4']."
  ([degrees] (deg degrees {}))
  ([degrees opts-or-oct]
   (let [opts (if (map? opts-or-oct) opts-or-oct {:octave (or opts-or-oct 3)})
         clean-vec (vec (map #(if (= % :_) nil %) (if (sequential? degrees) degrees [degrees])))]
     (with-meta clean-vec
       {:deg-pattern true
        :degrees     degrees
        :opts        opts})))
  ([root mode degrees] (deg root mode degrees {}))
  ([root mode degrees opts-or-oct]
   (let [octave (if (map? opts-or-oct) (get opts-or-oct :octave 3) (or opts-or-oct 3))
         sc (scale root mode {:octave octave :octaves 4})]
     (if (sequential? degrees)
       (let [notes (mapv (fn [d] (resolve-degree-note sc d)) degrees)]
         (with-meta notes {:degrees degrees :octave octave :root (keyword root) :mode (keyword mode)}))
       (resolve-degree-note sc degrees)))))

(declare chord progression)

(defn resolve-track-notes
  "Resolves notes, deferred deg-patterns, progression patterns, or raw degree vectors against a scale [root mode oct].
  Examples: (resolve-track-notes (deg [1 3 5] 3) [:c :dorian 1]) -> ['C3' 'Eb3' 'G3']."
  [notes [root mode oct] & [default-oct]]
  (let [target-oct (or default-oct oct 1)]
    (cond
      (and root (deg-pattern? notes))
      (deg root mode (:degrees (meta notes)) (merge {:octave target-oct} (:opts (meta notes))))

      (and root (vector? notes)
           (or (true? (:progression (meta notes)))
               (and (seq notes)
                    (vector? (first notes))
                    (number? (first (first notes))))))
      (let [{:keys [template items degrees type octave]} (meta notes)
            def-oct   (or default-oct octave target-oct 3)
            raw-items (or items template degrees notes)
            tpl       (or template notes)]
        (with-meta
          (mapv (fn [item]
                  (cond
                    (or (nil? item) (= item :_)) nil
                    (vector? item)
                    (if (true? (:chord-deg (meta item)))
                      (let [{:keys [degree quality octave inversion]} (meta item)
                            o (or octave def-oct)
                            r (deg root mode degree {:octave o})]
                        (when r (chord r quality {:octave o :inversion (or inversion 0)})))
                      (let [d (nth item 0)
                            q (nth item 1 (or type :min7))
                            o (nth item 2 def-oct)
                            r (deg root mode d {:octave o})]
                        (when r (chord r q o))))
                    (or (number? item) (keyword? item))
                    (let [r (deg root mode item {:octave def-oct})]
                      (when r (chord r (or type :min7) def-oct)))
                    :else item))
                raw-items)
          {:progression true
           :template    (if (and (meta tpl) (:progression (meta tpl)))
                          (vary-meta tpl assoc :octave def-oct)
                          (with-meta (vec raw-items) {:progression true :octave def-oct :type (or type :min7)}))
           :items       raw-items
           :degrees     degrees
           :type        (or type :min7)
           :octave      def-oct
           :scale       [root mode def-oct]}))

      (and root (vector? notes) (some #(and (vector? %) (true? (:chord-deg (meta %)))) notes))
      (with-meta
        (mapv (fn [item]
                (if (and (vector? item) (true? (:chord-deg (meta item))))
                  (let [{:keys [degree quality octave inversion]} (meta item)
                        o (or octave target-oct 3)
                        root-note (deg root mode degree {:octave o})]
                    (chord root-note quality {:octave o :inversion (or inversion 0)}))
                  item))
              notes)
        {:progression true :template notes :scale [root mode target-oct]})

      (and root (vector? notes) (some number? notes))
      (deg root mode notes {:octave target-oct})

      :else
      notes)))

(def ^:private memo-chord-raw
  (memoize
   (fn [root chord-type octave]
     (let [{:keys [pitch octave]} (parse-note root octave)
           root-midi (note->midi (str pitch octave))
           intervals (get chord-intervals (keyword chord-type) (:min chord-intervals))]
       (if root-midi
         (mapv (fn [i] (midi->note (+ root-midi i))) intervals)
         [])))))

(defn invert-chord
  "Inverts a vector of chord notes by N inversions.
  Examples: (invert-chord ['C3' 'E3' 'G3'] 1) -> ['E3' 'G3' 'C4'], (invert-chord ['C3' 'E3' 'G3'] -1) -> ['G2' 'C3' 'E3']."
  [notes inversion]
  (let [inv (or inversion 0)]
    (if (or (zero? inv) (empty? notes))
      (vec notes)
      (let [midis (mapv note->midi notes)
            shifted (reduce (fn [acc _]
                              (if (pos? inv)
                                (let [lowest (first acc)]
                                  (conj (subvec acc 1) (+ lowest 12)))
                                (let [highest (last acc)]
                                  (into [(- highest 12)] (subvec acc 0 (dec (count acc)))))))
                            midis
                            (range (js/Math.abs inv)))]
        (mapv midi->note shifted)))))

(defn chord
  "Generates chord notes for a given root note (or scale degree) and chord quality.
  Supports inversion and custom octave options.
  Examples:
    (chord :e :min9) -> ['E3' 'G3' 'B3' 'D4' 'F#4']
    (chord :c :maj7 {:inversion 1}) -> ['E3' 'G3' 'B3' 'C4']
    (chord 1 :min9 3) -> deferred degree chord."
  ([root-or-deg] (chord root-or-deg :maj {}))
  ([root-or-deg quality-or-opts]
   (if (map? quality-or-opts)
     (chord root-or-deg (:type quality-or-opts :maj) quality-or-opts)
     (chord root-or-deg quality-or-opts {})))
  ([root-or-deg quality opts-or-oct]
   (if (number? root-or-deg)
     (let [oct (if (map? opts-or-oct) (get opts-or-oct :octave 3) (or opts-or-oct 3))
           inv (if (map? opts-or-oct) (get opts-or-oct :inversion 0) 0)]
       (with-meta
         [:chord-deg root-or-deg (keyword quality) oct inv]
         {:chord-deg true
          :degree    root-or-deg
          :quality   (keyword quality)
          :octave    oct
          :inversion inv}))
     (let [oct       (if (map? opts-or-oct) (get opts-or-oct :octave 3) (or opts-or-oct 3))
           inversion (if (map? opts-or-oct) (get opts-or-oct :inversion 0) 0)
           raw-notes (memo-chord-raw (keyword root-or-deg) (keyword quality) oct)]
       (if (and (seq raw-notes) (not (zero? inversion)))
         (invert-chord raw-notes inversion)
         raw-notes)))))

(defn progression
  "Generates a chord progression sequence from scale degree numbers or degree-chord specs.
  Examples:
    (progression [1 4 7 3] :min7 3)
    (progression [[1 :min9] [4 :dom7] [7 :maj7] [3 :maj7]] 3)
    (progression :e :dorian [1 4] :type :min7) -> [['E3' 'G3' 'B3' 'D4'] ['A3' 'C4' 'E4' 'G4']]."
  ([items-or-degrees] (progression items-or-degrees :min7 3))
  ([items-or-root b & more]
   (if (or (keyword? items-or-root) (string? items-or-root))
     (let [root       items-or-root
           mode       b
           degrees    (first more)
           kvs        (rest more)
           opt-map    (if (and (seq kvs) (keyword? (first kvs))) (apply hash-map kvs) {})
           octave     (:octave opt-map 3)
           chord-type (:type opt-map :min7)
           root-notes (deg root mode degrees {:octave octave})]
       (with-meta
         (mapv (fn [r] (when r (chord r chord-type octave))) root-notes)
         {:progression true :degrees degrees :type chord-type :octave octave :template degrees}))
     (let [items      items-or-root
           opt-b      b
           octave     (cond
                        (number? opt-b) opt-b
                        (map? opt-b) (:octave opt-b 3)
                        (number? (first more)) (first more)
                        (map? (first more)) (:octave (first more) 3)
                        :else 3)
           chord-type (if (keyword? opt-b) opt-b :min7)]
       (with-meta
         (vec (map #(if (= % :_) nil %) items))
         {:progression true
          :template    items
          :items       items
          :degrees     (when (every? #(or (nil? %) (number? %) (keyword? %)) items) items)
          :type        chord-type
          :octave      octave})))))

(def prog
  "Shortcut alias for progression.
  Examples: (prog [1 4 7 3] :min7 3), (prog [[1 :min9] [4 :dom7]] 3)."
  progression)

(defn arp
  "Generates arpeggiator patterns (:up, :down, :up-down, :down-up, :random, :converge).
  Supports both (arp notes :up-down) and ->> pipelines.
  Examples:
    (arp ['C3' 'E3' 'G3'] :up-down) -> ['C3' 'E3' 'G3' 'E3']
    (->> (chord :e :min9) (arp :up-down)) -> ['E3' 'G3' 'B3' 'D4' 'F#4' 'D4' 'B3' 'G3']."
  ([notes]
   (if (keyword? notes)
     (fn [ch] (arp ch notes))
     (arp notes :up)))
  ([a b]
   (let [[notes pattern] (if (keyword? a)
                           [b a]
                           [a b])
         clean (vec (filter some? notes))]
     (if (empty? clean)
       []
       (case (keyword pattern)
         :up clean
         :down (vec (rseq clean))
         :up-down (if (<= (count clean) 2)
                    clean
                    (into clean (rseq (subvec clean 1 (dec (count clean))))))
         :down-up (let [rev (vec (rseq clean))]
                    (if (<= (count clean) 2)
                      rev
                      (into rev (rseq (subvec rev 1 (dec (count rev)))))))
         :random (vec (shuffle clean))
         :converge (let [n (count clean)]
                     (vec (mapcat (fn [i]
                                    (let [j (- (dec n) i)]
                                      (if (= i j) [(get clean i)] [(get clean i) (get clean j)])))
                                  (range (quot (inc n) 2)))))
         clean)))))
