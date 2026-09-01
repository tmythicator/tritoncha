(ns app.audio.dsp.telemetry-test
  "Unit tests for audio telemetry snapshot, status reporting, and formatting."
  (:require [app.audio.dsp.telemetry :as telemetry]
            [cljs.test :refer [deftest is testing]]))

(deftest telemetry-snapshot-structure-test
  (testing "Generates clean telemetry snapshot without runtime exceptions"
    (let [snap (telemetry/telemetry-snapshot)]
      (is (map? snap))
      (is (contains? snap :ctx-state))
      (is (contains? snap :bpm))
      (is (contains? snap :bpm-str))
      (is (contains? snap :position))
      (is (contains? snap :transport-state))
      (is (contains? snap :hardware-clock))
      (is (contains? snap :clock-drift))
      (is (contains? snap :latency-hint))
      (is (contains? snap :xrun-count))
      (is (contains? snap :min-headroom-ms))
      (is (contains? snap :output-latency))
      (is (contains? snap :active-tracks)))))

(deftest audio-status-printer-test
  (testing "Prints diagnostic report and returns map"
    (let [res (telemetry/audio-status)]
      (is (map? res))
      (is (string? (:bpm-str res)))
      (is (string? (:clock-drift res))))))

(deftest reset-telemetry-metrics-test
  (testing "Resets accumulated metrics"
    (is (= :reset (telemetry/reset-telemetry-metrics!)))))
