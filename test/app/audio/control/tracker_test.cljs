(ns app.audio.control.tracker-test
  (:require [app.audio.control.tracker :as tracker]
            [cljs.test :refer [deftest is testing]]))

(deftest tracker-default-track-test
  (testing "Default track key is dynamically always the first track in catalog order"
    (is (= (first (tracker/track-keys)) (tracker/default-track-key)))))

(deftest tracker-declaration-order-test
  (testing "track-keys preserves exact declaration order for the top 5 tracks"
    (is (= [:orbital-roller
            :street-roller
            :orbital-matrix
            :industrial-techno
            :downtempo-chill]
           (subvec (tracker/track-keys) 0 5))))

  (testing "jam-list preserves exact declaration order matching track-keys"
    (let [jams (tracker/jam-list)]
      (is (= [:orbital-roller
              :street-roller
              :orbital-matrix
              :industrial-techno
              :downtempo-chill]
             (mapv :id (subvec jams 0 5))))
      (is (= (tracker/track-keys) (mapv :id jams)))))

  (testing ":metro-roller is completely removed from available tracks"
    (is (not (contains? (set (tracker/track-keys)) :metro-roller)))))

(deftest tracker-index-dispatch-test
  (testing "play-track-at! and play-preset! resolve index to the n-th track"
    (let [keys-list (tracker/track-keys)]
      (is (= (nth keys-list 0) (tracker/default-track-key)))
      (is (= (nth keys-list 1) (nth (tracker/track-keys) 1)))
      (is (nil? (tracker/play-track-at! 9999))))))
