(ns app.eval.buffer
  "Pure buffer and S-expression navigation algorithms for in-browser live-coding."
  (:require [clojure.string :as str]))

(defn find-top-level-form-around
  "Finds the boundary of the outermost balanced parenthesized S-expression surrounding pos."
  [text pos]
  (let [len (count text)
        safe-pos (min (max 0 (or pos 0)) (dec (max 1 len)))
        start-idx (loop [i safe-pos]
                    (cond
                      (<= i 0) 0
                      (and (= (.charAt text i) "(")
                           (or (zero? i) (= (.charAt text (dec i)) "\n"))) i
                      :else (recur (dec i))))
        end-idx (loop [i start-idx
                       depth 0
                       found-open? false]
                  (if (< i len)
                    (let [ch (.charAt text i)]
                      (cond
                        (= ch "(") (recur (inc i) (inc depth) true)
                        (= ch ")") (if (= depth 1)
                                     (inc i)
                                     (recur (inc i) (dec depth) found-open?))
                        :else      (recur (inc i) depth found-open?)))
                    len))]
    (when (and (< start-idx end-idx) (= (.charAt text start-idx) "("))
      (.substring text start-idx end-idx))))

(defn get-code-at-cursor
  "Extracts the expression to evaluate: selected region, surrounding top-level form, or current line."
  ([text] (get-code-at-cursor text 0 0))
  ([text sel-start sel-end]
   (cond
     (str/blank? text)
     ""

     ;; 1. User selected a region
     (and (number? sel-start) (number? sel-end) (< sel-start sel-end))
     (let [sel (.substring text sel-start sel-end)]
       (if (str/blank? sel) text sel))

     ;; 2. Find balanced top-level S-expression around cursor
     :else
     (let [pos (or sel-start 0)
           form (find-top-level-form-around text pos)]
       (if (and form (not (str/blank? form)))
         form
         ;; 3. Fallback to current line
         (let [lines (str/split text #"\n")
               line-match (loop [remaining lines
                                 offset 0]
                            (if (seq remaining)
                              (let [l (first remaining)
                                    l-len (inc (count l))]
                                (if (and (<= offset pos) (<= pos (+ offset (count l))))
                                  l
                                  (recur (rest remaining) (+ offset l-len))))
                              (first lines)))]
           (if (and line-match (not (str/blank? line-match)))
             line-match
             text)))))))

(defn insert-tab
  "Inserts 2 spaces indentation at the given cursor position in text."
  [text start end]
  (let [s (or start 0)
        e (or end s)
        new-text (str (.substring text 0 s) "  " (.substring text e))]
    {:text new-text
     :cursor (+ s 2)}))
