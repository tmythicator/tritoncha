(ns app.audio.control.looper-test
  "Isolated unit tests for looper data normalization, step hit evaluation, track audibility, and stack parsing."
  (:require [app.audio.control.scheduler :as sched]
            [cljs.test :refer [deftest is testing]]))

(deftest normalize-pattern-data-test
  (testing "Normalizes raw vector of notes"
    (let [pat (sched/normalize-pattern-data :lead ["C4" "E4" "G4"])]
      (is (= ["C4" "E4" "G4"] (:hits-vec pat)))
      (is (= 3 (:hits-count pat)))
      (is (= "16n" (:step pat)))
      (is (= "16n" (:dur pat)))
      (is (= 0.9 (:vel pat)))))

  (testing "Normalizes map with degrees and octave"
    (let [pat (sched/normalize-pattern-data :bass {:deg [1 3 5] :oct 1 :step "8n"})]
      (is (= [1 3 5] (:deg pat)))
      (is (= 3 (:hits-count pat)))
      (is (= "8n" (:step pat)))
      (is (= 1 (:oct pat)))))

  (testing "Normalizes custom velocity vectors and masks"
    (let [pat (sched/normalize-pattern-data :hat {:notes [:hh-c :hh-c]
                                                  :mask [true false true]
                                                  :vel [0.4 0.8]
                                                  :dur "32n"})]
      (is (= [:hh-c :hh-c] (:hits-vec pat)))
      (is (= [true false true] (:mask-vec pat)))
      (is (= 3 (:mask-count pat)))
      (is (= [0.4 0.8] (:vel-vec pat)))
      (is (= 2 (:vel-count pat)))
      (is (= "32n" (:dur pat))))))

(deftest calculate-step-hit-test
  (testing "Evaluates active note hit at step index"
    (let [pat (sched/normalize-pattern-data :lead {:notes ["C4" nil "E4"] :dur "16n" :vel 0.8})]
      (is (= {:hit "C4" :vel 0.8 :dur "16n"} (sched/calculate-step-hit pat 0)))
      (is (nil? (sched/calculate-step-hit pat 1)) "Step 1 is a rest (nil)")
      (is (= {:hit "E4" :vel 0.8 :dur "16n"} (sched/calculate-step-hit pat 2)))))

  (testing "Cycles hits and velocity vectors with modulo"
    (let [pat (sched/normalize-pattern-data :bass {:notes ["E1" "G1"] :vel [0.5 0.9] :dur "8n"})]
      (is (= {:hit "E1" :vel 0.5 :dur "8n"} (sched/calculate-step-hit pat 0)))
      (is (= {:hit "G1" :vel 0.9 :dur "8n"} (sched/calculate-step-hit pat 1)))
      (is (= {:hit "E1" :vel 0.5 :dur "8n"} (sched/calculate-step-hit pat 2)) "Step 2 cycles back to first hit")
      (is (= {:hit "G1" :vel 0.9 :dur "8n"} (sched/calculate-step-hit pat 3)) "Step 3 cycles back to second hit")))

  (testing "Mask suppresses hits when masked"
    (let [pat (sched/normalize-pattern-data :hat {:notes [:hh-c] :mask [true false true nil]})]
      (is (some? (sched/calculate-step-hit pat 0)))
      (is (nil? (sched/calculate-step-hit pat 1)) "Mask false/nil suppresses hit")
      (is (some? (sched/calculate-step-hit pat 2)))
      (is (nil? (sched/calculate-step-hit pat 3)) "Mask nil suppresses hit"))))

(deftest track-audible-predicate-test
  (testing "Audibility with unmuted and muted states"
    (is (true? (sched/track-audible? {:muted? false :solo? false} false)))
    (is (false? (sched/track-audible? {:muted? true :solo? false} false))))

  (testing "Audibility in solo mode"
    (is (false? (sched/track-audible? {:muted? false :solo? false} true)) "In solo mode, non-solo track is muted")
    (is (true? (sched/track-audible? {:muted? false :solo? true} true)) "In solo mode, solo track is audible")
    (is (false? (sched/track-audible? {:muted? true :solo? true} true)) "Explicitly muted solo track is silent")))
