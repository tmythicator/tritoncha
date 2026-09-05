(ns app.audio.dsp.routing
  "Declarative DSP routing graph catalog, bus configuration, and live route application."
  (:require [app.audio.dsp.fx :as fx]
            [app.custom.routes :refer [user-routes]]
            [app.lib.routes :refer [core-routes]]
            [app.state :refer [audio-state repl-registry]]))

(defn register-routing!
  "Registers or updates a dynamic routing topology in the REPL registry.
  Examples: (register-routing! :dub-matrix {:busses [...] :routes {...}})."
  [routing-key spec]
  (swap! repl-registry assoc-in [:routings routing-key] spec)
  routing-key)

(defn all-routings
  "Returns a merged map of core built-in routings, user custom routings, and REPL routings."
  []
  (merge core-routes user-routes (:routings @repl-registry)))

(defn set-routing!
  "Switches the active DSP routing topology preset and applies its processors to the Rust WASM engine.
  Examples: (set-routing! :dub-echo), (route! :cyber-glitch)."
  [routing-key]
  (let [routings (all-routings)
        spec     (get routings routing-key)]
    (when spec
      (swap! audio-state assoc :current-routing routing-key)
      (let [{:keys [processors]} spec]
        ;; Apply Distortion / Overdrive
        (when-let [d (:distort processors)]
          (fx/set-distortion! (or (:distortion d) 0.35)))
        ;; Apply Bitcrusher
        (when-let [c (:crusher processors)]
          (fx/set-bitcrush! (or (:bits c) 8) (or (:sample-hold c) 1.0)))
        ;; Apply Chorus
        (when-let [ch (:chorus processors)]
          (fx/set-chorus! (or (:rate ch) 0.8) (or (:depth ch) 0.4) (or (:wet ch) 0.35)))
        ;; Apply Master Filter
        (when-let [f (:master-filter processors)]
          (fx/set-filter-cutoff! (or (:frequency f) 18000.0)))
        ;; Apply Delay
        (when-let [del (:delay processors)]
          (fx/set-delay-feedback! (or (:feedback del) 0.38))
          (when-let [w (:wet del)]
            (fx/set-reverb-wet! w)))
        ;; Apply Reverb
        (when-let [rev (:reverb processors)]
          (fx/set-reverb-wet! (or (:wet rev) 0.35))))
      routing-key)))
