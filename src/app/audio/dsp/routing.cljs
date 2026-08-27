(ns app.audio.dsp.routing
  "Declarative DSP routing graph compiler and Tone.js FX node factory."
  (:require
   ["tone" :as tone]
   [app.config :as cfg]
   [app.custom.routes :refer [user-routes]]
   [app.lib.routes :refer [core-routes]]
   [app.state :refer [repl-registry]]))

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

(defn- set-wet! [^js node wet]
  (when (and node wet)
    (set! (.. node -wet -value) wet))
  node)

(defn create-dsp-node
  "Instantiates a Tone.js audio node from a declarative specification map.
  Examples: (create-dsp-node {:type :volume :volume 0}), (create-dsp-node {:type :filter :frequency 4000})."
  [spec]
  (let [node-type (or (:type spec) (:node spec))]
    (case node-type
      :gain       (tone/Gain. (or (:gain spec) 1.0))
      :volume     (tone/Volume. (or (:volume spec) 0.0))
      :filter     (let [f (tone/Filter. (or (:frequency spec) cfg/default-filter-frequency)
                                        (or (:filter-type spec) cfg/default-filter-type)
                                        (or (:rolloff spec) cfg/default-filter-rolloff))]
                    (try
                      (when-let [^js bf (or (aget f "_filter") (aget f "input"))]
                        (set! (.-channelCountMode bf) "explicit")
                        (set! (.-channelCount bf) 2))
                      (catch js/Object _))
                    f)
      :delay      (set-wet! (tone/FeedbackDelay. (or (:time spec) cfg/default-delay-time)
                                                 (or (:feedback spec) cfg/default-delay-feedback))
                            (:wet spec))
      :reverb     (set-wet! (tone/Freeverb. #js {:roomSize (or (:roomSize spec) cfg/default-reverb-room-size)
                                                 :dampening (or (:dampening spec) cfg/default-reverb-dampening)})
                            (:wet spec))
      :distortion (set-wet! (tone/Distortion. (or (:distortion spec) cfg/default-distortion))
                            (:wet spec))
      :limiter    (tone/Limiter. (or (:threshold spec) cfg/default-limiter-threshold))
      :bitcrusher (set-wet! (tone/BitCrusher. (or (:bits spec) cfg/default-bitcrusher-bits))
                            (:wet spec))
      nil)))

(defn build-audio-graph!
  "Compiles the active DSP routing specification into a live Tone.js audio graph."
  ([] (build-audio-graph! :default))
  ([routing-key]
   (let [routings-map (all-routings)
         spec         (get routings-map routing-key (:default routings-map))
         destination  (if (fn? (.-getDestination tone))
                        (tone/getDestination)
                        (or (.-destination tone) (.-Destination tone)))
         bus-nodes    (update-vals (:busses spec) create-dsp-node)
         proc-nodes   (update-vals (:processors spec) create-dsp-node)
         all-nodes    (merge bus-nodes proc-nodes {:destination destination})]

     (doseq [chain (:routes spec)
             [from-k to-k] (partition 2 1 chain)]
       (when-let [^js from-node (get all-nodes from-k)]
         (when-let [^js to-node (get all-nodes to-k)]
           (.connect from-node to-node))))

     (when-let [^js limiter (get proc-nodes :limiter)]
       (if (fn? (.-toDestination limiter))
         (.toDestination limiter)
         (when destination (.connect limiter destination))))

     (merge
      proc-nodes
      bus-nodes
      {:filter  (get proc-nodes :master-filter)
       :distort (get proc-nodes :distort)
       :delay   (get proc-nodes :delay)
       :reverb  (get proc-nodes :reverb)
       :limiter (get proc-nodes :limiter)
       :busses  (update-vals bus-nodes (fn [node] {:input node :output node :node node}))}))))
