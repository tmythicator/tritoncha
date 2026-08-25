(ns app.visuals.engine
  (:require
   ["three" :as three]
   [app.custom.scenes :as custom-scenes]
   [app.lib.scenes :as lib-scenes]
   [app.state :refer [engine-ctx pulse! repl-registry visual-pulse
                      visual-state]]
   [app.utils :refer [max-dpr]]))

(defn all-scenes
  "Returns the complete merged catalog of built-in, custom and REPL-defined 3D scenes."
  []
  (merge lib-scenes/core-scenes
         custom-scenes/user-scenes
         (:scenes @repl-registry)))

(defn register-scene!
  "Registers or updates a dynamic 3D scene specification in the REPL registry.
  Examples: (register-scene! :hyper-knot {:geom :torus-knot :colors {:bg \"#000000\" :mesh \"#00ffff\"}})."
  [scene-key spec]
  (swap! repl-registry assoc-in [:scenes (keyword scene-key)] spec)
  (keyword scene-key))

(defn create-geom
  "Instantiates a Three.js BufferGeometry from keyword type or zero-arg constructor."
  [geom-spec]
  (cond
    (fn? geom-spec)        (geom-spec)
    (instance? three/BufferGeometry geom-spec) geom-spec
    :else
    (case (keyword geom-spec)
      :icosahedron  (three/IcosahedronGeometry. 2.0 1)
      :torus-knot   (three/TorusKnotGeometry. 1.8 0.45 128 32)
      :octahedron   (three/OctahedronGeometry. 2.2 0)
      :box          (three/BoxGeometry. 2.5 2.5 2.5)
      :sphere       (three/SphereGeometry. 2.0 32 32)
      :dodecahedron (three/DodecahedronGeometry. 2.0 0)
      (three/IcosahedronGeometry. 2.0 1))))

(defn- build-mesh-material [mesh-color wire-color wireframe? mat-spec]
  (let [opts (merge {:color       (three/Color. mesh-color)
                     :wireframe   (boolean wireframe?)
                     :roughness   0.2
                     :metalness   0.8
                     :emissive    (three/Color. (or wire-color "#ff007f"))
                     :emissiveIntensity 0.15}
                    mat-spec)]
    (three/MeshStandardMaterial. (clj->js opts))))

(defn- build-outer-mesh [outer-geom outer-color]
  (let [geom (if (fn? outer-geom)
               (outer-geom)
               (three/IcosahedronGeometry. 3.8 1))
        mat  (three/MeshBasicMaterial.
              #js {:color (three/Color. (or outer-color "#331144"))
                   :wireframe true
                   :transparent true
                   :opacity 0.25})]
    (three/Mesh. geom mat)))

(defn load-scene!
  "Switches active 3D scene preset live without dropping WebGL context."
  [scene-key-or-spec]
  (let [available (all-scenes)
        scene-key (when (keyword? scene-key-or-spec) scene-key-or-spec)
        spec      (if (map? scene-key-or-spec)
                    scene-key-or-spec
                    (get available (keyword scene-key-or-spec) (get available :cyber-torus)))]
    (when spec
      (let [{:keys [geom colors material outer-geom camera-pos animate]} spec
            {:keys [bg mesh wire outer]
             :or {bg "#050510" mesh "#00ffcc" wire "#ff007f" outer "#331144"}} colors
            wireframe? (get material :wireframe (:wireframe? @visual-state true))]

        (swap! visual-state assoc
               :current-scene (or scene-key :custom)
               :mesh-type     (if (keyword? geom) geom :custom)
               :bg-color      bg
               :mesh-color    mesh
               :wire-color    wire
               :wireframe?    wireframe?)

        (when-let [{:keys [scene camera ^js mesh ^js outer-mesh]} (:three @engine-ctx)]
          (set! (.. scene -background) (three/Color. bg))

          (when camera-pos
            (let [[cx cy cz] camera-pos]
              (.set (.-position camera) cx cy cz)))

          (when mesh
            (set! (.-geometry mesh) (create-geom geom))
            (set! (.-material mesh) (build-mesh-material mesh wire wireframe? material)))

          (when (and scene outer-mesh)
            (.remove scene outer-mesh)
            (let [new-outer (build-outer-mesh outer-geom outer)]
              (.add scene new-outer)
              (swap! engine-ctx update :three assoc :outer new-outer :outer-mesh new-outer :animate animate))))

        (pulse! 2.0)
        (or scene-key :custom)))))

(def scene! load-scene!)

(defn init-three!
  "Initializes the Three.js WebGL rendering context, camera, lighting, and mounts to DOM."
  []
  (when-let [container (.getElementById js/document "canvas-container")]
    (set! (.-innerHTML container) "")
    (let [w          (.-innerWidth js/window)
          h          (.-innerHeight js/window)
          aspect     (/ w h)
          responsive-z (if (< aspect 1.0) (/ 7.0 (max 0.45 aspect)) 7.0)
          scene      (three/Scene.)
          camera     (three/PerspectiveCamera. 60 aspect 0.1 1000)
          renderer   (three/WebGLRenderer. #js {:antialias true :alpha true})
          cur-scene  (get (all-scenes) (:current-scene @visual-state) (get (all-scenes) :cyber-torus))
          geom-spec  (or (:geom cur-scene) (:mesh-type @visual-state))
          mat-spec   (:material cur-scene)
          colors     (or (:colors cur-scene)
                         {:bg (:bg-color @visual-state) :mesh (:mesh-color @visual-state) :wire (:wire-color @visual-state) :outer "#331144"})
          mesh-geom  (create-geom geom-spec)
          mesh-mat   (build-mesh-material (:mesh colors) (:wire colors) (:wireframe? @visual-state) mat-spec)
          mesh       (three/Mesh. mesh-geom mesh-mat)
          outer      (build-outer-mesh (:outer-geom cur-scene) (:outer colors))]

      (.setSize renderer w h)
      (.setPixelRatio renderer (min (.-devicePixelRatio js/window) (max-dpr)))
      (set! (.. scene -background) (three/Color. (:bg colors)))
      (.appendChild container (.-domElement renderer))
      (.set (.-position camera) 0 0 responsive-z)

      (.add scene (three/AmbientLight. 0xffffff 0.6))
      (.add scene (doto (three/DirectionalLight. 0xffffff 1.2) (.position.set 5 10 7)))
      (.add scene mesh)
      (.add scene outer)

      (swap! engine-ctx assoc :three
             {:scene      scene
              :camera     camera
              :renderer   renderer
              :mesh       mesh
              :outer      outer
              :outer-mesh outer
              :animate    (:animate cur-scene)}))))

(defn render-loop!
  "Audio-reactive WebGL animation loop."
  []
  (when-let [{:keys [scene camera ^js renderer ^js mesh ^js outer animate]} (:three @engine-ctx)]
    (let [{:keys [sensitivity camera-speed]} @visual-state
          pulse        @visual-pulse
          target-scale (+ 1.0 (* pulse sensitivity 0.4))
          cur-scale    (if mesh (.-x (.-scale mesh)) 1.0)
          new-scale    (+ cur-scale (* (- target-scale cur-scale) 0.18))]

      (swap! visual-pulse #(max 0.0 (- % 0.06)))

      (when mesh
        (.set (.-scale mesh) new-scale new-scale new-scale))

      (if (fn? animate)
        (animate {:mesh         mesh
                  :outer        outer
                  :camera-speed camera-speed
                  :sensitivity  sensitivity
                  :pulse        pulse
                  :scale        new-scale})
        (do
          (when mesh
            (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.5)))
            (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.0))))
          (when outer
            (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.5))))))

      (.render renderer scene camera)))
  (js/requestAnimationFrame render-loop!))

(defn set-geometry!
  "Morphs the central 3D mesh geometry on the fly.
  Examples: (set-geometry! :icosahedron), (set-geometry! :torus-knot), (set-geometry! :sphere)."
  [geom-type]
  (swap! visual-state assoc :mesh-type (keyword geom-type))
  (when-let [{:keys [^js mesh]} (:three @engine-ctx)]
    (set! (.-geometry mesh) (create-geom geom-type))
    (pulse! 1.5)))

(defn set-colors!
  "Updates scene background and mesh colors on the fly."
  [bg-hex mesh-hex]
  (swap! visual-state assoc :bg-color bg-hex :mesh-color mesh-hex)
  (when-let [{:keys [scene ^js mesh]} (:three @engine-ctx)]
    (set! (.. scene -background) (three/Color. bg-hex))
    (.set (.. mesh -material -color) (three/Color. mesh-hex))))

(defn toggle-wireframe!
  "Toggles wireframe rendering mode on the central 3D mesh."
  []
  (let [new-val (not (:wireframe? @visual-state true))]
    (swap! visual-state assoc :wireframe? new-val)
    (when-let [{:keys [^js mesh]} (:three @engine-ctx)]
      (set! (.. mesh -material -wireframe) new-val))))
