(ns app.ui.tutorial.highlight
  "Fast, zero-dependency Clojure syntax highlighter for live REPL code editor.
  Produces native Reagent Hiccup virtual DOM spans for rock-solid browser rendering.")

(def ^:private live-fns
  #{"stack!" "unstack!" "l!" "loop!" "stop-loop!" "clear-loops!"
    "pat" "pattern" "euc" "euclid" "d" "deg" "chord" "arp" "progression" "scale" "sc"
    "fast" "slow" "rev" "shift" "take-steps" "every-n" "sometimes" "sometimes-by"
    "mod-all!" "modulate-all!" "tr-all!" "transpose-all!"
    "mute!" "unmute!" "solo!" "unsolo!" "undrum!" "redrum!" "toggle-drums!"
    "v!" "set-volume!" "b!" "set-bpm!" "play!" "jam!" "stop!" "click!" "toggle-click!"
    "f!" "set-filter-cutoff!" "fb!" "wet!" "sw!" "s!" "drop!"
    "scene!" "defscene!" "w!" "toggle-wireframe!" "c!" "set-colors!" "pulse!"
    "stat" "stats!" "definst!" "deftrack!" "demo!" "demo-stop!"})

(def ^:private clj-core
  #{"defn" "defn-" "def" "ns" "let" "fn" "comment" "when" "if" "cond" "case"
    "do" "when-let" "if-let" "doseq" "for" "->" "->>" "partial" "comp"
    "map" "filter" "reduce" "vec" "into" "assoc" "dissoc" "get" "update"})

(def ^:private clj-constants
  #{"true" "false" "nil" "_"})

(def ^:private token-pattern-str
  (str "(;[^\\r\\n]*)"                                              ; 1: Comments
       "|(\"(?:\\\\.|[^\"\\\\])*\")"                                 ; 2: Strings
       "|(:[a-zA-Z0-9._!+*?/-]+)"                                    ; 3: Keywords
       "|(-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)"       ; 4: Numbers
       "|([()\\[\\]{}])"                                             ; 5: Brackets
       "|([a-zA-Z0-9._!+*?/-]+)"                                     ; 6: Symbols
       "|(\\s+)"                                                     ; 7: Whitespace
       "|(.)"))                                                      ; 8: Any other

(defn highlight-clojure-hiccup
  "Tokenizes Clojure source code into native Reagent Hiccup spans.
  Zero string escaping bugs, zero dangerouslySetInnerHTML quirks."
  [code-str]
  (if (empty? code-str)
    [:code]
    (let [tokens #js []
          m      (js/RegExp. token-pattern-str "g")]
      (loop [idx 0]
        (let [match (.exec m code-str)]
          (when match
            (let [full-tok (aget match 0)
                  comment  (aget match 1)
                  string   (aget match 2)
                  keyword  (aget match 3)
                  number   (aget match 4)
                  bracket  (aget match 5)
                  symbol   (aget match 6)
                  ws       (aget match 7)]
              (cond
                (some? comment)
                (.push tokens [:span {:key idx :class "syn-comment"} comment])

                (some? string)
                (.push tokens [:span {:key idx :class "syn-string"} string])

                (some? keyword)
                (.push tokens [:span {:key idx :class "syn-keyword"} keyword])

                (some? number)
                (.push tokens [:span {:key idx :class "syn-number"} number])

                (some? bracket)
                (cond
                  (or (= bracket "(") (= bracket ")"))
                  (.push tokens [:span {:key idx :class "syn-paren"} bracket])

                  (or (= bracket "[") (= bracket "]"))
                  (.push tokens [:span {:key idx :class "syn-bracket"} bracket])

                  (or (= bracket "{") (= bracket "}"))
                  (.push tokens [:span {:key idx :class "syn-brace"} bracket])

                  :else
                  (.push tokens bracket))

                (some? symbol)
                (cond
                  (contains? live-fns symbol)
                  (.push tokens [:span {:key idx :class "syn-live"} symbol])

                  (contains? clj-core symbol)
                  (.push tokens [:span {:key idx :class "syn-core"} symbol])

                  (contains? clj-constants symbol)
                  (.push tokens [:span {:key idx :class "syn-const"} symbol])

                  :else
                  (.push tokens [:span {:key idx :class "syn-ident"} symbol]))

                (some? ws)
                (.push tokens ws)

                :else
                (.push tokens full-tok))
              (recur (inc idx))))))
      (into [:code] (vec tokens)))))
