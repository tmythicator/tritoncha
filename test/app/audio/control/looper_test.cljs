(ns app.audio.control.looper-test
  "Isolated unit tests for looper data normalization, mask resolution, and step multiplier."
  (:require [app.audio.control.looper :as looper]
            [app.audio.control.scheduler :as sched]
            [app.utils.audio :as audio-utils]
            [cljs.test :refer [deftest is testing]]))

(deftest normalize-pattern-data-test
  (testing "Normalizes raw vector of notes"
    (let [pat (sched/normalize-pattern-data :lead ["C4" "E4" "G4"])]
      (is (= ["C4" "E4" "G4"] (:notes pat)))
      (is (= ["C4" "E4" "G4"] (:hits-vec pat)))
      (is (= "16n" (:step pat)))
      (is (= "16n" (:dur pat)))
      (is (= 0.9 (:vel pat)))))

  (testing "Normalizes map with degrees and octave"
    (let [pat (sched/normalize-pattern-data :bass {:deg [1 3 5] :oct 1 :step "8n"})]
      (is (= [1 3 5] (:deg pat)))
      (is (= "8n" (:step pat)))
      (is (= 1 (:oct pat)))))

  (testing "Applies mask to hits in pattern normalization"
    (let [pat (sched/normalize-pattern-data :hat {:notes [:hh-c :hh-c]
                                                  :mask [true false true]
                                                  :vel [0.4 0.8]
                                                  :dur "32n"})]
      (is (= [:hh-c nil :hh-c] (:notes pat)))
      (is (= [:hh-c nil :hh-c] (:hits-vec pat)))
      (is (= [true false true] (:mask-vec pat)))
      (is (= [0.4 0.8] (:vel-vec pat)))
      (is (= "32n" (:dur pat))))))

(deftest apply-mask-test
  (testing "Mask applies boolean pattern with rests"
    (is (= [:hh-c nil :hh-c nil]
           (sched/apply-mask [:hh-c] [true false true nil])))
    (is (= ["C4" nil]
           (sched/apply-mask ["C4" "E4"] [true false])))
    (is (= ["A1" "B1" nil "D1"]
           (sched/apply-mask ["A1" "B1" "C1" "D1"] [true true nil true])))))

(deftest step->mult-test
  (testing "Converts step notation to multiplier relative to 64th notes"
    (is (= 1 (audio-utils/step->mult "64n")))
    (is (= 2 (audio-utils/step->mult "32n")))
    (is (= 4 (audio-utils/step->mult "16n")))
    (is (= 8 (audio-utils/step->mult "8n")))
    (is (= 16 (audio-utils/step->mult "4n")))
    (is (= 64 (audio-utils/step->mult "1m")))
    (is (= 4 (audio-utils/step->mult "unknown")))))

(deftest set-bpm-clamp-test
  (testing "BPM clamps within valid range"
    (is (= 40 (looper/set-bpm! 5)))
    (is (= 300 (looper/set-bpm! 999)))
    (is (= 174 (looper/set-bpm! 174)))))
