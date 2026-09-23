(ns app.audio.control.pattern-test
  "Unit tests for canonical track pattern normalization and validation predicates."
  (:require [app.audio.control.pattern :as pattern]
            [app.audio.control.scheduler :as sched]
            [cljs.test :refer [deftest is testing]]))

(deftest valid-pattern-predicate-test
  (testing "Validates pattern map"
    (is (true? (pattern/valid-pattern? {:inst :bass-analog
                                        :notes ["C2" "E2" "G2"]})))
    (is (true? (pattern/valid-pattern? {:inst :lead-pluck
                                        :notes ["A3" nil "C4"]}))))

  (testing "Rejects invalid or incomplete pattern maps"
    (is (false? (pattern/valid-pattern? nil)))
    (is (false? (pattern/valid-pattern? "not a map")))
    (is (false? (pattern/valid-pattern? {:notes ["C2"]})))
    (is (false? (pattern/valid-pattern? {:inst "not-a-keyword" :notes ["C2"]})))
    (is (false? (pattern/valid-pattern? {:inst :bass :notes "not-a-vector"})))))

(deftest pattern-normalization-test
  (testing "Normalizes raw vector into canonical pattern map"
    (let [pat (sched/normalize-pattern-data :bass ["C2" "E2"])]
      (is (true? (pattern/valid-pattern? pat)))
      (is (= :bass (:inst pat)))
      (is (= ["C2" "E2"] (:notes pat)))
      (is (= "16n" (:step pat)))
      (is (= "16n" (:dur pat)))))

  (testing "Preserves explicit canonical options"
    (let [pat (sched/normalize-pattern-data :lead {:inst :lead-8bit
                                                   :notes ["A3" "B3"]
                                                   :dur "8n"
                                                   :oct 2})]
      (is (true? (pattern/valid-pattern? pat)))
      (is (= :lead-8bit (:inst pat)))
      (is (= "8n" (:dur pat)))
      (is (= 2 (:oct pat))))))
