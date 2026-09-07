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
    (is (= :bus/drums (busses/instrument-bus :hats)))
    (is (= :bus/drums (busses/instrument-bus :perc)))
    (is (= :bus/drums (busses/instrument-bus :percussion)))
    (is (= :bus/drums (busses/instrument-bus :break)))
    (is (= :bus/bass (busses/instrument-bus :bass-analog)))
    (is (= :bus/bass (busses/instrument-bus :bass-303)))
    (is (= :bus/space (busses/instrument-bus :pad-cinema)))
    (is (= :bus/lead (busses/instrument-bus :lead-pluck)))
    (is (= :bus/glitch (busses/instrument-bus :glitch-texture)))
    (is (= :bus/master (busses/instrument-bus :unregistered-synth-xyz)))))

(deftest bus-predicates-and-spec-test
  (testing "Classifies instruments from keyword, track map, and explicit :bus in spec map"
    ;; Drums
    (is (true? (busses/drum? :kick)))
    (is (true? (busses/drum? :snare-wire)))
    (is (true? (busses/drum? :hats)))
    (is (true? (busses/drum? :perc)))
    (is (true? (busses/drum? :percussion)))
    (is (true? (busses/drum? :break)))
    (is (true? (busses/drum? {:bus :bus/drums})))
    (is (true? (busses/drum? {:inst :kick})))
    (is (false? (busses/drum? :bass-analog)))

    ;; Bass & Sub
    (is (true? (busses/bass? :bass-analog)))
    (is (true? (busses/bass? :bass-reese)))
    (is (true? (busses/bass? {:bus :bus/bass})))
    (is (true? (busses/bass? {:inst :sub-pure})))
    (is (false? (busses/bass? :kick)))

    (is (true? (busses/sub? :sub)))
    (is (true? (busses/sub? :sub-pure)))
    (is (true? (busses/sub? :sub-808)))
    (is (false? (busses/sub? :lead)))

    ;; Leads
    (is (true? (busses/lead? :lead-pluck)))
    (is (true? (busses/lead? :tokyo-drift)))
    (is (true? (busses/lead? {:bus :bus/lead})))
    (is (false? (busses/lead? :pad-cinema)))

    ;; Pads & Space
    (is (true? (busses/pad? :pad-cinema)))
    (is (true? (busses/pad? :pad-shimmer)))
    (is (true? (busses/pad? {:bus :bus/space})))
    (is (false? (busses/pad? :kick)))

    ;; Synth (non-drum)
    (is (true? (busses/synth? :bass-analog)))
    (is (true? (busses/synth? :lead-pluck)))
    (is (false? (busses/synth? :kick)))

    ;; Sound Category
    (is (= :drums (busses/sound-category :kick)))
    (is (= :bass (busses/sound-category :bass-analog)))
    (is (= :leads (busses/sound-category :lead-pluck)))
    (is (= :pads (busses/sound-category :pad-cinema)))
    (is (= :fx (busses/sound-category :fx-laser)))
    (is (= :fx (busses/sound-category {:category :fx :bus :bus/lead})))
    (is (= :pads (busses/sound-category {:bus :bus/space})))))

(deftest category-default-bus-test
  (testing "Resolves canonical default bus based directly on category"
    (is (= :bus/drums (busses/category-default-bus :drums)))
    (is (= :bus/drums (busses/category-default-bus :drum)))
    (is (= :bus/bass (busses/category-default-bus :bass)))
    (is (= :bus/space (busses/category-default-bus :pads)))
    (is (= :bus/space (busses/category-default-bus :pad)))
    (is (= :bus/lead (busses/category-default-bus :leads)))
    (is (= :bus/lead (busses/category-default-bus :lead)))
    (is (= :bus/lead (busses/category-default-bus :fx)))
    (is (= :bus/drums (busses/instrument-bus :drums)))
    (is (= :bus/bass (busses/instrument-bus :bass)))
    (is (= :bus/space (busses/instrument-bus :pads)))
    (is (= :bus/lead (busses/instrument-bus :leads)))))
