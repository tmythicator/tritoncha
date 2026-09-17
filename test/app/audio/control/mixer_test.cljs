(ns app.audio.control.mixer-test
  "Unit tests for mixer bus state transitions, mute logic, and drum toggles."
  (:require [app.audio.control.mixer :as mixer]
            [app.state :refer [audio-state]]
            [cljs.test :refer [deftest is testing]]))

(deftest track-mute-and-solo-test
  (testing "Mutes and solos tracks in active-tracks map"
    (let [k-pat (atom {:inst :kick :muted? false :solo? false})
          b-pat (atom {:inst :bass :muted? false :solo? false})]
      (swap! audio-state assoc :active-tracks
             {:kick {:pattern k-pat}
              :bass {:pattern b-pat}})

      (mixer/mute! :kick)
      (is (true? (:muted? @k-pat)))
      (is (false? (:muted? @b-pat)))

      (mixer/unmute! :kick)
      (is (false? (:muted? @k-pat)))

      (mixer/solo! :bass)
      (is (true? (:solo-mode? @audio-state)))
      (is (false? (:solo? @k-pat)))
      (is (true? (:solo? @b-pat)))

      (mixer/unsolo!)
      (is (false? (:solo-mode? @audio-state)))
      (is (false? (:solo? @b-pat))))))

(deftest toggle-drums-test
  (testing "toggle-drums! switches between undrum! and redrum!"
    (swap! audio-state assoc :drums-muted? false)
    (is (= :undrummed (mixer/toggle-drums!)))
    (is (true? (:drums-muted? @audio-state)))
    (is (= :redrummed (mixer/toggle-drums!)))
    (is (false? (:drums-muted? @audio-state)))))

(deftest master-bus-volume-and-mute-test
  (testing "Sets master bus volume and toggles mute state"
    (mixer/set-volume! :bus/master -4.5)
    (is (= -4.5 (get-in @audio-state [:bus-levels :bus/master])))

    (mixer/set-volume! :master 2.0)
    (is (= 2.0 (get-in @audio-state [:bus-levels :bus/master])))

    (mixer/mute-bus! :bus/master)
    (is (true? (get-in @audio-state [:bus-mutes :bus/master])))

    (mixer/unmute-bus! :bus/master)
    (is (false? (get-in @audio-state [:bus-mutes :bus/master])))

    (mixer/toggle-bus! :bus/master)
    (is (true? (get-in @audio-state [:bus-mutes :bus/master])))

    (mixer/toggle-bus! :bus/master)
    (is (false? (get-in @audio-state [:bus-mutes :bus/master])))))
