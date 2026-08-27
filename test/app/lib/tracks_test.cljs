(ns app.lib.tracks-test
  (:require [app.lib.tracks :refer [core-tracks]]
            [cljs.test :refer [deftest is testing]]))

(deftest core-tracks-catalog-test
  (testing "All core presets are defined and structurally valid"
    (let [presets [:roller :sub-roller :acid-roller :ambient-drift]]
      (doseq [preset-kw presets]
        (let [track (get core-tracks preset-kw)]
          (is (some? track) (str "Preset " preset-kw " must exist"))
          (is (string? (:name track)) (str "Preset " preset-kw " must have a name"))
          (is (number? (:bpm track)) (str "Preset " preset-kw " must have a numeric BPM"))
          (is (<= 60 (:bpm track) 240) (str "Preset " preset-kw " BPM must be in valid range"))
          (is (map? (:tracks track)) (str "Preset " preset-kw " must contain a :tracks map"))
          (is (seq (:tracks track)) (str "Preset " preset-kw " tracks map must not be empty")))))))

(deftest core-tracks-drum-structure-test
  (testing "Drum tracks preserve their hit keywords"
    (let [roller-drums (get-in core-tracks [:roller :tracks :drums])]
      (is (some? (:notes roller-drums)))
      (is (vector? (first (:notes roller-drums))))
      (is (= :kick (first (first (:notes roller-drums))))))))
