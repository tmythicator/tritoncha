(ns app.audio.dsp.busses
  "Audio bus registry, normalization, routing mappings, predicates, and gain automation."
  (:require [app.config :as cfg]
            [app.state :refer [engine-ctx]]
            [app.utils.audio :refer [safe-ramp!]]
            [app.utils.math :refer [db->gain]]))

(def valid-busses
  #{:bus/master :bus/direct :bus/drums :bus/bass :bus/space :bus/glitch})

(def default-instrument-busses
  {:kick           :bus/drums
   :snare          :bus/drums
   :sn-rs          :bus/drums
   :sn-clk         :bus/drums
   :sn-gh          :bus/drums
   :sn-roll        :bus/drums
   :hh-c           :bus/drums
   :hh-o           :bus/drums
   :hh-clk         :bus/drums
   :drums          :bus/drums
   :click          :bus/direct

   :saw-bass       :bus/bass
   :sub-sine       :bus/bass
   :acid-bass      :bus/bass
   :bass           :bus/bass
   :sub            :bus/bass
   :acid           :bus/bass

   :dark-pad       :bus/space
   :pad            :bus/space
   :ambient-glass  :bus/space
   :glass          :bus/space
   :fm-synth       :bus/space
   :fm             :bus/space
   :lead           :bus/space
   :poly           :bus/space
   :siren          :bus/space

   :glitch         :bus/glitch
   :glitch-texture :bus/glitch
   :noise          :bus/glitch
   :hihat          :bus/glitch})

(defn normalize-bus-key
  "Ensures a keyword is in the :bus/<name> format.
  Examples: (normalize-bus-key :drums) -> :bus/drums, (normalize-bus-key :bus/bass) -> :bus/bass."
  [k]
  (when k
    (if (keyword? k)
      (if (= (namespace k) "bus") k (keyword "bus" (name k)))
      (let [s (str k)]
        (if (.startsWith s "bus/") (keyword s) (keyword "bus" (name s)))))))

(defn valid-bus?
  "Checks if a keyword represents a valid registered audio bus."
  [k]
  (contains? valid-busses (normalize-bus-key k)))

(defn instrument-bus
  "Resolves the destination bus keyword for an instrument key (defaulting to :bus/master)."
  [inst-key]
  (get default-instrument-busses inst-key :bus/master))

(defn set-bus-gain!
  "Ramps the input gain of an audio bus in decibels.
  Examples: (set-bus-gain! :bus/drums -6 0.05), (set-bus-gain! :bus/bass 0)."
  ([bus-key db-val] (set-bus-gain! bus-key db-val cfg/default-ramp-time))
  ([bus-key db-val ramp-time]
   (let [b-key (normalize-bus-key bus-key)]
     (when-let [chain (get-in (:tone @engine-ctx) [:busses b-key])]
       (when-let [^js in-node (or (:input chain) chain)]
         (cond
           (exists? (.-gain in-node))   (safe-ramp! (.-gain in-node) (db->gain db-val) ramp-time)
           (exists? (.-volume in-node)) (safe-ramp! (.-volume in-node) db-val ramp-time)))))))
