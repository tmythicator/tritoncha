(ns app.audio.control.pattern
  "Canonical specifications, data contracts, and zero-overhead validation for audio track patterns."
  (:require [cljs.spec.alpha :as s]))

(def track-pattern-schema
  "Declarative schema specification for canonical active track patterns in Tritoncha."
  {:inst   {:type :keyword  :doc "Instrument identifier (e.g. :bass-analog, :kick)" :required true}
   :notes  {:type :vector   :doc "Sequence of step pitch strings, chord vectors, or nil for rests" :required true}
   :step   {:type :string   :doc "Sequencer grid division ('16n', '8n', '4n', '32n')" :required true}
   :dur    {:type :string   :doc "Gate trigger duration in musical time ('16n', '8n')" :required true}
   :vel    {:type :velocity :doc "Note velocity (0.0 to 1.0) or vector of step velocities" :required true}
   :oct    {:type :integer  :doc "Octave offset override" :required false}
   :bus    {:type :keyword  :doc "Custom DSP bus override (:bus/drums, :bus/space, etc.)" :required false}
   :mod    {:type :keyword  :doc "Drum character synthesis mode (:idm, :analog, etc.)" :required false}
   :muted? {:type :boolean  :doc "Mixer mute state" :required false}
   :solo?  {:type :boolean  :doc "Mixer solo state" :required false}})

(s/def ::inst keyword?)
(s/def ::notes (s/coll-of any? :kind vector?))
(s/def ::step string?)
(s/def ::dur string?)
(s/def ::vel (s/or :num number? :vec (s/coll-of number? :kind vector?)))

(s/def ::oct (s/nilable integer?))
(s/def ::bus (s/nilable keyword?))
(s/def ::mod (s/nilable keyword?))
(s/def ::muted? (s/nilable boolean?))
(s/def ::solo? (s/nilable boolean?))
(s/def ::mask (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::mask-vec (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::vel-vec (s/nilable (s/coll-of number? :kind vector?)))
(s/def ::deg (s/nilable (s/coll-of any? :kind vector?)))
(s/def ::progression (s/nilable any?))

(s/def ::track-pattern
  (s/keys :req-un [::inst ::notes ::step ::dur ::vel]
          :opt-un [::oct ::bus ::mod ::muted? ::solo? ::mask ::mask-vec ::vel-vec ::deg ::progression]))


(defn valid-pattern?
  "Returns true if the map conforms to the canonical track pattern specification.
  Examples: (valid-pattern? {:inst :bass :notes ['C2'] :step '16n' :dur '16n' :vel 0.9})."
  [pat]
  (boolean (and (map? pat) (s/valid? ::track-pattern pat))))

(defn explain-pattern
  "Returns a detailed explanation of why a pattern map fails the track pattern specification, or nil if valid.
  Examples: (explain-pattern {:inst :bass})."
  [pat]
  (when-not (s/valid? ::track-pattern pat)
    (s/explain-data ::track-pattern pat)))
