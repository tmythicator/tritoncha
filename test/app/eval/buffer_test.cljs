(ns app.eval.buffer-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.eval.buffer :as buffer]))

(deftest find-top-level-form-test
  (testing "Finds balanced top-level S-expressions accurately"
    (let [code "(jam! :roller)\n\n(loop! :bass\n  {:notes [\"E1\" \"E1\"]\n   :step \"16n\"})\n\n(set-bpm! 174)"]
      (is (= "(jam! :roller)" (buffer/find-top-level-form-around code 5)))
      (is (= "(loop! :bass\n  {:notes [\"E1\" \"E1\"]\n   :step \"16n\"})"
             (buffer/find-top-level-form-around code 25)))
      (is (= "(set-bpm! 174)" (buffer/find-top-level-form-around code (count code)))))))

(deftest get-code-at-cursor-test
  (testing "Returns selection when non-empty range selected"
    (let [code "(+ 1 2)\n(* 3 4)"]
      (is (= "(+ 1 2)" (buffer/get-code-at-cursor code 0 7)))
      (is (= "(* 3 4)" (buffer/get-code-at-cursor code 8 15)))))

  (testing "Falls back to line when no paren form matches"
    (let [code "some non-paren line\nanother line"]
      (is (= "some non-paren line" (buffer/get-code-at-cursor code 5 5))))))

(deftest insert-tab-test
  (testing "Inserts 2 spaces indentation at cursor"
    (let [{:keys [text cursor]} (buffer/insert-tab "(foo\nbar)" 5 5)]
      (is (= "(foo\n  bar)" text))
      (is (= 7 cursor)))))
