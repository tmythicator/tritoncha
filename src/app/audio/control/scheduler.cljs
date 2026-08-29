(ns app.audio.control.scheduler
  "Pure algorithmic scheduler logic, pattern canonicalization, step evaluation, and audibility rules."
  (:require [app.audio.control.session :as session]
            [app.config :as cfg]
            [app.utils.audio :refer [is-bass-track?]]))

(defn track-audible?
  "Checks whether a track should produce sound based on its pattern and global solo mode."
  [{:keys [muted? solo?]} solo-mode?]
  (and (not (boolean muted?))
       (or (not (boolean solo-mode?)) (boolean solo?))))

(defn normalize-pattern-data
  "Pure transform that canonicalizes pattern specifications, resolving degrees and precalculating arrays.
  Examples: (normalize-pattern-data :bass {:notes ['C2' 'E2'] :step '16n'})."
  [track-key pattern-map]
  (let [tk        (keyword track-key)
        raw-data  (if (vector? pattern-map) {:notes pattern-map} pattern-map)
        notes-in  (:notes raw-data)
        meta-info (when (vector? notes-in) (meta notes-in))
        degs      (or (:deg raw-data) (:degrees raw-data) (:degrees meta-info))
        with-degs (if (and degs (not notes-in))
                    (let [oct (or (:oct raw-data) (:octave raw-data))]
                      (assoc raw-data :notes (session/d degs (if oct {:octave oct} {}))))
                    raw-data)
        notes     (:notes with-degs)
        oct       (or (:oct with-degs)
                      (:octave with-degs)
                      (when (vector? notes) (:octave (meta notes)))
                      (:octave meta-info)
                      (if (is-bass-track? tk) cfg/default-bass-octave cfg/default-lead-octave))
        hits      (or notes (:hits-vec with-degs) (:pattern with-degs) (:hits with-degs) [true])
        hits-vec  (if (sequential? hits) (vec hits) [hits])
        hits-cnt  (count hits-vec)
        mask      (:mask with-degs)
        mask-vec  (when mask (if (sequential? mask) (vec mask) [mask]))
        mask-cnt  (if mask-vec (count mask-vec) 0)
        vel       (or (:vel with-degs) cfg/default-velocity)
        vel-vec   (when (vector? vel) vel)
        vel-cnt   (if vel-vec (count vel-vec) 0)]
    (cond-> (assoc with-degs
                   :oct oct
                   :hits-vec hits-vec
                   :hits-count hits-cnt
                   :mask-vec mask-vec
                   :mask-count mask-cnt
                   :vel vel
                   :vel-vec vel-vec
                   :vel-count vel-cnt
                   :dur (or (:dur with-degs) cfg/default-step)
                   :step (or (:step with-degs) cfg/default-step))
      degs (assoc :deg degs))))

(defn calculate-step-hit
  "Pure function evaluating which note and velocity to trigger at a given step index.
  Returns {:hit note :vel vel :dur dur} or nil if step is masked or resting."
  [{:keys [mask-vec mask-count hits-vec hits-count vel vel-vec vel-count dur]} step-idx]
  (when (and (pos? hits-count)
             (or (nil? mask-vec)
                 (let [m (nth mask-vec (rem step-idx mask-count))]
                   (and (some? m) (not (false? m))))))
    (when-let [hit (nth hits-vec (rem step-idx hits-count))]
      {:hit hit
       :vel (if vel-vec (nth vel-vec (rem step-idx vel-count)) vel)
       :dur dur})))
