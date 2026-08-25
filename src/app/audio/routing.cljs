(ns app.audio.routing
  "Declarative WebAudio graph compiler, node factory, and bus routing."
  (:require ["tone" :as tone]
            [app.utils :refer [enforce-stereo-mode!]]
            [app.state :refer [repl-registry]]
            [app.lib.routes :refer [default-graph core-routes]]
            [app.custom.routes :refer [user-routes]]))

;; Routing Registry (Built-in + Custom User + REPL Live Routing Graphs)

(defn register-routing!
  "Registers or updates a dynamic audio routing graph topology in the REPL registry.
  Examples: (register-routing! :shimmer-ambient {...})."
  [graph-key spec]
  (swap! repl-registry assoc-in [:routes graph-key] spec)
  graph-key)

(defn all-routings
  "Returns a merged map of default core, user custom and dynamic REPL routing graphs."
  []
  (merge core-routes user-routes (:routes @repl-registry)))

;; Audio Node Factory + Graph Compiler

(defn create-node
  "Instantiates a Tone.js audio processor node from a specification map."
  [{:keys [type volume decay wet time feedback frequency filter-type distortion threshold]}]
  (let [node (case type
               :volume     (tone/Volume. (or volume 0))
               :reverb     (tone/Reverb. #js {:decay (or decay 3.8) :wet (or wet 0.32)})
               :delay      (tone/FeedbackDelay. (or time "8n.") (or feedback 0.38))
               :filter     (tone/Filter. (or frequency 3400) (or filter-type "lowpass"))
               :distortion (tone/Distortion. (or distortion 0.35))
               :limiter    (tone/Limiter. (or threshold -2.0))
               (tone/Volume. 0))]
    (enforce-stereo-mode! node)
    node))

(defn build-graph!
  "Compiles a declarative routing graph specification map into active Tone.js nodes and connects the bus graph."
  ([] (build-graph! default-graph))
  ([graph-spec]
   (let [spec        (if (keyword? graph-spec) (get (all-routings) graph-spec default-graph) graph-spec)
         {:keys [busses processors routes]} spec
         all-specs   (merge busses processors)
         node-map    (into {} (for [[k n-spec] all-specs] [k (create-node n-spec)]))
         aliases     {:filter (:master-filter node-map)}]

     (doseq [chain routes]
       (let [resolved (for [target chain]
                        (if (= target :destination)
                          :destination
                          (get node-map target)))]
         (reduce (fn [src dst]
                   (cond
                     (= dst :destination)
                     (when src (.toDestination ^js src))

                     (and src dst)
                     (.connect ^js src ^js dst))
                   dst)
                 resolved)))

     (merge node-map aliases))))

(defn load-routing!
  "Switches and re-compiles the active DSP routing graph."
  [graph-key]
  (let [k (if (keyword? graph-key) graph-key :custom)]
    (swap! audio-state assoc :current-routing k)
    (build-graph! graph-key)))
