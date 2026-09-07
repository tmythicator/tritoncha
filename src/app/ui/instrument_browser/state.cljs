(ns app.ui.instrument-browser.state
  "State management and live patch mutations for Instrument Studio."
  (:require
   [app.audio.dsp.instruments :as instruments]
   [app.custom.instruments :refer [user-instruments]]
   [app.state :refer [repl-registry]]
   [app.ui.instrument-browser.audition :as audition]
   [reagent.core :as r]))

(defonce active-category (r/atom :all))
(defonce search-query (r/atom ""))
(defonce selected-inst (r/atom :bass-analog))
(defonce patch-revision (r/atom 0))

;; Persistent custom instruments defined on disk in src/app/custom/instruments.cljs
(defn custom-file-inst?
  "Check whether instrument key is defined in custom/instruments.cljs on disk.
  Examples: (custom-file-inst? :sub-roller) -> boolean."
  [inst-key]
  (contains? user-instruments inst-key))

;; In-memory ephemeral session instruments or live modifications (resets on F5)
(defn session-inst?
  "Check whether instrument key has session modifications or is REPL defined.
  Examples: (session-inst? :saw-bass) -> boolean."
  [inst-key]
  (contains? (:instruments @repl-registry) inst-key))

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
