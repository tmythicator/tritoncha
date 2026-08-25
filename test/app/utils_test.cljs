(ns app.utils-test
  (:require
   [app.utils :as utils]
   [cljs.test :refer [deftest is testing]]))

(deftest clamp-test
  (testing "Clamps numeric values between minimum and maximum bounds"
    (is (= 50 (utils/clamp 50 0 100)))
    (is (= 0 (utils/clamp -10 0 100)))
    (is (= 100 (utils/clamp 150 0 100)))
    (is (= 0 (utils/clamp 0 0 100)))
    (is (= 100 (utils/clamp 100 0 100)))))

(deftest lerp-test
  (testing "Linear interpolation"
    (is (= 50.0 (utils/lerp 0 100 0.5)))
    (is (= 0 (utils/lerp 0 100 0.0)))
    (is (= 100 (utils/lerp 0 100 1.0)))))

(deftest time-conversion-test
  (testing "Converts seconds to milliseconds and vice versa"
    (is (= 250.0 (utils/sec->ms 0.25)))
    (is (= 1000.0 (utils/sec->ms 1)))
    (is (= 0.25 (utils/ms->sec 250)))
    (is (= 1.0 (utils/ms->sec 1000)))
    (is (nil? (utils/sec->ms nil)))
    (is (nil? (utils/ms->sec nil)))))

(deftest scale-range-test
  (testing "Linear range mapping"
    (is (= 50.0 (utils/scale-range 5 0 10 0 100)))
    (is (= 0.0 (utils/scale-range 0 0 10 0 100)))
    (is (= 100.0 (utils/scale-range 10 0 10 0 100)))))

(deftest rotate-test
  (testing "Rotates vectors by N positions"
    (is (= [2 3 4 1] (utils/rotate 1 [1 2 3 4])))
    (is (= [3 4 1 2] (utils/rotate 2 [1 2 3 4])))
    (is (= [4 1 2 3] (utils/rotate -1 [1 2 3 4])))
    (is (= [1 2 3 4] (utils/rotate 4 [1 2 3 4])))
    (is (= [] (utils/rotate 1 [])))))

(deftest parse-note-test
  (testing "Parses note strings and keywords into pitch and octave"
    (is (= {:pitch "C", :octave 4} (utils/parse-note "C4")))
    (is (= {:pitch "F#", :octave 3} (utils/parse-note "F#3")))
    (is (= {:pitch "EB", :octave 2} (utils/parse-note :eb2)))
    (is (= {:pitch "A", :octave 3} (utils/parse-note "A" 3)))
    (is (nil? (utils/parse-note nil)))))

(deftest euclid-rhythm-test
  (testing "Euclidean rhythm generator (Bjorklund algorithm)"
    (is (= [true nil nil true nil true nil nil]
           (utils/euclid 3 8)))
    (is (= [true nil nil nil true nil nil nil true nil nil nil true nil nil nil]
           (utils/euclid 4 16)))
    (is (= [:kick nil nil nil :kick nil nil :kick nil nil :kick nil nil :kick nil nil]
           (utils/euclid 5 16 :kick)))
    (is (= [nil nil nil nil]
           (utils/euclid 0 4)))
    (is (= [true true true true]
           (utils/euclid 4 4)))))

(deftest pattern-mini-notation-test
  (testing "Mini-notation parser for drums and rests"
    (is (= [:kick nil nil nil :snare nil nil nil]
           (utils/pattern "k . . . s . . .")))
    (is (= [:hh-c :hh-o nil :hh-clk]
           (utils/pattern "h o . hc")))
    (is (= [true nil true nil]
           (utils/pattern "x . 1 0")))))

(deftest cycle-next-test
  (testing "Cycles to the next element wrapping around"
    (is (= :b (utils/cycle-next :a [:a :b :c])))
    (is (= :c (utils/cycle-next :b [:a :b :c])))
    (is (= :a (utils/cycle-next :c [:a :b :c])))
    (is (= :roller (utils/cycle-next :unknown [:roller :acid])))))
