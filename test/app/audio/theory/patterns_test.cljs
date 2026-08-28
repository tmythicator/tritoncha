(ns app.audio.theory.patterns-test
  "Unit tests for algorithmic pattern generators, Euclidean rhythms, and temporal combinators."
  (:require [app.audio.theory.patterns :as pat]
            [cljs.test :refer [deftest is testing]]))

(deftest euclid-pattern-test
  (testing "Euclidean distribution of hits across steps"
    (is (= [true nil nil true nil true nil nil]
           (pat/euclid 3 8)))
    (is (= [:kick nil nil nil :kick nil nil nil]
           (pat/euclid 2 8 :kick)))
    (is (= [true true true true]
           (pat/euclid 4 4)))
    (is (= [nil nil nil nil]
           (pat/euclid 0 4)))))

(deftest mini-notation-pattern-test
  (testing "Mini-notation string parsing into keyword vectors"
    (is (= [:kick nil nil nil :snare nil nil nil]
           (pat/pattern "k . . .  s . . .")))
    (is (= [:hh-c :hh-o :hh-clk :sn-rs]
           (pat/pattern "h o hc rs")))
    (is (= [true nil true nil]
           (pat/pattern "x . 1 0")))))

(deftest fast-and-slow-combinators-test
  (testing "fast repeats sequence"
    (is (= ["C4" "E4" "C4" "E4"]
           (pat/fast 2 ["C4" "E4"])))
    (is (= ["C4" "E4"]
           (pat/fast 1 ["C4" "E4"]))))

  (testing "slow stretches sequence by duplicating elements"
    (is (= ["C4" "C4" "E4" "E4"]
           (pat/slow 2 ["C4" "E4"])))
    (is (= ["C4" "E4"]
           (pat/slow 1 ["C4" "E4"])))))

(deftest rev-test
  (testing "rev reverses sequences"
    (is (= ["G4" "E4" "C4"]
           (pat/rev ["C4" "E4" "G4"])))
    (is (nil? (pat/rev nil)))))

(deftest map-notes-nested-test
  (testing "map-notes preserves nil rests and nested chord vectors"
    (is (= ["C4!" nil ["E4!" "G4!"]]
           (pat/map-notes #(str % "!") ["C4" nil ["E4" "G4"]])))
    (is (= [[["C4+" "E4+"] nil] ["G4+" ["B4+" "D5+"]]]
           (pat/map-notes #(str % "+") [[["C4" "E4"] nil] ["G4" ["B4" "D5"]]])))))

(deftest shift-and-take-steps-test
  (testing "shift rotates sequences circularly"
    (is (= ["E4" "G4" "C4"]
           (pat/shift 1 ["C4" "E4" "G4"])))
    (is (= ["G4" "C4" "E4"]
           (pat/shift -1 ["C4" "E4" "G4"]))))

  (testing "take-steps truncates or cycles sequence to exact count"
    (is (= ["C4" "E4" "C4" "E4"]
           (pat/take-steps 4 ["C4" "E4"])))
    (is (= ["C4" "E4"]
           (pat/take-steps 2 ["C4" "E4" "G4"])))))

(deftest pipeline-threading-test
  (testing "transformations compose cleanly via ->>"
    (let [res (->> ["C4" "E4"]
                   (pat/fast 2)
                   (pat/shift 1))]
      (is (= ["E4" "C4" "E4" "C4"] res)))))
