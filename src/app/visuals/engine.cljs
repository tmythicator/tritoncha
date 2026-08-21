(ns app.visuals.engine
  (:require ["three" :as three]
            [app.state :refer [state visual-pulse three-ctx pulse!]]))

(defn create-geom [geom-type]
  (case geom-type
    :icosahedron (three/IcosahedronGeometry. 2.2 2)
    :torus-knot  (three/TorusKnotGeometry. 1.8 0.5 128 32)
    :octahedron  (three/OctahedronGeometry. 2.5 2)
    :box         (three/BoxGeometry. 2.8 2.8 2.8)
    :sphere      (three/SphereGeometry. 2.2 32 32)
    (three/TorusKnotGeometry. 1.8 0.5 128 32)))

(defn init-three! []
  (when-let [container (.getElementById js/document "canvas-container")]
    (set! (.-innerHTML container) "")
    (let [w (.-innerWidth js/window)
          h (.-innerHeight js/window)
          scene (three/Scene.)
          camera (three/PerspectiveCamera. 60 (/ w h) 0.1 1000)
          renderer (three/WebGLRenderer. #js {:antialias true :alpha true})
          geom (create-geom (:mesh-type @state))
          mat (three/MeshStandardMaterial.
               #js {:color (three/Color. (:mesh-color @state))
                    :wireframe (:wireframe? @state)
                    :roughness 0.2
                    :metalness 0.8
                    :emissive (three/Color. (:wire-color @state))
                    :emissiveIntensity 0.3})
          mesh (three/Mesh. geom mat)
          outer (three/Mesh. (three/IcosahedronGeometry. 5 1)
                             (three/MeshBasicMaterial. #js {:color (three/Color. "#331144")
                                                            :wireframe true
                                                            :opacity 0.25}))]

      (.setSize renderer w h)
      (.setPixelRatio renderer (min (.-devicePixelRatio js/window) 2))
      (set! (.. scene -background) (three/Color. (:bg-color @state)))
      (.appendChild container (.-domElement renderer))
      (.set (.-position camera) 0 0 7)

      (.add scene (three/AmbientLight. 0xffffff 0.6))
      (.add scene (doto (three/DirectionalLight. 0xffffff 1.2) (.position.set 5 10 7)))
      (.add scene mesh)
      (.add scene outer)

      (reset! three-ctx {:scene scene :camera camera :renderer renderer :mesh mesh :outer outer}))))

(defn render-loop! []
  (when-let [{:keys [scene camera renderer ^js mesh ^js outer]} @three-ctx]
    (let [{:keys [sensitivity camera-speed]} @state
          pulse @visual-pulse
          target-scale (+ 1.0 (* pulse sensitivity 0.4))
          cur-scale (.-x (.-scale mesh))
          new-scale (+ cur-scale (* (- target-scale cur-scale) 0.18))]

      (swap! visual-pulse #(max 0.0 (- % 0.06)))

      (.set (.-scale mesh) new-scale new-scale new-scale)
      (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.5)))
      (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.0)))
      (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.5)))

      (.render renderer scene camera)))
  (js/requestAnimationFrame render-loop!))

(defn set-geometry! [geom-type]
  (swap! state assoc :mesh-type geom-type)
  (when-let [{:keys [^js mesh]} @three-ctx]
    (set! (.-geometry mesh) (create-geom geom-type))
    (pulse! 1.5)))

(defn set-colors! [bg-hex mesh-hex]
  (swap! state assoc :bg-color bg-hex :mesh-color mesh-hex)
  (when-let [{:keys [scene ^js mesh]} @three-ctx]
    (set! (.. scene -background) (three/Color. bg-hex))
    (.set (.. mesh -material -color) (three/Color. mesh-hex))))

(defn toggle-wireframe! []
  (let [new-val (not (:wireframe? @state))]
    (swap! state assoc :wireframe? new-val)
    (when-let [{:keys [^js mesh]} @three-ctx]
      (set! (.. mesh -material -wireframe) new-val))))
