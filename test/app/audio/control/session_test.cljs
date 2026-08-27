(ns app.audio.control.session-test
  "Unit tests for stateful session key management, degree resolution, modulation, and transposition."
  (:require [app.audio.control.session :as session]
            [app.audio.theory.harmony :as harmony :refer [_ chord deg]]
            [app.state :refer [audio-state]]
            [cljs.test :refer [deftest is testing]]))

(deftest session-key-orchestration-test
  (testing "set-key! and current-key session management"
    (session/set-key! :e :phrygian 1)
    (is (= {:root :e :mode :phrygian :octave 1} (session/current-key)))
    (is (= ["E1" nil "E1" "F1"] (session/d [1 _ 1 2])))
    (is (= ["E1" "F1" "G1" "A1" "B1" "C2" "D2"] (subvec (session/sc 1) 0 7)))))

(deftest transpose-all-melodic-tracks-test
  (testing "transpose-all! shifts all melodic tracks evenly by N semitones while preserving relative octaves"
    (let [bass-pat    (atom {:inst :bass    :notes ["E1" "E1" nil "G1"] :oct 1})
          sub-pat     (atom {:inst :sub     :notes ["E1" nil "E1" nil]  :oct 1})
          lead-pat    (atom {:inst :lead    :notes ["E3" "G3" "B3" "D4"] :oct 3})
          strings-pat (atom {:inst :strings :notes [(chord :e :min7 3)] :oct 3})
          drums-pat   (atom {:inst :drums   :notes [[:kick 1.0 "D1"] [:snare 1.0 "G3"]]})]
      (swap! audio-state assoc :active-tracks
             {:bass    {:pattern bass-pat    :muted? (atom false) :solo? (atom false)}
              :sub     {:pattern sub-pat     :muted? (atom false) :solo? (atom false)}
              :lead    {:pattern lead-pat    :muted? (atom false) :solo? (atom false)}
              :strings {:pattern strings-pat :muted? (atom false) :solo? (atom false)}
              :drums   {:pattern drums-pat   :muted? (atom false) :solo? (atom false)}})

      ;; Shift UP by +3 semitones (E -> G)
      (session/transpose-all! 3)
      (is (= ["G1" "G1" nil "A#1"] (:notes @bass-pat)) "Bass moves E1 -> G1 (stays in octave 1)")
      (is (= ["G1" nil "G1" nil] (:notes @sub-pat)) "Sub moves E1 -> G1 (stays in octave 1)")
      (is (= ["G3" "A#3" "D4" "F4"] (:notes @lead-pat)) "Lead moves E3 -> G3 (stays in octave 3-4)")
      (is (= [["G3" "A#3" "D4" "F4"]] (:notes @strings-pat)) "Chords transpose cleanly")
      (is (= [[:kick 1.0 "D1"] [:snare 1.0 "G3"]] (:notes @drums-pat)) "Drums are unaffected")

      ;; Shift DOWN by -3 semitones (G -> E, return to original)
      (session/transpose-all! -3)
      (is (= ["E1" "E1" nil "G1"] (:notes @bass-pat)) "Bass returns to E1")
      (is (= ["E1" nil "E1" nil] (:notes @sub-pat)) "Sub returns to E1")
      (is (= ["E3" "G3" "B3" "D4"] (:notes @lead-pat)) "Lead returns to E3")
      (is (= [["E3" "G3" "B3" "D4"]] (:notes @strings-pat)) "Chords return to E min7")

      (swap! audio-state assoc :active-tracks {}))))

(deftest modulate-all-modal-degrees-test
  (testing "modulate-all! modulates modal degrees preserving individual track register"
    (session/set-key! :e :phrygian 1)
    (let [bass-pat (atom {:inst :bass :notes (deg :e :phrygian [1 _ 1 3] {:octave 1}) :deg [1 _ 1 3] :oct 1})
          lead-pat (atom {:inst :lead :notes (deg :e :phrygian [1 3 5 7] {:octave 3}) :deg [1 3 5 7] :oct 3})
          kick-pat (atom {:inst :kick :notes [:kick nil :kick nil]})]
      (swap! audio-state assoc :active-tracks
             {:bass {:pattern bass-pat :muted? (atom false) :solo? (atom false)}
              :lead {:pattern lead-pat :muted? (atom false) :solo? (atom false)}
              :kick {:pattern kick-pat :muted? (atom false) :solo? (atom false)}})

      ;; Modulate to D Dorian (2-arg call without octave override)
      (session/modulate-all! :d :dorian)
      (is (= {:root :d :mode :dorian :octave 1} (session/current-key)))
      (is (= ["D1" nil "D1" "F1"] (:notes @bass-pat)) "Bass recalculates in D Dorian octave 1")
      (is (= ["D3" "F3" "A3" "C4"] (:notes @lead-pat)) "Lead recalculates in D Dorian octave 3")
      (is (= [:kick nil :kick nil] (:notes @kick-pat)) "Kick drums untouched")

      ;; Modulate to A Aeolian
      (session/modulate-all! :a :aeolian)
      (is (= ["A1" nil "A1" "C2"] (:notes @bass-pat)) "Bass stays deep in octave 1/2")
      (is (= ["A3" "C4" "E4" "G4"] (:notes @lead-pat)) "Lead stays high in octave 3/4")

      (swap! audio-state assoc :active-tracks {}))))
