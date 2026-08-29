(ns app.audio.theory.patterns
  "Algorithmic rhythm generators, mini-notation parser, and temporal pattern combinators."
  (:require [app.utils.coll :as coll]
            [clojure.string :as str]))

(defn euclid
  "Generates a Euclidean rhythm pattern distributing hits across steps.
  Examples: (euclid 3 8) -> [true nil nil true nil true nil nil], (euclid 5 16 :kick) -> [:kick nil nil nil :kick ...]."
  ([hits steps] (euclid hits steps true))
  ([hits steps hit-val]
   (let [k (max 0 (min hits steps))]
     (if (zero? k)
       (vec (repeat steps nil))
       (if (= k steps)
         (vec (repeat steps hit-val))
         (let [init-ones (mapv (fn [_] [hit-val]) (range k))
               init-zeros (mapv (fn [_] [nil]) (range (- steps k)))
               build-pattern (fn step [front back]
                               (if (empty? back)
                                 (apply concat front)
                                 (let [f-count (count front)
                                       b-count (count back)
                                       min-count (min f-count b-count)
                                       paired (mapv (fn [f b] (into (vec f) (vec b)))
                                                    (subvec front 0 min-count)
                                                    (subvec back 0 min-count))
                                       rem-front (when (> f-count min-count) (subvec front min-count))
                                       rem-back  (when (> b-count min-count) (subvec back min-count))]
                                   (cond
                                     (seq rem-front) (step paired rem-front)
                                     (seq rem-back)  (step paired rem-back)
                                     :else (apply concat paired)))))]
           (vec (build-pattern init-ones init-zeros))))))))

(defn pattern
  "Parses a compact mini-notation string into a pattern vector of drum keywords and rests.
  Examples: (pattern \"k . . .  s . . .\") -> [:kick nil nil nil :snare nil nil nil]."
  [s]
  (if (sequential? s)
    (vec s)
    (let [tokens (str/split (str/trim (str s)) #"\s+")]
      (mapv (fn [tok]
              (case tok
                ("." "_" "~" "-" "0") nil
                ("x" "1") true
                "k" :kick
                "s" :snare
                ("rs" "sn-rs") :sn-rs
                ("c" "clk" "sn-clk") :sn-clk
                ("g" "gh" "sn-gh") :sn-gh
                ("roll" "sn-roll") :sn-roll
                ("h" "hh" "hh-c") :hh-c
                ("o" "oh" "hh-o") :hh-o
                ("hc" "hh-clk") :hh-clk
                "b" :bass
                (keyword tok)))
            tokens))))

(defn fast
  "Speeds up and compresses a pattern by repeating it factor times within the same grid duration.
  Examples: (fast 2 ['C4' 'E4']) -> ['C4' 'E4' 'C4' 'E4']."
  [factor pat]
  (cond
    (nil? pat) nil
    (or (not (number? factor)) (<= factor 1)) (if (sequential? pat) (vec pat) [pat])
    (sequential? pat) (vec (mapcat identity (repeat (js/Math.round factor) pat)))
    :else (vec (repeat (js/Math.round factor) pat))))

(defn slow
  "Slows down and stretches a pattern by duplicating each step factor times.
  Examples: (slow 2 ['C4' 'E4']) -> ['C4' 'C4' 'E4' 'E4']."
  [factor pat]
  (cond
    (nil? pat) nil
    (or (not (number? factor)) (<= factor 1)) (if (sequential? pat) (vec pat) [pat])
    (sequential? pat) (vec (mapcat (fn [x] (repeat (js/Math.round factor) x)) pat))
    :else (vec (repeat (js/Math.round factor) pat))))

(defn rev
  "Reverses a note pattern or sequence.
  Examples: (rev ['C4' 'E4' 'G4']) -> ['G4' 'E4' 'C4']."
  [pat]
  (if (sequential? pat)
    (vec (reverse pat))
    (if (nil? pat) nil [pat])))

(defn map-notes
  "Applies function f to every note, preserving nil rests and nested chord vectors.
  Examples: (map-notes #(str % \"!\") [\"C4\" nil [\"E4\" \"G4\"]]) -> [\"C4!\" nil [\"E4!\" \"G4!\"]]."
  [f notes]
  (cond
    (nil? notes) nil
    (sequential? notes) (mapv #(map-notes f %) notes)
    :else (f notes)))

(defn shift
  "Shifts a pattern circularly by n steps to the left (or right if negative).
  Examples: (shift 1 ['C4' 'E4' 'G4']) -> ['E4' 'G4' 'C4'], (shift -1 ['C4' 'E4' 'G4']) -> ['G4' 'C4' 'E4']."
  [n pat]
  (if (and (sequential? pat) (pos? (count pat)))
    (let [cnt (count pat)
          offset (mod n cnt)]
      (into (subvec (vec pat) offset) (subvec (vec pat) 0 offset)))
    pat))

(defn take-steps
  "Truncates or cycles a pattern to exactly n steps.
  Examples: (take-steps 4 ['C4' 'E4']) -> ['C4' 'E4' 'C4' 'E4'], (take-steps 2 ['C4' 'E4' 'G4']) -> ['C4' 'E4']."
  [n pat]
  (if (and (number? n) (pos? n) (sequential? pat) (pos? (count pat)))
    (vec (take n (cycle pat)))
    pat))

(defn sometimes-by
  "Applies transformation function f to pattern with probability prob (0.0 to 1.0).
  Examples: (sometimes-by 0.5 rev ['C4' 'E4' 'G4'])."
  [p f pat]
  (if (coll/prob p)
    (f pat)
    pat))

(defn sometimes
  "Applies transformation function f to pattern with 50% probability.
  Examples: (sometimes rev ['C4' 'E4' 'G4'])."
  [f pat]
  (sometimes-by 0.5 f pat))

(defn every-n
  "Applies transformation function f to pattern every n-th step index, or based on condition.
  Examples: (every-n 4 rev ['C4' 'E4'])."
  [n f pat]
  (if (and (number? n) (pos? n))
    (f pat)
    pat))
