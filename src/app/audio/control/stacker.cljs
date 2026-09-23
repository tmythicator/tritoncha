(ns app.audio.control.stacker
  "Multi-track live loop batch launcher and arrangement orchestrator."
  (:require [app.audio.control.looper :as looper]
            [app.audio.control.transport :as transport]
            [app.audio.dsp.instruments.catalog :as catalog]
            [app.state :refer [audio-state]]))

(def unstack!
  "Stops and removes active tracks by keyword(s). Alias for stop-loop!."
  looper/stop-loop!)

(defn- extract-drum-mode
  "Extracts optional drum character mode and returns [drum-mode remaining-args]."
  [args]
  (let [[x y & more] args]
    (cond
      ;; (stack! :mod :idm ...)
      (= x :mod)
      [y more]

      ;; (stack! :idm ...)
      (catalog/drum-mode? x)
      [x (cons y more)]

      ;; (stack! {:mod :idm ...} ...)
      (and (map? x) (:mod x))
      [(:mod x) (let [clean (dissoc x :mod)]
                  (if (seq clean)
                    (cons clean (cons y more))
                    (cons y more)))]

      :else
      [nil args])))

(defn- normalize-track-pairs
  "Normalizes track arguments into a sequence of [track-key pattern-spec] pairs."
  [args]
  (let [x (first args)]
    (cond
      (map? x) x
      (and (= 1 (count args)) (sequential? x) (sequential? (first x))) x
      :else args)))

(defn stack!
  "Launches multiple live loops simultaneously from variadic vectors, track pairs, or a track map.
  Supports setting drum mode via :mod keyword, {:mod :idm}, or track options.
  Examples:
    (stack!
      [:kick  (pat \"k . . .  k . . .\")]
      [:snare (pat \". . . .  s . . .\")])
    (stack! :idm
      [:kick  (pat \"k . . .\")]
      [:snare (pat \"s . . .\")])
    (stack! {:mod :idm}
      [:kick  (pat \"k . . .\")])
    (stack! {:kick (pat \"k . . .\") :snare (pat \"s . . .\")})"
  [& args]
  (let [[drum-mod raw-tracks] (extract-drum-mode args)
        pairs                 (normalize-track-pairs raw-tracks)
        tks                   (into #{} (map first) pairs)]
    (when drum-mod
      (transport/set-drum-mode! drum-mod))
    (when (some catalog/individual-drum? tks)
      (looper/stop-loop! :drums :drum :break :kit))
    (when (some catalog/composite-drums? tks)
      (doseq [k (filter catalog/individual-drum? (keys (:active-tracks @audio-state)))]
        (looper/stop-loop! k)))
    (doseq [[k spec] pairs]
      (when (and k spec) (looper/loop! k spec)))
    (mapv first pairs)))
