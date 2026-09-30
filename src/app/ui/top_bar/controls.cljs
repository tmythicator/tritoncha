(ns app.ui.top-bar.controls
  "Top bar engine playback, track scrubbing, tempo slider, musical key/scale selectors, preset reset, and scene controls."
  (:require
   [app.audio.control.session :as session]
   [app.audio.control.tracker :as tracker :refer [next-jam! prev-jam!]]
   [app.audio.control.transport :as transport]
   [app.config :as cfg]
   [app.state :refer [audio-state visual-state]]
   [clojure.string :as str]))

(def ^:private scale-options
  [[:phrygian          "Phrygian"]
   [:dorian            "Dorian"]
   [:minor             "Minor"]
   [:harmonic-minor    "Harmonic Min"]
   [:blues             "Blues"]
   [:major             "Major"]
   [:lydian            "Lydian"]
   [:mixolydian        "Mixolydian"]
   [:phrygian-dominant "Phryg Dom"]])

(def ^:private chromatic-keys
  ["c" "c#" "d" "d#" "e" "f" "f#" "g" "g#" "a" "a#" "b"])

(defn- icon-chevron-left
  "Render crisp SVG left navigation chevron arrow."
  []
  [:svg.player-nav-icon {:viewBox "0 0 16 16" :aria-hidden "true"}
   [:path {:d "M10 13L5 8l5-5" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}]])

(defn- icon-chevron-right
  "Render crisp SVG right navigation chevron arrow."
  []
  [:svg.player-nav-icon {:viewBox "0 0 16 16" :aria-hidden "true"}
   [:path {:d "M6 3l5 5-5 5" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}]])

(defn- track-orig-bpm
  "Retrieve original declared tempo for the active track preset.
  Examples: (track-orig-bpm :orbital-roller) -> 160."
  [cur-jam]
  (let [tracks-map (tracker/all-tracks)
        t          (get tracks-map cur-jam)]
    (or (:bpm t) cfg/default-bpm)))

(defn- track-orig-scale
  "Retrieve original declared [root mode octave] vector for the active track preset.
  Examples: (track-orig-scale :orbital-roller) -> [:e :minor 1]."
  [cur-jam]
  (let [tracks-map (tracker/all-tracks)
        t          (get tracks-map cur-jam)
        scale      (:scale t)]
    (if (vector? scale)
      scale
      [(:root cfg/default-key :e) (:mode cfg/default-key :phrygian) (:octave cfg/default-key 1)])))

(defn- reset-to-original!
  "Restore playback tempo and harmonize all melodic loops back to original track preset values.
  Examples: (reset-to-original! 160 :e :minor 1)."
  [orig-bpm orig-root orig-mode orig-oct]
  (transport/set-bpm! orig-bpm)
  (session/modulate-all! orig-root orig-mode (or orig-oct 1)))

(defn- track-deck
  "Render interactive track scrubber deck with index counter, chevron steppers, and wheel scrolling.
  Examples: [track-deck opts]."
  [{:keys [cur-idx total-count cur-title toggle-track-browser! cycle-jam!]}]
  (let [idx-str (let [n (inc cur-idx)] (if (< n 10) (str "0" n) (str n)))
        tot-str (if (< total-count 10) (str "0" total-count) (str total-count))]
    [:div.player-track-deck
     {:on-wheel (fn [e]
                  (.preventDefault e)
                  (if (pos? (.-deltaY e))
                    (next-jam!)
                    (prev-jam!)))
      :title    (str "Track " idx-str "/" tot-str ": " cur-title
                     "\nScroll wheel or arrows to cycle tracks\nClick badge for presets library")}
     [:button.player-btn-nav
      {:on-click   prev-jam!
       :aria-label "Previous track preset"
       :title      "Previous track preset (or scroll wheel up / Left Arrow)"}
      [icon-chevron-left]]

     [:div.player-track-center
      [:span.track-deck-idx (str idx-str "/" tot-str)]
      [:button.player-track-badge
       {:on-click   (or toggle-track-browser! cycle-jam!)
        :aria-label "Open track presets library"
        :title      "Open track presets library (J)"}
       [:span.track-deck-name cur-title]
       [:span.track-deck-arrow "▾"]]]

     [:button.player-btn-nav
      {:on-click   next-jam!
       :aria-label "Next track preset"
       :title      "Next track preset (or scroll wheel down / Right Arrow)"}
      [icon-chevron-right]]]))

