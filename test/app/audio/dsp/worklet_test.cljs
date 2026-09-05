(ns app.audio.dsp.worklet-test
  (:require [app.audio.dsp.worklet :as worklet]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest test-worklet-parse-freq
  (testing "Frequency parsing from note names and MIDI numbers"
    (is (< (js/Math.abs (- (worklet/parse-freq "A4") 440.0)) 0.01))
    (is (< (js/Math.abs (- (worklet/parse-freq 69) 440.0)) 0.01))
    (is (< (js/Math.abs (- (worklet/parse-freq "C4") 261.63)) 0.1))
    (is (= (worklet/parse-freq 880.0) 880.0))))

(deftest test-parse-step-hit-articulation
  (testing "Normal, accent (!), and ghost (_) velocities on drum hits"
    (let [normal (worklet/parse-step-hit :snare :snare)
          accent (worklet/parse-step-hit :snare! :snare)
          ghost  (worklet/parse-step-hit :snare_ :snare)]
      (is (= 1 (:inst-id normal)))
      (is (< (js/Math.abs (- (:vel normal) 0.9)) 0.01))
      (is (= 1 (:inst-id accent)))
      (is (< (js/Math.abs (- (:vel accent) 1.15)) 0.01))
      (is (= 1 (:inst-id ghost)))
      (is (< (js/Math.abs (- (:vel ghost) 0.35)) 0.01)))

    (let [rb-accent (worklet/parse-step-hit :rb! :drums)
          th-accent (worklet/parse-step-hit :th! :drums)
          tm-ghost  (worklet/parse-step-hit :tm_ :drums)
          tl-hit    (worklet/parse-step-hit :tl :drums)
          cr16-acc  (worklet/parse-step-hit :cr16! :drums)
          cr17-hit  (worklet/parse-step-hit :cr17 :drums)
          cr18-hit  (worklet/parse-step-hit :cr18 :drums)
          sp-ghost  (worklet/parse-step-hit :sp_ :drums)
          ch-accent (worklet/parse-step-hit :ch! :drums)
          cb-hit    (worklet/parse-step-hit :cb :drums)]
      (is (= 64 (:inst-id rb-accent)))
      (is (< (js/Math.abs (- (:vel rb-accent) 1.15)) 0.01))
      (is (= 65 (:inst-id th-accent)))
      (is (= 66 (:inst-id tm-ghost)))
      (is (< (js/Math.abs (- (:vel tm-ghost) 0.35)) 0.01))
      (is (= 67 (:inst-id tl-hit)))
      (is (= 68 (:inst-id cr16-acc)))
      (is (= 69 (:inst-id cr17-hit)))
      (is (= 70 (:inst-id cr18-hit)))
      (is (= 71 (:inst-id sp-ghost)))
      (is (= 72 (:inst-id ch-accent)))
      (is (= 73 (:inst-id cb-hit))))))
