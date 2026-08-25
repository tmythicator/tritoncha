(ns app.lib.scenes-test
  (:require
   [app.custom.scenes :refer [user-scenes]]
   [app.lib.scenes :refer [core-scenes]]
   [app.state :refer [repl-registry]]
   [cljs.test :refer [deftest is testing]]))

(defn- all-test-scenes []
  (merge core-scenes user-scenes (:scenes @repl-registry)))

(defn- register-test-scene! [scene-key spec]
  (swap! repl-registry assoc-in [:scenes (keyword scene-key)] spec)
  (keyword scene-key))

(deftest core-scenes-catalog-test
  (testing "All built-in 3D scene presets are valid"
    (let [required-scenes [:cyber-torus :quantum-polyhedron :monolith-core :crystal-octahedron :acid-sphere]]
      (doseq [sc-key required-scenes]
        (let [scene (get core-scenes sc-key)]
          (is (some? scene) (str "Scene " sc-key " must exist in core-scenes"))
          (is (some? (:geom scene)) (str "Scene " sc-key " must have a geometry specified"))
          (is (map? (:colors scene)) (str "Scene " sc-key " must have colors map"))
          (is (string? (get-in scene [:colors :bg])) (str "Scene " sc-key " must have :bg hex color"))
          (is (string? (get-in scene [:colors :mesh])) (str "Scene " sc-key " must have :mesh hex color")))))))

(deftest dynamic-scene-registration-test
  (testing "Custom 3D scene can be registered live and retrieved"
    (let [custom-key :test-neon-grid
          custom-spec {:geom :box
                       :colors {:bg "#000000" :mesh "#00ff00" :wire "#ff0000"}}]
      (register-test-scene! custom-key custom-spec)
      (let [scenes (all-test-scenes)]
        (is (contains? scenes custom-key) "Custom scene must be present in all-scenes")
        (is (= custom-spec (get scenes custom-key)) "Registered scene spec must match")))))
