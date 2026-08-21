(ns app.audio.theory
  (:require [app.utils :refer [parse-note]]))

(def ^:dynamic _
  "Rest placeholder symbol for note vectors.
  Examples: [1 _ 1 2]."
  nil)

(def ^:private note-offsets
  {"C" 0 "C#" 1 "DB" 1 "D" 2 "D#" 3 "EB" 3 "E" 4 "F" 5
   "F#" 6 "GB" 6 "G" 7 "G#" 8 "AB" 8 "A" 9 "A#" 10 "BB" 10 "B" 11})

(def ^:private midi-names
  ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])

(defn note->midi
  "Converts a note string or keyword to MIDI pitch number.
  Examples: (note->midi 'C4') -> 60, (note->midi :eb2) -> 39, (note->midi :a4) -> 69."
  [note-val]
  (cond
    (number? note-val) note-val
    (nil? note-val) nil
    :else
    (when-let [{:keys [pitch octave]} (parse-note note-val 4)]
      (when-let [offset (get note-offsets pitch)]
        (+ (* (inc octave) 12) offset)))))

(defn midi->note
  "Converts MIDI pitch number to note string.
  Examples: (midi->note 60) -> 'C4', (midi->note 69) -> 'A4'."
  [midi-num]
  (when midi-num
    (let [m (js/Math.round midi-num)
          pitch (get midi-names (mod m 12))
          oct (- (quot m 12) 1)]
      (str pitch oct))))

(defn transpose
  "Transposes a single note or note vector by N semitones.
  Examples: (transpose 'C4' 7) -> 'G4', (transpose ['C4' 'E4'] 2) -> ['D4' 'F#4']."
  [note semitones]
  (cond
    (nil? note) nil
    (sequential? note) (mapv #(transpose % semitones) note)
    :else (when-let [m (note->midi note)]
            (midi->note (+ m semitones)))))

(defn oct-shift
  "Shifts note or note vector by N octaves (+1, -1, etc.).
  Examples: (oct-shift 'E1' 2) -> 'E3', (oct-shift ['E1' 'G1'] 1) -> ['E2' 'G2']."
  [note octaves]
  (transpose note (* 12 octaves)))

(def ^:private scale-intervals
  {:major             [0 2 4 5 7 9 11]
   :ionian            [0 2 4 5 7 9 11]
   :dorian            [0 2 3 5 7 9 10]
   :phrygian          [0 1 3 5 7 8 10]
   :lydian            [0 2 4 6 7 9 11]
   :mixolydian        [0 2 4 5 7 9 10]
   :minor             [0 2 3 5 7 8 10]
   :aeolian           [0 2 3 5 7 8 10]
   :locrian           [0 1 3 5 6 8 10]

   :harmonic-minor    [0 2 3 5 7 8 11]
   :melodic-minor     [0 2 3 5 7 9 11]
   :hungarian-minor   [0 2 3 6 7 8 11]
   :neapolitan-minor  [0 1 3 5 7 8 11]

   :pentatonic-minor  [0 3 5 7 10]
   :pentatonic-major  [0 2 4 7 9]
   :blues             [0 3 5 6 7 10]
   :major-blues       [0 2 3 4 7 9]

   :hirajoshi         [0 2 3 7 8]
   :insen             [0 1 5 7 10]
   :iwato             [0 1 5 6 10]
   :kumoi             [0 2 3 7 9]
   :arabic            [0 1 4 5 7 8 11]
   :double-harmonic   [0 1 4 5 7 8 11]
   :persian           [0 1 4 5 6 8 11]

   :whole-tone        [0 2 4 6 8 10]
   :diminished        [0 2 3 5 6 8 9 11]
   :bebop-dominant    [0 2 4 5 7 9 10 11]})

(defn scale
  "Generates note names for a given root note and scale mode.
  Examples: (scale :d :dorian) -> ['D3' 'E3' 'F3' 'G3' 'A3' 'B3' 'C4'], (scale :e :phrygian 1) -> ['E1' 'F1' 'G1' 'A1' 'B1' 'C2' 'D2']."
  ([root mode] (scale root mode {}))
  ([root mode opts-or-oct]
   (let [octave (if (map? opts-or-oct) (get opts-or-oct :octave 3) (or opts-or-oct 3))
         num-octs (if (map? opts-or-oct) (get opts-or-oct :octaves 1) 1)
         {:keys [pitch octave]} (parse-note root octave)
         root-midi (note->midi (str pitch octave))
         intervals (get scale-intervals (keyword mode) (:minor scale-intervals))]
     (if root-midi
       (vec
        (for [o (range num-octs)
              i intervals]
          (midi->note (+ root-midi (* o 12) i))))
       []))))

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
     (mapv (fn [d]
             (when-let [idx (degree->idx d)]
               (get sc idx)))
           degrees))))

