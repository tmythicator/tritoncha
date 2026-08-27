(ns app.utils.math-test
  "Unit tests for math utilities, range scaling, and decibel conversions."
  (:require [app.utils.math :as math]
            [cljs.test :refer [deftest is testing]]))

(deftest clamp-test
  (testing "Clamps numeric values between minimum and maximum bounds"
    (is (= 50 (math/clamp 50 0 100)))
    (is (= 0 (math/clamp -10 0 100)))
    (is (= 100 (math/clamp 150 0 100)))
    (is (= 0 (math/clamp 0 0 100)))
    (is (= 100 (math/clamp 100 0 100)))))

(deftest lerp-test
  (testing "Linear interpolation"
    (is (= 50.0 (math/lerp 0 100 0.5)))
    (is (= 0 (math/lerp 0 100 0.0)))
    (is (= 100 (math/lerp 0 100 1.0)))))

(deftest time-conversion-test
  (testing "Converts seconds to milliseconds and vice versa"
    (is (= 250.0 (math/sec->ms 0.25)))
    (is (= 1000.0 (math/sec->ms 1)))
    (is (= 0.25 (math/ms->sec 250)))
    (is (= 1.0 (math/ms->sec 1000)))
    (is (nil? (math/sec->ms nil)))
    (is (nil? (math/ms->sec nil)))))

(deftest scale-range-test
  (testing "Linear range mapping"
    (is (= 50.0 (math/scale-range 5 0 10 0 100)))
    (is (= 0.0 (math/scale-range 0 0 10 0 100)))
    (is (= 100.0 (math/scale-range 10 0 10 0 100)))))

(deftest db-gain-conversion-test
  (testing "Decibel to gain conversion"
    (is (= 1.0 (math/db->gain 0)))
    (is (< 0.49 (math/db->gain -6) 0.51))
    (is (< 0.09 (math/db->gain -20) 0.11)))

  (testing "Gain to decibel conversion"
    (is (= 0.0 (math/gain->db 1.0)))
    (is (< -6.1 (math/gain->db 0.5) -5.9))))
