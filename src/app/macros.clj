(ns app.macros
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defmacro load-tutorial-content []
  (let [f (io/file "src/app/demo/tutorial.cljs")
        content (if (.exists f)
                  (slurp f)
                  "")
        comment-idx (.indexOf content "(comment")
        extracted (if (neg? comment-idx)
                    content
                    (let [sub (subs content (+ comment-idx 8))
                          end-idx (.lastIndexOf sub ")")]
                      (if (neg? end-idx) sub (subs sub 0 end-idx))))]
    (str/trim extracted)))
