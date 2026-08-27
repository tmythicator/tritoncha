(ns app.lib.instruments-test
  (:require
   [app.custom.instruments :refer [user-instruments]]
   [app.lib.instruments :refer [core-instruments]]
   [app.state :refer [repl-registry]]
   [cljs.test :refer [deftest is testing]]))

(def ^:private test-instrument-aliases
  {:bass :saw-bass
   :sub  :sub-sine
   :pad  :dark-pad})

(defn- all-test-instruments []
  (merge core-instruments user-instruments (:instruments @repl-registry)))

(defn- resolve-test-instrument-spec [spec]
  (cond
    (map? spec) spec
    (keyword? spec) (let [canonical (get test-instrument-aliases spec spec)]
                      (get (all-test-instruments) canonical spec))
    :else spec))

(defn- register-test-instrument! [inst-key spec]
  (swap! repl-registry assoc-in [:instruments inst-key] spec)
  inst-key)

(deftest core-instruments-catalog-test
  (testing "Core instruments catalog contains standard melodic synthesizer voices"
    (let [required-voices [:saw-bass :acid-bass :sub-sine :fm-growl :dark-pad :ambient-glass :pluck-lead :siren]]
      (doseq [inst-key required-voices]
        (let [spec (get core-instruments inst-key)]
          (is (some? spec) (str "Instrument " inst-key " must exist in core-instruments"))
          (is (keyword? (:type spec)) (str "Instrument " inst-key " must specify a keyword :type")))))))

(deftest instrument-alias-resolution-test
  (testing "Standard aliases :bass, :sub, :pad resolve to canonical specs"
    (is (= (get core-instruments :saw-bass) (resolve-test-instrument-spec :bass)))
    (is (= (get core-instruments :sub-sine) (resolve-test-instrument-spec :sub)))
    (is (= (get core-instruments :dark-pad) (resolve-test-instrument-spec :pad)))))

(deftest dynamic-instrument-registration-test
  (testing "Custom instrument preset can be registered live and resolved"
    (let [custom-key :test-supersaw
          custom-spec {:type :mono :bus :space :options {:oscillator {:type "sawtooth"}}}]
      (register-test-instrument! custom-key custom-spec)
      (let [insts (all-test-instruments)]
        (is (contains? insts custom-key) "Custom instrument must be present in all-instruments")
        (is (= custom-spec (resolve-test-instrument-spec custom-key)) "resolve-test-instrument-spec must return registered spec")))))
