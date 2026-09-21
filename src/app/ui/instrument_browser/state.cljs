(ns app.ui.instrument-browser.state
  "State management and live patch mutations for Instrument Studio."
  (:require
   [app.audio.dsp.busses :as busses]
   [app.audio.dsp.instruments :as instruments]
   [app.audio.dsp.worklet :as worklet]
   [app.custom.drums :refer [user-drums]]
   [app.custom.synth :refer [user-synths]]
   [app.lib.drums :refer [core-drums]]
   [app.lib.synth :refer [core-synths]]
   [app.state :refer [repl-registry]]
   [app.ui.instrument-browser.audition :as audition]
   [reagent.core :as r]))

(defonce active-category (r/atom :all))
(defonce search-query (r/atom ""))
(defonce selected-inst (r/atom :bass-analog))
(defonce patch-revision (r/atom 0))

(defn custom-file-inst?
  "Check whether instrument key is defined in custom/synth.cljs or custom/drums.cljs on disk.
  Examples: (custom-file-inst? :fat-kick) -> boolean."
  [inst-key]
  (or (contains? user-synths inst-key)
      (contains? user-drums inst-key)))

(defn session-inst?
  "Check whether instrument key has session modifications or is REPL defined.
  Examples: (session-inst? :saw-bass) -> boolean."
  [inst-key]
  (let [repl-spec (get (:instruments @repl-registry) inst-key)
        canonical (get instruments/instrument-aliases inst-key inst-key)
        base-spec (or (get user-synths inst-key)
                      (get user-drums inst-key)
                      (get core-synths inst-key)
                      (get core-drums inst-key)
                      (get user-synths canonical)
                      (get user-drums canonical)
                      (get core-synths canonical)
                      (get core-drums canonical))]
    (and (some? repl-spec)
         (or (nil? base-spec)
             (not= repl-spec base-spec)))))

(defn inst-matches-category?
  "Predicate checking if instrument belongs to active category filter tab.
  Examples: (inst-matches-category? :saw-bass spec :bass) -> true."
  [inst-key spec cat-filter]
  (case cat-filter
    :all     true
    :custom  (custom-file-inst? inst-key)
    :session (session-inst? inst-key)
    (= (audition/sound-family inst-key spec) cat-filter)))

(defn select-instrument!
  "Select instrument and update audition loop if currently running.
  Examples: (select-instrument! :pad) -> nil."
  [inst-key]
  (reset! selected-inst inst-key)
  (let [spec (instruments/resolve-instrument-spec inst-key)]
    (when (busses/drum? (or spec inst-key))
      (worklet/set-worklet-drum-patch! inst-key spec)))
  (when @audition/audition-loop-active?
    (audition/start-audition-loop! inst-key)))

(defn patch-param!
  "Patch single root parameter or nested section parameter.
  Examples: (patch-param! :lead :bus :bus/space cur-spec) -> nil."
  ([inst-key root-param val _cur-spec]
   (instruments/patch! inst-key root-param val)
   (swap! patch-revision inc)
   (audition/touch-preview-note! inst-key))
  ([inst-key section-key param-key val cur-spec]
   (instruments/patch! inst-key section-key
                       (assoc (or (get cur-spec section-key) {}) param-key val))
   (swap! patch-revision inc)
   (audition/touch-preview-note! inst-key)))

(defn reset-param!
  "Reset instrument to baseline definition.
  Examples: (reset-param! :saw-bass) -> nil."
  [inst-key]
  (instruments/reset-instrument! inst-key)
  (swap! patch-revision inc)
  (audition/touch-preview-note! inst-key))
