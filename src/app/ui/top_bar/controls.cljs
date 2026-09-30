(ns app.ui.top-bar.controls
  "Top bar engine playback, tempo slider, musical key/scale selectors, preset reset, and scene controls."
  (:require
   [app.audio.control.session :as session]
   [app.audio.control.tracker :as tracker :refer [default-track-key next-jam! prev-jam!]]
   [app.audio.control.transport :as transport]
   [app.config :as cfg]
   [app.state :refer [audio-state visual-state]]
   [clojure.string :as str]))

(def ^:private scale-options
  [[:phrygian          "Phrygian"]
   [:dorian            "Dorian"]
   [:minor             "Minor"]
   [:harmonic-minor    "Harmonic Min"]
   [:hungarian-minor   "Hungarian Min"]
   [:hirajoshi         "Hirajoshi"]
   [:blues             "Blues"]
   [:pentatonic-minor  "Pentatonic"]
   [:major             "Major"]
   [:lydian            "Lydian"]
   [:mixolydian        "Mixolydian"]
   [:arabic            "Arabic"]
   [:phrygian-dominant "Phryg Dom"]])

(def ^:private chromatic-keys
  ["c" "c#" "d" "d#" "e" "f" "f#" "g" "g#" "a" "a#" "b"])

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

(defn controls-component
  "Render top bar engine transport, interactive tempo slider, key/scale selectors, preset reset, and scene controls.
  Examples: [controls-component props]."
  [{:keys [toggle-play! cycle-jam! toggle-track-browser! cycle-scene!]}]
  (let [{:keys [active? current-jam bpm key]} @audio-state
        {:keys [current-scene]}               @visual-state
        jam-name       (-> (or current-jam (default-track-key)) name str/upper-case)
        scene-name     (-> (or current-scene :cyber-torus) name str/upper-case)
        root-kw        (:root key :e)
        mode-kw        (:mode key :phrygian)
        oct-num        (:octave key 1)
        orig-bpm       (track-orig-bpm current-jam)
        [orig-root orig-mode orig-oct] (track-orig-scale current-jam)
        orig-oct       (or orig-oct 1)
        modified?      (or (not= bpm orig-bpm)
                           (not= root-kw orig-root)
                           (not= mode-kw orig-mode)
                           (not= oct-num orig-oct))]
    [:div.top-bar-controls
     [:button.player-play-btn {:on-click toggle-play!
                               :class (when active? "active")
                               :aria-label (if active? "Stop Audio Engine" "Start Audio Engine")}
      (if active? "■ STOP ENGINE" "▶ PLAY ENGINE")]

     [:div.player-track-deck
      [:button.player-btn-nav {:on-click prev-jam!
                               :aria-label "Previous track preset"}
       "◄"]
      [:button.player-track-badge {:on-click (or toggle-track-browser! cycle-jam!)
                                   :aria-label "Open track browser"}
       (str "TRACK: " jam-name " ▾")]
      [:button.player-btn-nav {:on-click next-jam!
                               :aria-label "Next track preset"}
       "►"]]

     [:div.top-bar-tools
      ;; 1. Direct Tempo Slider + Numeric Readout
      [:div.tool-tempo-box
       [:span.tempo-readout
        {:on-double-click #(transport/set-bpm! orig-bpm)
         :title           (str "Tempo: " bpm " BPM (double-click to reset to " orig-bpm " BPM)")}
        (str bpm " BPM")]
       [:input.neo-tempo-slider
        {:type            "range"
         :min             cfg/min-bpm
         :max             240
         :step            1
         :value           bpm
         :on-double-click #(transport/set-bpm! orig-bpm)
         :title           (str "Tempo: " bpm " BPM (drag to adjust, double-click to reset to " orig-bpm " BPM)")
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
       "↺ RESET"]

      ;; 5. Swap 3D Scene Button
      [:button.neo-btn-stats.btn-swap-scene
       {:on-click   cycle-scene!
        :aria-label "Swap 3D visual scene"
        :title      "Click to swap 3D visual scene (or press G)"}
       (str "⟳ SWAP SCENE: " scene-name)]]]))
