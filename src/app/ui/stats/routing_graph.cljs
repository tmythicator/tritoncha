(ns app.ui.stats.routing-graph
  "Audio routing topology visualization subcomponent."
  (:require
   [app.audio.dsp.busses :as busses]
   [app.audio.dsp.fx :as fx]
   [app.audio.dsp.routing :as routing]
   [app.lib.routes :refer [default-graph]]
   [app.state :refer [audio-state engine-ctx]]
   [clojure.string :as str]))

(def ^:private canonical-bus-order
  [:bus/drums :bus/bass :bus/lead :bus/space :bus/direct :bus/master])

(defn- bus-badge-info [bus-key]
  (case (busses/normalize-bus-key bus-key)
    :bus/drums  {:label "DRUMS"  :class "bus-drums"}
    :bus/bass   {:label "BASS"   :class "bus-bass"}
    :bus/lead   {:label "LEAD"   :class "bus-lead"}
    :bus/space  {:label "SPACE"  :class "bus-space"}
    :bus/direct {:label "CLICK"  :class "bus-direct"}
    :bus/master {:label "MASTER" :class "bus-master"}
    (let [clean (-> (name bus-key) (str/replace #"-bus$" "") str/upper-case)]
      {:label clean :class "bus-direct"})))

(defn- sort-busses [b-keys]
  (let [idx-map (into {} (map-indexed (fn [i k] [k i]) canonical-bus-order))]
    (sort-by #(get idx-map % 99) b-keys)))

(defn- format-route-title [rk spec]
  (or (:title spec)
      (-> (name rk) (str/replace #"-" " ") str/upper-case)))

(defn- prioritize-default [keys-coll]
  (if (some #{:default} keys-coll)
    (cons :default (remove #{:default} keys-coll))
    keys-coll))

(defn- format-node-label [node-key processors live-ctx]
  (case node-key
    :out         "OUT"
    :bus/master  "MASTER"
    :thru        "THRU"
    :direct      "THRU"
    (let [{:keys [type frequency cutoff ratio enabled time]} (get processors node-key)]
      (case type
        :filter
        (let [hz (or (when-let [obj (get live-ctx node-key)]
                       (when (.-frequency ^js obj)
                         (js/Math.round (.. obj -frequency -value))))
                     cutoff
                     (:cutoff (fx/get-filter-state))
                     frequency
                     12000)]
          (str "LP-FILTER (" (Math/round hz) " Hz)"))

        :compressor
        (let [st (fx/get-compressor-state)
              en (if (some? enabled) enabled (:enabled st))
              r  (or ratio (:ratio st) 4.0)]
          (if en (str "COMP (" r ":1)") "COMP [OFF]"))

        :delay      (str "DELAY (" (or time "8n.") ")")
        :chorus     "CHORUS"
        :reverb     "REVERB"
        :freeverb   "FREEVERB"
        :distortion "DISTORTION"
        :bitcrusher "BITCRUSHER"
        :limiter    "LIMITER"
        :volume     "VOLUME"
        (-> (name node-key) (str/replace #"-" " ") str/upper-case)))))

(defn- display-nodes [route-entry]
  (let [{:keys [inserts target]} route-entry]
    (if (empty? inserts)
      [:thru (or target :bus/master)]
      (conj (vec inserts) (or target :bus/master)))))

(defn- node-view [node-key processors live-ctx terminal?]
  (let [lbl (format-node-label node-key processors live-ctx)]
    (if terminal?
      [:span {:class (if (= node-key :bus/master) "neo-master-dest" "neo-dest")} lbl]
      [:<>
       [:span.neo-node lbl]
       [:span.neo-arrow " > "]])))

(defn- route-row [bus-key route-entry processors live-ctx]
  (let [{:keys [label class]} (bus-badge-info bus-key)
        master?               (= bus-key :bus/master)
        chain                 (display-nodes route-entry)]
    [:div.neo-route-row {:class (when master? "master-row")}
     [:span.neo-bus-tag {:class class} label]
     [:div.neo-route-chain
      (map-indexed
       (fn [idx k]
         (let [terminal? (or (= k :out)
                             (and (not master?) (= k :bus/master)))]
           ^{:key (str "n-" idx)}
           [node-view k processors live-ctx terminal?]))
       chain)]]))

(defn- route-tabs [cur-route-k routings]
  (let [route-keys (prioritize-default (keys routings))]
    [:div.neo-route-selector
     (for [rk route-keys
           :let [active?   (= rk cur-route-k)
                 spec      (get routings rk)
                 title-str (format-route-title rk spec)]]
       ^{:key (str rk)}
       [:button.neo-route-tab-btn
        {:class    (when active? "active")
         :title    (str "Activate " (name rk) " routing topology")
         :on-click #(routing/set-routing! rk)}
        title-str])]))

(defn routing-graph-component []
  (let [routings    (routing/all-routings)
        cur-route-k (:current-routing @audio-state :default)
        active-spec (or (get routings cur-route-k)
                        (get routings :default)
                        default-graph)
        routes-map  (routing/normalize-routes (:routes active-spec))
        bus-order   (sort-busses (or (seq (keys (:busses active-spec))) canonical-bus-order))
        processors  (:processors active-spec)
        live-ctx    (:tone @engine-ctx)]
    [:div.neo-section
     [:div.neo-section-header
      [:div.neo-section-label (str "$ routing_topology [" (str/upper-case (name cur-route-k)) "]")]
      [route-tabs cur-route-k routings]]
     [:div.neo-routing-box
      (for [bk bus-order]
        ^{:key (str bk)}
        [route-row bk (get routes-map bk) processors live-ctx])]]))
