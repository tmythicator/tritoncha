(ns app.audio.control.stacker-test
  "Unit tests for multi-track batch loop stacking and track arguments resolution."
  (:require [app.audio.control.stacker :as stacker]
            [app.state :refer [audio-state]]
            [cljs.test :refer [deftest is testing]]))

(deftest stack-pairs-test
  (testing "Launches tracks from vector pairs"
    (stacker/stack!
     [:kick {:notes ["C2"] :step "4n"}]
     [:snare {:notes ["D2"] :step "8n"}])
    (let [active (:active-tracks @audio-state)]
      (is (contains? active :kick))
      (is (contains? active :snare)))
    (stacker/unstack! :kick :snare)
    (is (not (contains? (:active-tracks @audio-state) :kick)))
    (is (not (contains? (:active-tracks @audio-state) :snare)))))

(deftest drum-exclusion-test
  (testing "Launching individual drum tracks automatically silences composite drum track"
    (stacker/stack! [:drums {:notes ["C2"] :step "4n"}])
    (is (contains? (:active-tracks @audio-state) :drums))
    (stacker/stack! [:kick {:notes ["C2"] :step "4n"}])
    (is (contains? (:active-tracks @audio-state) :kick))
    (is (not (contains? (:active-tracks @audio-state) :drums)))
    (stacker/unstack! :kick))

  (testing "Launching composite drum track automatically silences individual drum tracks"
    (stacker/stack! [:kick {:notes ["C2"]}] [:snare {:notes ["D2"]}])
    (is (contains? (:active-tracks @audio-state) :kick))
    (is (contains? (:active-tracks @audio-state) :snare))
    (stacker/stack! [:drums {:notes ["C2"]}])
    (is (contains? (:active-tracks @audio-state) :drums))
    (is (not (contains? (:active-tracks @audio-state) :kick)))
    (is (not (contains? (:active-tracks @audio-state) :snare)))
    (stacker/unstack! :drums)))

(deftest stack-input-formats-test
  (testing "Supports mode prefix keyword"
    (stacker/stack! :idm [:kick {:notes ["C2"]}])
    (is (contains? (:active-tracks @audio-state) :kick))
    (stacker/unstack! :kick))

  (testing "Supports :mod prefix pair"
    (stacker/stack! :mod :analog [:snare {:notes ["D2"]}])
    (is (contains? (:active-tracks @audio-state) :snare))
    (stacker/unstack! :snare))

  (testing "Supports map syntax with :mod"
    (stacker/stack! {:mod :natural :kick {:notes ["C2"]} :snare {:notes ["D2"]}})
    (is (contains? (:active-tracks @audio-state) :kick))
    (is (contains? (:active-tracks @audio-state) :snare))
    (stacker/unstack! :kick :snare))

  (testing "Supports plain map syntax"
    (stacker/stack! {:kick {:notes ["C2"]} :hat {:notes ["G2"]}})
    (is (contains? (:active-tracks @audio-state) :kick))
    (is (contains? (:active-tracks @audio-state) :hat))
    (stacker/unstack! :kick :hat)))
