(ns app.utils.dom-test
  "Unit tests for device detection, DPR clamps, and lookahead buffer metrics."
  (:require [app.config :as cfg]
            [app.utils.dom :as dom]
            [cljs.test :refer [deftest is testing]]))

(deftest lookahead-buffer-test
  (testing "Returns optimal lookahead buffer duration"
    (let [val (dom/active-lookahead)]
      (is (number? val))
      (is (or (= val cfg/lookahead-desktop) (= val cfg/lookahead-mobile))))))

(deftest max-dpr-test
  (testing "Returns devicePixelRatio clamp boundary"
    (let [dpr (dom/max-dpr)]
      (is (number? dpr))
      (is (pos? dpr))
      (is (or (= dpr cfg/max-dpr-desktop) (= dpr cfg/max-dpr-mobile))))))
