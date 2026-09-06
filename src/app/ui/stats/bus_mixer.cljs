(ns app.ui.stats.bus-mixer
  "Bus mixer strip with cyberpunk rotary knobs and mute controls for telemetry."
  (:require
   [app.audio.control.mixer :as mixer]
   [app.state :refer [audio-state]]
   [app.utils.math :refer [clamp]]
   [reagent.core :as r]))

(def ^:private bus-specs
  [{:key :bus/drums  :label "DRUMS"  :color "var(--bus-drums-color)"  :tag-class "bus-drums"  :default 0.0}
   {:key :bus/bass   :label "BASS"   :color "var(--bus-bass-color)"   :tag-class "bus-bass"   :default 0.0}
   {:key :bus/lead   :label "LEAD"   :color "var(--bus-lead-color)"   :tag-class "bus-lead"   :default 0.0}
   {:key :bus/space  :label "SPACE"  :color "var(--bus-space-color)"  :tag-class "bus-space"  :default 0.0}
   {:key :bus/direct :label "DIRECT" :color "var(--bus-direct-color)" :tag-class "bus-direct" :default 0.0}])

(def ^:private min-db -24.0)
(def ^:private max-db 6.0)
(def ^:private arc-radius 18.0)
(def ^:private arc-circumference (* 2.0 (.-PI js/Math) arc-radius)) ;; ~113.1
(def ^:private arc-total-len (* 0.75 arc-circumference))              ;; ~84.82

