(ns app.lib.routes-test
  (:require
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
        (is (contains? busses :drum-bus) "Must contain :drum-bus")
        (is (contains? busses :bass-bus) "Must contain :bass-bus")
        (is (contains? busses :space-bus) "Must contain :space-bus")
        (is (contains? busses :direct-bus) "Must contain :direct-bus"))

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
          custom-spec {:busses {:pad-bus {:type :volume :volume 0}}
                       :processors {:shimmer {:type :reverb :wet 0.8}}
                       :routes [[:pad-bus :shimmer]
                                [:shimmer :destination]]}]

      (register-test-routing! custom-key custom-spec)
      (let [all (all-test-routings)]
        (is (contains? all custom-key) "Custom routing must be present in all-routings")
        (is (= custom-spec (get all custom-key)) "Registered spec must match input"))

      (testing "audio-state stores current-routing"
        (swap! audio-state assoc :current-routing custom-key)
        (is (= custom-key (:current-routing @audio-state)) "audio-state must reflect loaded routing key")))))
