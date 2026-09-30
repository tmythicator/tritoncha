(ns app.audio.dsp.processors-test
  "Unit tests for DSP processor parameter compilation, fallbacks, and Worklet insert serialization."
  (:require [app.audio.dsp.processors :as proc]
            [cljs.test :refer [deftest is testing]]))

(deftest compile-filter-spec-test
  (testing "Compiles numeric, vector, and map filter specifications"
    (is (= {:type :filter :frequency 3500.0 :q 0.0 :filter-type "lowpass"}
           (proc/compile-filter-spec 3500)))
    (is (= {:type :filter :frequency 4200.0 :q 0.3 :filter-type "lowpass"}
           (proc/compile-filter-spec [4200 0.3])))
    (is (= {:type :filter :frequency 8000.0 :q 0.5 :filter-type "lowpass"}
           (proc/compile-filter-spec {:cutoff 8000 :resonance 0.5})))))

(deftest compile-distort-spec-test
  (testing "Compiles numeric and map distortion specifications"
    (is (= {:type :distortion :distortion 0.25 :algorithm :adaa :bits 16.0 :sample-hold 1.0 :wet 1.0}
           (proc/compile-distort-spec 0.25)))
    (is (= {:type :distortion :distortion 0.4 :algorithm :foldback :bits 12.0 :sample-hold 2.0 :wet 0.8}
           (proc/compile-distort-spec {:drive 0.4 :algo :foldback :bits 12.0 :sample-hold 2.0 :wet 0.8})))))

(deftest compile-crusher-spec-test
  (testing "Compiles numeric, vector, and map bitcrusher specifications"
    (is (= {:type :bitcrusher :bits 8.0 :sample-hold 1.0 :wet 1.0}
           (proc/compile-crusher-spec 8)))
    (is (= {:type :bitcrusher :bits 6.0 :sample-hold 2.0 :wet 0.5}
           (proc/compile-crusher-spec [6 2.0 0.5])))
    (is (= {:type :bitcrusher :bits 4.0 :sample-hold 3.0 :drive 0.2 :wet 0.9}
           (proc/compile-crusher-spec {:bits 4.0 :sample-hold 3.0 :drive 0.2 :wet 0.9})))))

(deftest compile-delay-spec-test
  (testing "Compiles delay specifications with string time notes and numerical values"
    (is (= {:type :delay :time 0.25 :feedback 0.35 :wet 0.25}
           (proc/compile-delay-spec 0.25)))
    (is (= {:type :delay :time "8n." :feedback 0.4 :wet 0.3}
           (proc/compile-delay-spec ["8n." 0.4 0.3])))
    (is (= {:type :delay :time "4n" :feedback 0.5 :wet 0.0}
           (proc/compile-delay-spec {:time "4n" :fb 0.5})))))

(deftest compile-bus-insert-serialization-test
  (testing "Serializes processor specs into exact Rust WASM worklet JSON maps"
    (let [specs {:filter {:cutoff 5000 :resonance 0.2}
                 :delay  {:time-s 0.30 :feedback 0.45 :wet 0.25}
                 :distort {:drive 0.35 :bits 14.0 :sampleHold 1.5}}]
      (is (= {:type "filter" :cutoffHz 5000.0 :resonance 0.2}
             (proc/compile-bus-insert :filter specs)))
      (is (= {:type "delay" :timeS 0.30 :feedback 0.45 :wet 0.25}
             (proc/compile-bus-insert :delay specs)))
      (is (= {:type "distort" :drive 0.35 :bits 14.0 :sampleHold 1.5}
             (proc/compile-bus-insert :distort specs))))))
