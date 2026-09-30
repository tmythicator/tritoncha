(ns app.ui.common-test
  "Unit tests for shared UI common primitives, modal shells, badges, and bus tags."
  (:require
   [app.ui.common :as common]
   [cljs.test :refer [deftest is testing]]
   [clojure.string :as str]))

(deftest modal-header-test
  (testing "Renders modal header with prompt and title"
    (let [hiccup (common/modal-header {:title "TEST TITLE"})]
      (is (= :div.neo-header (first hiccup)))
      (is (str/includes? (str hiccup) "TEST TITLE"))
      (is (str/includes? (str hiccup) "neo-prompt"))))

  (testing "Renders close button when on-close callback is provided"
    (let [closed? (atom false)
          hiccup  (common/modal-header {:title    "MODAL"
                                        :on-close #(reset! closed? true)})]
      (is (str/includes? (str hiccup) "neo-btn-close"))
      (is (str/includes? (str hiccup) "[X]"))))

  (testing "Renders right-content when provided"
    (let [hiccup (common/modal-header {:title         "MODAL"
                                       :right-content [:span.test-right "ONLINE"]})]
      (is (str/includes? (str hiccup) "test-right"))
      (is (str/includes? (str hiccup) "ONLINE")))))

(deftest modal-footer-test
  (testing "Renders modal footer with command and keyboard hint"
    (let [hiccup (common/modal-footer {:cmd   "> ./test --cmd"
                                       :hint  "[Press X to exit]"
                                       :class "custom-footer"})]
      (is (= :div.neo-footer (first hiccup)))
      (is (str/includes? (str hiccup) "> ./test --cmd"))
      (is (str/includes? (str hiccup) "[Press X to exit]"))
      (is (= "custom-footer" (get-in hiccup [1 :class]))))))

(deftest status-badge-test
  (testing "Renders online status badge"
    (let [hiccup (common/status-badge {:online? true})]
      (is (= :div.neo-status-badge (first hiccup)))
      (is (str/includes? (str hiccup) "online"))
      (is (str/includes? (str hiccup) "ONLINE"))))

  (testing "Renders offline status badge"
    (let [hiccup (common/status-badge {:online? false})]
      (is (= :div.neo-status-badge (first hiccup)))
      (is (str/includes? (str hiccup) "offline"))
      (is (str/includes? (str hiccup) "OFFLINE")))))

(deftest badge-test
  (testing "Renders default badge"
    (let [hiccup (common/badge "174 BPM")]
      (is (= :span.neo-badge (first hiccup)))
      (is (= "174 BPM" (second hiccup)))))

  (testing "Renders cyan badge variant"
    (let [hiccup (common/badge {:variant :cyan} "D MINOR")]
      (is (= :span.neo-badge.badge-cyan (first hiccup)))
      (is (= "D MINOR" (second hiccup))))))

(deftest bus-badge-class-test
  (testing "Normalizes and maps bus keywords to corresponding CSS classes"
    (is (= "bus-drums"  (common/bus-badge-class :bus/drums)))
    (is (= "bus-drums"  (common/bus-badge-class :drums)))
    (is (= "bus-bass"   (common/bus-badge-class :bus/bass)))
    (is (= "bus-lead"   (common/bus-badge-class :bus/lead)))
    (is (= "bus-space"  (common/bus-badge-class :bus/space)))
    (is (= "bus-direct" (common/bus-badge-class :bus/direct)))
    (is (= "bus-master" (common/bus-badge-class :bus/master)))))

(deftest bus-tag-test
  (testing "Renders bus tag element with normalized class and label"
    (let [hiccup (common/bus-tag :bus/drums)]
      (is (= :span (first hiccup)))
      (is (str/includes? (get-in hiccup [1 :class]) "bus-drums"))
      (is (= "DRUMS" (last hiccup))))

    (let [hiccup (common/bus-tag {:bus :bus/space :mini? true})]
      (is (= :span (first hiccup)))
      (is (str/includes? (get-in hiccup [1 :class]) "bus-space"))
      (is (str/includes? (get-in hiccup [1 :class]) "mini"))
      (is (= "SPACE" (last hiccup))))))
