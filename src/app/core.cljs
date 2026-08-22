(ns app.core
  (:require ["three" :as three]
            ["tone" :as tone]
            [app.state :refer [state tone-ctx three-ctx events-bound?]]
            [app.audio.engine :refer [init-audio!]]
            [app.visuals.engine :as engine :refer [init-three! render-loop! toggle-wireframe!]]
            [app.audio.looper :refer [toggle-click!]]
            [app.audio.mixer :refer [stop! toggle-bus! undrum! redrum!]]
            [app.audio.fx :refer [trigger-dub-siren! trigger-sub-drop!]]
            [app.audio.tracker :refer [play-preset!]]
            [app.ui.hud :refer [render-ui! toggle-hud! toggle-stats!]]
            [app.lib.instruments]
            [app.lib.tracks]
            [app.lib.routes]
            [app.lib.scenes]
            [app.audio.voices]
            [app.demo.tutorial]
            [app.custom.instruments]
            [app.custom.tracks]
            [app.custom.routes]
            [app.custom.scenes]
            [app.api]
            [app.live.jam]))

(defn toggle-play! []
  (if (:active? @state)
    (stop!)
    (play-preset! (:current-jam @state :roller))))

(defn cycle-scene! []
  (let [all-keys (vec (keys (engine/all-scenes)))
        cur      (:current-scene @state :cyber-torus)
        cur-name (name cur)
        idx      (or (first (keep-indexed (fn [i k] (when (= (name k) cur-name) i)) all-keys)) 0)
        next-s   (get all-keys (mod (inc idx) (count all-keys)) :cyber-torus)]
    (engine/scene! next-s)))

(defn- handle-visibility-change!
  "Adjusts WebAudio lookahead buffer when the tab is backgrounded to prevent x-runs."
  []
  (when-let [ctx (when @tone-ctx (.-context tone))]
    (if (.-hidden js/document)
      (set! (.-lookAhead ctx) 0.45)
      (set! (.-lookAhead ctx) 0.25))))

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
  (let [k (.-key e)]
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
      ("Escape" "Esc") (when (:stats-visible? @state) (toggle-stats!))
      nil)))

(defn- handle-window-resize!
  "Adapts Three.js camera aspect ratio and renderer viewport to window size changes."
  []
  (when-let [{:keys [^js camera ^js renderer]} @three-ctx]
    (let [w (.-innerWidth js/window)
          h (.-innerHeight js/window)]
      (set! (.-aspect camera) (/ w h))
      (.updateProjectionMatrix camera)
      (.setSize renderer w h))))

(defn- bind-events!
  "Registers browser event listeners guarded by events-bound? atom to prevent hot-reload duplicates."
  []
  (when-not @events-bound?
    (reset! events-bound? true)
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
  (when-let [{:keys [scene ^js mesh]} @three-ctx]
    (set! (.. scene -background) (three/Color. (:bg-color @state)))
    (set! (.. mesh -material -wireframe) (:wireframe? @state))
    (.set (.. mesh -material -color) (three/Color. (:mesh-color @state)))))