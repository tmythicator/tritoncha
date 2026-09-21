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
    (let [compiled   (get (routing/all-routings) :default)
          busses     (:busses compiled)
          processors (:processors compiled)
          routes     (:routes compiled)
          norm       (routing/normalize-routes routes)]
      (is (map? busses) "Compiled default graph must have :busses map")
      (is (map? processors) "Compiled default graph must have :processors map")
      (is (map? routes) "Compiled default graph must have :routes map")

      (testing "Default topology strictly contains only compressor and limiter processors"
        (is (contains? processors :compressor) "Default must have compressor")
        (is (contains? processors :limiter) "Default must have limiter")
        (is (not (contains? processors :delay)) "Default must NOT have delay processor")
        (is (not (contains? processors :reverb)) "Default must NOT have reverb processor")
        (is (not (contains? processors :filter)) "Default must NOT have filter processor")
        (is (not (contains? processors :distort)) "Default must NOT have distort processor")
        (is (not (contains? processors :crusher)) "Default must NOT have crusher processor")
        (is (not (contains? processors :chorus)) "Default must NOT have chorus processor")
        (is (= [:compressor :limiter] (:bus/master routes)) "Master inserts must be compressor and limiter")
        (is (= [] (:bus/drums routes)) "Drums must have no inserts")
        (is (= [] (:bus/bass routes)) "Bass must have no inserts")
        (is (= [] (:bus/lead routes)) "Lead must have no inserts")
        (is (= [] (:bus/space routes)) "Space must have no inserts"))

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

(deftest compile-route-dsl-test
  (testing "compile-route compiles graph-based DSL into canonical routing"
    (let [compiled (routing/compile-route
                    :test-route
                    {:graph {:master [:distort :filter]
                             :drums  [:out]
                             :lead   [:delay :reverb]}
                     :processors {:filter  [3200 0.4]
                                  :distort {:drive 0.3 :algo :classic}
                                  :delay   ["16n" 0.5 0.35]
                                  :reverb  {:algo :freeverb :size 0.7 :wet 0.25}}})]
      (is (= "TEST ROUTE" (:title compiled)))
      (is (contains? (:busses compiled) :bus/master))
      (is (= 3200.0 (get-in compiled [:processors :filter :frequency])))
      (is (= 0.4 (get-in compiled [:processors :filter :q])))
      (is (= :classic (get-in compiled [:processors :distort :algorithm])))
      (is (= :freeverb (get-in compiled [:processors :reverb :algorithm])))
      (is (= :out (get-in compiled [:routes :bus/drums])))
      (is (= [:delay :reverb] (get-in compiled [:routes :bus/lead])))
      (is (= [:distort :filter] (get-in compiled [:routes :bus/master])))))

  (testing "compile-route compiles vector paths DSL as well"
    (let [compiled (routing/compile-route
                    :test-vec
                    {:graph [[:master :distort :filter :out]
                             [:drums :out]]})]
      (is (= :out (get-in compiled [:routes :bus/drums])))
      (is (= [:distort :filter] (get-in compiled [:routes :bus/master])))))

  (testing "unconfigured processors in :graph automatically take sensible studio defaults"
    (let [compiled (routing/compile-route
                    :auto-comp
                    {:graph {:master [:compressor]}})]
      (is (contains? (:processors compiled) :compressor))
      (is (true? (get-in compiled [:processors :compressor :enabled])))
      (is (= 4.0 (get-in compiled [:processors :compressor :ratio]))))))

(deftest dynamic-routing-registry-test
  (testing "Dynamic routing graph registration and lookup"
    (let [custom-key  :test-ambient-routing
          custom-spec {:title  "TEST AMBIENT"
                       :filter 3500
                       :delay  ["8n." 0.4 0.2]
                       :reverb {:algo :fdn :wet 0.5}}]

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
                :bus/bass   [:distort]
                :bus/space  [:delay]}
          norm (routing/normalize-routes spec)]
      (is (= {:inserts [] :target :out} (:bus/drums norm)))
      (is (= {:inserts [:distort] :target :bus/master} (:bus/bass norm)))
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

    (is (= :void-chamber (routing/set-routing! :void-chamber)))
    (is (= :void-chamber (:current-routing @audio-state)))
    (is (= :fdn (:reverb-mode @audio-state)))

    (is (= :default (routing/set-routing! :default)))
    (is (= :default (:current-routing @audio-state)))
    (is (= :fdn (:reverb-mode @audio-state))))

  (testing "crematorium routes drums to :out and sets bus-bypass-master-fx"
    (routing/set-routing! :crematorium)
    (is (true? (get-in @audio-state [:bus-bypass-master-fx :bus/drums])))
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/bass])))
    (routing/set-routing! :default)
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/drums]))))

  (testing "switching across routings and back to default completely resets state"
    (routing/set-routing! :cyber-glitch)
    (is (= 5000.0 (:cutoff (fx/get-filter-state))))
    (routing/set-routing! :default)
    (is (= 18000.0 (:cutoff (fx/get-filter-state))))
    (is (= 0.0 (:resonance (fx/get-filter-state))))
    (is (= :fdn (:reverb-mode @audio-state))))

  (testing "switching to dub-echo applies dub-echo parameters and switching back resets to default"
    (routing/set-routing! :dub-echo)
    (is (= 4200.0 (:cutoff (fx/get-filter-state))))
    (routing/set-routing! :default)
    (is (= 18000.0 (:cutoff (fx/get-filter-state)))))

  (testing "default routing strictly leaves all fx dry and open except compressor"
    (routing/set-routing! :ambient-prism)
    (is (= 0.38 (:wet (fx/get-delay-state))))
    (is (= 0.52 (:wet (fx/get-reverb-state))))
    (is (= 15000.0 (:cutoff (fx/get-filter-state))))
    (is (= 0.45 (:wet (fx/get-chorus-state))))

    (routing/set-routing! :default)
    (is (= 0.0 (:wet (fx/get-delay-state))) "Default must have 0.0 delay wet")
    (is (= 0.0 (:wet (fx/get-reverb-state))) "Default must have 0.0 reverb wet")
    (is (= 18000.0 (:cutoff (fx/get-filter-state))) "Default must have 18000 Hz filter cutoff")
    (is (= 0.0 (:drive (fx/get-drive-state))) "Default must have 0.0 distortion")
    (is (= 0.0 (:wet (fx/get-chorus-state))) "Default must have 0.0 chorus wet")
    (is (true? (:enabled (fx/get-compressor-state))) "Default must have compressor enabled")
    (is (= 3.0 (:ratio (fx/get-compressor-state))) "Default compressor ratio must be 3.0"))

  (testing "custom dynamic routing registration and switching"
    (routing/register-routing! :custom-matrix
                               {:bypass [:drums]
                                :filter 4000})
    (is (contains? (routing/all-routings) :custom-matrix) "Dynamic custom matrix must be present")
    (is (= :custom-matrix (routing/set-routing! :custom-matrix)))
    (is (= :custom-matrix (:current-routing @audio-state))))

  (testing "per-bus bypass flags update correctly when switching between topologies"
    (routing/set-routing! :liminal-prison)
    (is (true? (get-in @audio-state [:bus-bypass-master-fx :bus/lead])) "Lead in liminal-prison routes to out")
    (is (true? (get-in @audio-state [:bus-bypass-master-fx :bus/space])) "Space in liminal-prison routes to out")
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/drums])) "Drums route to master")
    (routing/set-routing! :default)
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/lead])) "Default leaves lead going to master")
    (is (false? (get-in @audio-state [:bus-bypass-master-fx :bus/space])) "Default leaves space going to master"))

  (testing "neutral processors set delay and reverb wet to 0.0 when not declared"
    (routing/register-routing! :dry-matrix
                               {:bypass [:drums]})
    (routing/set-routing! :dry-matrix)
    (is (= 0.0 (:wet (fx/get-delay-state))))
    (is (= 0.0 (:wet (fx/get-reverb-state))))
    (routing/set-routing! :default))

  (testing "reset-fx! restores active topology processors and sends after live tweaks"
    (routing/set-routing! :dub-echo)
    (is (= 4200.0 (:cutoff (fx/get-filter-state))))
    (fx/set-filter-cutoff! 1200.0)
    (fx/set-delay-feedback! 0.90)
    (is (= 1200.0 (:cutoff (fx/get-filter-state))))
    (is (= 0.90 (:feedback (fx/get-delay-state))))
    (routing/reset-fx!)
    (is (= 4200.0 (:cutoff (fx/get-filter-state))))
    (is (= 0.44 (:feedback (fx/get-delay-state))))
    (routing/set-routing! :default))

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

