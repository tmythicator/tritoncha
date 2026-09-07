(ns app.eval.buffer-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.eval.buffer :as buffer]))

(deftest find-top-level-form-test
  (testing "Finds balanced top-level S-expressions accurately"
    (let [code "(jam! :roller)\n\n(loop! :bass\n  {:notes [\"E1\" \"E1\"]\n   :step \"16n\"})\n\n(set-bpm! 174)"]
      (is (= "(jam! :roller)" (buffer/find-top-level-form-around code 5)))
      (is (= "(loop! :bass\n  {:notes [\"E1\" \"E1\"]\n   :step \"16n\"})"
             (buffer/find-top-level-form-around code 25)))
      (is (= "(set-bpm! 174)" (buffer/find-top-level-form-around code (count code))))))

  (testing "Accurately extracts indented multi-line definition regardless of cursor line"
    (let [code "  ;; Live Sound Design\n  (definst! :supersaw-cus\n    {:category :leads\n     :osc {:type :supersaw}\n     :glide 0.03})\n\n  (demo! :supersaw-cus)"]
      ;; Cursor on (definst! line
      (is (= "(definst! :supersaw-cus\n    {:category :leads\n     :osc {:type :supersaw}\n     :glide 0.03})"
             (buffer/find-sexp-at-cursor code 26)))
      ;; Cursor on nested line (:osc {:type :supersaw})
      (is (= "(definst! :supersaw-cus\n    {:category :leads\n     :osc {:type :supersaw}\n     :glide 0.03})"
             (buffer/find-sexp-at-cursor code 70)))
      ;; Cursor on closing line (:glide 0.03})
      (is (= "(definst! :supersaw-cus\n    {:category :leads\n     :osc {:type :supersaw}\n     :glide 0.03})"
             (buffer/find-sexp-at-cursor code 100)))
      ;; Cursor on next form (demo! :supersaw-cus)
      (is (= "(demo! :supersaw-cus)"
             (buffer/find-sexp-at-cursor code 125)))))

  (testing "Extracts inner forms inside (comment ...) instead of the comment wrapper"
    (let [code "(comment\n  (jam! :roller)\n  (jam! :sub-roller))"]
      (is (= "(jam! :roller)" (buffer/find-sexp-at-cursor code 15)))
      (is (= "(jam! :sub-roller)" (buffer/find-sexp-at-cursor code 30))))))

(deftest get-code-at-cursor-test
  (testing "Returns selection when non-empty range selected"
    (let [code "(+ 1 2)\n(* 3 4)"]
      (is (= "(+ 1 2)" (buffer/get-code-at-cursor code 0 7)))
      (is (= "(* 3 4)" (buffer/get-code-at-cursor code 8 15)))))

  (testing "Evaluates SEXP at cursor when no selection"
    (let [code "  (definst! :foo\n    {:glide 0.03})"]
      (is (= "(definst! :foo\n    {:glide 0.03})" (buffer/get-code-at-cursor code 25 25)))))

  (testing "Falls back to line when no paren form matches"
    (let [code "some non-paren line\nanother line"]
      (is (= "some non-paren line" (buffer/get-code-at-cursor code 5 5))))))

(deftest insert-tab-test
  (testing "Inserts 2 spaces indentation at cursor"
    (let [{:keys [text cursor]} (buffer/insert-tab "(foo\nbar)" 5 5)]
      (is (= "(foo\n  bar)" text))
      (is (= 7 cursor)))))
