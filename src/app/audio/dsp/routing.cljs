(ns app.audio.dsp.routing
  "Declarative DSP routing graph catalog, bus configuration, and live route application."
  (:require [app.audio.control.mixer :as mixer]
            [app.audio.dsp.fx :as fx]
            [app.custom.routes :refer [user-routes]]
            [app.lib.routes :refer [core-routes]]
            [app.state :refer [audio-state repl-registry]]))

;; Neutral Baseline DSP Processor Configuration
(def ^:private neutral-processors
  {:distort    {:distortion 0.0 :algorithm :adaa}
   :crusher    {:bits 16.0 :sample-hold 1.0}
   :chorus     {:rate 0.8 :depth 0.4 :wet 0.0}
   :filter     {:q 0.0}
   :delay      {:time "8n." :feedback 0.35 :wet 0.25}
   :reverb     {:room-size 0.75 :wet 0.35 :algorithm :fdn}
   :compressor {:enabled false :threshold -12.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 0.0}})

(defn normalize-routes
  "Normalizes route specifications into a standard map:
  {bus-key {:inserts [fx ...] :target (:bus/master or :out)}}.
  Internal :bus/direct is automatically added with target :out.
  Examples:
    (normalize-routes {:bus/drums []})
    -> {:bus/drums {:inserts [] :target :bus/master}
        :bus/direct {:inserts [] :target :out}}"
  [routes]
  (let [base (if (map? routes)
               (into {}
                     (map (fn [[bus val]]
                            (let [master? (= bus :bus/master)]
                              (cond
                                (= val :out)
                                [bus {:inserts [] :target :out}]

                                (vector? val)
                                (let [last-item   (last val)
                                      target-out? (= last-item :out)
                                      target      (cond
                                                    target-out?               :out
                                                    master?                   :out
                                                    (= last-item :bus/master) :bus/master
                                                    :else                     :bus/master)
                                      inserts     (if (or target-out? (= last-item :bus/master))
                                                    (vec (butlast val))
                                                    val)]
                                  [bus {:inserts inserts :target target}])

                                :else
                                [bus {:inserts [] :target (if master? :out :bus/master)}]))))
                     routes)
               {})]
    (cond-> base
      (not (contains? base :bus/direct)) (assoc :bus/direct {:inserts [] :target :out}))))

(defn- resolve-filter-frequency
  "Resolves the cutoff frequency in Hz: declared topology frequency, active track cutoff, or 18000 Hz."
  [spec]
  (or (:frequency spec)
      (:track-cutoff @audio-state)
      18000.0))

(defn- apply-processor!
  "Applies a declared DSP processor configuration map into the Rust WASM engine."
  [p-type spec]
  (case p-type
    :distort
    (do
      (fx/set-distortion! (or (:distortion spec) 0.0))
      (fx/set-drive-mode! (or (:algorithm spec) (:mode spec) :adaa)))

    :crusher
    (fx/set-bitcrush! (or (:bits spec) 16.0) (or (:sample-hold spec) 1.0))

    :chorus
    (fx/set-chorus! (or (:rate spec) 0.8) (or (:depth spec) 0.4) (or (:wet spec) 0.0))

    :filter
    (let [freq (resolve-filter-frequency spec)
          q    (or (:q spec) 0.0)]
      (fx/set-filter-cutoff! freq)
      (fx/set-filter-q! q))

    :delay
    (fx/set-delay! (or (:time spec) "8n.") (or (:feedback spec) 0.35) (or (:wet spec) 0.0))

    :reverb
    (do
      (fx/set-reverb! (or (:roomSize spec) (:room-size spec) 0.75) (or (:wet spec) 0.0))
      (fx/set-reverb-mode! (or (:algorithm spec) (:mode spec) :fdn)))

    :compressor
    (fx/set-compressor! spec)

    nil))

(defn- apply-bus-sends!
  "Applies declared bus send amounts into the mixer, falling back to default-bus-sends."
  [busses-spec]
  (let [target-sends (merge mixer/default-bus-sends busses-spec)]
    (doseq [[b-key s-map] target-sends]
      (when (contains? s-map :delay)
        (mixer/set-send! b-key :delay (:delay s-map)))
      (when (contains? s-map :reverb)
        (mixer/set-send! b-key :reverb (:reverb s-map))))))

(defn register-routing!
  "Registers or updates a dynamic routing topology in the REPL registry.
  Examples: (register-routing! :dub-matrix {:busses [...] :routes {...}})."
  [routing-key spec]
  (swap! repl-registry assoc-in [:routings routing-key] spec)
  routing-key)

(defn all-routings
  "Returns a merged map of core built-in routings, user custom routings, and REPL routings."
  []
  (merge core-routes user-routes (:routings @repl-registry)))

(defn set-routing!
  "Switches the active DSP routing topology preset and applies its processors to the Rust WASM engine.
  Examples: (set-routing! :dub-echo), (route! :cyber-glitch)."
  [routing-key]
  (when-let [spec (get (all-routings) routing-key)]
    (swap! audio-state assoc :current-routing routing-key)
    (apply-bus-sends! (:busses spec))
    (let [norm (normalize-routes (:routes spec))]
      (doseq [b-key [:bus/drums :bus/bass :bus/space :bus/lead]]
        (let [bypass? (= (:target (get norm b-key)) :out)]
          (mixer/set-bus-bypass-master-fx! b-key bypass?))))
    (let [dsp (merge-with merge neutral-processors (:processors spec))]
      (doseq [[p-type p-spec] dsp]
        (apply-processor! p-type p-spec))
      routing-key)))
