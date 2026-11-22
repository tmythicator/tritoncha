(ns app.lib.drums-test
  (:require
   [app.audio.dsp.instruments.catalog :refer [drum-keyword? resolve-target-inst]]
   [app.audio.dsp.worklet.compiler :refer [parse-step-hit]]
   [app.audio.dsp.worklet.protocol :refer [drum-remaps perc-body->id]]
   [app.custom.drums :refer [user-drums]]
   [app.lib.drums :refer [core-drums]]
   [cljs.test :refer [deftest is testing]]))

(def valid-physical-bodies
  #{:membrane :shell :cymbal :idiophone :burst})

(deftest core-drums-catalog-test
  (testing "Core drum instruments catalog contains 18 distinct physical drum voices"
    (let [required-instruments [:kick :fat-kick :snare :lofi-snare
                                :hat-closed :hat-open :clap
                                :tom-high :tom-mid :tom-low
                                :ride :ride-bell
                                :crash-16 :crash-17 :crash-18 :splash :china :cowbell]]
      (is (= 18 (count core-drums)))
      (doseq [k required-instruments]
        (let [spec (get core-drums k)]
          (is (some? spec) (str "Drum instrument " k " must exist in core-drums"))
          (is (keyword? (:type spec)) (str "Drum instrument " k " must specify a keyword :type"))
          (is (string? (:title spec)) (str "Drum instrument " k " must specify a string :title")))))
    (doseq [removed [:tom :snare-rim :snare-ghost :snare-body :snare-wire :sn-crack]]
      (is (not (contains? core-drums removed))
          (str "Redundant or articulation-only entry " removed " must not clutter core-drums"))))

  (testing "Every drum in core-drums uses one of the 5 physical percussion body families"
    (doseq [[k spec] core-drums]
      (is (contains? valid-physical-bodies (:body spec))
          (str "Core drum " k " has invalid :body " (:body spec)
               ", expected one of " valid-physical-bodies))))

  (testing "perc-body->id maps the 5 physical body families to Rust discriminants 0..4"
    (is (= 0 (perc-body->id :membrane)))
    (is (= 1 (perc-body->id :shell)))
    (is (= 2 (perc-body->id :cymbal)))
    (is (= 3 (perc-body->id :idiophone)))
    (is (= 4 (perc-body->id :burst)))))

(deftest user-drums-catalog-test
  (testing "User custom drums catalog contains valid drum models"
    (is (map? user-drums))
    (doseq [[k spec] user-drums]
      (is (keyword? (:type spec)) (str "User drum " k " must specify a keyword :type"))
      (is (= :drums (:category spec)) (str "User drum " k " must belong to :drums category"))
      (is (string? (:title spec)) (str "User drum " k " must specify a string :title"))))

  (testing "Core drum identification includes fat-kick and lofi-snare"
    (is (contains? core-drums :fat-kick))
    (is (contains? core-drums :lofi-snare))
    (is (= :kick (:role (get core-drums :fat-kick))))
    (is (= :snare (:role (get core-drums :lofi-snare))))))

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
    (is (= :hat-open (resolve-target-inst :hat-open :hat-closed)) "Open hat hit must not be swallowed by :hat-closed")
    (is (= :hat-open (resolve-target-inst :hat-open :hats)) "Open hat hit must not be swallowed by generic :hats track key")
    (is (= :fat-kick (:inst-key (parse-step-hit "k!" :fat-kick))))
    (is (= :fat-kick (:inst-key (parse-step-hit :kick! :fat-kick))))
    (is (= :lofi-snare (:inst-key (parse-step-hit "s!" :lofi-snare))))
    (is (= :snare (:inst-key (parse-step-hit "g" :snare))))
    (is (< (:vel (parse-step-hit "g" :snare 0.9)) 0.5) "Ghost modifier token 'g' must produce ghost velocity on snare")
    (is (= :clap (:inst-key (parse-step-hit "cp!" :lofi-snare))))
    (is (= :hat-open (:inst-key (parse-step-hit "o_" :hats))))
    (is (= :hat-closed (:inst-key (parse-step-hit "h_" :hats))))))
