(ns app.eval.buffer
  "Pure buffer and S-expression navigation algorithms for in-browser live-coding."
  (:require [clojure.string :as str]))

(defn parse-all-forms
  "Parses all balanced S-expressions in text, returning a vector of maps:
   {:start <int>, :end <int>, :type <str>, :text <str>, :depth <int>}."
  [text]
  (let [len (count text)]
    (loop [i 0
           stack []
           in-string? false
           escape? false
           in-comment? false
           forms []]
      (if (>= i len)
        forms
        (let [ch (.charAt text i)]
          (cond
            in-comment?
            (if (= ch "\n")
              (recur (inc i) stack false false false forms)
              (recur (inc i) stack false false true forms))

            in-string?
            (cond
              escape?
              (recur (inc i) stack true false false forms)
              (= ch "\\")
              (recur (inc i) stack true true false forms)
              (= ch "\"")
              (recur (inc i) stack false false false forms)
              :else
              (recur (inc i) stack true false false forms))

            (= ch ";")
            (recur (inc i) stack false false true forms)

            (= ch "\"")
            (recur (inc i) stack true false false forms)

            (= ch "\\")
            (recur (+ i 2) stack false false false forms)

            (or (= ch "(") (= ch "[") (= ch "{"))
            (let [actual-start (if (and (pos? i) (= (.charAt text (dec i)) "#"))
                                 (dec i)
                                 i)
                  depth (count stack)
                  frame {:ch ch :start actual-start :depth depth}]
              (recur (inc i) (conj stack frame) false false false forms))

            (or (= ch ")") (= ch "]") (= ch "}"))
            (if (seq stack)
              (let [top (peek stack)
                    open-ch (:ch top)]
                (if (or (and (= open-ch "(") (= ch ")"))
                        (and (= open-ch "[") (= ch "]"))
                        (and (= open-ch "{") (= ch "}")))
                  (let [new-stack (pop stack)
                        depth (count new-stack)
                        end (inc i)
                        form {:start (:start top)
                              :end   end
                              :type  open-ch
                              :depth depth
                              :text  (.substring text (:start top) end)}]
                    (recur (inc i) new-stack false false false (conj forms form)))
                  (recur (inc i) (pop stack) false false false forms)))
              (recur (inc i) stack false false false forms))

            :else
            (recur (inc i) stack false false false forms)))))))

(defn- comment-form?
  "Returns true if form text is a (comment ...) wrapper."
  [{:keys [text]}]
  (let [trimmed (str/trim (or text ""))]
    (or (= trimmed "(comment")
        (str/starts-with? trimmed "(comment ")
        (str/starts-with? trimmed "(comment\n")
        (str/starts-with? trimmed "(comment\t"))))

(defn find-sexp-at-cursor
  "Finds the best matching S-expression around cursor pos:
   1. If cursor is inside or immediately at the end of a parenthesized form, return the innermost parenthesized form.
   2. If cursor is between forms (e.g. on a comment or blank line), find nearest preceding or closest form."
  [text pos]
  (let [forms (parse-all-forms text)]
    (when (seq forms)
      (let [len (count text)
            safe-pos (min (max 0 (or pos 0)) len)

            ;; Filter out (comment ...) wrappers
            non-comment-forms (into [] (remove comment-form?) forms)
            parens (into [] (filter #(= (:type %) "(")) non-comment-forms)

            ;; 1. Check if cursor is directly inside or at boundary of any parenthesized form
            enclosing-parens (filter (fn [f]
                                       (and (<= (:start f) safe-pos)
                                            (<= safe-pos (:end f))))
                                     parens)

            ;; Innermost enclosing paren form (highest depth)
            innermost-enclosing (last (sort-by :depth enclosing-parens))

            ;; Also check if cursor is on the same line after a paren form's end (trailing spaces/comment on same line)
            same-line-paren (when-not innermost-enclosing
                              (last (sort-by :depth
                                             (filter (fn [f]
                                                       (let [end (:end f)]
                                                         (and (<= end safe-pos)
                                                              (not (str/includes? (.substring text end safe-pos) "\n")))))
                                                     parens))))]

        (cond
          innermost-enclosing
          (:text innermost-enclosing)

          same-line-paren
          (:text same-line-paren)

          :else
          (let [candidates (if (seq parens) parens non-comment-forms)
                prev-form (last (filter #(<= (:end %) safe-pos) candidates))
                next-form (first (filter #(>= (:start %) safe-pos) candidates))
                dist-prev (when prev-form (- safe-pos (:end prev-form)))
                dist-next (when next-form (- (:start next-form) safe-pos))]
            (cond
              (and prev-form next-form)
              ;; Prefer prev-form unless next-form is strictly closer
              (if (and dist-next dist-prev (< dist-next dist-prev))
                (:text next-form)
                (:text prev-form))

              prev-form
              (:text prev-form)

              next-form
              (:text next-form)

              :else
              (:text (first candidates)))))))))

(defn find-top-level-form-around
  "Finds the boundary of the outermost balanced parenthesized S-expression surrounding pos."
  [text pos]
  (find-sexp-at-cursor text pos))

(defn get-code-at-cursor
  "Extracts the expression to evaluate: selected region, surrounding S-expression, or current line."
  ([text] (get-code-at-cursor text 0 0))
  ([text sel-start sel-end]
   (cond
     (str/blank? text)
     ""

     (and (number? sel-start) (number? sel-end) (< sel-start sel-end))
     (let [sel (.substring text sel-start sel-end)]
       (if (str/blank? sel) text sel))

     :else
     (let [pos (or sel-start 0)
           form (find-sexp-at-cursor text pos)]
       (if (and form (not (str/blank? form)))
         form
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
