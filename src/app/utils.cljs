(ns app.utils
  (:require [clojure.string :as str]))

(defn parse-note
  "Parses a note string or keyword into pitch name and octave.
  Examples: (parse-note 'C#4') -> {:pitch 'C#', :octave 4}, (parse-note :eb) -> {:pitch 'EB', :octave 3}."
  [n-val & [default-oct]]
  (when n-val
    (let [s (-> (name n-val) str/trim str/upper-case)]
      (when-let [[_ pitch oct-str] (re-matches #"([A-G][#B]?)(-?\d+)?" s)]
        {:pitch pitch
         :octave (if oct-str (js/parseInt oct-str 10) (or default-oct 3))}))))

(defn clamp
  "Clamps a numeric value between min and max bounds.
  Examples: (clamp 150 0 100) -> 100, (clamp -5 0 10) -> 0."
  [val min-val max-val]
  (js/Math.max min-val (js/Math.min max-val val)))

(defn lerp
  "Linear interpolation between a and b by factor t (0.0 to 1.0).
  Examples: (lerp 0 100 0.5) -> 50.0."
  [a b t]
  (+ a (* (- b a) t)))

(defn scale-range
  "Maps a value from an input range to an output range.
  Examples: (scale-range 5 0 10 0 100) -> 50.0."
  [val in-min in-max out-min out-max]
  (let [norm (/ (- val in-min) (- in-max in-min))]
    (+ out-min (* norm (- out-max out-min)))))

(defn rotate
  "Rotates a collection by N steps (positive rotates left, negative rotates right).
  Examples: (rotate 1 [1 2 3 4]) -> [2 3 4 1], (rotate -1 [1 2 3 4]) -> [4 1 2 3]."
  [n coll]
  (let [v (vec coll)
        cnt (count v)]
    (if (zero? cnt)
      []
      (let [shift (mod n cnt)]
        (into (subvec v shift) (subvec v 0 shift))))))

(defn pick-one
  "Returns a random element from a collection.
  Examples: (pick-one [1 3 5 7])."
  [coll]
  (when (seq coll)
    (rand-nth (vec coll))))

(defn prob
  "Returns true with the given probability (0.0 to 1.0)."
  [p]
  (< (rand) (or p 0.5)))

(defn enforce-stereo-mode!
  "Enforces explicit 2-channel stereo routing on WebAudio/Tone.js nodes to prevent dynamic allocation glitches."
  [^js node]
  (when node
    (try
      (doseq [field ["input" "output" "_filter" "_gainNode" "_node"]]
        (when-let [^js sub (aget node field)]
          (when (exists? (.-channelCount sub))
            (set! (.-channelCount sub) 2)
            (set! (.-channelCountMode sub) "explicit")
            (set! (.-channelInterpretation sub) "speakers"))))
      (when (exists? (.-channelCount node))
        (set! (.-channelCount node) 2)
        (set! (.-channelCountMode node) "explicit")
        (set! (.-channelInterpretation node) "speakers"))
      (catch js/Object _)))
  node)