(defn master-deck-component
  "Render primary master transport, track scrubber deck, and 3D scene switcher.
  Examples: [master-deck-component props]."
  [{:keys [toggle-play! cycle-jam! toggle-track-browser! cycle-scene!]}]
  (let [{:keys [active? current-jam]} @audio-state
        {:keys [current-scene]}       @visual-state
        tks         (tracker/track-keys)
        cur-jam     (or current-jam (tracker/default-track-key))
        cur-idx     (let [i (.indexOf tks cur-jam)] (if (neg? i) 0 i))
        total-count (count tks)
        all-t       (tracker/all-tracks)
        cur-t       (get all-t cur-jam)
        cur-title   (or (:name cur-t) (-> cur-jam name str/upper-case))
        scene-name  (-> (or current-scene :cyber-torus) name str/upper-case)]
    [:div.hud-master-deck
     [:button.player-play-btn
      {:on-click   toggle-play!
       :class      (when active? "active")
       :aria-label (if active? "Stop Audio Engine" "Start Audio Engine")}
      (if active? "■ STOP ENGINE" "▶ PLAY ENGINE")]

     (track-deck {:cur-idx               cur-idx
                  :total-count           total-count
                  :cur-title             cur-title
                  :toggle-track-browser! toggle-track-browser!
                  :cycle-jam!            cycle-jam!})

     [:button.neo-btn-stats.btn-swap-scene
      {:on-click   cycle-scene!
       :on-wheel   (fn [e]
                     (.preventDefault e)
                     (cycle-scene!))
       :aria-label "Swap 3D visual scene"
       :title      (str "3D Scene: " scene-name "\nClick or scroll wheel to swap scene (G)")}
      (str "⟳ SCENE: " scene-name)]]))

(defn tweaks-deck-component
  "Render sound design, tempo slider with wheel support, key/scale selects, and preset reset button.
  Examples: [tweaks-deck-component props]."
  [_props]
  (let [{:keys [bpm key current-jam]} @audio-state
        root-kw   (:root key :e)
        mode-kw   (:mode key :phrygian)
        oct-num   (:octave key 1)
        orig-bpm  (track-orig-bpm current-jam)
        [orig-root orig-mode orig-oct] (track-orig-scale current-jam)
        orig-oct  (or orig-oct 1)
        modified? (or (not= bpm orig-bpm)
                      (not= root-kw orig-root)
                      (not= mode-kw orig-mode)
                      (not= oct-num orig-oct))]
    [:div.top-bar-tools
     ;; 1. Direct Tempo Slider + Numeric Readout with wheel support
     [:div.tool-tempo-box
      {:on-wheel (fn [e]
                   (.preventDefault e)
                   (let [delta (if (pos? (.-deltaY e)) -1 1)]
                     (transport/set-bpm! (min 240 (max cfg/min-bpm (+ bpm delta))))))
       :title    (str "Tempo: " bpm " BPM (drag slider, scroll wheel, double-click to reset to " orig-bpm " BPM)")}
      [:span.tempo-readout
       {:on-double-click #(transport/set-bpm! orig-bpm)}
       (str bpm " BPM")]
      [:input.neo-tempo-slider
       {:type            "range"
        :min             cfg/min-bpm
        :max             240
        :step            1
        :value           bpm
        :on-double-click #(transport/set-bpm! orig-bpm)
        :aria-label      "Adjust tempo BPM"
        :on-change       #(let [v (js/parseInt (.. % -target -value) 10)]
                            (when-not (js/isNaN v)
                              (transport/set-bpm! v)))}]]

     ;; 2. Musical Root Key Selector
     [:select.neo-select.neo-select-key
      {:value       (name root-kw)
       :aria-label  "Musical root key"
       :title       "Select musical root key"
       :on-change   #(session/modulate-all! (keyword (.. % -target -value)) mode-kw oct-num)}
      (for [r chromatic-keys]
        ^{:key r}
        [:option {:value r} (str/upper-case r)])]

     ;; 3. Musical Scale / Mode Selector
     [:select.neo-select.neo-select-scale
      {:value       (name mode-kw)
       :aria-label  "Musical scale mode"
       :title       "Select musical scale mode"
       :on-change   #(session/modulate-all! root-kw (keyword (.. % -target -value)) oct-num)}
      (for [[sk label] scale-options]
        ^{:key (str sk)}
        [:option {:value (name sk)} label])]

     ;; 4. Reset to Original Preset Values
     [:button.neo-btn-stats.btn-reset
      {:class      (when modified? "active-modified")
       :on-click   #(reset-to-original! orig-bpm orig-root orig-mode orig-oct)
       :aria-label "Reset BPM, key, and scale to track defaults"
       :title      (str "Reset to track defaults: " orig-bpm " BPM · "
                        (str/upper-case (name orig-root)) " "
                        (str/capitalize (name orig-mode)))}
      "↺ RESET"]]))

(defn controls-component
  "Render top bar master and tweaks control decks.
  Examples: [controls-component props]."
  [props]
  [:div.top-bar-controls
   [master-deck-component props]
   [tweaks-deck-component props]])
