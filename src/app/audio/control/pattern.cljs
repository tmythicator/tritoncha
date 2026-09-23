(ns app.audio.control.pattern
  "Canonical pattern data normalization for the audio looper."
  (:require [app.config :as cfg]))

(defn canonicalize-pattern
  "Normalizes a track pattern map or note vector with canonical defaults.
  Examples: (canonicalize-pattern :bass [\"C2\"])
            -> {:track :bass :inst :bass :notes [\"C2\"] :step \"16n\" :dur \"16n\" :vel 0.9}."
  [track-key pat]
  (let [tk   (keyword track-key)
        data (if (vector? pat) {:notes pat} (or pat {}))]
    (merge {:track tk
            :inst  (or (:inst data) tk)
            :dur   cfg/default-step
            :step  cfg/default-step
            :vel   cfg/default-velocity}
           data)))

(defn valid-pattern?
  "Checks if a pattern map has valid notes and an instrument key.
  Examples: (valid-pattern? {:inst :bass :notes [\"C2\"]})."
  [pat]
  (boolean
   (and (map? pat)
        (keyword? (:inst pat))
        (vector? (:notes pat)))))
