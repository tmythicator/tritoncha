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

(deftest duration-and-step-test
  (testing "dur->seconds and step->mult calculations"
    (is (pos? (audio/dur->seconds "16n" 168)))
    (is (pos? (audio/dur->seconds "1m" 168)))
    (is (= 1 (audio/step->mult "64n")))
    (is (= 2 (audio/step->mult "32n")))
    (is (= 4 (audio/step->mult "16n")))
    (is (= 8 (audio/step->mult "8n")))
    (is (= 16 (audio/step->mult "4n")))
    (is (= 64 (audio/step->mult "1m")))))

(deftest format-key-test
  (testing "format-key outputs clean uppercase harmonic strings"
    (is (= "E PHRYGIAN" (audio/format-key {:root :e :mode :phrygian})))
    (is (= "D DORIAN" (audio/format-key {:root :d :mode :dorian :octave 2})))
    (is (= "E PHRYGIAN" (audio/format-key nil)))))

(deftest normalize-opts-test
  (testing "normalize-opts unifies numbers and option maps"
    (is (= {:octave 2} (audio/normalize-opts 2 1)))
    (is (= {:octave 4 :octaves 2} (audio/normalize-opts {:octave 4 :octaves 2} 1)))
    (is (= {:octave 1} (audio/normalize-opts nil 1)))))
