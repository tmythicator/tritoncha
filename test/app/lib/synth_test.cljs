(ns app.lib.synth-test
  (:require
   [app.custom.synth :refer [user-synths]]
   [app.lib.synth :refer [core-synths]]
   [app.state :refer [repl-registry]]
   [cljs.test :refer [deftest is testing]]))

(def ^:private test-instrument-aliases
  {:bass :bass-analog
   :sub  :sub-pure
   :pad  :pad-cinema})

(defn- all-test-synths []
  (merge core-synths user-synths (:instruments @repl-registry)))

(defn- resolve-test-synth-spec [spec]
  (cond
    (map? spec) spec
    (keyword? spec) (let [canonical (get test-instrument-aliases spec spec)]
                      (get (all-test-synths) canonical spec))
    :else spec))

(defn- register-test-synth! [inst-key spec]
  (swap! repl-registry assoc-in [:instruments inst-key] spec)
  inst-key)

(deftest core-synths-catalog-test
  (testing "Core synths catalog contains standard melodic synthesizer voices with titles"
    (let [mnemonic-voices [:bass-analog :bass-303 :sub-moog :bass-moog :sub-pure :sub-808 :bass-slap :bass-neuro
                           :acid-beast :liquid-reese
                           :pad-cinema :pad-strings :pad-vocal :pad-glass :pad-drone
                           :pad-dreamy :blade-runner
                           :lead-pluck :lead-supersaw :lead-fm :lead-blade :lead-hoover :lead-string :lead-8bit :lead-bell
                           :tokyo-drift :glass-mallet :lead-nbell
                           :click :fx-siren :fx-laser :fx-zap]]
      (doseq [inst-key mnemonic-voices]
        (let [spec (get core-synths inst-key)]
          (is (some? spec) (str "Instrument " inst-key " must exist in core-synths"))
          (is (keyword? (:type spec)) (str "Instrument " inst-key " must specify a keyword :type"))
          (is (string? (:title spec)) (str "Instrument " inst-key " must specify a string :title")))))))

(deftest synth-alias-resolution-test
  (testing "Standard aliases :bass, :sub, :pad resolve to canonical specs"
    (is (= (get core-synths :bass-analog) (resolve-test-synth-spec :bass)))
    (is (= (get core-synths :sub-pure) (resolve-test-synth-spec :sub)))
    (is (= (get core-synths :pad-cinema) (resolve-test-synth-spec :pad)))))

(deftest dynamic-synth-registration-test
  (testing "Custom instrument preset can be registered live and resolved"
    (let [custom-key :test-supersaw
          custom-spec {:title "Test Supersaw" :type :mono :bus :space :osc {:type :supersaw}}]
      (register-test-synth! custom-key custom-spec)
      (let [insts (all-test-synths)]
        (is (contains? insts custom-key) "Custom synth must be present in all-test-synths")
        (is (= custom-spec (resolve-test-synth-spec custom-key)) "resolve-test-synth-spec must return registered spec")))))
