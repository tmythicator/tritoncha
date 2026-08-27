(ns app.audio.dsp.busses-test
  "Unit tests for audio bus taxonomy, normalization, and routing mappings."
  (:require [app.audio.dsp.busses :as busses]
            [cljs.test :refer [deftest is testing]]))

(deftest bus-normalization-test
  (testing "Normalizes keywords to :bus/<name> format"
    (is (= :bus/drums (busses/normalize-bus-key :drums)))
    (is (= :bus/drums (busses/normalize-bus-key :bus/drums)))
    (is (= :bus/bass (busses/normalize-bus-key :bass)))
    (is (= :bus/master (busses/normalize-bus-key :master)))
    (is (nil? (busses/normalize-bus-key nil)))))

(deftest bus-validity-test
  (testing "Identifies registered and valid audio busses"
    (is (true? (busses/valid-bus? :bus/master)))
    (is (true? (busses/valid-bus? :drums)))
    (is (true? (busses/valid-bus? :bus/bass)))
    (is (true? (busses/valid-bus? :bus/space)))
    (is (true? (busses/valid-bus? :bus/glitch)))
    (is (false? (busses/valid-bus? :non-existent-bus)))
    (is (false? (busses/valid-bus? nil)))))

(deftest instrument-bus-mapping-test
  (testing "Resolves default bus for instruments with master fallback"
    (is (= :bus/drums (busses/instrument-bus :kick)))
    (is (= :bus/drums (busses/instrument-bus :snare)))
    (is (= :bus/bass (busses/instrument-bus :saw-bass)))
    (is (= :bus/bass (busses/instrument-bus :acid-bass)))
    (is (= :bus/space (busses/instrument-bus :dark-pad)))
    (is (= :bus/glitch (busses/instrument-bus :glitch-texture)))
    (is (= :bus/master (busses/instrument-bus :unregistered-synth-xyz)))))
