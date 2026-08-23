(ns app.custom.scenes
  "User-defined 3D scenes, procedural geometries and shader effects."
  (:require ["three" :as three]))

;; Custom 3D Scenes Catalog (Same format as app.lib.scenes)
;;
;; Scene specification keys:
;;   :name        - Human-readable scene description
;;   :geom        - Built-in geometry keyword (:torus-knot, :icosahedron, :octahedron, :box, :sphere, :dodecahedron, :cylinder, :torus)
;;                  or custom zero-arg constructor function `(fn [] (three/TubeGeometry. ...))`
;;   :colors      - Map of {:bg "#hex" :mesh "#hex" :wire "#hex" :outer "#hex"}
;;   :material    - Material parameters (:wireframe, :roughness, :metalness, :emissiveIntensity, :opacity)
;;   :outer-geom  - Optional zero-arg constructor function for the surrounding ambient cage/ring
;;   :camera-pos  - Vector [x y z] camera position (default: [0 0 7])
;;   :animate     - Per-frame animation hook `(fn [{:keys [mesh outer camera-speed sensitivity pulse]}] ...)`

(def user-scenes
  {:neon-prism
   {:name        "Neon Glass Prism"
    :geom        :tetrahedron
    :colors      {:bg "#080010" :mesh "#ff007f" :wire "#00e5ff" :outer "#220033"}
    :material    {:wireframe true :roughness 0.1 :metalness 0.9 :emissiveIntensity 0.5}
    :outer-geom  (fn [] (three/TorusGeometry. 4.0 0.15 16 64))
    :camera-pos  [0 0 6.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 2.0)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 1.5))))
                   (when outer
                     (set! (.. outer -rotation -z) (+ (.. outer -rotation -z) (* camera-speed 0.8)))))}

   :hyper-cube
   {:name        "Hyperdimensional Cube"
    :geom        :box
    :colors      {:bg "#02040a" :mesh "#00ffcc" :wire "#c77dff" :outer "#051124"}
    :material    {:wireframe true :roughness 0.2 :metalness 0.8 :emissiveIntensity 0.4}
    :outer-geom  (fn [] (three/BoxGeometry. 5.2 5.2 5.2))
    :camera-pos  [0 0 7.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.5)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 1.8))))
                   (when outer
                     (set! (.. outer -rotation -x) (- (.. outer -rotation -x) (* camera-speed 0.5)))
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.5)))))}
   })