(def ^:private chord-intervals
  {;; Standard chords
   :maj         [0 4 7]
   :min         [0 3 7]
   :dim         [0 3 6]
   :aug         [0 4 8]
   :sus2        [0 2 7]
   :sus4        [0 5 7]
   :5           [0 7]
   :power       [0 7 12]
   :7           [0 4 7 10]
   :dom7        [0 4 7 10]
   :maj7        [0 4 7 11]
   :min7        [0 3 7 10]
   :m7          [0 3 7 10]
   :m7b5        [0 3 6 10]
   :dim7        [0 3 6 9]
   :9           [0 4 7 10 14]
   :maj9        [0 4 7 11 14]
   :min9        [0 3 7 10 14]
   :m9          [0 3 7 10 14]
   :m11         [0 3 7 10 14 17]

   ;; Electronic chords
   :dark-m9     [0 3 7 10 14]
   :dark-sus    [0 5 7 10 15]
   :saw-fifth   [0 7 12 19]})

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
   (let [oct (if (map? opts) (get opts :octave 3) (or opts 3))
         inversion (if (map? opts) (get opts :inversion 0) 0)
         {:keys [pitch octave]} (parse-note root oct)
         root-midi (note->midi (str pitch octave))
         intervals (get chord-intervals (keyword quality) (:min chord-intervals))]
     (if root-midi
       (let [notes (mapv #(midi->note (+ root-midi %)) intervals)]
         (invert-chord notes inversion))
       []))))

(defn progression
  "Generates a chord progression sequence from scale degree numbers.
  Examples: (progression :e :dorian [1 4] :type :min7) -> [['E3' 'G3' 'B3' 'D4'] ['A3' 'C4' 'E4' 'G4']]."
  [root mode degrees & {:keys [octave type] :or {octave 3 type :min7}}]
  (let [root-notes (deg root mode degrees {:octave octave})]
    (mapv (fn [r] (when r (chord r type))) root-notes)))

(defn arp
  "Generates arpeggiator patterns (:up, :down, :up-down, :down-up, :random, :converge).
  Examples: (arp ['C3' 'E3' 'G3'] :up-down) -> ['C3' 'E3' 'G3' 'E3'], (arp ['C3' 'E3' 'G3'] :down) -> ['G3' 'E3' 'C3']."
  ([notes] (arp notes :up))
  ([notes pattern]
   (let [clean (vec (filter some? notes))]
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

(defonce global-key (atom {:root :e :mode :dorian :octave 2}))

(defn set-key!
  "Sets the session global key, mode, and octave.
  Examples: (set-key! :e :phrygian 1) -> {:root :e, :mode :phrygian, :octave 1}."
  ([root mode] (set-key! root mode 2))
  ([root mode octave]
   (reset! global-key {:root (keyword root) :mode (keyword mode) :octave octave})))

(defn d
  "Resolves degree numbers using the current session key.
  Examples: (d [1 _ 1 2]) -> ['E2' nil 'E2' 'F#2'], (d [1 3 5] 1) -> ['E1' 'G1' 'B1']."
  ([degrees] (d degrees {}))
  ([degrees opts-or-oct]
   (let [{:keys [root mode octave]} @global-key
         opt-map (cond
                   (map? opts-or-oct) opts-or-oct
                   (number? opts-or-oct) {:octave opts-or-oct}
                   :else {:octave octave})
         effective-oct (get opt-map :octave octave)]
     (deg root mode degrees {:octave effective-oct}))))

(defn sc
  "Returns pitch strings for the current active scale.
  Examples: (sc 1) -> ['E2' 'F#2' 'G2' 'A2' 'B2' 'C#3' 'D3']."
  ([]
   (let [{:keys [root mode octave]} @global-key]
     (scale root mode {:octave octave :octaves 2})))
  ([octaves]
   (let [{:keys [root mode octave]} @global-key]
     (scale root mode {:octave octave :octaves octaves}))))
