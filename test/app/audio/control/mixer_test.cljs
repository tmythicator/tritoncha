(ns app.audio.control.mixer-test
  "Unit tests for mixer bus state transitions, mute logic, and drum toggles."
  (:require [app.audio.control.mixer :as mixer]
            [app.state :refer [audio-state]]
            [cljs.test :refer [deftest is testing]]))

(deftest track-mute-and-solo-test
  (testing "Mutes and solos tracks in active-tracks map"
    (let [k-pat (atom {:inst :kick})
          b-pat (atom {:inst :bass})
          k-muted (atom false)
          b-muted (atom false)
          k-solo (atom false)
          b-solo (atom false)]
      (swap! audio-state assoc :active-tracks
             {:kick {:pattern k-pat :muted? k-muted :solo? k-solo}
              :bass {:pattern b-pat :muted? b-muted :solo? b-solo}})

      (mixer/mute! :kick)
      (is (true? @k-muted))
      (is (false? @b-muted))

      (mixer/unmute! :kick)
      (is (false? @k-muted))

      (mixer/solo! :bass)
      (is (true? (:solo-mode? @audio-state)))
      (is (false? @k-solo))
      (is (true? @b-solo))

      (mixer/unsolo!)
      (is (false? (:solo-mode? @audio-state)))
      (is (false? @b-solo)))))

(deftest toggle-drums-test
  (testing "toggle-drums! switches between undrum! and redrum!"
    (swap! audio-state assoc :drums-muted? false)
    (is (= :undrummed (mixer/toggle-drums!)))
    (is (true? (:drums-muted? @audio-state)))
    (is (= :redrummed (mixer/toggle-drums!)))
    (is (false? (:drums-muted? @audio-state)))))
