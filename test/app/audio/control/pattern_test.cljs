(ns app.audio.control.pattern-test
  "Unit tests for track pattern spec, canonicalization, and validation predicates."
  (:require [app.audio.control.pattern :as pattern]
            [app.audio.control.scheduler :as sched]
            [cljs.test :refer [deftest is testing]]))

(deftest valid-pattern-predicate-test
  (testing "Validates complete canonical pattern map"
    (is (true? (pattern/valid-pattern? {:inst :bass-analog
                                        :notes ["C2" "E2" "G2"]
                                        :step "16n"
                                        :dur "16n"
                                        :vel 0.9})))
    (is (true? (pattern/valid-pattern? {:inst :lead-pluck
                                        :notes ["A3" nil "C4"]
                                        :step "8n"
                                        :dur "8n"
                                        :vel [0.9 0.7 0.8]
                                        :oct 2
                                        :bus :bus/space
                                        :muted? false
                                        :solo? false}))))

  (testing "Rejects invalid or incomplete pattern maps"
    (is (false? (pattern/valid-pattern? nil)))
    (is (false? (pattern/valid-pattern? "not a map")))
    (is (false? (pattern/valid-pattern? {:notes ["C2"]})))
    (is (false? (pattern/valid-pattern? {:inst "not-a-keyword" :notes ["C2"] :step "16n" :dur "16n" :vel 0.9})))
    (is (false? (pattern/valid-pattern? {:inst :bass :notes "not-a-vector" :step "16n" :dur "16n" :vel 0.9})))))

(deftest pattern-normalization-canonicalization-test
  (testing "Normalizes raw vector into fully valid canonical pattern"
    (let [pat (sched/normalize-pattern-data :bass ["C2" "E2"])]
      (is (true? (pattern/valid-pattern? pat)))
      (is (= :bass (:inst pat)))
      (is (= ["C2" "E2"] (:notes pat)))
      (is (= "16n" (:step pat)))
      (is (= "16n" (:dur pat)))
      (is (nil? (:hits-vec pat)))
      (is (nil? (:octave pat)))))

  (testing "Canonicalizes user aliases :synth, :duration, :octave"
    (let [pat (sched/normalize-pattern-data :lead {:synth :lead-8bit
                                                   :notes ["A3" "B3"]
                                                   :duration "8n"
                                                   :octave 2})]
      (is (true? (pattern/valid-pattern? pat)))
      (is (= :lead-8bit (:inst pat)))
      (is (nil? (:synth pat)))
      (is (= "8n" (:dur pat)))
      (is (nil? (:duration pat)))
      (is (= 2 (:oct pat)))
      (is (nil? (:octave pat))))))

(deftest explain-pattern-test
  (testing "Returns nil on valid pattern and explain-data on invalid pattern"
    (is (nil? (pattern/explain-pattern {:inst :bass :notes ["C2"] :step "16n" :dur "16n" :vel 0.9})))
    (is (some? (pattern/explain-pattern {:notes ["C2"]})))
    (is (some? (pattern/explain-pattern {:inst :bass :notes 123})))))
