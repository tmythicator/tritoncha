(ns app.audio.control.pattern
  "Canonical pattern data normalization for the audio looper."
  (:require [app.audio.theory.patterns :as patterns]
            [app.config :as cfg]))

(defn canonicalize-pattern
  "Normalizes a track pattern map, string, or note vector with canonical defaults."
  [track-key pat]
  (let [tk        (keyword track-key)
        data      (cond
                    (vector? pat) {:notes pat}
                    (string? pat) {:notes (patterns/pattern pat)}
                    (map? pat)    pat
                    :else         {})
        notes-raw (or (:notes data) (:pattern data) (:pat data) (:hits data))
        notes-vec (cond
                    (nil? notes-raw)        nil
                    (string? notes-raw)     (patterns/pattern notes-raw)
                    (vector? notes-raw)     notes-raw
                    (sequential? notes-raw) (vec notes-raw)
                    :else                   notes-raw)]
    (merge {:track tk
            :inst  (or (:inst data) tk)
            :dur   cfg/default-step
            :step  cfg/default-step
            :vel   cfg/default-velocity}
           data
           (when (some? notes-vec)
             {:notes notes-vec}))))

(defn valid-pattern?
  "Checks if a pattern map has valid notes and an instrument key."
  [pat]
  (boolean
   (and (map? pat)
        (keyword? (:inst pat))
        (vector? (:notes pat)))))