(deftest modular-per-bus-routing-test
  (testing "compile-bus-insert maps processor specs into Worklet/Rust FFI parameters"
    (let [specs {:filter     {:frequency 3500 :q 0.5}
                 :delay      {:time "8n." :feedback 0.45 :wet 0.35}
                 :distort    {:drive 0.25 :bits 12.0 :sample-hold 1.0}
                 :crusher    {:bits 6.0 :sample-hold 2.0}
                 :chorus     {:rate 0.8 :depth 0.4 :wet 0.3}
                 :reverb     {:room-size 0.85 :wet 0.40}
                 :compressor {:threshold -16.0 :ratio 4.0 :makeup 3.0}}]
      (is (= {:type "filter" :cutoffHz 3500.0 :resonance 0.5}
             (routing/compile-bus-insert :filter specs)))
      (is (= {:type "delay" :timeS 0.27 :feedback 0.45 :wet 0.35}
             (routing/compile-bus-insert :delay specs)))
      (is (= {:type "distort" :drive 0.25 :bits 12.0 :sampleHold 1.0}
             (routing/compile-bus-insert :distort specs)))
      (is (= {:type "distort" :drive 0.0 :bits 6.0 :sampleHold 2.0}
             (routing/compile-bus-insert :crusher specs)))
      (is (= {:type "chorus" :rateHz 0.8 :depth 0.4 :mix 0.3}
             (routing/compile-bus-insert :chorus specs)))
      (is (= {:type "reverb" :roomSize 0.85 :wet 0.40}
             (routing/compile-bus-insert :reverb specs)))
      (is (= {:type "compressor" :thresholdDb -16.0 :ratio 4.0 :attackS 0.01 :releaseS 0.1 :makeupDb 3.0 :mix 1.0}
             (routing/compile-bus-insert :compressor specs)))))

  (testing "liminal-prison routes drums through filter and delay to master, space with reverb and delay to out, lead to out, and master through chorus, compressor, and limiter"
    (let [compiled (get (routing/all-routings) :liminal-prison)
          norm     (routing/normalize-routes (:routes compiled))]
      (is (= {:inserts [:filter :delay] :target :bus/master}
             (:bus/drums norm)))
      (is (= {:inserts [:filter] :target :out}
             (:bus/lead norm)))
      (is (= {:inserts [:reverb :delay] :target :out}
             (:bus/space norm)))
      (is (= {:inserts [:chorus :compressor :limiter] :target :out}
             (:bus/master norm)))
      (is (= {:inserts [] :target :bus/master}
             (:bus/bass norm)))
      (is (= {:inserts [] :target :out}
             (:bus/direct norm))))))
