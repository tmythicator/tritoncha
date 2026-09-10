(ns app.utils.coll-test
  "Unit tests for collection cycling and rotation utilities."
  (:require [app.utils.coll :as coll]
            [cljs.test :refer [deftest is testing]]))

(deftest cycle-next-test
  (testing "Cycles to the next element wrapping around"
    (is (= :b (coll/cycle-next :a [:a :b :c])))
    (is (= :c (coll/cycle-next :b [:a :b :c])))
    (is (= :a (coll/cycle-next :c [:a :b :c])))
    (is (= :roller (coll/cycle-next :unknown [:roller :acid])))))

(deftest rotate-test
  (testing "Rotates vectors by N positions"
    (is (= [2 3 4 1] (coll/rotate 1 [1 2 3 4])))
    (is (= [3 4 1 2] (coll/rotate 2 [1 2 3 4])))
    (is (= [4 1 2 3] (coll/rotate -1 [1 2 3 4])))
    (is (= [1 2 3 4] (coll/rotate 4 [1 2 3 4])))
    (is (= [] (coll/rotate 1 [])))))

(deftest vec3-test
  (testing "Normalizes scalar, vector and nil into 3-element vector"
    (is (= [1.5 1.5 1.5] (coll/vec3 1.5)))
    (is (= [1 2 3] (coll/vec3 [1 2 3])))
    (is (= [1 2 0] (coll/vec3 [1 2])))
    (is (= [0 0 0] (coll/vec3 nil)))
    (is (= [1.0 1.0 1.0] (coll/vec3 nil 1.0)))))
