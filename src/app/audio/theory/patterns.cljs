(ns app.audio.theory.patterns
  "Algorithmic rhythm generators, mini-notation parser, and temporal pattern combinators."
  (:require [app.audio.dsp.worklet.protocol :refer [drum-remaps]]
            [app.utils.coll :as coll]
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

(def ^:private mini-alias-pattern
  (let [aliases (sort-by (comp - count) (keys drum-remaps))
        escaped (map #(str/replace % #"([.*+?^${}()|\[\]/\\])" "\\\\$1") aliases)]
    (re-pattern (str "(?:" (str/join "|" escaped) "|[a-zA-Z0-9.-])[!_]?"))))

(defn- token-with-suffix? [tok]
  (let [clean (cond
                (str/ends-with? tok "!")
                (subs tok 0 (dec (count tok)))

                (and (> (count tok) 1) (str/ends-with? tok "_"))
                (subs tok 0 (dec (count tok)))

                :else tok)]
    (or (contains? drum-remaps clean)
        (contains? #{"" "." "_" "-" "0" "x" "1" "b"} clean))))

(defn- expand-mini-tokens [tokens]
  (mapcat (fn [tok]
            (cond
              (or (<= (count tok) 1)
                  (token-with-suffix? tok))
              [tok]

              (re-matches #"^[a-zA-Z0-9._\-!]+$" tok)
              (or (re-seq mini-alias-pattern tok)
                  [tok])

              :else
              [tok]))
          tokens))

(defn pattern
  "Parses a compact mini-notation string into a pattern vector of drum keywords and rests.
  Supports articulation modifiers: ! for accents and _ for ghost notes.
  Examples:
    (pattern \"k! . . .  s! . . .  . . s_ .  s! . s_ .\")
    (pattern \"cr16! . . .  rb . rb_ .  th! tm_ tl! cb_\")."
  [s]
  (if (sequential? s)
    (vec s)
    (let [raw-tokens (str/split (str/trim (str s)) #"\s+")
          tokens     (expand-mini-tokens raw-tokens)]
      (mapv (fn [tok]
              (let [has-accent? (str/ends-with? tok "!")
                    has-ghost?  (and (> (count tok) 1) (str/ends-with? tok "_"))
                    clean       (cond
                                  has-accent? (subs tok 0 (dec (count tok)))
                                  has-ghost?  (subs tok 0 (dec (count tok)))
                                  :else tok)
                    suffix      (cond
                                  has-accent? "!"
                                  has-ghost?  "_"
                                  :else "")]
                (cond
                  (contains? #{"" "." "_" "-" "0"} clean) nil
                  (= clean "x") (if (seq suffix) (keyword (str "x" suffix)) true)
                  (= clean "1") (if (seq suffix) (keyword (str "1" suffix)) true)
                  (= clean "b") (keyword (str "bass" suffix))
                  :else
                  (if-let [alias-kw (get drum-remaps clean)]
                    (keyword (str (name alias-kw) suffix))
                    (keyword tok)))))
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

(defn apply-mask
  "Combines a sequence of hits with a boolean or truthy mask sequence.
  Falsy or nil mask values become rests (nil).
  Examples: (apply-mask [:c4 :e4] [true false true]) -> [:c4 nil :e4]."
  [hits-vec mask-vec]
  (let [h-cnt (count hits-vec)
        m-cnt (count mask-vec)
        total (max h-cnt m-cnt)]
    (mapv (fn [i]
            (let [m (nth mask-vec (rem i m-cnt))]
              (when (and (some? m) (not (false? m)))
                (nth hits-vec (rem i h-cnt)))))
          (range total))))

(defn extract-pattern-degs
  "Extracts scale degrees from a pattern map or notes vector metadata.
  Examples: (extract-pattern-degs {:deg [1 3 5]} nil) -> [1 3 5]."
  [pat notes]
  (or (:deg pat)
      (:degrees pat)
      (when (vector? notes) (:degrees (meta notes)))))

(defn extract-pattern-prog
  "Extracts chord progression template from a pattern map or notes vector metadata.
  Examples: (extract-pattern-prog {:progression [[1 :min9]]} nil) -> [[1 :min9]]."
  [pat notes]
  (or (:progression pat)
      (when (and (vector? notes) (:progression (meta notes))) notes)
      (when (vector? notes) (:template (meta notes)))
      (when (and (vector? notes)
                 (seq notes)
                 (vector? (first notes))
                 (number? (first (first notes))))
        notes)))

(defn extract-pattern-octave
  "Extracts base octave from pattern map, notes metadata, progression, or degree metadata.
  Falls back to fallback-oct when not specified.
  Examples: (extract-pattern-octave {:octave 2} nil nil nil 3) -> 2."
  [pat notes degs prog fallback-oct]
  (or (:oct pat)
      (:octave pat)
      (when (and prog (meta prog)) (:octave (meta prog)))
      (when (and prog (map? prog)) (:octave prog))
      (when (vector? notes) (:octave (meta notes)))
      (when (vector? notes) (let [sc (:scale (meta notes))] (when (vector? sc) (last sc))))
      (when (and degs (meta degs)) (:octave (:opts (meta degs))))
      (when (and degs (meta degs)) (:octave (meta degs)))
      fallback-oct))

