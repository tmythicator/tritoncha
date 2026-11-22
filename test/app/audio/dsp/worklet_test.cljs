(ns app.audio.dsp.worklet-test
  (:require [app.audio.dsp.instruments.catalog :refer [find-instrument-spec]]
            [app.audio.dsp.worklet :as worklet]
            [app.audio.dsp.worklet.compiler :as compiler]
            [app.utils.audio :as audio]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest test-worklet-parse-freq
  (testing "Frequency parsing from note names and MIDI numbers"
    (is (< (js/Math.abs (- (audio/parse-freq "A4") 440.0)) 0.01))
    (is (< (js/Math.abs (- (audio/parse-freq 69) 440.0)) 0.01))
    (is (< (js/Math.abs (- (audio/parse-freq "C4") 261.63)) 0.1))
    (is (= (audio/parse-freq 880.0) 880.0))))

(deftest test-parse-step-hit-articulation
  (testing "Normal, accent (!), and ghost (_) velocities and canonical drum keys on drum hits"
    (let [normal (compiler/parse-step-hit :snare :snare)
          accent (compiler/parse-step-hit :snare! :snare)
          ghost  (compiler/parse-step-hit :snare_ :snare)]
      (is (= :snare (:inst-key normal)))
      (is (< (js/Math.abs (- (:vel normal) 0.9)) 0.01))
      (is (= :snare (:inst-key accent)))
      (is (< (js/Math.abs (- (:vel accent) 1.15)) 0.01))
      (is (= :snare (:inst-key ghost)))
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
      (is (= :ride-bell (:inst-key rb-accent)))
      (is (< (js/Math.abs (- (:vel rb-accent) 1.15)) 0.01))
      (is (= :tom-high (:inst-key th-accent)))
      (is (= :tom-mid (:inst-key tm-ghost)))
      (is (< (js/Math.abs (- (:vel tm-ghost) 0.35)) 0.01))
      (is (= :tom-low (:inst-key tl-hit)))
      (is (= :crash-16 (:inst-key cr16-acc)))
      (is (= :crash-17 (:inst-key cr17-hit)))
      (is (= :crash-18 (:inst-key cr18-hit)))
      (is (= :splash (:inst-key sp-ghost)))
      (is (= :china (:inst-key ch-accent)))
      (is (= :cowbell (:inst-key cb-hit))))))

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

(deftest test-per-track-kit-compilation
  (testing "Track-local kit compiler assigns 0..N kit indices to drums and -1 to synths"
    (let [drum-res  (compiler/compile-track-steps :drums ["k!" nil "s_" "rb!" "k"] (constantly 0.9))
          synth-res (compiler/compile-track-steps :bass-analog [36 nil 43 48] (constantly 0.9))]
      (is (= [:kick :snare :ride-bell] (:kit drum-res)))
      (is (= [0 0 1 2 0] (:inst-ids drum-res)))
      (is (empty? (:kit synth-res)))
      (is (= [-1 -1 -1 -1] (:inst-ids synth-res))))))

(deftest test-compile-perc-params-contract
  (testing "Compiles drum spec into 24-float JS array with genuine 0.0 preservation"
    (let [arr (compiler/compile-perc-params
               {:body :shell :pitch 180.0 :decay 0.35 :click 0.0 :noise-mix 0.0 :fold 0.0
                :burst-count 4 :choke-group 1})]
      (is (= 24 (alength arr)))
      (is (= 1.0 (aget arr 0)))   ;; :shell = 1
      (is (= 180.0 (aget arr 1))) ;; pitch
      (is (= 0.0 (aget arr 5)))   ;; genuine 0.0 click
      (is (= 4.0 (aget arr 7)))   ;; burst-count
      (is (= 0.0 (aget arr 11)))  ;; genuine 0.0 noise-mix
      (is (= 0.0 (aget arr 18)))  ;; genuine 0.0 fold
      (is (= 1.0 (aget arr 23)))))) ;; choke-group

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

