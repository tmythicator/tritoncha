(ns app.audio.dsp.routing
  "Declarative DSP routing graph catalog, bus configuration, and live route application."
  (:require [app.audio.dsp.busses :as busses]
            [app.audio.dsp.fx :as fx]
            [app.audio.dsp.worklet :as worklet]
            [app.custom.routes :refer [user-routes]]
            [app.lib.routes :refer [core-routes]]
            [app.state :refer [audio-state repl-registry]]
            [clojure.string :as str]))

;; Neutral Baseline DSP Processor Configuration

(def ^:private neutral-processors
  {:distort    {:distortion 0.0 :algorithm :adaa}
   :crusher    {:bits 16.0 :sample-hold 1.0}
   :chorus     {:rate 0.8 :depth 0.4 :wet 0.0}
   :filter     {:frequency 18000.0 :q 0.0}
   :delay      {:time "8n." :feedback 0.35 :wet 0.0}
   :reverb     {:room-size 0.75 :wet 0.0 :algorithm :fdn}
   :compressor {:enabled false :threshold -12.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 0.0}})

;; Route Compilation and Normalization

(defn- compile-filter-spec [v]
  (cond
    (number? v)
    {:type :filter :frequency (float v) :q 0.0 :filter-type "lowpass"}

    (vector? v)
    {:type :filter :frequency (float (first v)) :q (float (or (second v) 0.0)) :filter-type "lowpass"}

    (map? v)
    {:type        :filter
     :filter-type "lowpass"
     :frequency   (float (or (:frequency v) (:cutoff v) (:cutoff-hz v) (:freq v) 18000.0))
     :q           (float (or (:q v) (:resonance v) 0.0))}

    :else nil))

(defn- compile-distort-spec [v]
  (cond
    (number? v)
    {:type :distortion :distortion (float v) :algorithm :adaa :bits 16.0 :sample-hold 1.0 :wet 1.0}

    (map? v)
    {:type        :distortion
     :distortion  (float (or (:drive v) (:distortion v) 0.0))
     :algorithm   (or (:algo v) (:algorithm v) :adaa)
     :bits        (float (or (:bits v) 16.0))
     :sample-hold (float (or (:sample-hold v) (:sampleHold v) 1.0))
     :wet         (float (or (:wet v) 1.0))}

    :else nil))

(defn- compile-crusher-spec [v]
  (cond
    (number? v)
    {:type :bitcrusher :bits (float v) :sample-hold 1.0 :wet 1.0}

    (vector? v)
    {:type        :bitcrusher
     :bits        (float (or (first v) 16.0))
     :sample-hold (float (or (second v) 1.0))
     :wet         (float (or (nth v 2 nil) 1.0))}

    (map? v)
    {:type        :bitcrusher
     :bits        (float (or (:bits v) 16.0))
     :sample-hold (float (or (:sample-hold v) (:sampleHold v) 1.0))
     :drive       (float (or (:drive v) (:distortion v) 0.0))
     :wet         (float (or (:wet v) 1.0))}

    :else nil))

(defn- compile-chorus-spec [v]
  (cond
    (number? v)
    {:type :chorus :rate 0.8 :depth 0.4 :wet (float v)}

    (map? v)
    {:type  :chorus
     :rate  (float (or (:rate v) (:rate-hz v) 0.8))
     :depth (float (or (:depth v) 0.4))
     :wet   (float (or (:wet v) (:mix v) 0.0))}

    :else nil))

(defn- compile-delay-spec [v]
  (cond
    (number? v)
    {:type     :delay
     :time     (float v)
     :feedback 0.35
     :wet      0.25}

    (vector? v)
    {:type     :delay
     :time     (str (or (first v) "8n."))
     :feedback (float (or (second v) 0.35))
     :wet      (float (or (nth v 2 nil) 0.25))}

    (map? v)
    {:type     :delay
     :time     (str (or (:time v) "8n."))
     :feedback (float (or (:feedback v) (:fb v) 0.35))
     :wet      (float (or (:wet v) 0.0))}

    :else nil))

