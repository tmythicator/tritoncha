(ns app.audio.dsp.worklet.transport
  "Low-level WebAudio AudioWorklet transport and WASM binary loader."
  (:require [app.config :as config]))

(defonce ^:private worklet-state
  (atom {:ctx nil :node nil :wasm-module nil :ready? false}))

(defonce ^:private pending-messages
  (atom []))

(defonce ^:private ready-callbacks
  (atom []))

(defn worklet-ready?
  "Returns true if the AudioWorklet WASM engine is initialized and ready.
  Examples: (worklet-ready?) -> true."
  []
  (:ready? @worklet-state))

(defn get-audio-context
  "Returns the active WebAudio AudioContext instance.
  Examples: (get-audio-context) -> #object[AudioContext]."
  []
  (:ctx @worklet-state))

(defn on-worklet-ready!
  "Registers a callback to execute when the AudioWorklet WASM engine reports ready.
  Examples: (on-worklet-ready! (fn [] (println \"Ready\")))."
  [cb]
  (if (:ready? @worklet-state)
    (cb)
    (swap! ready-callbacks conj cb)))

(defn send-msg!
  "Sends a JSON or JS message to the AudioWorklet processor message port.
  Queues message if processor node is not yet attached."
  [msg-js]
  (if-let [^js node (:node @worklet-state)]
    (.postMessage (.-port node) msg-js)
    (swap! pending-messages conj msg-js)))

(defn- flush-pending-messages!
  "Flushes any messages queued before the AudioWorklet node was attached."
  [^js node]
  (let [queued @pending-messages]
    (reset! pending-messages [])
    (doseq [msg queued]
      (.postMessage (.-port node) msg))))

(defn- notify-worklet-ready!
  "Marks worklet state as ready and invokes registered ready callbacks."
  []
  (swap! worklet-state assoc :ready? true)
  (let [cbs @ready-callbacks]
    (reset! ready-callbacks [])
    (doseq [cb cbs]
      (try (cb) (catch js/Object _)))))

(defn- fetch-wasm-bytes
  "Fetches the compiled Tritoncha DSP WebAssembly binary as an ArrayBuffer."
  [wasm-url]
  (-> (js/fetch wasm-url)
      (.then (fn [^js resp] (.arrayBuffer resp)))))

(defn- attach-worklet-node!
  "Constructs and connects the AudioWorkletNode, wiring its port messages."
  [^js audio-ctx ^js wasm-buffer]
  (let [node (js/AudioWorkletNode. audio-ctx config/wasm-processor-name
                                   #js {:numberOfInputs 0
                                        :numberOfOutputs 1
                                        :outputChannelCount #js [2]
                                        :processorOptions #js {:sampleRate (.-sampleRate audio-ctx)}})]
    (.connect node (.-destination audio-ctx))
    (set! (.-onmessage (.-port node))
          (fn [^js ev]
            (when (= (.-type (.-data ev)) "ready")
              (notify-worklet-ready!))))
    (.postMessage (.-port node) #js {:type "initWasm" :wasmBinary wasm-buffer :wasmBytes wasm-buffer})
    (swap! worklet-state assoc :node node)
    (flush-pending-messages! node)
    true))

(defn- create-audio-context
  "Instantiates and stores the native WebAudio context, resuming immediately if suspended."
  []
  (let [AudioCtx (or (.-AudioContext js/window) (.-webkitAudioContext js/window))
        ctx      (or (:ctx @worklet-state) (AudioCtx.))]
    (swap! worklet-state assoc :ctx ctx)
    (when (= (.-state ctx) "suspended")
      (.resume ctx))
    ctx))

(defn- resolve-asset-url
  "Resolves asset path relative to the active document base URL."
  [path]
  (if (exists? js/document.baseURI)
    (.-href (js/URL. path js/document.baseURI))
    path))

(defn init-audio-worklet!
  "Asynchronously loads WASM binary and attaches Tritoncha AudioWorklet processor."
  ([]
   (if (exists? js/window)
     (init-audio-worklet! (create-audio-context))
     (js/Promise.resolve false)))
  ([^js audio-ctx]
   (cond
     (nil? audio-ctx)
     (js/Promise.resolve false)

     (:node @worklet-state)
     (js/Promise.resolve (boolean (:ready? @worklet-state)))

     :else
     (do
       (swap! worklet-state assoc :ctx audio-ctx)
       (when (= (.-state audio-ctx) "suspended")
         (.resume audio-ctx))
       (let [worklet-url (resolve-asset-url config/worklet-script-path)
             wasm-url    (resolve-asset-url config/wasm-binary-path)]
         (-> (.addModule (.-audioWorklet audio-ctx) worklet-url)
             (.then #(fetch-wasm-bytes wasm-url))
             (.then #(attach-worklet-node! audio-ctx %))
             (.catch (fn [err]
                       (println "AudioWorklet init notice:" (.-message err))
                       false))))))))
