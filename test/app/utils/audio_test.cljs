(ns app.utils.audio-test
  "Unit tests for audio safety utilities, pitch parsing, note, and MIDI conversions."
  (:require [app.utils.audio :as audio]
            [cljs.test :refer [deftest is testing]]))

(deftest parse-note-test
  (testing "Parses note strings and keywords into pitch and octave"
    (is (= {:pitch "C", :octave 4} (audio/parse-note "C4")))
    (is (= {:pitch "F#", :octave 3} (audio/parse-note "F#3")))
    (is (= {:pitch "EB", :octave 2} (audio/parse-note :eb2)))
    (is (= {:pitch "A", :octave 3} (audio/parse-note "A" 3)))
    (is (nil? (audio/parse-note nil)))))

(deftest note-midi-conversion-test
  (testing "note->midi and midi->note bidirectional conversion"
    (is (= 60 (audio/note->midi "C4")))
    (is (= 69 (audio/note->midi "A4")))
    (is (= 40 (audio/note->midi "E2")))
    (is (= "C4" (audio/midi->note 60)))
    (is (= "A4" (audio/midi->note 69)))
    (is (= "E2" (audio/midi->note 40)))))

(deftest track-predicates-test
  (testing "is-drum-track? and is-bass-track? predicates"
    (is (true? (audio/is-drum-track? :kick)))
    (is (true? (audio/is-drum-track? :snare)))
    (is (true? (audio/is-drum-track? :drums)))
    (is (true? (audio/is-drum-track? :hh-c)))
    (is (false? (audio/is-drum-track? :bass)))
    (is (false? (audio/is-drum-track? :lead)))
    (is (true? (audio/is-bass-track? :bass)))
    (is (true? (audio/is-bass-track? :sub)))
    (is (true? (audio/is-bass-track? :acid)))
    (is (false? (audio/is-bass-track? :pad)))))

(deftest format-key-test
  (testing "format-key outputs clean uppercase harmonic strings"
    (is (= "E PHRYGIAN" (audio/format-key {:root :e :mode :phrygian})))
    (is (= "D DORIAN" (audio/format-key {:root :d :mode :dorian :octave 2})))
    (is (= "E PHRYGIAN" (audio/format-key nil)))))