(defn- compile-reverb-spec [v]
  (cond
    (number? v)
    {:type :reverb :algorithm :fdn :roomSize 0.75 :wet (float v)}

    (map? v)
    (let [algo (or (:algo v) (:algorithm v) :fdn)]
      {:type      (if (= algo :freeverb) :freeverb :reverb)
       :algorithm algo
       :roomSize  (float (or (:size v) (:room-size v) 0.75))
       :wet       (float (or (:wet v) 0.35))})

    :else nil))

(defn- compile-compressor-spec [v]
  (cond
    (boolean? v)
    {:type :compressor :enabled v :threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}

    (map? v)
    (merge {:type :compressor :enabled (get v :enabled true)
            :threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
           v)

    :else nil))

(defn compile-bus-insert
  "Compiles a processor keyword and its parameters into the map format expected by the Rust WASM worklet.
  Examples: (compile-bus-insert :filter {:filter {:frequency 3500 :q 0.0}})
            -> {:type \"filter\" :cutoffHz 3500.0 :resonance 0.0}."
  [proc-key proc-specs]
  (let [spec (get proc-specs proc-key)]
    (case proc-key
      :filter
      {:type      "filter"
       :cutoffHz  (float (or (:frequency spec) (:cutoff spec) (:cutoff-hz spec) 18000.0))
       :resonance (float (or (:resonance spec) (:q spec) 0.0))}

      :delay
      {:type     "delay"
       :timeS    (float (or (when-let [t (or (:time spec) (:time-s spec))]
                              (fx/parse-delay-time-s t))
                            0.35))
       :feedback (float (or (:feedback spec) (:fb spec) 0.35))
       :wet      (float (or (:wet spec) 0.30))}

      :distort
      {:type       "distort"
       :drive      (float (or (:drive spec) (:distortion spec) 0.0))
       :bits       (float (or (:bits spec) 16.0))
       :sampleHold (float (or (:sample-hold spec) (:sampleHold spec) 1.0))}

      (:crusher :bitcrusher)
      {:type       "distort"
       :drive      (float (or (:drive spec) 0.0))
       :bits       (float (or (:bits spec) 8.0))
       :sampleHold (float (or (:sample-hold spec) (:sampleHold spec) 1.0))}

      :chorus
      {:type   "chorus"
       :rateHz (float (or (:rate spec) (:rate-hz spec) 0.8))
       :depth  (float (or (:depth spec) 0.4))
       :mix    (float (or (:mix spec) (:wet spec) 0.30))}

      :reverb
      {:type     "reverb"
       :roomSize (float (or (:roomSize spec) (:room-size spec) (:size spec) 0.75))
       :wet      (float (or (:wet spec) 0.35))}

      :compressor
      {:type        "compressor"
       :thresholdDb (float (or (:threshold spec) (:threshold-db spec) -12.0))
       :ratio       (float (or (:ratio spec) 4.0))
       :attackS     (float (or (:attack spec) (:attack-s spec) 0.010))
       :releaseS    (float (or (:release spec) (:release-s spec) 0.100))
       :makeupDb    (float (or (:makeup spec) (:makeup-db spec) 2.5))
       :mix         (float (or (:mix spec) 1.0))}

      (:limiter :limitter)
      {:type        "compressor"
       :thresholdDb (float (or (:threshold spec) (:threshold-db spec) -1.0))
       :ratio       20.0
       :attackS     0.001
       :releaseS    0.050
       :makeupDb    0.0
       :mix         1.0}

      nil)))

(def ^:private arrow-tokens #{:-> '-> '=> :>})

(defn- clean-edge [edge]
  (vec (remove arrow-tokens edge)))

(def ^:private default-active-specs
  {:filter     {:frequency 12000.0 :q 0.0}
   :distort    {:drive 0.20 :algo :adaa :wet 0.5}
   :crusher    {:bits 8.0 :sample-hold 1.0 :wet 0.5}
   :chorus     {:rate 0.8 :depth 0.4 :wet 0.3}
   :compressor {:threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
   :delay      ["8n." 0.35 0.25]
   :reverb     {:algo :fdn :size 0.75 :wet 0.35}})

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

         bus-proc-keys (distinct (mapcat (fn [[_ v]]
                                           (if (vector? v) (remove #{:out} v) []))
                                         routes))
         active-proc-keys (distinct (concat bus-proc-keys (keys proc-defs)))

         f-spec  (when (some #{:filter} active-proc-keys)
                   (compile-filter-spec (or (:filter proc-defs) (:filter default-active-specs))))
         d-spec  (when (some #{:distort} active-proc-keys)
                   (compile-distort-spec (or (:distort proc-defs) (:distort default-active-specs))))
         c-spec  (when (some #{:crusher} active-proc-keys)
                   (compile-crusher-spec (or (:crusher proc-defs) (:crusher default-active-specs))))
         ch-spec (when (some #{:chorus} active-proc-keys)
                   (compile-chorus-spec (or (:chorus proc-defs) (:chorus default-active-specs))))
         dl-spec (when (some #{:delay} active-proc-keys)
                   (compile-delay-spec (or (:delay proc-defs) (:delay default-active-specs))))
         rv-spec (when (some #{:reverb} active-proc-keys)
                   (compile-reverb-spec (or (:reverb proc-defs) (:reverb default-active-specs))))
         cp-spec (when (some #{:compressor} active-proc-keys)
                   (compile-compressor-spec (or (:compressor proc-defs) (:compressor default-active-specs))))

         processors (cond-> {:limiter {:type :limiter :threshold -1.0}}
                      f-spec  (assoc :filter f-spec)
                      d-spec  (assoc :distort d-spec)
                      c-spec  (assoc :crusher c-spec)
                      ch-spec (assoc :chorus ch-spec)
                      dl-spec (assoc :delay dl-spec)
                      rv-spec (assoc :reverb rv-spec)
                      cp-spec (assoc :compressor cp-spec))

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

(defn- apply-processor!
  "Applies a declared DSP processor configuration map into the Rust WASM engine."
  [p-type spec]
  (case p-type
    :distort
    (do
      (fx/set-distortion! (or (:distortion spec) (:drive spec) 0.0))
      (fx/set-drive-mode! (or (:algorithm spec) :adaa)))

    :crusher
    (fx/set-bitcrush! (or (:bits spec) 16.0) (or (:sample-hold spec) 1.0))

    :chorus
    (fx/set-chorus! (or (:rate spec) 0.8) (or (:depth spec) 0.4) (or (:wet spec) 0.0))

    :filter
    (let [freq (or (:frequency spec) (:cutoff spec) 18000.0)
          q    (or (:q spec) (:resonance spec) 0.0)]
      (fx/set-filter-cutoff! freq)
      (fx/set-filter-q! q))

    :delay
    (fx/set-delay! (or (:time spec) "8n.") (or (:feedback spec) 0.35) (or (:wet spec) 0.0))

    :reverb
    (do
      (fx/set-reverb! (or (:roomSize spec) (:room-size spec) 0.75) (or (:wet spec) 0.0))
      (fx/set-reverb-mode! (or (:algorithm spec) :fdn)))

    :compressor
    (fx/set-compressor! spec)

    nil))

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
              compiled-inserts (vec (keep #(compile-bus-insert % proc-specs) raw-inserts))]
          (worklet/set-worklet-bus-chain! bus-idx target-out? compiled-inserts)))

      ;; 3. Apply processor parameters to ClojureScript state atoms and global fallbacks
      (let [dsp (merge-with merge neutral-processors proc-specs)]
        (doseq [[p-type p-spec] dsp]
          (apply-processor! p-type p-spec)))
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
