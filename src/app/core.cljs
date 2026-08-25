(ns app.core
  (:require
   ["three" :as three]
   ["tone" :as tone]
   [app.api]
   [app.audio.engine :refer [init-audio!]]
   [app.audio.fx :refer [trigger-dub-siren! trigger-sub-drop!]]
   [app.audio.looper :refer [toggle-click!]]
   [app.audio.mixer :refer [redrum! stop! toggle-bus! undrum!]]
   [app.audio.tracker :refer [play-preset!]]
   [app.audio.voices]
   [app.config :as cfg]
   [app.custom.instruments]
   [app.custom.routes]
   [app.custom.scenes]
   [app.custom.tracks]
   [app.demo.tutorial]
   [app.lib.instruments]
   [app.lib.routes]
   [app.lib.scenes]
   [app.lib.tracks]
   [app.live.jam]
   [app.state :refer [audio-state engine-ctx ui-state visual-state]]
   [app.ui.hud :refer [render-ui! toggle-hud! toggle-stats! toggle-tutorial!]]
   [app.utils :refer [active-lookahead cycle-next]]
   [app.visuals.engine :as engine :refer [init-three! render-loop!
                                          toggle-wireframe!]]))

(defn toggle-play! []
  (if (:active? @audio-state)
    (stop!)
    (play-preset! (:current-jam @audio-state :roller))))

(defn cycle-scene! []
  (let [next-scene (cycle-next (:current-scene @visual-state :cyber-torus) (keys (engine/all-scenes)))]
    (engine/scene! next-scene)))

(defn- handle-visibility-change!
  "Adjusts WebAudio lookahead buffer when the tab is backgrounded to prevent x-runs."
  []
  (when-let [ctx (when (:tone @engine-ctx) (.-context tone))]
    (set! (.-lookAhead ctx)
          (if (.-hidden js/document)
            cfg/lookahead-bg
            (active-lookahead)))))

(defn- handle-pointer-down!
  "Silently unlocks and resumes the WebAudio context on first user interaction."
  []
  (init-audio!)
  (when-let [ctx (.-context tone)]
    (when (not= (.-state ctx) "running")
      (try (.resume ctx) (catch js/Object _)))))

(defn- handle-key-down!
  "Global hotkey dispatcher for performance controls, presets, and HUD overlays."
  [^js e]
  (let [k   (.-key e)
        tag (some-> (.-target e) (.-tagName) (.toLowerCase))
        in-editor? (or (= tag "textarea") (= tag "input"))]
    (if in-editor?
      (when (and (#{"Escape" "Esc"} k) (:tutorial-visible? @ui-state))
        (toggle-tutorial!))
      (case k
        " " (do (.preventDefault e) (toggle-play!))
        "1" (play-preset! :roller)
        "2" (play-preset! :sub-roller)
        "3" (play-preset! :acid-roller)
        "4" (play-preset! :ambient-drift)
        ("c" "C") (toggle-click!)
        ("d" "D") (toggle-bus! :drums)
        ("u" "U") (undrum!)
        ("r" "R") (redrum!)
        ("s" "S") (trigger-dub-siren!)
        ("b" "B") (trigger-sub-drop!)
        ("g" "G") (cycle-scene!)
        ("w" "W") (toggle-wireframe!)
        ("h" "H") (toggle-hud!)
        ("i" "I") (toggle-stats!)
        ("t" "T") (toggle-tutorial!)
        ("Escape" "Esc") (do
                           (when (:stats-visible? @ui-state) (toggle-stats!))
                           (when (:tutorial-visible? @ui-state) (toggle-tutorial!)))
        nil))))

(defn- handle-window-resize!
  "Adapts Three.js camera aspect ratio, responsive Z distance, and renderer viewport to window size changes."
  []
  (when-let [{:keys [^js camera ^js renderer]} (:three @engine-ctx)]
    (let [w (.-innerWidth js/window)
          h (.-innerHeight js/window)
          aspect (/ w h)
          responsive-z (if (< aspect 1.0)
                         (/ 7.0 (max 0.45 aspect))
                         7.0)]
      (set! (.-aspect camera) aspect)
      (set! (.. camera -position -z) responsive-z)
      (.updateProjectionMatrix camera)
      (.setSize renderer w h))))

(defn- bind-events!
  "Registers browser event listeners guarded by events flag in engine-ctx to prevent hot-reload duplicates."
  []
  (when-not (:events @engine-ctx)
    (swap! engine-ctx assoc :events true)
    (.addEventListener js/document "visibilitychange" handle-visibility-change!)
    (.addEventListener js/window "pointerdown" handle-pointer-down!)
    (.addEventListener js/window "keydown" handle-key-down!)
    (.addEventListener js/window "resize" handle-window-resize!)))

(defn ^:export init! []
  (js/console.log "Initializing Tritoncha Live Studio...")
  (render-ui!)
  (init-three!)
  (render-loop!)
  (bind-events!)
  (js/console.log "Audio + WebGL Engines Ready. Connect REPL or evaluate live in app.live.jam."))

(defn ^:export reload! []
  (js/console.log "Hot Reloading ClojureScript app.core...")
  (render-ui!)
  (when-let [{:keys [scene ^js mesh]} (:three @engine-ctx)]
    (let [{:keys [bg-color wireframe? mesh-color]} @visual-state]
      (set! (.. scene -background) (three/Color. bg-color))
      (set! (.. mesh -material -wireframe) wireframe?)
      (.set (.. mesh -material -color) (three/Color. mesh-color)))))