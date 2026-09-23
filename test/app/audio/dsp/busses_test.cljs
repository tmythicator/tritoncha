(ns app.audio.dsp.busses-test
  "Unit tests for audio bus taxonomy, normalization, and routing mappings."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.instruments.catalog :as catalog]
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
    (is (true? (busses/valid-bus? :bus/lead)))
    (is (true? (busses/valid-bus? :bus/direct)))
    (is (false? (busses/valid-bus? :bus/glitch)))
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
    (is (= :bus/direct (busses/instrument-bus :click)))
    (is (= :bus/master (busses/instrument-bus :unregistered-synth-xyz)))))

(deftest instrument-catalog-predicates-and-spec-test
  (testing "Classifies instruments from keyword, track map, and explicit :bus in spec map"
    ;; Drums
    (is (true? (catalog/drum? :kick)))
    (is (true? (catalog/drum? :snare-wire)))
    (is (true? (catalog/drum? :hat)))
    (is (true? (catalog/drum? :hats)))
    (is (true? (catalog/drum? :cymb)))
    (is (true? (catalog/drum? :toms)))
    (is (true? (catalog/drum? :perc)))
    (is (true? (catalog/drum? :percussion)))
    (is (true? (catalog/drum? :break)))
    (is (true? (catalog/drum? {:bus :bus/drums})))
    (is (true? (catalog/drum? {:inst :kick})))
    (is (true? (catalog/drum? {:pattern "k . . ."})))
    (is (false? (catalog/drum? :bass-analog)))
    (is (false? (catalog/lead? :cymb)))
    (is (false? (catalog/lead? :hat)))

    ;; Bass + Sub
    (is (true? (catalog/bass? :bass-analog)))
    (is (true? (catalog/bass? :liquid-reese)))
    (is (true? (catalog/bass? {:bus :bus/bass})))
    (is (true? (catalog/bass? {:inst :sub-pure})))
    (is (false? (catalog/bass? :kick)))

    (is (true? (catalog/sub? :sub)))
    (is (true? (catalog/sub? :sub-pure)))
    (is (true? (catalog/sub? :sub-808)))
    (is (false? (catalog/sub? :lead)))

    ;; Leads
    (is (true? (catalog/lead? :lead-pluck)))
    (is (true? (catalog/lead? :tokyo-drift)))
    (is (true? (catalog/lead? {:bus :bus/lead})))
    (is (false? (catalog/lead? :pad-cinema)))

    ;; Pads + Space
    (is (true? (catalog/pad? :pad-cinema)))
    (is (true? (catalog/pad? :pad-strings)))
    (is (true? (catalog/pad? {:bus :bus/space})))
    (is (false? (catalog/pad? :kick)))

    ;; Synth (non-drum)
    (is (true? (catalog/synth? :bass-analog)))
    (is (true? (catalog/synth? :lead-pluck)))
    (is (false? (catalog/synth? :kick)))

    ;; Sound Category
    (is (= :drums (catalog/sound-category :kick)))
    (is (= :bass (catalog/sound-category :bass-analog)))
    (is (= :leads (catalog/sound-category :lead-pluck)))
    (is (= :pads (catalog/sound-category :pad-cinema)))
    (is (= :fx (catalog/sound-category :fx-laser)))
    (is (= :fx (catalog/sound-category {:category :fx :bus :bus/lead})))
    (is (= :pads (catalog/sound-category {:bus :bus/space})))))

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

(deftest drum-mode-predicate-test
  (testing "Classifies valid drum character modes"
    (is (true? (catalog/drum-mode? :idm)))
    (is (true? (catalog/drum-mode? :natural)))
    (is (true? (catalog/drum-mode? :analog)))
    (is (true? (catalog/drum-mode? :industrial)))
    (is (false? (catalog/drum-mode? :bass)))
    (is (false? (catalog/drum-mode? :kick)))
    (is (false? (catalog/drum-mode? nil)))
    (is (false? (catalog/drum-mode? "idm")))))

(deftest composite-and-individual-drum-predicates-test
  (testing "Distinguishes all-in-one composite drum tracks from individual drum voices"
    (is (true? (catalog/composite-drums? :drums)))
    (is (true? (catalog/composite-drums? :drum)))
    (is (true? (catalog/composite-drums? :break)))
    (is (true? (catalog/composite-drums? :kit)))
    (is (false? (catalog/composite-drums? :kick)))
    (is (false? (catalog/composite-drums? :snare)))

    (is (true? (catalog/individual-drum? :kick)))
    (is (true? (catalog/individual-drum? :snare)))
    (is (true? (catalog/individual-drum? :hat)))
    (is (true? (catalog/individual-drum? :tom)))
    (is (false? (catalog/individual-drum? :drums)))
    (is (false? (catalog/individual-drum? :drum)))
    (is (false? (catalog/individual-drum? :bass-analog)))))
