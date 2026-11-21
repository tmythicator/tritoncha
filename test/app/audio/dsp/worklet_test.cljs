(ns app.audio.dsp.worklet-test
  (:require [app.audio.dsp.instruments.catalog :refer [drum-keywords find-instrument-spec]]
            [app.audio.dsp.worklet :as worklet]
            [app.audio.dsp.worklet.compiler :as compiler]
            [app.audio.dsp.worklet.protocol :as protocol]
            [app.lib.synth :as synths]
            [app.utils.audio :as audio]
            [clojure.set :as set]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest test-worklet-parse-freq
  (testing "Frequency parsing from note names and MIDI numbers"
    (is (< (js/Math.abs (- (audio/parse-freq "A4") 440.0)) 0.01))
    (is (< (js/Math.abs (- (audio/parse-freq 69) 440.0)) 0.01))
    (is (< (js/Math.abs (- (audio/parse-freq "C4") 261.63)) 0.1))
    (is (= (audio/parse-freq 880.0) 880.0))))

(deftest test-parse-step-hit-articulation
  (testing "Normal, accent (!), and ghost (_) velocities on drum hits"
    (let [normal (compiler/parse-step-hit :snare :snare)
          accent (compiler/parse-step-hit :snare! :snare)
          ghost  (compiler/parse-step-hit :snare_ :snare)]
      (is (= 1 (:inst-id normal)))
      (is (< (js/Math.abs (- (:vel normal) 0.9)) 0.01))
      (is (= 1 (:inst-id accent)))
      (is (< (js/Math.abs (- (:vel accent) 1.15)) 0.01))
      (is (= 1 (:inst-id ghost)))
      (is (< (js/Math.abs (- (:vel ghost) 0.35)) 0.01)))

    (let [rb-accent (compiler/parse-step-hit :rb! :drums)
          th-accent (compiler/parse-step-hit :th! :drums)
          tm-ghost  (compiler/parse-step-hit :tm_ :drums)
          tl-hit    (compiler/parse-step-hit :tl :drums)
          cr16-acc  (compiler/parse-step-hit :cr16! :drums)
          cr17-hit  (compiler/parse-step-hit :cr17 :drums)
          cr18-hit  (compiler/parse-step-hit :cr18 :drums)
          sp-ghost  (compiler/parse-step-hit :sp_ :drums)
          ch-accent (compiler/parse-step-hit :ch! :drums)
          cb-hit    (compiler/parse-step-hit :cb :drums)]
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

(deftest test-parse-step-hit-custom-velocity
  (testing "Custom track velocity propagates to notes, chords, and accents"
    (let [low-note  (compiler/parse-step-hit "E3" :pad 0.28)
          low-chord (compiler/parse-step-hit ["E3" "G3"] :pad 0.28)
          low-bool  (compiler/parse-step-hit true :pad 0.28)
          low-midi  (compiler/parse-step-hit 60 :pad 0.28)
          low-ghost (compiler/parse-step-hit "E3_" :pad 0.28)]
      (is (< (js/Math.abs (- (:vel low-note) 0.28)) 0.01))
      (is (< (js/Math.abs (- (:vel low-chord) 0.28)) 0.01))
      (is (< (js/Math.abs (- (:vel low-bool) 0.28)) 0.01))
      (is (< (js/Math.abs (- (:vel low-midi) 0.28)) 0.01))
      (is (< (js/Math.abs (- (:vel low-ghost) (* 0.28 (/ 0.35 0.9)))) 0.01)))))

(deftest test-track-slots-for
  (testing "Returns assigned slots and sub-slots for chords"
    (swap! worklet/track-slot-assignments assoc :pad 4 :pad-v1 5 :pad-v2 6 :bass 1)
    (let [slots (worklet/track-slots-for :pad)]
      (is (contains? (set slots) 4))
      (is (contains? (set slots) 5))
      (is (contains? (set slots) 6))
      (is (not (contains? (set slots) 1))))))

(deftest test-mode-switching-dispatch
  (testing "Dispatches drive and reverb algorithm mode messages safely"
    (is (= :adaa (worklet/set-worklet-drive-mode! :adaa)))
    (is (= :classic (worklet/set-worklet-drive-mode! :classic)))
    (is (= :fdn (worklet/set-worklet-reverb-mode! :fdn)))
    (is (= :freeverb (worklet/set-worklet-reverb-mode! :freeverb)))))

(deftest test-filter-type-mapping
  (testing "Filter type keyword mapping to numeric IDs including 24dB Ladder"
    (is (= 0 (worklet/filter-type->id :lowpass)))
    (is (= 0 (worklet/filter-type->id :lp)))
    (is (= 1 (worklet/filter-type->id :highpass)))
    (is (= 2 (worklet/filter-type->id :bandpass)))
    (is (= 3 (worklet/filter-type->id :notch)))
    (is (= 4 (worklet/filter-type->id :ladder)))
    (is (= 4 (worklet/filter-type->id :ladder24)))))

(deftest test-sound-registration-no-id-collisions
  (testing "Track-centric synths use sentinel ID (-1) and never collide with drum IDs"
    (let [drum-ids   #{0 1 2 3 18 20 21 22 23 24 25 26 64 65 66 67 68 69 70 71 72 73}
          synth-keys (keys synths/core-synths)
          synth-ids  (set (map protocol/inst-keyword->id synth-keys))]
      ;; Ensure drums and synths have empty intersection
      (is (empty? (set/intersection drum-ids synth-ids)))
      ;; All synths resolve to track-centric sentinel -1 (no global slot table)
      (is (= #{protocol/synth-step-inst-id} synth-ids))
      (is (= -1 protocol/synth-step-inst-id))
      (is (= -1 (protocol/inst-keyword->id :test-user-synth)))
      ;; Drum IDs remain intact until Step 4
      (is (= 20 (protocol/canonical-inst-ids :ride)))
      (is (= 21 (protocol/canonical-inst-ids :tom)))
      (doseq [dk (disj drum-keywords :click)]
        (when-let [id (protocol/canonical-inst-ids dk)]
          (is (contains? drum-ids id)))))))

(deftest test-resolve-track-patch-spec-bus-override
  (testing "Resolves instrument spec without bus override"
    (let [spec (worklet/resolve-track-patch-spec :lead {:inst :lead-8bit})]
      (is (= :bus/lead (:bus spec)))))

  (testing "Applies custom bus override directly to track spec without mutating registry"
    (let [spec (worklet/resolve-track-patch-spec :bass {:inst :lead-8bit :bus :bus/bass})]
      (is (= :bus/bass (:bus spec)))
      (is (nil? (find-instrument-spec :lead-8bit--bass)))))

  (testing "Applies custom bus override for pads"
    (let [spec (worklet/resolve-track-patch-spec :echo {:inst :pad-strings :bus :bus/lead})]
      (is (= :bus/lead (:bus spec)))))

  (testing "Returns nil synth patch spec for drums"
    (is (nil? (worklet/resolve-track-patch-spec :kick {:inst :kick :bus :bus/direct})))))

(deftest test-compile-track-patch-msg-packed-params
  (testing "Compiles synth spec into setTrackPatch with 22-float :params vector"
    (let [msg (compiler/compile-track-patch-msg
               3
               {:type :mono
                :bus :bus/bass
                :glide 0.035
                :osc {:type :supersaw :sub-level 0.35 :pulse-width 0.5 :noise 0.1 :drift 0.25}
                :filter {:type :ladder :cutoff 2400 :env-amount 4500 :key-track 2.0 :q 0.55 :drive 0.35}
                :amp-env {:attack 0.005 :decay 0.22 :sustain 0.35 :release 0.20}
                :mod-env {:attack 0.005 :decay 0.22}
                :pitch-env {:amount 12 :decay 0.020}})
          params (.-params msg)]
      (is (= "setTrackPatch" (.-type msg)))
      (is (= 3 (.-trackIdx msg)))
      (is (some? params) "Message must include packed :params array")
      (is (= 22 (alength params)) "Packed :params array must have exactly 22 floats")
      (is (= 4.0 (aget params 0)))    ;; oscType (:supersaw = 4)
      (is (< (js/Math.abs (- (aget params 1) 0.35)) 1e-4)) ;; subLevel
      (is (= 4.0 (aget params 3)))    ;; filterType (:ladder = 4)
      (is (= 2400.0 (aget params 4))) ;; cutoffBase
      (is (= 1.0 (aget params 14)))   ;; busId (:bus/bass = 1)
      (is (= 1.0 (aget params 15)))   ;; polyphony (:mono = 1)
      (is (< (js/Math.abs (- (aget params 21) 0.25)) 1e-4))))) ;; analogDrift

