(ns app.core
  (:require ["three" :as three]
            ["tone" :as tone]
            [app.state :refer [state three-ctx]]
            [app.audio.engine :refer [init-audio!]]
            [app.visuals.engine :refer [init-three! render-loop! set-geometry! toggle-wireframe!]]
            [app.audio.looper :refer [toggle-click!]]
            [app.audio.mixer :refer [stop! toggle-bus! undrum! redrum!]]
            [app.audio.fx :refer [trigger-dub-siren! trigger-sub-drop!]]
            [app.audio.tracker :refer [play-preset!]]
            [app.ui.hud :refer [render-ui! toggle-hud!]]
            [app.lib.instruments]
            [app.lib.tracks]
            [app.lib.routes]
            [app.audio.voices]
            [app.demo.tutorial]
            [app.custom.instruments]
            [app.custom.tracks]
            [app.custom.routes]
            [app.live.jam]))

(defn toggle-play! []
  (if (:active? @state)
    (stop!)
    (play-preset! (:current-jam @state :roller))))

(defn cycle-geometry! []
  (let [geoms [:torus-knot :icosahedron :octahedron :box :sphere]
        cur   (:mesh-type @state)
        next-g (let [idx (.indexOf (clj->js geoms) (name cur))]
                 (get geoms (mod (inc (if (neg? idx) 0 idx)) (count geoms))))]
    (set-geometry! next-g)))

(defn bind-ui! []
  ;; Silently unlock and resume WebAudio context on first user interaction without auto-playing tracks
  (.addEventListener js/window "pointerdown"
                     (fn []
                       (init-audio!)
                       (when-let [ctx (.-context tone)]
                         (when (not= (.-state ctx) "running")
                           (try (.resume ctx) (catch js/Object _))))))

  (.addEventListener js/window "keydown"
                     (fn [^js e]
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
                           ("g" "G") (cycle-geometry!)
                           ("w" "W") (toggle-wireframe!)
                           ("h" "H") (toggle-hud!)
                           nil))))

  (.addEventListener js/window "resize"
                     (fn []
                       (when-let [{:keys [^js camera ^js renderer]} @three-ctx]
                         (let [w (.-innerWidth js/window) h (.-innerHeight js/window)]
                           (set! (.-aspect camera) (/ w h))
                           (.updateProjectionMatrix camera)
                           (.setSize renderer w h))))))

(defn ^:export init! []
  (js/console.log "Initializing Tritoncha Live Studio...")
  (render-ui!)
  (init-three!)
  (render-loop!)
  (bind-ui!)
  (js/console.log "Audio + WebGL Engines Ready. Connect REPL or evaluate live in app.live.jam."))

(defn ^:export reload! []
  (js/console.log "Hot Reloading ClojureScript app.core...")
  (render-ui!)
  (when-let [{:keys [scene ^js mesh]} @three-ctx]
    (set! (.. scene -background) (three/Color. (:bg-color @state)))
    (set! (.. mesh -material -wireframe) (:wireframe? @state))
    (.set (.. mesh -material -color) (three/Color. (:mesh-color @state)))))