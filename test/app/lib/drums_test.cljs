(ns app.lib.drums-test
  (:require
   [app.lib.drums :refer [core-drum-instruments core-drum-voices]]
   [cljs.test :refer [deftest is testing]]))

(deftest core-drum-instruments-catalog-test
  (testing "Core drum instruments catalog contains synth components"
    (let [required-instruments [:kick :snare-body :snare-wire :snare-rim :snare-ghost :hat-closed :hat-open :click]]
      (doseq [k required-instruments]
        (let [spec (get core-drum-instruments k)]
          (is (some? spec) (str "Drum instrument " k " must exist in core-drum-instruments"))
          (is (keyword? (:type spec)) (str "Drum instrument " k " must specify a keyword :type")))))))

(deftest drum-voices-catalog-test
  (testing "Drum voices catalog contains layered kits and hits"
    (let [required-drums [:kick :snare :sn-rs :sn-clk :sn-gh :sn-roll :hh-c :hh-o :hh-clk :click]]
      (doseq [drum-key required-drums]
        (let [voice (get core-drum-voices drum-key)]
          (is (some? voice) (str "Drum voice " drum-key " must exist in core-drum-voices"))
          (is (or (keyword? (:node voice)) (vector? (:layers voice)))
              (str "Drum voice " drum-key " must specify a :node or :layers")))))))
