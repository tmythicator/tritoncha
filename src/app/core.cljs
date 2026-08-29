(ns app.core
  (:require
   ["three" :as three]
   [app.api]
   [app.audio.control.looper :refer [toggle-click!]]
   [app.audio.control.mixer :refer [toggle-bus! toggle-drums!]]
   [app.audio.control.tracker :refer [play-preset! toggle-play!]]
   [app.audio.dsp.engine :refer [init-audio! resume-audio-context!]]
   [app.audio.dsp.fx :refer [trigger-dub-siren! trigger-sub-drop!]]
   [app.audio.dsp.instruments]
   [app.custom.instruments]
   [app.custom.routes]
   [app.custom.scenes]
   [app.custom.tracks]
   [app.demo.tutorial]
   [app.lib.drums]
   [app.lib.instruments]
   [app.lib.routes]
   [app.lib.scenes]
   [app.lib.tracks]
   [app.state :refer [engine-ctx ui-state visual-state]]
   [app.ui.hud :refer [render-ui! toggle-hud! toggle-stats! toggle-tutorial!]]
   [app.visuals.engine :as engine :refer [cycle-scene! init-three! render-loop!
                                          resize-viewport! toggle-wireframe!]]))

(defn- handle-visibility-change!
  "Ensures WebAudio context is running on visibility change."
  []
  (when (and (exists? js/document) (not (.-hidden js/document)))
    (resume-audio-context!)))

(defn- handle-pointer-down!
  "Silently unlocks and resumes the WebAudio context on first user interaction."
  []
  (init-audio!)
  (resume-audio-context!))

(defn- handle-key-down!
  "Global hotkey dispatcher for performance controls, presets, and HUD overlays."
  [^js e]
  (init-audio!)
  (resume-audio-context!)
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
        ("d" "D") (toggle-drums!)
        ("c" "C") (toggle-click!)
        ("b" "B") (toggle-bus! :bus/bass)
        ("s" "S") (trigger-dub-siren!)
        ("x" "X") (trigger-sub-drop!)
        ("g" "G") (cycle-scene!)
        ("w" "W") (toggle-wireframe!)
        ("h" "H") (toggle-hud!)
        ("i" "I") (toggle-stats!)
        ("t" "T" "?") (toggle-tutorial!)
        ("Escape" "Esc") (do
                           (when (:stats-visible? @ui-state) (toggle-stats!))
                           (when (:tutorial-visible? @ui-state) (toggle-tutorial!)))
        nil))))

(defn- bind-events!
  "Registers browser event listeners guarded by events flag in engine-ctx to prevent hot-reload duplicates."
  []
  (when-not (:events @engine-ctx)
    (swap! engine-ctx assoc :events true)
    (.addEventListener js/document "visibilitychange" handle-visibility-change!)
    (.addEventListener js/window "pointerdown" handle-pointer-down!)
    (.addEventListener js/window "keydown" handle-key-down!)
    (.addEventListener js/window "resize" resize-viewport!)))

(defn ^:export init! []
  (js/console.log "Initializing Tritoncha Live Studio...")
  (render-ui!)
  (init-three!)
  (render-loop!)
  (bind-events!)
  (js/console.log "Audio + WebGL Engines Ready. Connect REPL or evaluate live in app.live.jam."))

(defn ^:dev/after-load ^:export reload! []
  (js/console.log "Hot Reloading ClojureScript app.core...")
  (render-ui!)
  (when-let [{:keys [scene ^js mesh]} (:three @engine-ctx)]
    (let [{:keys [bg-color wireframe? mesh-color]} @visual-state]
      (set! (.. scene -background) (three/Color. bg-color))
      (set! (.. mesh -material -wireframe) wireframe?)
      (.set (.. mesh -material -color) (three/Color. mesh-color)))))