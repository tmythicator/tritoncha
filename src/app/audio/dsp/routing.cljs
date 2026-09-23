(ns app.audio.dsp.routing
  "Declarative DSP routing graph catalog, bus configuration, and live route application."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.processors :as proc]
            [app.audio.dsp.worklet :as worklet]
            [app.custom.routes :refer [user-routes]]
            [app.lib.routes :refer [core-routes]]
            [app.state :refer [audio-state repl-registry]]
            [clojure.string :as str]))

;; Processor Compilation Alias

(def compile-bus-insert
  "Compiles a processor keyword and its parameters into the map format expected by the Rust WASM worklet.
  Examples: (compile-bus-insert :filter {:filter {:frequency 3500 :q 0.0}})
            -> {:type \"filter\" :cutoffHz 3500.0 :resonance 0.0}."
  proc/compile-bus-insert)

;; Graph Edge and Route Compilation

(def ^:private arrow-tokens #{:-> '-> '=> :>})

(defn- clean-edge [edge]
  (vec (remove arrow-tokens edge)))

(defn compile-route
  "Compiles a graph-based routing specification into a canonical routing map.
  Supports clean map DSL:
    :graph {:drums [:filter :delay] :space :out :lead [:filter :out] :master [:chorus]}
    :processors {:filter {:cutoff 3500} ...}
  Also supports vector graph DSL:
    :graph [[:master :filter :compressor :out] [:drums :out]]"
  ([raw-spec] (compile-route :route raw-spec))
  ([rk raw-spec]
   (let [title (or (:title raw-spec)
                   (-> (name rk) (str/replace #"-" " ") str/upper-case))
         proc-defs (merge (:processors raw-spec)
                          (:nodes raw-spec)
                          (dissoc raw-spec :title :graph :nodes :routes :busses :processors :bypass :sends))
         raw-graph (:graph raw-spec)

         parsed-routes
         (cond
           (map? raw-graph)
           (into {}
                 (for [[b v] raw-graph]
                   [(busses/normalize-bus-key b)
                    (cond
                      (= v :out) :out
                      (vector? v)
                      (let [ce (clean-edge v)]
                        (if (= ce [:out])
                          :out
                          ce))
                      :else (vec [v]))]))

           (sequential? raw-graph)
           (let [edges (map clean-edge raw-graph)]
             (reduce (fn [acc e]
                       (if (empty? e)
                         acc
                         (let [orig-b   (first e)
                               b        (busses/normalize-bus-key orig-b)
                               has-out? (some #{:out} e)
                               ins      (vec (remove #{orig-b b :out} e))]
                           (cond
                             (and (= (count e) 2) (= (second e) :out))
                             (assoc acc b :out)

                             (empty? ins)
                             (if has-out? (assoc acc b :out) (assoc acc b []))

                             has-out?
                             (if (= b :bus/master)
                               (assoc acc b ins)
                               (assoc acc b (conj ins :out)))

                             :else
                             (assoc acc b ins)))))
                     {}
                     edges))

           :else
           {})

         explicit-bypasses (set (map busses/normalize-bus-key (or (:bypass raw-spec) [])))

         default-master-chain (if (contains? parsed-routes :bus/master)
                                (let [m (:bus/master parsed-routes)]
                                  (if (= m :out) [] (vec (remove #{:out} m))))
                                (if (some #{:compressor} (keys proc-defs))
                                  [:compressor]
                                  [:compressor]))

         resolve-bus-route
         (fn [b-key]
           (cond
             (explicit-bypasses b-key) :out
             (contains? parsed-routes b-key) (get parsed-routes b-key)
             :else []))

         routes
         {:bus/drums  (resolve-bus-route :bus/drums)
          :bus/bass   (resolve-bus-route :bus/bass)
          :bus/lead   (resolve-bus-route :bus/lead)
          :bus/space  (resolve-bus-route :bus/space)
          :bus/master default-master-chain}

         bus-proc-keys    (distinct (mapcat (fn [[_ v]]
                                              (if (vector? v) (remove #{:out} v) []))
                                            routes))
         active-proc-keys (distinct (concat bus-proc-keys (keys proc-defs)))
         processors       (proc/compile-processors active-proc-keys proc-defs)

         busses {:bus/drums {} :bus/bass {} :bus/lead {} :bus/space {} :bus/master {}}]
     {:title      title
      :busses     busses
      :processors processors
      :routes     routes})))

(defn normalize-routes
  "Normalizes route specifications into a standard map:
  {bus-key {:inserts [fx ...] :target (:bus/master or :out)}}.
  Internal :bus/direct is automatically added with target :out."
  [routes]
  (let [base (into {}
                   (map (fn [[bus val]]
                          (let [master? (= bus :bus/master)]
                            (cond
                              (= val :out)
                              [bus {:inserts [] :target :out}]

                              (map? val)
                              [bus (merge {:inserts [] :target (if master? :out :bus/master)} val)]

                              (vector? val)
                              (let [has-out? (some #{:out} val)
                                    ins (vec (remove #{:out} val))]
                                [bus {:inserts ins :target (if (or master? has-out?) :out :bus/master)}])

                              :else
                              [bus {:inserts [] :target (if master? :out :bus/master)}]))))
                   routes)]
    (assoc base :bus/direct {:inserts [] :target :out})))

;; Routing Presets and Engine Application

(defn register-routing!
  "Registers or updates a dynamic routing topology in the REPL registry.
  Examples: (register-routing! :dub-matrix {:filter 3000 :delay [\"8n\" 0.4 0.3]})."
  [routing-key spec]
  (swap! repl-registry assoc-in [:routings routing-key] spec)
  routing-key)

(defn all-routings
  "Returns a merged map of compiled built-in routings, user custom routings, and REPL routings."
  []
  (let [raw (merge core-routes user-routes (:routings @repl-registry))]
    (into {} (map (fn [[rk spec]] [rk (compile-route rk spec)]) raw))))

(defn set-routing!
  "Switches the active DSP routing topology preset and applies its processors to the Rust WASM engine.
  Examples: (set-routing! :dub-echo), (route! :cyber-glitch)."
  [routing-key]
  (when-let [spec (get (all-routings) routing-key)]
    (swap! audio-state assoc :current-routing routing-key)
    (let [norm (normalize-routes (:routes spec))
          proc-specs (:processors spec)]
      ;; 1. Update ClojureScript audio-state bypass flags for UI and telemetry
      (doseq [b-key [:bus/drums :bus/bass :bus/space :bus/lead]]
        (let [bypass? (= (:target (get norm b-key)) :out)]
          (swap! audio-state assoc-in [:bus-bypass-master-fx b-key] bypass?)))

      ;; 2. Transmit modular insert chains into Rust WASM per-bus DSP engine
      (doseq [[b-key bus-idx] [[:bus/drums 0]
                               [:bus/bass 1]
                               [:bus/space 2]
                               [:bus/lead 3]
                               [:bus/direct 4]
                               [:bus/master 5]]]
        (let [route-entry (get norm b-key)
              target-out? (= (:target route-entry) :out)
              raw-inserts (or (:inserts route-entry) [])
              compiled-inserts (vec (keep #(proc/compile-bus-insert % proc-specs) raw-inserts))]
          (worklet/set-worklet-bus-chain! bus-idx target-out? compiled-inserts)))

      ;; 3. Apply processor parameters to ClojureScript state atoms and global fallbacks
      (proc/apply-processors! proc-specs)
      routing-key)))

(defn init-routing!
  "Applies the active routing topology to the Rust WASM engine."
  []
  (set-routing! (or (:current-routing @audio-state) :default)))

(defn reset-fx!
  "Resets all DSP processors and bus chains back to the specification of the active routing topology.
  Examples: (reset-fx!)."
  []
  (set-routing! (or (:current-routing @audio-state) :default)))
