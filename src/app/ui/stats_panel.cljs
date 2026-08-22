(ns app.ui.stats-panel
  "Audio engine statistics, routing graph and active loop information."
  (:require ["tone" :as tone]
            [reagent.core :as r]
            [clojure.string :as str]
            [app.state :refer [state active-tracks tone-ctx last-clock-sample global-key]]
            [app.audio.voices :as voices]))

(defn- bus-badge-class [bus-kw]
  (case bus-kw
    :drums  "bus-drums"
    :bass   "bus-bass"
    :space  "bus-space"
    :direct "bus-direct"
    "bus-master"))

(defn stats-header [{:keys [ctx-state]}]
  [:div.neo-header
   [:div.neo-title
    [:span.neo-prompt "> "]
    [:span "SYSTEM AUDIO STATUS"]]
   [:div.neo-status-badge
    [:span.neo-dot {:class (if (= ctx-state "running") "online" "offline")}]
    [:span (if (= ctx-state "running") "ONLINE" "OFFLINE")]]])

(defn telemetry-grid [{:keys [ctx-state drift tone-now sample-rate lookahead base-lat transport-st bpm-str pos key-str scene-name]}]
  [:div.neo-section
   [:div.neo-section-label "$ engine_telemetry"]
   [:div.neo-grid
    [:div.neo-item [:span.neo-k "Context: "] [:span.neo-v {:class (if (= ctx-state "running") "v-cyan" "v-pink")} (str/upper-case ctx-state)]]
    [:div.neo-item [:span.neo-k "Clock Drift: "] [:span.neo-v.v-cyan drift]]
    [:div.neo-item [:span.neo-k "Hardware Clock: "] [:span.neo-v (if (number? tone-now) (str (.toFixed tone-now 2) "s") "N/A")]]
    [:div.neo-item [:span.neo-k "Sample Rate: "] [:span.neo-v (if sample-rate (str sample-rate " Hz") "N/A")]]
    [:div.neo-item [:span.neo-k "Lookahead: "] [:span.neo-v (if lookahead (str (.toFixed lookahead 0) "ms") "N/A")]]
    [:div.neo-item [:span.neo-k "Base Latency: "] [:span.neo-v (if base-lat (str (.toFixed base-lat 2) "ms") "N/A")]]
    [:div.neo-item [:span.neo-k "Transport: "] [:span.neo-v (str (str/upper-case transport-st) " @ " bpm-str " BPM")]]
    [:div.neo-item [:span.neo-k "Position: "] [:span.neo-v pos]]
    [:div.neo-item [:span.neo-k "Harmonic Key: "] [:span.neo-v.v-cyan key-str]]
    [:div.neo-item [:span.neo-k "3D Scene: "] [:span.neo-v scene-name]]]])

