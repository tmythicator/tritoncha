(ns app.audio.control.scheduler
  "Pure algorithmic scheduler logic, pattern canonicalization and mask resolution."
  (:require [app.audio.control.session :as session]
            [app.audio.dsp.busses :as busses]
            [app.config :as cfg]))

(defn apply-mask
  "Combines a sequence of hits with a boolean or truthy mask sequence.
  Falsy or nil mask values become rests (nil).
  Examples: (apply-mask [:c4 :e4] [true false true]) -> [:c4 nil :e4]."
  [hits-vec mask-vec]
  (let [h-cnt (count hits-vec)
        m-cnt (count mask-vec)
        total (max h-cnt m-cnt)]
    (mapv (fn [i]
            (let [m (nth mask-vec (rem i m-cnt))]
              (when (and (some? m) (not (false? m)))
                (nth hits-vec (rem i h-cnt)))))
          (range total))))

(defn normalize-pattern-data
  "Pure transform that canonicalizes pattern specifications, resolving degrees and masks.
  Examples: (normalize-pattern-data :bass {:notes ['C2' 'E2'] :step '16n'})."
  [track-key pattern-map]
  (let [tk         (keyword track-key)
        raw-data   (if (vector? pattern-map) {:notes pattern-map} pattern-map)
        notes-in   (:notes raw-data)
        meta-info  (when (vector? notes-in) (meta notes-in))
        degs       (or (:deg raw-data) (:degrees raw-data) (when meta-info (:degrees meta-info)))
        with-degs  (if (and degs (not notes-in))
                     (let [oct (or (:oct raw-data) (:octave raw-data))]
                       (assoc raw-data :notes (session/d degs (if oct {:octave oct} {}))))
                     raw-data)
        notes      (:notes with-degs)
        oct        (or (:oct with-degs)
                       (:octave with-degs)
                       (when (vector? notes) (:octave (meta notes)))
                       (when meta-info (:octave meta-info))
                       (if (busses/bass? tk) cfg/default-bass-octave cfg/default-lead-octave))
        raw-hits   (or notes (:hits-vec with-degs) (:pattern with-degs) (:hits with-degs) [true])
        hits-vec   (if (sequential? raw-hits) (vec raw-hits) [raw-hits])
        mask       (:mask with-degs)
        mask-vec   (when mask (if (sequential? mask) (vec mask) [mask]))
        final-hits (if (and mask-vec (seq mask-vec))
                     (apply-mask hits-vec mask-vec)
                     hits-vec)
        vel        (or (:vel with-degs) cfg/default-velocity)]
    (cond-> (assoc with-degs
                   :oct oct
                   :notes final-hits
                   :hits-vec final-hits
                   :vel vel
                   :dur (or (:dur with-degs) cfg/default-step)
                   :step (or (:step with-degs) cfg/default-step))
      mask-vec (assoc :mask-vec mask-vec)
      (vector? vel) (assoc :vel-vec vel)
      degs (assoc :deg degs))))
