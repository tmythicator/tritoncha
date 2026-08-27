(ns app.utils.coll
  "Collection manipulation, rotation, probability selection, and cycling helpers.")

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

(defn prob
  "Returns true with the given probability (0.0 to 1.0).
  Examples: (prob 0.7)."
  [p]
  (< (rand) (or p 0.5)))
