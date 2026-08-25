(ns app.ui.stats.routing-graph
  "Audio routing topology visualization subcomponent."
  (:require
   [app.audio.routing :as routing]
   [app.lib.routes :refer [default-graph]]
   [app.state :refer [audio-state engine-ctx]]
   [clojure.string :as str]))

(defn- bus-badge-info [bus-key]
  (case (keyword bus-key)
    (:drum-bus :drums)   {:label "DRUMS"  :class "bus-drums"}
    (:bass-bus :bass)    {:label "BASS"   :class "bus-bass"}
    (:space-bus :space)  {:label "SPACE"  :class "bus-space"}
    (:direct-bus :direct) {:label "DIRECT" :class "bus-direct"}
    (let [clean (-> (name bus-key) (str/replace #"-bus$" "") str/upper-case)]
      {:label clean :class "bus-direct"})))

(defn- format-node-label [node-key processors live-ctx]
  (if (= node-key :destination)
    "OUT"
    (let [spec     (get processors node-key)
          node-obj (get live-ctx node-key)
          p-type   (:type spec)]
      (case p-type
        :filter
        (let [freq (if (and node-obj (.-frequency ^js node-obj))
                     (js/Math.round (.. node-obj -frequency -value))
                     (:frequency spec 3400))]
          (str "LP-FILTER (" freq " Hz)"))

        :delay
        (let [t (or (:time spec) "8n.")]
          (str "DELAY (" t ")"))

        :reverb
        "REVERB"

        :freeverb
        "FREEVERB"

        :distortion
        "DISTORTION"

        :limiter
        "LIMITER"

        :volume
        "VOLUME"

        (-> (name node-key) str/upper-case (str/replace #"-" " "))))))

(defn- resolve-bus-chain [bus-key routes-list max-depth]
  (let [adj (reduce (fn [acc chain]
                      (reduce (fn [m [src dst]] (assoc m src dst))
                              acc
                              (partition 2 1 chain)))
                    {}
                    routes-list)
        step (fn step [curr visited depth]
               (cond
                 (or (nil? curr) (= curr :destination) (visited curr) (>= depth max-depth))
                 (when curr [curr])

                 :else
                 (cons curr (step (get adj curr) (conj visited curr) (inc depth)))))]
    (vec (step bus-key #{} 0))))

(defn routing-graph-component []
  (let [routings     (routing/all-routings)
        cur-route-k  (:current-routing @audio-state :default)
        active-spec  (or (get routings cur-route-k)
                         (get routings :default)
                         default-graph)
        busses       (:busses active-spec)
        processors   (:processors active-spec)
        routes       (:routes active-spec)
        live-ctx     (:tone @engine-ctx)
        bus-keys     (keys busses)]
    [:div.neo-section
     [:div.neo-section-label (str "$ routing_topology [" (str/upper-case (name cur-route-k)) "]")]
     [:div.neo-routing-box
      (for [bk bus-keys
            :let [{:keys [label class]} (bus-badge-info bk)
                  chain                 (resolve-bus-chain bk routes 12)
                  proc-nodes            (rest chain)]]
        ^{:key (str bk)}
        [:div.neo-route-row
         [:span.neo-bus-tag {:class class} label]
         (if (empty? proc-nodes)
           [:div.neo-route-chain
            [:span.neo-node "DIRECT PASSTHROUGH"]
            [:span.neo-arrow ">"]
            [:span.neo-dest "OUT"]]
           (into [:div.neo-route-chain]
                 (mapcat (fn [idx node-key]
                           (let [lbl (format-node-label node-key processors live-ctx)
                                 dest? (= node-key :destination)]
                             (if dest?
                               [[:span.neo-dest {:key (str "n-" idx)} lbl]]
                               [[:span.neo-node {:key (str "n-" idx)} lbl]
                                [:span.neo-arrow {:key (str "a-" idx)} ">"]])))
                         (range)
                         proc-nodes)))])]]))