(defn dsp-graph-view []
  [:div.neo-section
   [:div.neo-section-label "$ routing_topology"]
   [:div.neo-routing-box
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-drums "DRUMS"]
     [:div.neo-route-chain
      [:span.neo-node "LP-FILTER (3.4k)"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-bass "BASS"]
     [:div.neo-route-chain
      [:span.neo-node "DISTORTION"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LP-FILTER"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-space "SPACE"]
     [:div.neo-route-chain
      [:span.neo-node "DELAY (8n.)"]
      [:span.neo-arrow ">"]
      [:span.neo-node "REVERB (3.8s)"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-direct "DIRECT"]
     [:div.neo-route-chain
      [:span.neo-node "DIRECT PASSTHROUGH"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]]])

(defn- track-card [kw info]
  (let [muted?   (boolean @(:muted? info))
        solo?    (boolean @(:solo? info))
        pat      @(:pattern info)
        step     (or (:step pat) "16n")
        dur      (or (:dur pat) step)
        inst     (or (:inst pat) kw)
        bus      (voices/instrument-bus inst)
        events   (or (:notes pat) (:hits pat) (:pattern pat))
        len      (if (sequential? events) (count events) 1)]
    [:div.neo-track-box {:class (cond solo? "box-solo" muted? "box-muted" :else "box-live")}
     [:div.track-main-line
      [:div.track-left
       [:span.track-name (name kw)]
       [:span.track-sep "/"]
       [:span.track-inst (name inst)]]
      [:div.track-right
       [:span.neo-bus-tag {:class (bus-badge-class bus)} (name bus)]
       [:span.track-state-badge {:class (cond solo? "badge-solo" muted? "badge-muted" :else "badge-live")}
        (cond solo? "SOLO" muted? "MUTED" :else "LIVE")]]]
     [:div.track-sub-line
      [:span.track-metric [:span.metric-label "grid "] [:span.metric-val step]]
      [:span.metric-dot "•"]
      [:span.track-metric [:span.metric-label "gate "] [:span.metric-val dur]]
      [:span.metric-dot "•"]
      [:span.track-metric [:span.metric-label "steps "] [:span.metric-val len]]]]))

(defn active-loops-view [tracks-map]
  (let [active-count (count tracks-map)]
    [:div.neo-section
     [:div.neo-section-label (str "# ACTIVE LOOPS (" active-count ")")]
     (if (empty? tracks-map)
       [:div.neo-empty "no active loops (use (play!) or (l! :track-name {...}))"]
       [:div.neo-tracks-grid {:tab-index 0
                              :role "region"
                              :aria-label "Active audio loops list"}
        (for [[kw info] tracks-map]
          ^{:key kw}
          [track-card kw info])])]))

(defn stats-footer []
  [:div.neo-footer
   [:span.neo-foot-cmd "> ./tritoncha --stats [1 Hz poll]"]
   [:span.neo-foot-hint "[Press I or click STATS to close]"]])

(defn stats-panel-component []
  (let [tick-atom   (r/atom 0)
        interval-id (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (reset! interval-id (js/setInterval #(swap! tick-atom inc) 1000)))

      :component-will-unmount
      (fn [_]
        (when-let [id @interval-id]
          (js/clearInterval id)))

      :reagent-render
      (fn []
        (let [_            @tick-atom
              ctx          (when @tone-ctx (.-context tone))
              transport    (.-Transport tone)
              raw-ctx      (when ctx (.-rawContext ctx))
              sample-rate  (when raw-ctx (.-sampleRate raw-ctx))
              base-lat     (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (* 1000 (.-baseLatency raw-ctx)))
              lookahead    (when ctx (* 1000 (.-lookAhead ctx)))
              ctx-state    (if ctx (.-state ctx) "uninitialized")
              raw-pos      (when transport (str (.-position transport)))
              pos          (if raw-pos (first (str/split raw-pos #"\.")) "0:0:0")
              bpm-val      (if transport (.. transport -bpm -value) (:bpm @state))
              bpm-str      (if (number? bpm-val) (.toFixed bpm-val 0) (str bpm-val))
              transport-st (if transport (.-state transport) "stopped")
              tone-now     (tone/now)
              sys-now      (/ (.now js/performance) 1000)
              drift        (if-let [{:keys [t-tone t-sys]} @last-clock-sample]
                             (let [dt-tone (- tone-now t-tone)
                                   dt-sys  (- sys-now t-sys)
                                   d-ms    (* 1000 (- dt-tone dt-sys))]
                               (reset! last-clock-sample {:t-tone tone-now :t-sys sys-now})
                               (str (if (pos? d-ms) "+" "") (.toFixed d-ms 3) " ms"))
                             (do
                               (reset! last-clock-sample {:t-tone tone-now :t-sys sys-now})
                               "0.000 ms (calibrated)"))
              tracks-map   @active-tracks
              key-data     @global-key
              key-str      (str (str/upper-case (name (:root key-data :e))) " " (name (:mode key-data :phrygian)))
              scene-name   (name (:current-scene @state :cyber-torus))]
          [:aside.neo-stats-card {:aria-label "System Audio Status"}
           [stats-header {:ctx-state ctx-state}]
           [:div.neo-body
            [telemetry-grid {:ctx-state    ctx-state
                             :drift        drift
                             :tone-now     tone-now
                             :sample-rate  sample-rate
                             :lookahead    lookahead
                             :base-lat     base-lat
                             :transport-st transport-st
                             :bpm-str      bpm-str
                             :pos          pos
                             :key-str      key-str
                             :scene-name   scene-name}]
            [dsp-graph-view]
            [active-loops-view tracks-map]]
           [stats-footer]]))})))
