(ns app.eval.core-test
  (:require [app.audio.control.session :as session]
            [app.audio.theory.harmony :as harmony]
            [app.audio.theory.patterns :as patterns]
            [cljs.test :refer [deftest is testing]]
            [sci.core :as sci]))

(def ^:private test-ctx
  (sci/init {:bindings {'chord        harmony/chord
                        'scale        harmony/scale
                        'arp          harmony/arp
                        'euc          patterns/euclid
                        'euclid       patterns/euclid
                        'pat          patterns/pattern
                        'pattern      patterns/pattern
                        'fast         patterns/fast
                        'slow         patterns/slow
                        'rev          patterns/rev
                        'sometimes-by patterns/sometimes-by
                        'sometimes    patterns/sometimes
                        'set-key!     session/set-key!
                        'd            session/d
                        '_            harmony/_}}))

(deftest eval-code-basic-test
  (testing "Evaluates basic math and data expressions in SCI"
    (is (= 42 (sci/eval-string* test-ctx "(+ 10 32)")))
    (is (= [1 2 3] (sci/eval-string* test-ctx "[1 2 3]")))))

(deftest eval-code-theory-test
  (testing "Evaluates music theory bindings in SCI"
    (is (= ["E3" "G3" "B3" "D4" "F#4"] (sci/eval-string* test-ctx "(chord :e :min9 3)")))
    (is (= ["D3" "E3" "F3" "G3" "A3" "B3" "C4"] (sci/eval-string* test-ctx "(scale :d :dorian)")))
    (is (= ["E2" "F#2" "G2" "B2" "C3"] (sci/eval-string* test-ctx "(scale :e :hirajoshi 2)")))
    (is (= [true nil nil true nil true nil nil] (sci/eval-string* test-ctx "(euc 3 8)")))))

(deftest eval-tutorial-forms-test
  (testing "Evaluates all algorithmic patterns and theory forms used in the tutorial"
    (session/set-key! :e :phrygian 1)
    (is (= ["E3" "G3" "B3" "D4" "F#4" "D4" "B3" "G3"]
           (sci/eval-string* test-ctx "(arp (chord :e :min9 3) :up-down)")))
    (is (= ["E1" nil "E1" "F1"]
           (sci/eval-string* test-ctx "(d [1 _ 1 2])")))
    (is (= ["E1" "E1" nil nil "E1" "E1" "F1" "F1"]
           (sci/eval-string* test-ctx "(slow 2 (d [1 _ 1 2]))")))
    (is (= ["F1" "E1" nil "E1"]
           (sci/eval-string* test-ctx "(rev (d [1 _ 1 2]))")))))

(deftest eval-code-error-handling-test
  (testing "Catches syntax and runtime errors gracefully in SCI"
    (is (thrown? js/Error (sci/eval-string* test-ctx "(non-existent-function-call 1 2 3)")))))
