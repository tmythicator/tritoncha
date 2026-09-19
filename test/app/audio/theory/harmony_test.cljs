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
           (harmony/deg :d :dorian [1 3 5 7]))))
  (testing "Accidental scale degrees (flat/sharp)"
    (is (= ["E1" "F1" "G1" "A1" "A#1"]
           (harmony/deg :e :phrygian [1 2 3 4 :b5] 1)))
    (is (= ["D3" "F3" "A3" "C4"]
           (harmony/deg :d :dorian [1 :3 5 7]))))
  (testing "Deferred scale-degree pattern creation and resolution"
    (let [pat (harmony/deg [1 harmony/_ 3 5] {:octave 3})]
      (is (harmony/deg-pattern? pat))
      (is (= [1 nil 3 5] pat))
      (is (= ["C3" nil "D#3" "G3"]
             (harmony/resolve-track-notes pat [:c :dorian 1]))))))

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
      (is (= ["C3" "G3" "E3"] (harmony/arp notes :converge)))))

  (testing "Thread-friendly arp with ->>"
    (let [res (->> (harmony/chord :c :maj 3)
                   (harmony/arp :up-down))]
      (is (= ["C3" "E3" "G3" "E3"] res)))))

(deftest progression-test
  (testing "Chord progression generation"
    (is (= [["E3" "G3" "B3" "D4"] ["A3" "C4" "E4" "G4"]]
           (harmony/progression :e :dorian [1 4] :type :min7))))

  (testing "Deferred chord progression resolution"
    (let [prog (harmony/progression [1 4] :min7 3)
          res  (harmony/resolve-track-notes prog [:e :dorian 1])]
      (is (= [["E3" "G3" "B3" "D4"] ["A3" "C4" "E4" "G4"]] res))))

  (testing "Progression with explicit chord qualities per degree"
    (let [prog (harmony/progression [[1 :min9] [4 :dom7]] 3)
          res  (harmony/resolve-track-notes prog [:c :dorian 1])]
      (is (= [["C3" "D#3" "G3" "A#3" "D4"] ["F3" "A3" "C4" "D#4"]] res)))))

(deftest mathematical-modes-and-intervals-test
  (testing "All defined scales strictly start at 0, are strictly ascending, and stay within octave [0, 12)"
    (doseq [[mode intervals] harmony/scale-intervals]
      (is (vector? intervals) (str mode " must be a vector"))
      (is (zero? (first intervals)) (str mode " must start at interval 0 (root)"))
      (is (apply < intervals) (str mode " must be strictly ascending intervals"))
      (is (every? #(and (>= % 0) (< % 12)) intervals) (str mode " must be within single octave 0..11"))))

  (testing "7 Diatonic modes are exact cyclic rotations of the major scale step intervals"
    (let [major-steps [2 2 1 2 2 2 1]
          rotate (fn [coll n] (vec (concat (drop n coll) (take n coll))))
          steps->intervals (fn [steps]
                             (reduce (fn [acc step] (conj acc (+ (peek acc) step)))
                                     [0]
                                     (pop steps)))]
      (is (= (harmony/scale-intervals :ionian)     (steps->intervals (rotate major-steps 0))))
      (is (= (harmony/scale-intervals :dorian)     (steps->intervals (rotate major-steps 1))))
      (is (= (harmony/scale-intervals :phrygian)   (steps->intervals (rotate major-steps 2))))
      (is (= (harmony/scale-intervals :lydian)     (steps->intervals (rotate major-steps 3))))
      (is (= (harmony/scale-intervals :mixolydian) (steps->intervals (rotate major-steps 4))))
      (is (= (harmony/scale-intervals :aeolian)    (steps->intervals (rotate major-steps 5))))
      (is (= (harmony/scale-intervals :locrian)    (steps->intervals (rotate major-steps 6))))))

  (testing "Mathematical proof: E Minor and E Phrygian differ strictly on Degree 2"
    (let [e-minor    (harmony/scale :e :minor 1)
          e-phrygian (harmony/scale :e :phrygian 1)]
      ;; Both scales have 7 notes
      (is (= 7 (count e-minor) (count e-phrygian)))
      ;; Degrees 1, 3, 4, 5, 6, 7 are 100% identical in note name and pitch
      (is (= (get e-minor 0) (get e-phrygian 0) "E1")  "Degree 1 is identical (E)")
      (is (= (get e-minor 2) (get e-phrygian 2) "G1")  "Degree 3 is identical (G)")
      (is (= (get e-minor 3) (get e-phrygian 3) "A1")  "Degree 4 is identical (A)")
      (is (= (get e-minor 4) (get e-phrygian 4) "B1")  "Degree 5 is identical (B)")
      (is (= (get e-minor 5) (get e-phrygian 5) "C2")  "Degree 6 is identical (C)")
      (is (= (get e-minor 6) (get e-phrygian 6) "D2")  "Degree 7 is identical (D)")
      ;; Degree 2 is the single point of difference: F# vs F (1 semitone difference)
      (is (= "F#1" (get e-minor 1))    "E Minor has major 2nd (F#)")
      (is (= "F1"  (get e-phrygian 1)) "E Phrygian has minor 2nd (F)"))))

