(ns app.lib.scenes
  "Built-in 3D scene specifications and geometry catalog."
  (:require ["three" :as three]))

(defn create-geometry [geom-type]
  (case (keyword geom-type)
    :torus-knot   (three/TorusKnotGeometry. 1.8 0.5 128 32)
    :icosahedron  (three/IcosahedronGeometry. 2.2 2)
    :octahedron   (three/OctahedronGeometry. 2.5 2)
    :box          (three/BoxGeometry. 2.8 2.8 2.8)
    :sphere       (three/SphereGeometry. 2.2 32 32)
    :dodecahedron (three/DodecahedronGeometry. 2.3 1)
    :tetrahedron  (three/TetrahedronGeometry. 2.5 2)
    :cylinder     (three/CylinderGeometry. 1.5 1.5 3.0 32)
    :torus        (three/TorusGeometry. 2.0 0.6 30 100)
    (three/TorusKnotGeometry. 1.8 0.5 128 32)))

(def core-scenes
  {:cyber-torus
   {:name        "Cyberpunk Torus Knot"
    :geom        :torus-knot
    :colors      {:bg "#080412" :mesh "#00ffcc" :wire "#ff007f" :outer "#331144"}
    :material    {:wireframe true :roughness 0.2 :metalness 0.8 :emissiveIntensity 0.3}
    :outer-geom  (fn [] (three/IcosahedronGeometry. 5 1))
    :camera-pos  [0 0 7]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.5)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.0))))
                   (when outer
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.5)))))}

   :quantum-polyhedron
   {:name        "Quantum Polyhedron"
    :geom        :icosahedron
    :colors      {:bg "#030814" :mesh "#00e5ff" :wire "#00ff88" :outer "#0a1b3a"}
    :material    {:wireframe true :roughness 0.1 :metalness 0.9 :emissiveIntensity 0.4}
    :outer-geom  (fn [] (three/TorusGeometry. 4.5 0.2 16 64))
    :camera-pos  [0 0 7.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 2.2)))
                     (set! (.. mesh -rotation -z) (+ (.. mesh -rotation -z) (* camera-speed 1.2))))
                   (when outer
                     (set! (.. outer -rotation -x) (+ (.. outer -rotation -x) (* camera-speed 0.8)))
                     (set! (.. outer -rotation -y) (+ (.. outer -rotation -y) (* camera-speed 1.1)))))}

   :monolith-core
   {:name        "Monolith Grid Core"
    :geom        :box
    :colors      {:bg "#050508" :mesh "#ffaa00" :wire "#ff0055" :outer "#221100"}
    :material    {:wireframe true :roughness 0.3 :metalness 0.7 :emissiveIntensity 0.35}
    :outer-geom  (fn [] (three/SphereGeometry. 4.8 16 16))
    :camera-pos  [0 0 7.2]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.0)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 1.4)))
                     (set! (.. mesh -rotation -z) (+ (.. mesh -rotation -z) (* camera-speed 0.8))))
                   (when outer
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.3)))))}

   :crystal-octahedron
   {:name        "Crystal Octahedron"
    :geom        :octahedron
    :colors      {:bg "#0a0014" :mesh "#c77dff" :wire "#00e5ff" :outer "#2a0845"}
    :material    {:wireframe true :roughness 0.15 :metalness 0.85 :emissiveIntensity 0.45}
    :outer-geom  (fn [] (three/OctahedronGeometry. 4.6 1))
    :camera-pos  [0 0 6.8]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.5)))
                     (set! (.. mesh -rotation -z) (+ (.. mesh -rotation -z) (* camera-speed 1.5))))
                   (when outer
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 1.2)))
                     (set! (.. outer -rotation -x) (+ (.. outer -rotation -x) (* camera-speed 0.6)))))}

   :acid-sphere
   {:name        "Resonant Acid Sphere"
    :geom        :sphere
    :colors      {:bg "#020f08" :mesh "#00ff88" :wire "#00e5ff" :outer "#06331a"}
    :material    {:wireframe true :roughness 0.2 :metalness 0.8 :emissiveIntensity 0.5}
    :outer-geom  (fn [] (three/DodecahedronGeometry. 4.5 1))
    :camera-pos  [0 0 6.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.8)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 1.6))))
                   (when outer
                     (set! (.. outer -rotation -z) (+ (.. outer -rotation -z) (* camera-speed 0.9)))))}

   :dodeca-cage
   {:name        "Dodecahedron Matrix"
    :geom        :dodecahedron
    :colors      {:bg "#080412" :mesh "#ff007f" :wire "#c77dff" :outer "#1f0933"}
    :material    {:wireframe true :roughness 0.25 :metalness 0.75 :emissiveIntensity 0.4}
    :outer-geom  (fn [] (three/IcosahedronGeometry. 4.8 1))
    :camera-pos  [0 0 7.0]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.4)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.1))))
                   (when outer
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.7)))))}

   :wire-cylinder
   {:name        "Tunnel Cylinder"
    :geom        :cylinder
    :colors      {:bg "#000814" :mesh "#0088ff" :wire "#00e5ff" :outer "#00204a"}
    :material    {:wireframe true :roughness 0.2 :metalness 0.8 :emissiveIntensity 0.35}
    :outer-geom  (fn [] (three/TorusGeometry. 4.2 0.3 16 50))
    :camera-pos  [0 0 7.2]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.2)))
                     (set! (.. mesh -rotation -z) (+ (.. mesh -rotation -z) (* camera-speed 2.0))))
                   (when outer
                     (set! (.. outer -rotation -x) (+ (.. outer -rotation -x) (* camera-speed 0.5)))))}

   :warp-torus
   {:name        "Warp Torus Ring"
    :geom        :torus
    :colors      {:bg "#120208" :mesh "#ff0055" :wire "#ffaa00" :outer "#3b0517"}
    :material    {:wireframe true :roughness 0.1 :metalness 0.9 :emissiveIntensity 0.4}
    :outer-geom  (fn [] (three/OctahedronGeometry. 4.6 2))
    :camera-pos  [0 0 6.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 2.4)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 1.2))))
                   (when outer
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.6)))))}

   :synthwave-grid
   {:name        "Synthwave Horizon Grid"
    :geom        (fn [] (three/PlaneGeometry. 24 24 32 32))
    :colors      {:bg "#05020c" :mesh "#ff007f" :wire "#00e5ff" :outer "#20003b"}
    :material    {:wireframe true :roughness 0.1 :metalness 0.9 :emissiveIntensity 0.5}
    :outer-geom  (fn [] (three/RingGeometry. 3.0 5.0 32))
    :camera-pos  [0 1.2 5.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -x) -1.35)
                     (set! (.. mesh -rotation -z) (+ (.. mesh -rotation -z) (* camera-speed 0.8))))
                   (when outer
                     (set! (.. outer -rotation -x) (+ (.. outer -rotation -x) (* camera-speed 0.6)))
                     (set! (.. outer -rotation -y) (+ (.. outer -rotation -y) (* camera-speed 0.9)))))}

   :star-tunnel
   {:name        "Hyperspace Warp Vortex"
    :geom        (fn [] (three/TorusGeometry. 3.2 1.6 24 100))
    :colors      {:bg "#000208" :mesh "#00ffcc" :wire "#7928ca" :outer "#001830"}
    :material    {:wireframe true :roughness 0.05 :metalness 0.95 :emissiveIntensity 0.6}
    :outer-geom  (fn [] (three/TorusGeometry. 5.2 0.25 16 64))
    :camera-pos  [0 0 4.2]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -z) (+ (.. mesh -rotation -z) (* camera-speed 3.2)))
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 0.8))))
                   (when outer
                     (set! (.. outer -rotation -z) (- (.. outer -rotation -z) (* camera-speed 1.6)))))}

   :orbital-matrix
   {:name        "Saturn Orbital Rings"
    :geom        (fn [] (three/IcosahedronGeometry. 1.8 2))
    :colors      {:bg "#06040e" :mesh "#00e5ff" :wire "#ffaa00" :outer "#2a0845"}
    :material    {:wireframe true :roughness 0.15 :metalness 0.85 :emissiveIntensity 0.4}
    :outer-geom  (fn [] (three/RingGeometry. 3.2 5.5 48))
    :camera-pos  [0 1.2 6.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.5)))
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 1.0))))
                   (when outer
                     (set! (.. outer -rotation -x) 1.2)
                     (set! (.. outer -rotation -z) (+ (.. outer -rotation -z) (* camera-speed 1.8)))))}

   :dna-nexus
   {:name        "Helical Nexus Spine"
    :geom        (fn [] (three/CylinderGeometry. 1.2 1.2 6.5 8 20 true))
    :colors      {:bg "#02080c" :mesh "#00ff88" :wire "#00e5ff" :outer "#002b28"}
    :material    {:wireframe true :roughness 0.15 :metalness 0.85 :emissiveIntensity 0.4}
    :outer-geom  (fn [] (three/DodecahedronGeometry. 4.5 1))
    :camera-pos  [0 0 6.5]
    :animate     (fn [{:keys [^js mesh ^js outer camera-speed]}]
                   (when mesh
                     (set! (.. mesh -rotation -y) (+ (.. mesh -rotation -y) (* camera-speed 2.8)))
                     (set! (.. mesh -rotation -x) (+ (.. mesh -rotation -x) (* camera-speed 0.7))))
                   (when outer
                     (set! (.. outer -rotation -y) (- (.. outer -rotation -y) (* camera-speed 0.9)))))}
   })
