(ns app.audio.dsp.styles-test
  (:require [app.audio.dsp.instruments :as instruments]
            [app.audio.dsp.styles :as styles]
            [app.audio.dsp.worklet.compiler :as compiler]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest test-builtin-styles-exist
  (testing "Built-in genre styles :analog, :natural, :idm, :industrial exist"
    (doseq [s [:analog :natural :idm :industrial]]
      (is (map? (styles/resolve-style s)) (str "Style " s " must be defined")))))

(deftest test-apply-single-style-by-role
  (testing "Single style applies :all and role-specific multiplicative/additive/override rules"
    (let [kick {:role :kick :body :membrane :pitch 50.0 :pitch-drop 160.0 :decay 0.25 :click 0.3 :drive 1.5 :fm-depth 0.0 :crush 0.0}
          idm  (styles/apply-style kick :idm)
          ind  (styles/apply-style kick :industrial)]
      (is (> (:fm-depth idm) 0.0) "IDM style must add FM depth to kick")
      (is (> (:pitch-drop idm) (:pitch-drop kick)) "IDM style must boost kick pitch-drop")
      (is (> (:crush idm) 0.0) "IDM style must add bitcrush")
      (is (> (:fold ind) 0.0) "Industrial style must add wavefolder")
      (is (> (:drive ind) (:drive kick)) "Industrial style must boost drive"))))

(deftest test-apply-blended-styles
  (testing "Weighted map blends numeric params linearly and picks discrete fields from highest weight"
    (let [kick    {:role :kick :body :membrane :pitch 50.0 :decay 0.25 :drive 1.5 :fold 0.0 :variation 0.0}
          nat     (styles/apply-style kick :natural)
          ind     (styles/apply-style kick :industrial)
          blended (styles/apply-style kick {:natural 0.7 :industrial 0.3})
          expected-fold (+ (* 0.7 (:fold nat 0.0)) (* 0.3 (:fold ind 0.0)))]
      (is (< (js/Math.abs (- (:fold blended 0.0) expected-fold)) 1e-4)
          "Blended fold must be weighted linear interpolation")
      (is (= (:body nat) (:body blended))
          "Discrete :body must come from highest-weighted style (:natural)"))))

(deftest test-defstyle-custom-genre
  (testing "User can define and apply custom genre styles via defstyle!"
    (styles/defstyle! :gabber-test
      {:all  {:drive [:* 2.0]}
       :kick {:fold 0.85 :pitch [:+ 8.0]}})
    (let [kick {:role :kick :pitch 50.0 :drive 1.5 :fold 0.0}
          out  (styles/apply-style kick :gabber-test)]
      (is (= 3.0 (:drive out)))
      (is (= 0.85 (:fold out)))
      (is (= 58.0 (:pitch out))))))

(deftest test-mode-switching-is-idempotent
  (testing "Switching :china through :natural -> :idm -> :industrial -> :analog restores exact initial analog params"
    (instruments/reset-instrument! :china)
    (let [initial-spec   (instruments/resolve-instrument-spec :china)
          initial-params (vec (compiler/compile-perc-params (styles/apply-style initial-spec :analog)))]
      (instruments/patch! :china :mod :natural)
      (instruments/patch! :china :mod :idm)
      (instruments/patch! :china :mod :industrial)
      (instruments/patch! :china :mod :analog)
      (let [final-spec   (instruments/resolve-instrument-spec :china)
            final-params (vec (compiler/compile-perc-params (styles/apply-style final-spec (:mod final-spec))))]
        (is (= initial-params final-params)
            "Returning to :analog must produce bit-for-bit identical 24-float PercPatch")
        (instruments/reset-instrument! :china)))))
