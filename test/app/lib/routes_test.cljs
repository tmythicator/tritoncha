(ns app.lib.routes-test
  (:require
   [app.audio.dsp.fx :as fx]
   [app.audio.dsp.routing :as routing]
   [app.custom.routes :refer [user-routes]]
   [app.lib.routes :refer [core-routes default-graph]]
   [app.state :refer [audio-state repl-registry]]
   [cljs.test :refer [deftest is testing]]))

(defn- all-test-routings []
  (merge core-routes user-routes (:routings @repl-registry)))

(defn- register-test-routing! [graph-key spec]
  (swap! repl-registry assoc-in [:routings graph-key] spec)
  graph-key)

(deftest default-routing-graph-structure-test
  (testing "Default routing graph has required keys and valid types"
    (is (map? default-graph) "Default graph must be a map")
    (is (map? (:busses default-graph)) "Default graph must have :busses map")
    (is (map? (:processors default-graph)) "Default graph must have :processors map")
    (is (map? (:routes default-graph)) "Default graph must have :routes map")

    (let [busses     (:busses default-graph)
          processors (:processors default-graph)
          routes     (:routes default-graph)
          norm       (routing/normalize-routes routes)]

      (testing "All musical busses exist"
        (is (contains? busses :bus/drums) "Must contain :bus/drums")
        (is (contains? busses :bus/bass) "Must contain :bus/bass")
        (is (contains? busses :bus/lead) "Must contain :bus/lead")
        (is (contains? busses :bus/space) "Must contain :bus/space")
        (is (contains? busses :bus/master) "Must contain :bus/master"))

      (testing "All routes reference declared busses, processors or :out"
        (doseq [[bus {:keys [inserts target]}] norm]
          (is (or (contains? busses bus) (= bus :bus/direct)) (str "Bus " bus " must be a declared bus or internal direct"))
          (is (contains? #{:out :bus/master} target) (str "Target " target " must be :out or :bus/master"))
          (doseq [proc inserts]
            (is (contains? processors proc)
                (str "Insert processor " proc " on bus " bus " must be a declared processor")))))

      (testing "Every bus has a path towards :out"
        (doseq [b (conj (keys busses) :bus/direct)]
          (let [terminates? (loop [curr b visited #{} depth 0]
                              (cond
                                (= curr :out) true
                                (visited curr) false
                                (>= depth 10) false
                                (nil? curr) false
                                :else (recur (:target (get norm curr)) (conj visited curr) (inc depth))))]
            (is (true? terminates?) (str "Bus " b " must terminate at :out"))))))))

(deftest dynamic-routing-registry-test
  (testing "Dynamic routing graph registration and lookup"
    (let [custom-key  :test-ambient-routing
          custom-spec {:busses     {:bus/pad    {}
                                    :bus/master {}}
                       :processors {:shimmer {:type :reverb :wet 0.8}}
                       :routes     {:bus/pad    [:shimmer :bus/master]
                                    :bus/master :out}}]

      (register-test-routing! custom-key custom-spec)
      (let [all (all-test-routings)]
        (is (contains? all custom-key) "Custom routing must be present in all-routings")
        (is (= custom-spec (get all custom-key)) "Registered spec must match input"))

      (testing "audio-state stores current-routing"
        (swap! audio-state assoc :current-routing custom-key)
        (is (= custom-key (:current-routing @audio-state)) "audio-state must reflect loaded routing key")))))

(deftest normalize-routes-test
  (testing "normalize-routes handles map syntax and automatically adds internal :bus/direct to :out"
    (let [spec {:bus/drums  []
                :bus/lead   [:chorus :delay :reverb]
                :bus/master [:filter :limiter]}
          norm (routing/normalize-routes spec)]
      (is (= {:inserts [] :target :bus/master} (:bus/drums norm)))
      (is (= {:inserts [:chorus :delay :reverb] :target :bus/master} (:bus/lead norm)))
      (is (= {:inserts [] :target :out} (:bus/direct norm)))
      (is (= {:inserts [:filter :limiter] :target :out} (:bus/master norm)))))

  (testing "normalize-routes handles custom target overrides in map DSL"
    (let [spec {:bus/drums  :out
                :bus/bass   [:distort :out]
                :bus/space  [:delay :bus/master]}
          norm (routing/normalize-routes spec)]
      (is (= {:inserts [] :target :out} (:bus/drums norm)))
      (is (= {:inserts [:distort] :target :out} (:bus/bass norm)))
      (is (= {:inserts [:delay] :target :bus/master} (:bus/space norm))))))

(deftest set-routing-switch-test
  (testing "set-routing! switches active routing and updates audio-state"
    (is (= :dub-echo (routing/set-routing! :dub-echo)))
    (is (= :dub-echo (:current-routing @audio-state)))

    (is (= :cyber-glitch (routing/set-routing! :cyber-glitch)))
    (is (= :cyber-glitch (:current-routing @audio-state)))

    (is (= :vintage-schroeder (routing/set-routing! :vintage-schroeder)))
    (is (= :vintage-schroeder (:current-routing @audio-state)))
    (is (= :freeverb (:reverb-mode @audio-state)))

    (is (= :default (routing/set-routing! :default)))
    (is (= :default (:current-routing @audio-state)))
    (is (= :fdn (:reverb-mode @audio-state))))

  (testing "crematorium routes drums to :out and sets bus-bypass-master-fx"
    (routing/set-routing! :crematorium)
    (is (true? (get-in @audio-state [:bus-bypass-master-fx :bus/drums])))
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/bass])))
    (routing/set-routing! :default)
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/drums]))))

  (testing "switching across routings and back to default completely resets state without active track cutoff"
    (swap! audio-state dissoc :track-cutoff)
    (routing/set-routing! :cyber-glitch)
    (is (= 5000.0 (:cutoff (fx/get-filter-state))))
    (routing/set-routing! :default)
    (is (= 18000.0 (:cutoff (fx/get-filter-state))))
    (is (= 0.0 (:resonance (fx/get-filter-state))))
    (is (= :fdn (:reverb-mode @audio-state))))

  (testing "switching to dub-echo and back to default restores active track cutoff"
    (swap! audio-state assoc :track-cutoff 5000.0)
    (routing/set-routing! :dub-echo)
    (is (= 4200.0 (:cutoff (fx/get-filter-state))))
    (routing/set-routing! :default)
    (is (= 5000.0 (:cutoff (fx/get-filter-state)))))

  (testing "custom dynamic routing registration and switching"
    (routing/register-routing! :custom-matrix
                               {:busses     {:bus/drums  {}
                                             :bus/master {}}
                                :processors {}
                                :routes     {:bus/drums  :out
                                             :bus/master :out}})
    (is (contains? (routing/all-routings) :custom-matrix) "Dynamic custom matrix must be present")
    (is (= :custom-matrix (routing/set-routing! :custom-matrix)))
    (is (= :custom-matrix (:current-routing @audio-state))))

  (testing "all built-in and user custom topologies switch cleanly and have valid termination"
    (let [all (routing/all-routings)]
      (doseq [[rk spec] all]
        (is (= rk (routing/set-routing! rk)) (str "Routing " rk " must switch successfully"))
        (is (= rk (:current-routing @audio-state)) (str "Audio state must store " rk))
        (is (map? (:busses spec)) (str "Routing " rk " must define :busses"))
        (is (map? (:routes spec)) (str "Routing " rk " must define :routes map"))))))

(deftest bus-master-routing-topology-test
  (testing "All built-in and user custom topologies route standard busses to :out via :bus/master or direct"
    (let [all (routing/all-routings)]
      (doseq [[rk spec] all]
        (let [busses (:busses spec)
              norm   (routing/normalize-routes (:routes spec))]
          (is (contains? busses :bus/master) (str "Routing " rk " must declare :bus/master"))
          (doseq [b (conj (keys busses) :bus/direct)]
            (let [terminates? (loop [curr b visited #{} depth 0]
                                (cond
                                  (= curr :out) true
                                  (visited curr) false
                                  (>= depth 12) false
                                  (nil? curr) false
                                  :else (recur (:target (get norm curr)) (conj visited curr) (inc depth))))]
              (is (true? terminates?) (str "In routing " rk ", bus " b " must terminate at :out")))))))))
