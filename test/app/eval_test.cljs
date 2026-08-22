(ns app.eval-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.utils :as utils]
            [app.audio.theory :as theory]
            [sci.core :as sci]))

(def ^:private test-ctx
  (sci/init {:bindings {'chord theory/chord
                        'scale theory/scale
                        'euc!  utils/euclid
                        'd     theory/d}}))

(deftest eval-code-basic-test
  (testing "Evaluates basic math and data expressions in SCI"
    (is (= 42 (sci/eval-string* test-ctx "(+ 10 32)")))
    (is (= [1 2 3] (sci/eval-string* test-ctx "[1 2 3]")))))

(deftest eval-code-theory-test
  (testing "Evaluates music theory bindings in SCI"
    (is (= ["E3" "G3" "B3" "D4" "F#4"] (sci/eval-string* test-ctx "(chord :e :min9)")))
    (is (= ["D3" "E3" "F3" "G3" "A3" "B3" "C4"] (sci/eval-string* test-ctx "(scale :d :dorian)")))
    (is (= [true nil nil true nil true nil nil] (sci/eval-string* test-ctx "(euc! 3 8)")))))

(deftest eval-code-error-handling-test
  (testing "Catches syntax and runtime errors gracefully in SCI"
    (is (thrown? js/Error (sci/eval-string* test-ctx "(non-existent-function-call 1 2 3)")))))
