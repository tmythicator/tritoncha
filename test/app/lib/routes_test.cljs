(ns app.lib.routes-test
  (:require
   [app.audio.dsp.fx :as fx]
   [app.audio.dsp.routing :as routing]
   [app.custom.routes :refer [user-routes]]
   [app.lib.routes :refer [core-routes default-graph]]
   [app.state :refer [audio-state repl-registry]]
   [cljs.test :refer [deftest is testing]]))

(defn- all-test-routings []
  (merge core-routes user-routes (:routes @repl-registry)))

(defn- register-test-routing! [graph-key spec]
  (swap! repl-registry assoc-in [:routes graph-key] spec)
  graph-key)

(deftest default-routing-graph-structure-test
  (testing "Default routing graph has required keys and valid types"
    (is (map? default-graph) "Default graph must be a map")
    (is (map? (:busses default-graph)) "Default graph must have :busses map")
    (is (map? (:processors default-graph)) "Default graph must have :processors map")
    (is (vector? (:routes default-graph)) "Default graph must have :routes vector")

    (let [busses (:busses default-graph)
          processors (:processors default-graph)
          routes (:routes default-graph)
          all-nodes (into #{:destination} (concat (keys busses) (keys processors)))]

      (testing "All standard busses exist"
        (is (contains? busses :bus/drums) "Must contain :bus/drums")
        (is (contains? busses :bus/bass) "Must contain :bus/bass")
        (is (contains? busses :bus/space) "Must contain :bus/space")
        (is (contains? busses :bus/direct) "Must contain :bus/direct"))

      (testing "All routes reference declared busses, processors or :destination"
        (doseq [chain routes]
          (is (vector? chain) "Each route chain must be a vector")
          (doseq [node chain]
            (is (contains? all-nodes node)
                (str "Node " node " in route chain " chain " must be a declared bus, processor, or :destination")))))

      (testing "Every bus has a path towards :destination"
        (let [edges (reduce (fn [acc chain]
                              (reduce (fn [m [src dst]] (assoc m src dst))
                                      acc
                                      (partition 2 1 chain)))
                            {}
                            routes)]
          (doseq [b (keys busses)]
            (let [terminates? (loop [curr b visited #{} depth 0]
                                (cond
                                  (= curr :destination) true
                                  (visited curr) false
                                  (>= depth 10) false
                                  (nil? curr) false
                                  :else (recur (get edges curr) (conj visited curr) (inc depth))))]
              (is (true? terminates?) (str "Bus " b " must terminate at :destination")))))))))

(deftest dynamic-routing-registry-test
  (testing "Dynamic routing graph registration and lookup"
    (let [custom-key :test-ambient-routing
          custom-spec {:busses {:bus/pad {:type :volume :volume 0}}
                       :processors {:shimmer {:type :reverb :wet 0.8}}
                       :routes [[:bus/pad :shimmer]
                                [:shimmer :destination]]}]

      (register-test-routing! custom-key custom-spec)
      (let [all (all-test-routings)]
        (is (contains? all custom-key) "Custom routing must be present in all-routings")
        (is (= custom-spec (get all custom-key)) "Registered spec must match input"))

      (testing "audio-state stores current-routing"
        (swap! audio-state assoc :current-routing custom-key)
        (is (= custom-key (:current-routing @audio-state)) "audio-state must reflect loaded routing key")))))

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
                               {:busses {:bus/direct {:type :volume :volume 0}}
                                :processors {}
                                :routes [[:bus/direct :destination]]})
    (is (contains? (routing/all-routings) :custom-matrix) "Dynamic custom matrix must be present")
    (is (= :custom-matrix (routing/set-routing! :custom-matrix)))
    (is (= :custom-matrix (:current-routing @audio-state)))))
