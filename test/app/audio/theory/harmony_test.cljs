(ns app.audio.theory.harmony-test
  "Unit tests for pure harmonic music theory: scales, degrees, chords, arpeggiators, and transpositions."
  (:require [app.audio.theory.harmony :as harmony]
            [cljs.test :refer [deftest is testing]]))

(deftest transpose-test
  (testing "Note transpositions and octave shifts"
    (is (= "G4" (harmony/transpose "C4" 7)))
    (is (= "C5" (harmony/transpose "C4" 12)))
    (is (= "C3" (harmony/transpose "C4" -12)))
    (is (= ["D4" "F#4" "A4"] (harmony/transpose ["C4" "E4" "G4"] 2)))
    (is (= "E3" (harmony/oct-shift "E1" 2)))
    (is (= ["E2" "G2"] (harmony/oct-shift ["E1" "G1"] 1)))))

(deftest scale-generation-test
  (testing "Dorian scale generation"
    (is (= ["D3" "E3" "F3" "G3" "A3" "B3" "C4"] (harmony/scale :d :dorian))))

  (testing "Phrygian scale generation"
    (is (= ["E1" "F1" "G1" "A1" "B1" "C2" "D2"] (harmony/scale :e :phrygian 1))))

  (testing "Hirajoshi Japanese pentatonic scale"
    (is (= ["E2" "F#2" "G2" "B2" "C3"] (harmony/scale :e :hirajoshi 2))))

  (testing "Hungarian Minor exotic scale"
    (is (= ["A3" "B3" "C4" "D#4" "E4" "F4" "G#4"] (harmony/scale :a :hungarian-minor 3)))))

(deftest scale-degrees-test
  (testing "Resolves degrees to scale notes and respects rests"
    (let [res (harmony/deg :e :phrygian [1 harmony/_ 1 2] 1)]
      (is (= ["E1" nil "E1" "F1"] res))
      (is (= [1 nil 1 2] (:degrees (meta res))))))
  (testing "Dorian scale degrees"
    (is (= ["D3" "F3" "A3" "C4"]
           (harmony/deg :d :dorian [1 3 5 7])))))

(deftest chord-generation-test
  (testing "Major, minor and extended chords"
    (is (= ["C3" "E3" "G3"] (harmony/chord :c :maj 3)))
    (is (= ["A3" "C4" "E4"] (harmony/chord :a :min 3)))
    (is (= ["E3" "G3" "B3" "D4"] (harmony/chord :e :min7 3)))
    (is (= ["E3" "G3" "B3" "D4" "F#4"] (harmony/chord :e :min9 3))))

  (testing "Chord inversions"
    (is (= ["E3" "G3" "C4"] (harmony/invert-chord ["C3" "E3" "G3"] 1)))
    (is (= ["G2" "C3" "E3"] (harmony/invert-chord ["C3" "E3" "G3"] -1)))))

(deftest arp-test
  (testing "Arpeggiator patterns"
    (let [notes ["C3" "E3" "G3"]]
      (is (= ["C3" "E3" "G3"] (harmony/arp notes :up)))
      (is (= ["G3" "E3" "C3"] (harmony/arp notes :down)))
      (is (= ["C3" "E3" "G3" "E3"] (harmony/arp notes :up-down)))
      (is (= ["G3" "E3" "C3" "E3"] (harmony/arp notes :down-up)))
      (is (= ["C3" "G3" "E3"] (harmony/arp notes :converge))))))

(deftest progression-test
  (testing "Chord progression generation"
    (is (= [["E3" "G3" "B3" "D4"] ["A3" "C4" "E4" "G4"]]
           (harmony/progression :e :dorian [1 4] :type :min7)))))