(defn- db->norm
  "Maps decibel value to normalized 0.0 to 1.0 rotary position.
  0.0 dB (unity gain) is centered at 0.5 (12 o'clock)."
  [db]
  (let [val (clamp db min-db max-db)]
    (if (<= val 0.0)
      (* 0.5 (/ (- val min-db) (- min-db)))
      (+ 0.5 (* 0.5 (/ val max-db))))))

(defn- format-db [db]
  (let [rounded (/ (.round js/Math (* db 10.0)) 10.0)]
    (if (pos? rounded)
      (str "+" (.toFixed rounded 1) " dB")
      (str (.toFixed rounded 1) " dB"))))

(defn- rotary-knob
  "Interactive cyberpunk rotary knob for bus volume adjustment.
  Supports pointer vertical drag, mouse wheel and double-click to reset."
  [_props]
  (let [drag-state (r/atom {:dragging? false :start-y 0 :start-val 0.0})]
    (fn [{:keys [value default-val color muted? on-change]}]
      (let [val-clamped   (clamp value min-db max-db)
            norm          (db->norm val-clamped)
            dash-active   (* norm arc-total-len)
            dash-arr      (str (.toFixed dash-active 2) " " (.toFixed arc-circumference 2))
            angle-deg     (+ 135.0 (* norm 270.0))
            angle-rad     (* angle-deg (/ (.-PI js/Math) 180.0))
            cx            25.0
            cy            25.0
            r-inner       8.0
            r-outer       14.0
            p1-x          (+ cx (* r-inner (.cos js/Math angle-rad)))
            p1-y          (+ cy (* r-inner (.sin js/Math angle-rad)))
            p2-x          (+ cx (* r-outer (.cos js/Math angle-rad)))
            p2-y          (+ cy (* r-outer (.sin js/Math angle-rad)))]
        [:div.neo-knob-container
         {:on-pointer-down
          (fn [e]
            (when (zero? (.-button e))
              (let [target (.-currentTarget e)
                    pid    (.-pointerId e)]
                (when (and target (.-setPointerCapture target))
                  (.setPointerCapture target pid))
                (reset! drag-state {:dragging? true
                                    :start-y   (.-clientY e)
                                    :start-val val-clamped}))))
          :on-pointer-move
          (fn [e]
            (when (:dragging? @drag-state)
              (let [dy       (- (:start-y @drag-state) (.-clientY e))
                    step-db  0.22
                    raw-next (+ (:start-val @drag-state) (* dy step-db))
                    next-val (clamp (/ (.round js/Math (* raw-next 10.0)) 10.0) min-db max-db)]
                (on-change next-val))))
          :on-pointer-up
          (fn [e]
            (when (:dragging? @drag-state)
              (let [target (.-currentTarget e)
                    pid    (.-pointerId e)]
                (when (and target (.-releasePointerCapture target))
                  (try (.releasePointerCapture target pid) (catch :default _ nil)))
                (swap! drag-state assoc :dragging? false))))
          :on-pointer-cancel
          (fn [_]
            (swap! drag-state assoc :dragging? false))
          :on-wheel
          (fn [e]
            (.preventDefault e)
            (let [dy       (.-deltaY e)
                  delta    (if (pos? dy) -0.5 0.5)
                  next-val (clamp (/ (.round js/Math (* (+ val-clamped delta) 10.0)) 10.0) min-db max-db)]
              (on-change next-val)))
          :on-double-click
          (fn [e]
            (.stopPropagation e)
            (on-change default-val))
          :title "Drag up or down, or use wheel to adjust volume. Double click to reset."
          :role  "slider"
          :aria-valuenow val-clamped
          :aria-valuemin min-db
          :aria-valuemax max-db}
         [:svg.neo-knob-svg {:viewBox "0 0 50 50"}
          ;; Background track arc
          [:circle
           {:cx cx :cy cy :r arc-radius
            :fill "none"
            :stroke "rgba(255, 255, 255, 0.10)"
            :stroke-width "3.5"
            :stroke-linecap "round"
            :stroke-dasharray (str (.toFixed arc-total-len 2) " " (.toFixed arc-circumference 2))
            :transform "rotate(135 25 25)"}]
          ;; Active value arc
          [:circle
           {:cx cx :cy cy :r arc-radius
            :fill "none"
            :stroke (if muted? "var(--text-tertiary)" color)
            :stroke-width "3.5"
            :stroke-linecap "round"
            :stroke-dasharray dash-arr
            :transform "rotate(135 25 25)"
            :style {:filter (if muted? "none" (str "drop-shadow(0 0 3px " color ")"))}}]
          ;; Central dial disc
          [:circle
           {:cx cx :cy cy :r "13"
            :fill "var(--bg-surface-elevated, #161624)"
            :stroke (if muted? "var(--border-subtle)" "rgba(255,255,255,0.22)")
            :stroke-width "1.5"}]
          ;; Needle / pointer
          [:line
           {:x1 p1-x :y1 p1-y :x2 p2-x :y2 p2-y
            :stroke (if muted? "var(--text-tertiary)" color)
            :stroke-width "2.5"
            :stroke-linecap "round"}]]]))))

(defn- bus-strip [{:keys [key label color tag-class default]}]
  (let [levels  (:bus-levels @audio-state)
        mutes   (:bus-mutes @audio-state)
        val     (get levels key default)
        muted?  (get mutes key false)]
    [:div.neo-bus-strip {:class (when muted? "muted")}
     [:span.neo-bus-tag.mini {:class tag-class} label]
     [rotary-knob {:value       val
                   :default-val default
                   :color       color
                   :muted?      muted?
                   :on-change   (fn [new-db]
                                  (mixer/set-volume! key new-db))}]
     [:div.neo-bus-readout
      (if muted?
        [:span.neo-v.v-pink "MUTE"]
        [:span.neo-v {:class (if (pos? val) "v-pink" "v-cyan")}
         (format-db val)])]
     [:button.neo-mute-btn
      {:class    (when muted? "active")
       :title    (if muted? (str "Unmute " label) (str "Mute " label))
       :on-click (fn [e]
                   (.stopPropagation e)
                   (mixer/toggle-bus! key))}
      "M"]]))

(defn bus-mixer-component
  "Renders the 5-bus mixer strip with interactive rotary knobs and mute toggles."
  []
  [:div.neo-section
   [:div.neo-section-label "$ bus_matrix [LEVELS + MUTE]"]
   [:div.neo-bus-mixer-strip
    (for [spec bus-specs]
      ^{:key (str (:key spec))}
      [bus-strip spec])]])
