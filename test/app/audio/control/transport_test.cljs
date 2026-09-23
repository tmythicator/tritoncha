(ns app.audio.control.transport-test
  "Unit tests for master transport tempo clamping and drum synthesis mode dispatch."
  (:require [app.audio.control.transport :as transport]
            [cljs.test :refer [deftest is testing]]))

(deftest set-bpm-clamp-test
  (testing "BPM clamps within valid range"
    (is (= 40 (transport/set-bpm! 5)))
    (is (= 300 (transport/set-bpm! 999)))
    (is (= 174 (transport/set-bpm! 174)))))

(deftest drum-mode-test
  (testing "Configures drum character mode and returns keyword"
    (is (= :natural (transport/set-drum-mode! :natural)))
    (is (= :idm (transport/set-drum-mode! :idm)))
    (is (= :analog (transport/mod! :analog)))
    (is (= :industrial (transport/mod! :industrial)))))
