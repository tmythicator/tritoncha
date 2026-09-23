(ns app.audio.control.metronome-test
  "Unit tests for metronome click pattern compilation and rhythm notation parsing."
  (:require [app.audio.control.metronome :as metronome]
            [cljs.test :refer [deftest is testing]]))

(deftest parse-click-pattern-test
  (testing "Parses string notation with spaces into tokens"
    (let [cfg (metronome/set-click! "X . x .")]
      (is (= "4n" (:step cfg)))
      (is (= "32n" (:dur cfg)))
      (is (= 4 (count (:notes cfg))))
      ;; Accent hit
      (is (= :click (first (nth (:notes cfg) 0))))
      (is (= 1.0 (nth (nth (:notes cfg) 0) 1)))
      (is (= "C6" (nth (nth (:notes cfg) 0) 2)))
      ;; Rest
      (is (= 0.0 (nth (nth (:notes cfg) 1) 1)))
      ;; Regular beat hit
      (is (= :click (first (nth (:notes cfg) 2))))
      (is (= 0.55 (nth (nth (:notes cfg) 2) 1)))
      (is (= "G5" (nth (nth (:notes cfg) 2) 2))))))

(deftest vector-click-pattern-test
  (testing "Configures click from boolean/integer vectors"
    (let [cfg (metronome/set-click! [1 0 0 0])]
      (is (= 4 (count (:notes cfg))))
      (is (= 1.0 (nth (first (:notes cfg)) 1)))
      (is (= 0.55 (nth (second (:notes cfg)) 1))))))
