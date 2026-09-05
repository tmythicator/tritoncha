(ns app.lib.drums-test
  (:require
   [app.lib.drums :refer [core-drum-instruments core-drum-voices drum-keyword? mini-notation-aliases]]
   [cljs.test :refer [deftest is testing]]))

(deftest core-drum-instruments-catalog-test
  (testing "Core drum instruments catalog contains synth components"
    (let [required-instruments [:kick :snare-body :snare-wire :snare-rim :snare-ghost :hat-closed :hat-open
                                :ride :tom :sn-crack
                                :ride-bell :tom-high :tom-mid :tom-low
                                :crash-16 :crash-17 :crash-18 :splash :china :cowbell]]
      (doseq [k required-instruments]
        (let [spec (get core-drum-instruments k)]
          (is (some? spec) (str "Drum instrument " k " must exist in core-drum-instruments"))
          (is (keyword? (:type spec)) (str "Drum instrument " k " must specify a keyword :type")))))))

(deftest drum-voices-catalog-test
  (testing "Drum voices catalog contains layered kits and hits"
    (let [required-drums [:kick :snare :sn-rs :sn-clk :sn-gh :sn-roll :sn-crack :hh-c :hh-o :hh-clk
                          :ride :ride-bell :tom :tom-high :tom-mid :tom-low
                          :crash-16 :crash-17 :crash-18 :splash :china :cowbell :click]]
      (doseq [drum-key required-drums]
        (let [voice (get core-drum-voices drum-key)]
          (is (some? voice) (str "Drum voice " drum-key " must exist in core-drum-voices"))
          (is (or (keyword? (:node voice)) (vector? (:layers voice)))
              (str "Drum voice " drum-key " must specify a :node or :layers")))))))

(deftest expanded-drum-aliases-test
  (testing "Mini-notation drum aliases resolve to canonical keywords"
    (is (= :ride-bell (get mini-notation-aliases "rb")))
    (is (= :ride-bell (get mini-notation-aliases "ride-bell")))
    (is (= :tom-high (get mini-notation-aliases "th")))
    (is (= :tom-mid (get mini-notation-aliases "tm")))
    (is (= :tom-low (get mini-notation-aliases "tl")))
    (is (= :crash-16 (get mini-notation-aliases "cr16")))
    (is (= :crash-17 (get mini-notation-aliases "cr17")))
    (is (= :crash-18 (get mini-notation-aliases "cr18")))
    (is (= :splash (get mini-notation-aliases "sp")))
    (is (= :china (get mini-notation-aliases "ch")))
    (is (= :cowbell (get mini-notation-aliases "cb"))))

  (testing "Drum keyword recognition recognizes expanded drum set and short aliases"
    (doseq [k [:rb :th :tm :tl :cr16 :cr17 :cr18 :sp :ch :cb
               :ride-bell :tom-high :tom-mid :tom-low
               :crash-16 :crash-17 :crash-18 :splash :china :cowbell]]
      (is (drum-keyword? k) (str "Expected " k " to be recognized as drum keyword")))))
