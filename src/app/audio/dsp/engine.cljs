(ns app.audio.dsp.engine
  "Tone.js WebAudio engine initialization and audio context lifecycle."
  (:require ["tone" :as tone]
            [app.audio.dsp.instruments :refer [create-default-instruments!]]
            [app.audio.dsp.routing :refer [build-audio-graph!]]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx]]
            [app.utils.dom :refer [active-lookahead]]))

(defn resume-audio-context!
  "Resumes the WebAudio context if currently suspended."
  []
  (try
    (when-let [ctx (or (.-context tone)
                       (some-> (:tone @engine-ctx) :transport .-context))]
      (when (and ctx (not= (.-state ctx) "running"))
        (.resume ctx)))
    (tone/start)
    (catch js/Object _)))

(defn init-audio!
  "Idempotently initializes Tone.js audio context, lookahead buffer, and routing graph."
  []
  (resume-audio-context!)
  (when-not (:initialized? @audio-state)
    (try
      (let [ctx (.-context tone)]
        (when ctx
          (set! (.-lookAhead ctx) (active-lookahead)))
        (when (fn? (.-getDestination tone))
          (let [dest (tone/getDestination)]
            (set! (.-mute dest) false)
            (set! (.. dest -volume -value) 0.0)))
        (let [busses      (build-audio-graph!)
              instruments (create-default-instruments! busses)
              transport   (if (fn? (.-getTransport tone)) (tone/getTransport) (.-Transport tone))
              engine-map  (merge busses instruments {:transport transport})]
          (swap! engine-ctx assoc :tone engine-map)
          (swap! audio-state assoc :initialized? true)))
      (catch js/Object e
        (println "Failed to start Tone.js audio engine:" e)))))

(defn create-sequence
  "Creates and starts a Tone.Sequence scheduler object."
  [callback events step]
  (doto (tone/Sequence. callback events (or step cfg/default-step))
    (-> .-loop (set! true))
    (.start 0)))
