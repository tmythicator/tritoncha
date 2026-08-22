(ns app.visuals.engine
  (:require ["three" :as three]
            [app.state :refer [state visual-pulse three-ctx repl-scenes pulse!]]
            [app.lib.scenes :as lib-scenes]
            [app.custom.scenes :as custom-scenes]))
(defn all-scenes
  "Returns the complete merged catalog of built-in, custom and REPL-defined 3D scenes."
  []
  (merge lib-scenes/core-scenes
         custom-scenes/user-scenes
         @repl-scenes))

(defn register-scene!
  "Registers or updates a dynamic 3D scene specification in the REPL registry.
  Examples: (register-scene! :hyper-knot {:geom :torus-knot :colors {:bg \"#000000\" :mesh \"#00ffff\"}})."
  [scene-key spec]
  (swap! repl-scenes assoc (keyword scene-key) spec)
  (keyword scene-key))

(def defscene! register-scene!)

(defn create-geom
  "Instantiates a Three.js BufferGeometry from keyword type or zero-arg constructor."
  [geom-spec]
  (cond
    (fn? geom-spec)      (geom-spec)
    (keyword? geom-spec) (lib-scenes/create-geometry geom-spec)
    :else                (lib-scenes/create-geometry :torus-knot)))

(defn- build-mesh-material [mesh-color wire-color wireframe? mat-opts]
  (let [{:keys [roughness metalness emissiveIntensity opacity]
         :or {roughness 0.2 metalness 0.8 emissiveIntensity 0.3 opacity 1.0}} mat-opts]
    (three/MeshStandardMaterial.
     #js {:color             (three/Color. mesh-color)
          :wireframe         wireframe?
          :roughness         roughness
          :metalness         metalness
          :emissive          (three/Color. wire-color)
          :emissiveIntensity emissiveIntensity
          :transparent       (< opacity 1.0)
          :opacity           opacity})))

(defn- build-outer-mesh [outer-geom-fn outer-color]
  (let [geom (if (fn? outer-geom-fn) (outer-geom-fn) (three/IcosahedronGeometry. 5 1))
        mat  (three/MeshBasicMaterial.
              #js {:color       (three/Color. (or outer-color "#331144"))
                   :wireframe   true
                   :transparent true
                   :opacity     0.25})]
    (three/Mesh. geom mat)))

(defn load-scene!
  "Switches to a registered 3D scene by keyword or applies an ad-hoc scene specification map.
  Examples: (load-scene! :quantum-polyhedron), (load-scene! :crystal-octahedron)."
  [scene-key-or-spec]
  (let [scene-key (when (keyword? scene-key-or-spec) scene-key-or-spec)
        spec      (if (keyword? scene-key-or-spec)
                    (get (all-scenes) scene-key)
                    scene-key-or-spec)]
    (when spec
      (let [{:keys [geom colors material outer-geom camera-pos animate]} spec
            {:keys [bg mesh wire outer]
             :or {bg "#050510" mesh "#00ffcc" wire "#ff007f" outer "#331144"}} colors
            wireframe? (get material :wireframe (:wireframe? @state))]

        (swap! state assoc
               :current-scene (or scene-key :custom)
               :mesh-type     (if (keyword? geom) geom :custom)
               :bg-color      bg
               :mesh-color    mesh
               :wire-color    wire
               :wireframe?    wireframe?)

        (when-let [{:keys [scene camera ^js mesh ^js outer-mesh]} @three-ctx]
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
              (swap! three-ctx assoc :outer new-outer :outer-mesh new-outer :animate animate))))

        (pulse! 2.0)
        (or scene-key :custom)))))

(def set-scene! load-scene!)
(def scene! load-scene!)

(defn init-three!
  "Initializes the Three.js WebGL rendering context, camera, lighting, and mounts to DOM."
  []
  (when-let [container (.getElementById js/document "canvas-container")]
    (set! (.-innerHTML container) "")
    (let [w          (.-innerWidth js/window)
          h          (.-innerHeight js/window)
          scene      (three/Scene.)
          camera     (three/PerspectiveCamera. 60 (/ w h) 0.1 1000)
          renderer   (three/WebGLRenderer. #js {:antialias true :alpha true})
          cur-scene  (get (all-scenes) (:current-scene @state) (get (all-scenes) :cyber-torus))
          geom-spec  (or (:geom cur-scene) (:mesh-type @state))
          mat-spec   (:material cur-scene)
          colors     (or (:colors cur-scene)
                         {:bg (:bg-color @state) :mesh (:mesh-color @state) :wire (:wire-color @state) :outer "#331144"})
          mesh-geom  (create-geom geom-spec)
          mesh-mat   (build-mesh-material (:mesh colors) (:wire colors) (:wireframe? @state) mat-spec)
          mesh       (three/Mesh. mesh-geom mesh-mat)
          outer      (build-outer-mesh (:outer-geom cur-scene) (:outer colors))]

      (.setSize renderer w h)
      (.setPixelRatio renderer (min (.-devicePixelRatio js/window) 2))
      (set! (.. scene -background) (three/Color. (:bg colors)))
      (.appendChild container (.-domElement renderer))
      (.set (.-position camera) 0 0 7)

      (.add scene (three/AmbientLight. 0xffffff 0.6))
      (.add scene (doto (three/DirectionalLight. 0xffffff 1.2) (.position.set 5 10 7)))
      (.add scene mesh)
      (.add scene outer)

      (reset! three-ctx {:scene      scene
                         :camera     camera
                         :renderer   renderer
                         :mesh       mesh
                         :outer      outer
                         :outer-mesh outer
                         :animate    (:animate cur-scene)}))))

(defn render-loop!
  "Audio-reactive WebGL animation loop."
  []
  (when-let [{:keys [scene camera renderer ^js mesh ^js outer animate]} @three-ctx]
    (let [{:keys [sensitivity camera-speed]} @state
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
  (swap! state assoc :mesh-type (keyword geom-type))
  (when-let [{:keys [^js mesh]} @three-ctx]
    (set! (.-geometry mesh) (create-geom geom-type))
    (pulse! 1.5)))

(defn set-colors!
  "Updates scene background and mesh colors on the fly."
  [bg-hex mesh-hex]
  (swap! state assoc :bg-color bg-hex :mesh-color mesh-hex)
  (when-let [{:keys [scene ^js mesh]} @three-ctx]
    (set! (.. scene -background) (three/Color. bg-hex))
    (.set (.. mesh -material -color) (three/Color. mesh-hex))))

(defn toggle-wireframe!
  "Toggles wireframe rendering mode on the central 3D mesh."
  []
  (let [new-val (not (:wireframe? @state))]
    (swap! state assoc :wireframe? new-val)
    (when-let [{:keys [^js mesh]} @three-ctx]
      (set! (.. mesh -material -wireframe) new-val))))
