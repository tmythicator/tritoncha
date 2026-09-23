(ns app.ui.common
  "Shared reusable UI primitives, modal shells, badges, bus tags, and controls."
  (:require
   [app.audio.dsp.busses :as busses]
   [clojure.string :as str]))

;; Modal Shell Components

(defn modal-close-btn
  "Render modal close button.
  Examples: [modal-close-btn {:on-click f :aria-label \"Close modal\"}]."
  [{:keys [on-click aria-label]
    :or   {aria-label "Close modal"}}]
  [:button.neo-btn-close
   {:on-click   on-click
    :aria-label aria-label}
   "[X]"])

(defn modal-header
  "Render standardized cyber-terminal modal header.
  Supports title prompt, optional right-aligned controls, and close button.
  Examples: [modal-header {:title \"SYSTEM STATUS\" :on-close f}]."
  [{:keys [title on-close close-label right-content right-class]
    :or   {right-class "neo-header-right"}}]
  [:div.neo-header
   [:div.neo-title
    [:span.neo-prompt "> "]
    [:span title]]
   (cond
     right-content
     [:div {:class right-class}
      right-content
      (when on-close
        (modal-close-btn {:on-click on-close :aria-label close-label}))]

     on-close
     (modal-close-btn {:on-click on-close :aria-label close-label}))])

(defn modal-footer
  "Render standardized cyber-terminal modal footer with command prompt and shortcut hint.
  Examples: [modal-footer {:cmd \"> ./tritoncha --stats\" :hint \"[Press I to close]\"}]."
  [{:keys [cmd hint class cmd-class hint-class]}]
  [:div.neo-footer {:class class}
   [:span.neo-foot-cmd {:class cmd-class} cmd]
   [:span.neo-foot-hint {:class hint-class} hint]])

;; Badges and Status Indicators

(defn status-badge
  "Render online or offline status pill badge with pulsing dot.
  Examples: [status-badge {:online? true}]."
  [{:keys [online?]}]
  [:div.neo-status-badge
   [:span.neo-dot {:class (if online? "online" "offline")}]
   [:span (if online? "ONLINE" "OFFLINE")]])

(defn badge
  "Render cyberpunk terminal badge.
  Examples: [badge \"174 BPM\"] or [badge {:variant :cyan} \"D MINOR\"]."
  ([text]
   [:span.neo-badge text])
  ([{:keys [variant class]} text]
   (let [tag (case variant
               :cyan :span.neo-badge.badge-cyan
               :pink :span.neo-badge.badge-pink
               :span.neo-badge)]
     (if (seq class)
       [tag {:class class} text]
       [tag text]))))

(defn bus-badge-class
  "Return CSS class for bus badge styling based on normalized bus keyword.
  Examples: (bus-badge-class :bus/bass) -> \"bus-bass\"."
  [bus-key]
  (case (busses/normalize-bus-key bus-key)
    :bus/drums  "bus-drums"
    :bus/bass   "bus-bass"
    :bus/lead   "bus-lead"
    :bus/space  "bus-space"
    :bus/direct "bus-direct"
    :bus/master "bus-master"
    (let [_clean (-> (name bus-key) (str/replace #"-bus$" "") str/upper-case)]
      "bus-direct")))

(defn bus-label
  "Format bus keyword to short uppercase display title.
  Examples: (bus-label :bus/direct) -> \"CLICK\"."
  [bus-key]
  (case (busses/normalize-bus-key bus-key)
    :bus/drums  "DRUMS"
    :bus/bass   "BASS"
    :bus/lead   "LEAD"
    :bus/space  "SPACE"
    :bus/direct "CLICK"
    :bus/master "MASTER"
    (-> (name bus-key) (str/replace #"-bus$" "") str/upper-case)))

(defn bus-tag
  "Render styled bus badge tag with color coding.
  Examples: [bus-tag :bus/drums] or [bus-tag {:bus :bus/drums :mini? true}]."
  [opts-or-bus]
  (let [{:keys [bus label mini? class]} (if (map? opts-or-bus) opts-or-bus {:bus opts-or-bus})
        b-class                         (bus-badge-class bus)
        classes                         (cond-> (str "neo-bus-tag " b-class)
                                          mini?       (str " mini")
                                          (seq class) (str " " class))]
    [:span {:class classes}
     (or label (bus-label bus))]))

;; Generic Interactive Controls

(defn param-slider
  "Render slider input with numeric readout.
  Examples: [param-slider {:label \"Cutoff\" :val-str \"2400 Hz\" :min 40 :max 14000 :step 50 :value 2400}]."
  [{:keys [label val-str min max step value on-down on-change]}]
  [:div.inst-param-slider
   [:div.inst-slider-header
    [:span.inst-slider-label label]
    [:span.neo-v.v-cyan val-str]]
   [:input {:type            "range"
            :min             min
            :max             max
            :step            step
            :value           value
            :on-pointer-down on-down
            :on-change       on-change}]])

(defn pill-selector
  "Render compact pill button selector.
  Examples: [pill-selector {:label \"WAVEFORM\" :items [:saw :pulse] :current :saw :on-select f}]."
  [{:keys [label items current on-select]}]
  [:div.inst-pill-selector
   (when label
     [:span.inst-pill-label label])
   [:div.inst-pill-group
    (for [item items]
      (let [active? (= current item)]
        ^{:key (str item)}
        [:button.inst-pill-btn
         {:class    (when active? "active")
          :on-click #(on-select item)}
         (str/upper-case (if (keyword? item) (name item) (str item)))]))]])
