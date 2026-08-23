(ns app.ui.stats-panel
  "Audio engine statistics panel coordinating telemetry, DSP graph and loops monitors."
  (:require ["tone" :as tone]
            [reagent.core :as r]
            [clojure.string :as str]
            [app.state :refer [audio-state visual-state engine-ctx]]
            [app.ui.stats.telemetry :refer [telemetry-component]]
            [app.ui.stats.routing-graph :refer [routing-graph-component]]
            [app.ui.stats.loops :refer [active-loops-component]]))

(defn- stats-header [{:keys [ctx-state]}]
  [:div.neo-header
   [:div.neo-title
    [:span.neo-prompt "> "]
    [:span "SYSTEM AUDIO STATUS"]]
   [:div.neo-status-badge
    [:span.neo-dot {:class (if (= ctx-state "running") "online" "offline")}]
    [:span (if (= ctx-state "running") "ONLINE" "OFFLINE")]]])

(defn- stats-footer []
  [:div.neo-footer
   [:span.neo-foot-cmd "> ./tritoncha --stats"]
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
              ctx          (when (:tone @engine-ctx) (.-context tone))
              transport    (.-Transport tone)
              raw-ctx      (when ctx (.-rawContext ctx))
              sample-rate  (when raw-ctx (.-sampleRate raw-ctx))
              base-lat     (when (and raw-ctx (number? (.-baseLatency raw-ctx))) (* 1000 (.-baseLatency raw-ctx)))
              lookahead    (when ctx (* 1000 (.-lookAhead ctx)))
              ctx-state    (if ctx (.-state ctx) "uninitialized")
              raw-pos      (when transport (str (.-position transport)))
              pos          (if raw-pos (first (str/split raw-pos #"\.")) "0:0:0")
              bpm-val      (if transport (.. transport -bpm -value) (:bpm @audio-state 168))
              bpm-str      (if (number? bpm-val) (.toFixed bpm-val 0) (str bpm-val))
              transport-st (if transport (.-state transport) "stopped")
              tone-now     (tone/now)
              sys-now      (/ (.now js/performance) 1000)
              drift        (if-let [{:keys [t-tone t-sys]} (:clock-sample @audio-state)]
                             (let [dt-tone (- tone-now t-tone)
                                   dt-sys  (- sys-now t-sys)
                                   d-ms    (* 1000 (- dt-tone dt-sys))]
                               (swap! audio-state assoc :clock-sample {:t-tone tone-now :t-sys sys-now})
                               (str (if (pos? d-ms) "+" "") (.toFixed d-ms 3) " ms"))
                             (do
                               (swap! audio-state assoc :clock-sample {:t-tone tone-now :t-sys sys-now})
                               "0.000 ms (calibrated)"))
              tracks-map   (or (:active-tracks @audio-state) {})
              key-data     (:key @audio-state {:root :e :mode :phrygian :octave 1})
              key-str      (str (str/upper-case (name (:root key-data :e))) " " (name (:mode key-data :phrygian)))
              scene-name   (name (:current-scene @visual-state :cyber-torus))]
          [:aside.neo-stats-card {:aria-label "System Audio Status"}
           [stats-header {:ctx-state ctx-state}]
           [:div.neo-body
            [telemetry-component {:ctx-state    ctx-state
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
            [routing-graph-component]
            [active-loops-component tracks-map]]
           [stats-footer]]))})))
