(ns app.audio.dsp.engine
  "Native WebAudio context lifecycle and AudioWorklet engine initialization."
  (:require [app.audio.dsp.instruments :refer [reload-instruments!]]
            [app.audio.dsp.worklet :as worklet]
            [app.state :refer [audio-state]]))

(defn resume-audio-context!
  "Resumes the WebAudio context if currently suspended."
  []
  (when (exists? js/window)
    (try
      (when-let [^js ctx (worklet/get-audio-context)]
        (when (= (.-state ctx) "suspended")
          (.resume ctx)))
      (catch js/Object _))))

(defn- attach-state-auto-resume!
  "Automatically restores WebAudio playback if mobile OS puts context into interrupted or suspended state."
  [^js ctx]
  (when (and (exists? js/window) ctx)
    (try
      (set! (.-onstatechange ctx)
            (fn []
              (when (and (:active? @audio-state)
                         (not= (.-state ctx) "running"))
                (try (.resume ctx) (catch js/Object _)))))
      (catch js/Object _))))

(defn init-audio!
  "Idempotently initializes the native WebAudio context and Rust WASM AudioWorklet engine."
  []
  (when (exists? js/window)
    (when-not (:initialized? @audio-state)
      (try
        (let [promise (worklet/init-audio-worklet!)]
          (.then promise
                 (fn []
                   (when-let [^js ctx (worklet/get-audio-context)]
                     (attach-state-auto-resume! ctx)
                     (when (= (.-state ctx) "suspended")
                       (.resume ctx)))
                   (reload-instruments!)
                   (swap! audio-state assoc :initialized? true))))
        (catch js/Object e
          (println "Failed to start WebAudio engine:" e))))
    (resume-audio-context!)))
