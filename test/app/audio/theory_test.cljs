(ns app.audio.theory-test
  (:require [cljs.test :refer [deftest is testing]]
            [app.audio.theory :as theory]))

(deftest note-conversion-test
  (testing "note->midi and midi->note bidirectional conversion"
    (is (= 60 (theory/note->midi "C4")))
    (is (= 69 (theory/note->midi "A4")))
    (is (= 40 (theory/note->midi "E2")))
    (is (= "C4" (theory/midi->note 60)))
    (is (= "A4" (theory/midi->note 69)))
    (is (= "E2" (theory/midi->note 40)))))

(deftest transpose-test
  (testing "Note transpositions"
    (is (= "G4" (theory/transpose "C4" 7)))
    (is (= "C5" (theory/transpose "C4" 12)))
    (is (= "C3" (theory/transpose "C4" -12)))
    (is (= ["D4" "F#4" "A4"] (theory/transpose ["C4" "E4" "G4"] 2)))))

(deftest scale-generation-test
  (testing "Dorian scale generation"
    (is (= ["D3" "E3" "F3" "G3" "A3" "B3" "C4"] (theory/scale :d :dorian))))

  (testing "Phrygian scale generation"
    (is (= ["E1" "F1" "G1" "A1" "B1" "C2" "D2"] (theory/scale :e :phrygian 1))))

  (testing "Hirajoshi Japanese pentatonic scale"
    (is (= ["E2" "F#2" "G2" "B2" "C3"] (theory/scale :e :hirajoshi 2))))

  (testing "Hungarian Minor exotic scale"
    (is (= ["A3" "B3" "C4" "D#4" "E4" "F4" "G#4"] (theory/scale :a :hungarian-minor 3)))))

(deftest scale-degrees-test
  (testing "Resolves degrees to scale notes and respects rests"
    (is (= ["E1" nil "E1" "F1"]
           (theory/deg :e :phrygian [1 theory/_ 1 2] 1)))
    (is (= ["D3" "F3" "A3" "C4"]
           (theory/deg :d :dorian [1 3 5 7])))))

(deftest chord-generation-test
  (testing "Major, minor and extended chords"
    (is (= ["C3" "E3" "G3"] (theory/chord :c :maj 3)))
    (is (= ["A3" "C4" "E4"] (theory/chord :a :min 3)))
    (is (= ["E3" "G3" "B3" "D4"] (theory/chord :e :min7 3)))
    (is (= ["E3" "G3" "B3" "D4" "F#4"] (theory/chord :e :min9 3)))))

(deftest arp-test
  (testing "Arpeggiator patterns"
    (let [notes ["C3" "E3" "G3"]]
      (is (= ["C3" "E3" "G3"] (theory/arp notes :up)))
      (is (= ["G3" "E3" "C3"] (theory/arp notes :down)))
      (is (= ["C3" "E3" "G3" "E3"] (theory/arp notes :up-down)))
      (is (= ["G3" "E3" "C3" "E3"] (theory/arp notes :down-up)))
      (is (= ["C3" "G3" "E3"] (theory/arp notes :converge))))))
