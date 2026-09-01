(ns app.audio.dsp.engine
  "Tone.js WebAudio engine initialization and audio context lifecycle."
  (:require ["tone" :as tone]
            [app.audio.dsp.instruments :refer [create-default-instruments!]]
            [app.audio.dsp.routing :refer [build-audio-graph!]]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx]]
            [app.utils.dom :as dom]))

(when (exists? js/window)
  (set! (.-Tone js/window) tone))

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

(defn- configure-audio-context!
  "Configures Tone.js audio context with device-adaptive latencyHint and lookahead buffer."
  []
  (when (exists? js/window)
    (let [target-hint (if (dom/mobile?) "playback" "interactive")
          lookahead   (dom/active-lookahead)
          ctx         (.-context tone)]
      (when (and ctx (not= (or (.-latencyHint ctx) (some-> ctx .-rawContext .-latencyHint)) target-hint))
        (try
          (tone/setContext (tone/Context. #js {:latencyHint target-hint
                                               :lookAhead   lookahead}))
          (catch js/Object _)))
      (when-let [active-ctx (.-context tone)]
        (set! (.-lookAhead active-ctx) lookahead)))))

(defn- configure-destination!
  "Initializes master destination volume and unmutes output."
  []
  (when (fn? (.-getDestination tone))
    (let [dest (tone/getDestination)]
      (set! (.-mute dest) false)
      (set! (.. dest -volume -value) 0.0))))

(defn- start-engine-graph!
  "Compiles the DSP routing graph, initializes default instruments, and registers nodes in engine-ctx."
  []
  (let [busses      (build-audio-graph!)
        instruments (create-default-instruments! busses)
        transport   (if (fn? (.-getTransport tone)) (tone/getTransport) (.-Transport tone))
        engine-map  (merge busses instruments {:transport transport})]
    (swap! engine-ctx assoc :tone engine-map)))

(defn- attach-state-auto-resume!
  "Automatically restores WebAudio playback if mobile OS puts context into interrupted or suspended state."
  []
  (when (exists? js/window)
    (try
      (when-let [ctx (.-context tone)]
        (let [raw-ctx    (or (.-rawContext ^js ctx) ctx)
              native-ctx (or (when raw-ctx (.-_nativeAudioContext ^js raw-ctx)) raw-ctx)]
          (when native-ctx
            (set! (.-onstatechange ^js native-ctx)
                  (fn []
                    (when (and (:active? @audio-state)
                               (not= (.-state ^js native-ctx) "running"))
                      (try (.resume ^js native-ctx) (catch js/Object _))))))))
      (catch js/Object _))))

(defn init-audio!
  "Idempotently initializes Tone.js audio context, lookahead buffer, and routing graph."
  []
  (resume-audio-context!)
  (when-not (:initialized? @audio-state)
    (try
      (configure-audio-context!)
      (configure-destination!)
      (start-engine-graph!)
      (attach-state-auto-resume!)
      (swap! audio-state assoc :initialized? true)
      (catch js/Object e
        (println "Failed to start Tone.js audio engine:" e)))))

(defn create-sequence
  "Creates and starts a Tone.Sequence scheduler object."
  [callback events step]
  (doto (tone/Sequence. callback events (or step cfg/default-step))
    (-> .-loop (set! true))
    (.start 0)))
