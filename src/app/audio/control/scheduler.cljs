(ns app.audio.control.scheduler
  "Pure algorithmic scheduler logic, pattern canonicalization and mask resolution."
  (:require [app.audio.control.session :as session]
            [app.audio.dsp.busses :as busses]
            [app.audio.theory.patterns :as patterns]
            [app.config :as cfg]))

(defn normalize-pattern-data
  "Pure transform that canonicalizes pattern specifications, resolving degrees and masks.
  Examples: (normalize-pattern-data :bass {:notes ['C2' 'E2'] :step '16n'})."
  [track-key pattern-map]
  (let [tk         (keyword track-key)
        raw-data   (if (vector? pattern-map) {:notes pattern-map} pattern-map)
        notes-in   (:notes raw-data)
        degs       (patterns/extract-pattern-degs raw-data notes-in)
        prog       (patterns/extract-pattern-prog raw-data notes-in)
        with-degs  (if (and degs (not notes-in))
                     (let [oct (or (:oct raw-data) (:octave raw-data))]
                       (assoc raw-data :notes (session/d degs (if oct {:octave oct} {}))))
                     raw-data)
        notes      (:notes with-degs)
        def-oct    (if (busses/bass? tk) cfg/default-bass-octave cfg/default-lead-octave)
        oct        (patterns/extract-pattern-octave with-degs notes degs prog def-oct)
        raw-hits   (or notes (:hits-vec with-degs) (:pattern with-degs) (:hits with-degs) [true])
        hits-vec   (if (sequential? raw-hits) (vec raw-hits) [raw-hits])
        mask       (:mask with-degs)
        mask-vec   (when mask (if (sequential? mask) (vec mask) [mask]))
        final-hits (if (and mask-vec (seq mask-vec))
                     (patterns/apply-mask hits-vec mask-vec)
                     hits-vec)
        vel        (or (:vel with-degs) cfg/default-velocity)]
    (cond-> (assoc with-degs
                   :oct oct
                   :octave oct
                   :notes final-hits
                   :hits-vec final-hits
                   :vel vel
                   :dur (or (:dur with-degs) cfg/default-step)
                   :step (or (:step with-degs) cfg/default-step))
      mask-vec (assoc :mask-vec mask-vec)
      (vector? vel) (assoc :vel-vec vel)
      degs (assoc :deg degs)
      prog (assoc :progression prog))))
