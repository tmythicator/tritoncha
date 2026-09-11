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
   (fn [root mode octave num-octs]
     (let [{:keys [pitch octave]} (parse-note root octave)
           root-midi (note->midi (str pitch octave))
           intervals (get scale-intervals (keyword mode) (:minor scale-intervals))]
       (if root-midi
         (vec
          (for [o (range num-octs)
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

(defn- degree->idx [d]
  (cond
    (number? d)  (dec (int d))
    (keyword? d) (let [n (js/parseInt (name d) 10)]
                   (when-not (js/isNaN n) (dec n)))
    :else nil))

(defn deg
  "Resolves 1-based scale degrees to note names in the given scale.
  Examples: (deg :e :phrygian [1 _ 1 2] 1) -> ['E1' nil 'E1' 'F1'], (deg :d :dorian [1 3 5 7]) -> ['D3' 'F3' 'A3' 'C4']."
  ([root mode degrees] (deg root mode degrees {}))
  ([root mode degrees opts-or-oct]
   (let [octave (if (map? opts-or-oct) (get opts-or-oct :octave 3) (or opts-or-oct 3))
         sc (scale root mode {:octave octave :octaves 4})]
     (if (sequential? degrees)
       (let [notes (mapv (fn [d]
                           (when-let [idx (degree->idx d)]
                             (get sc idx)))
                         degrees)]
         (with-meta notes {:degrees degrees :octave octave :root (keyword root) :mode (keyword mode)}))
       (when-let [idx (degree->idx degrees)]
         (get sc idx))))))

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
  "Generates chord notes for a given root and chord quality.
  Examples: (chord :e :min9) -> ['E3' 'G3' 'B3' 'D4' 'F#4'], (chord :c :maj7 {:inversion 1}) -> ['E3' 'G3' 'B3' 'C4']."
  ([root quality] (chord root quality {}))
  ([root quality opts]
   (let [oct       (if (map? opts) (get opts :octave 3) (or opts 3))
         inversion (if (map? opts) (get opts :inversion 0) 0)
         raw-notes (memo-chord-raw (keyword root) (keyword quality) oct)]
     (if (and (seq raw-notes) (not (zero? inversion)))
       (invert-chord raw-notes inversion)
       raw-notes))))

(defn progression
  "Generates a chord progression sequence from scale degree numbers.
  Examples: (progression :e :dorian [1 4] :type :min7) -> [['E3' 'G3' 'B3' 'D4'] ['A3' 'C4' 'E4' 'G4']]."
  [root mode degrees & {:keys [octave type] :or {octave 3 type :min7}}]
  (let [root-notes (deg root mode degrees {:octave octave})]
    (mapv (fn [r] (when r (chord r type))) root-notes)))

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
