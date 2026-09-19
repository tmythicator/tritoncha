(ns app.lib.drums-test
  (:require
   [app.audio.dsp.busses :refer [drum-keyword?]]
   [app.audio.dsp.worklet.compiler :refer [parse-step-hit resolve-target-inst]]
   [app.audio.dsp.worklet.protocol :refer [drum-remaps inst-keyword->id]]
   [app.custom.drums :refer [user-drums]]
   [app.lib.drums :refer [core-drums]]
   [app.ui.instrument-browser.state :refer [custom-file-inst?]]
   [cljs.test :refer [deftest is testing]]))

(deftest core-drums-catalog-test
  (testing "Core drum instruments catalog contains synth components"
    (let [required-instruments [:kick :snare-body :snare-wire :snare-rim :snare-ghost :hat-closed :hat-open
                                :ride :tom :sn-crack
                                :ride-bell :tom-high :tom-mid :tom-low
                                :crash-16 :crash-17 :crash-18 :splash :china :cowbell]]
      (doseq [k required-instruments]
        (let [spec (get core-drums k)]
          (is (some? spec) (str "Drum instrument " k " must exist in core-drums"))
          (is (keyword? (:type spec)) (str "Drum instrument " k " must specify a keyword :type"))
          (is (string? (:title spec)) (str "Drum instrument " k " must specify a string :title")))))))

(deftest user-drums-catalog-test
  (testing "User custom drums catalog contains valid drum models"
    (is (map? user-drums))
    (doseq [[k spec] user-drums]
      (is (keyword? (:type spec)) (str "User drum " k " must specify a keyword :type"))
      (is (= :drums (:category spec)) (str "User drum " k " must belong to :drums category"))
      (is (string? (:title spec)) (str "User drum " k " must specify a string :title"))))

  (testing "Custom drum identification recognizes user-drums as custom"
    (is (true? (custom-file-inst? :fat-kick)))
    (is (true? (custom-file-inst? :lofi-snare))))

  (testing "Custom drums map to proper Rust drum voice IDs instead of modular synths"
    (is (= 0 (inst-keyword->id :fat-kick)) "fat-kick must dispatch to DRUM_KICK voice (0)")
    (is (= 1 (inst-keyword->id :lofi-snare)) "lofi-snare must dispatch to DRUM_SNARE voice (1)")))

(deftest expanded-drum-aliases-test
  (testing "Drum token remaps resolve to canonical keywords"
    (is (= :ride-bell (get drum-remaps "rb")))
    (is (= :tom-high (get drum-remaps "th")))
    (is (= :tom-mid (get drum-remaps "tm")))
    (is (= :tom-low (get drum-remaps "tl")))
    (is (= :crash-16 (get drum-remaps "cr16")))
    (is (= :crash-17 (get drum-remaps "cr17")))
    (is (= :crash-18 (get drum-remaps "cr18")))
    (is (= :splash (get drum-remaps "sp")))
    (is (= :china (get drum-remaps "ch")))
    (is (= :cowbell (get drum-remaps "cb"))))

  (testing "Drum keyword recognition recognizes expanded drum set"
    (doseq [k [:ride-bell :tom-high :tom-mid :tom-low
               :crash-16 :crash-17 :crash-18 :splash :china :cowbell]]
      (is (drum-keyword? k) (str "Expected " k " to be recognized as drum keyword")))))

(deftest track-level-drum-override-test
  (testing "Generic track-level :inst or :synth overrides matching drum hits"
    (is (= :fat-kick (resolve-target-inst :kick :fat-kick)))
    (is (= :lofi-snare (resolve-target-inst :snare :lofi-snare)))
    (is (= :clap (resolve-target-inst :clap :lofi-snare)) "Different drum in pattern remains unchanged")
    (is (= 0 (:inst-id (parse-step-hit "k!" :fat-kick))))
    (is (= 0 (:inst-id (parse-step-hit :kick! :fat-kick))))
    (is (= 1 (:inst-id (parse-step-hit "s!" :lofi-snare))))
    (is (= 18 (:inst-id (parse-step-hit "cp!" :lofi-snare))))))
