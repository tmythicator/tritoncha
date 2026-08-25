(ns app.utils
  (:require [clojure.string :as str]
            [app.config :as cfg]))

(defn mobile?
  "Returns true if running on a mobile browser or touch device."
  []
  (and (exists? js/navigator)
       (or (boolean (re-find #"(?i)android|iphone|ipad|ipod|mobile" (or (.-userAgent js/navigator) "")))
           (pos? (or (.-maxTouchPoints js/navigator) 0)))))

(defn active-lookahead
  "Returns optimal lookahead buffer duration based on device class."
  []
  (if (mobile?) cfg/lookahead-mobile cfg/lookahead-desktop))

(defn max-dpr
  "Returns optimal maximum devicePixelRatio based on device class."
  []
  (if (mobile?) cfg/max-dpr-mobile cfg/max-dpr-desktop))

(defn cycle-next
  "Finds the next item in a collection after current, wrapping around to the beginning.
  If current is not found in coll, returns the first element.
  Examples: (cycle-next :b [:a :b :c]) -> :c, (cycle-next :c [:a :b :c]) -> :a."
  [current coll]
  (let [v (vec coll)
        cnt (count v)]
    (cond
      (zero? cnt) current
      (nil? current) (first v)
      :else
      (let [cur-str (name current)
            found-idx (first (keep-indexed (fn [i x] (when (= (name x) cur-str) i)) v))]
        (if found-idx
          (get v (mod (inc found-idx) cnt))
          (first v))))))

(defn parse-note
  "Parses a note string or keyword into pitch name and octave.
  Examples: (parse-note 'C#4') -> {:pitch 'C#', :octave 4}, (parse-note :eb) -> {:pitch 'EB', :octave 3}."
  [n-val & [default-oct]]
  (when n-val
    (let [s (-> (name n-val) str/trim str/upper-case)]
      (when-let [[_ pitch oct-str] (re-matches #"([A-G][#B]?)(-?\d+)?" s)]
        {:pitch pitch
         :octave (if oct-str (js/parseInt oct-str 10) (or default-oct 3))}))))

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
  "Parses a compact mini-notation string into a pattern vector of drum keywords and rests."
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
                ("r" "roll" "sn-roll") :sn-roll
                ("h" "hh" "hh-c") :hh-c
                ("o" "oh" "hh-o") :hh-o
                ("hc" "hh-clk") :hh-clk
                "b" :bass
                (keyword tok)))
            tokens))))

(defn sec->ms
  "Converts seconds to milliseconds.
  Examples: (sec->ms 0.25) -> 250.0."
  [s]
  (when (number? s)
    (* s 1000.0)))

(defn ms->sec
  "Converts milliseconds to seconds.
  Examples: (ms->sec 250) -> 0.25."
  [ms]
  (when (number? ms)
    (/ ms 1000.0)))

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

(defn css-var
  "Reads a CSS custom property from the DOM :root in style.css.
  Examples: (css-var \"--cyan\") -> \"#00e5ff\", (css-var \"--bg-black\" \"#000000\")."
  ([var-name] (css-var var-name nil))
  ([var-name fallback]
   (if (exists? js/document)
     (let [val (-> (js/getComputedStyle (.-documentElement js/document))
                   (.getPropertyValue var-name)
                   .trim)]
       (if (seq val) val fallback))
     fallback)))

(defn colors
  "Reads live CSS theme colors directly from :root in style.css.
  Examples: (colors) -> {:cyan \"#00e5ff\" ...}, (colors :cyan) -> \"#00e5ff\"."
  ([]
   {:black  (css-var "--bg-black" "#000000")
    :white  (css-var "--text-white" "#ffffff")
    :cyan   (css-var "--cyan" "#00e5ff")
    :green  (css-var "--green" "#00ff88")
    :pink   (css-var "--pink" "#ff007f")
    :purple (css-var "--purple" "#c77dff")
    :amber  (css-var "--amber" "#ffaa00")
    :blue   (css-var "--blue" "#0088ff")})
  ([k]
   (get (colors) k (css-var (str "--" (name k)) "#00e5ff"))))
