(ns app.audio.control.scheduler
  "Pure algorithmic scheduler logic, pattern canonicalization and mask resolution."
  (:require [app.audio.control.pattern :as pattern]
            [app.audio.control.session :as session]
            [app.audio.dsp.instruments.catalog :as catalog]
            [app.audio.theory.patterns :as patterns]
            [app.config :as cfg]))

(defn- to-vec [x]
  (when x (if (sequential? x) (vec x) [x])))

(defn- resolve-track-notes
  "Resolves note pitches from scale degrees or explicit notes, applying rhythm masks."
  [pat notes-in degs oct]
  (let [raw  (cond
               (seq notes-in)
               notes-in

               degs
               (session/d degs (if oct {:octave oct} {}))

               (seq (:mask pat))
               [true]

               :else
               nil)
        hits (to-vec raw)
        mask (to-vec (:mask pat))]
    (if (and hits (seq mask))
      (patterns/apply-mask hits mask)
      hits)))

(defn normalize-pattern-data
  "Pure transform that canonicalizes pattern specifications, resolving degrees and masks into a canonical track pattern map."
  [track-key pattern-map]
  (let [tk       (keyword track-key)
        pat      (pattern/canonicalize-pattern tk pattern-map)
        notes-in (:notes pat)
        degs     (patterns/extract-pattern-degs pat notes-in)
        prog     (patterns/extract-pattern-prog pat notes-in)
        def-oct  (if (catalog/bass? tk) cfg/default-bass-octave cfg/default-lead-octave)
        oct      (patterns/extract-pattern-octave pat notes-in degs prog def-oct)
        notes    (resolve-track-notes pat notes-in degs oct)]
    (cond-> (assoc pat :notes notes)
      oct  (assoc :oct oct)
      degs (assoc :deg degs)
      prog (assoc :progression prog))))
