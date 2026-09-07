(ns app.audio.dsp.worklet.compiler
  "Pure compilers and parsers for score notation, articulation, and DSP patches."
  (:require [app.audio.dsp.busses :refer [find-instrument-spec]]
            [app.audio.dsp.worklet.protocol :refer [bus-key->id drum-mod->id filter-type->id
                                                    inst-keyword->id osc-type->id]]
            [app.lib.drums :refer [drum-keyword? mini-notation-aliases]]
            [app.utils.audio :refer [midi->freq note->midi]]
            [clojure.string :as str]))

(defn parse-freq
  "Parses note name, MIDI number, or raw frequency into frequency in Hertz.
  Examples: (parse-freq \"A4\") -> 440.0, (parse-freq 69) -> 440.0."
  [pitch]
  (cond
    (number? pitch) (if (> pitch 127) pitch (midi->freq pitch))
    (string? pitch) (if-let [m (note->midi pitch)] (midi->freq m) 440.0)
    (keyword? pitch) (if-let [m (note->midi (name pitch))] (midi->freq m) 440.0)
    :else 440.0))

(defn parse-midi-note
  "Parses note name or number into integer MIDI pitch number or -1 for rests.
  Examples: (parse-midi-note \"C4\") -> 60, (parse-midi-note nil) -> -1."
  [pitch]
  (cond
    (nil? pitch) -1
    (number? pitch) (int pitch)
    (string? pitch) (if-let [m (note->midi pitch)] (int m) -1)
    (keyword? pitch) (if-let [m (note->midi (name pitch))] (int m) -1)
    :else -1))

(defn extract-articulation
  "Splits a string or keyword into [clean-token vel].
  Supports '!' suffix for accents and '_' suffix for ghost notes.
  Examples: (extract-articulation :snare!) -> [\"snare\" 1.15]."
  ([token] (extract-articulation token 0.9))
  ([token default-vel]
   (let [s        (if (keyword? token) (name token) (str token))
         base-vel (float (or default-vel 0.9))]
     (cond
       (str/ends-with? s "!")
       [(subs s 0 (dec (count s))) (min 1.25 (* base-vel (/ 1.15 0.9)))]

       (and (> (count s) 1) (str/ends-with? s "_"))
       [(subs s 0 (dec (count s))) (* base-vel (/ 0.35 0.9))]

       :else
       [s base-vel]))))

(defn parse-step-hit
  "Parses a step hit into {:inst-id :note :vel} map respecting default-vel.
  Examples: (parse-step-hit \"C3\" :bass 0.4) -> {:inst-id 4, :note 48, :vel 0.4}."
  ([hit default-inst-key] (parse-step-hit hit default-inst-key 0.9))
  ([hit default-inst-key default-vel]
   (let [def-v (float (or default-vel 0.9))]
     (cond
       (nil? hit)
       {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}

       (boolean? hit)
       (if hit
         {:inst-id (inst-keyword->id default-inst-key) :note 60 :vel def-v}
         {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0})

       (and (vector? hit) (keyword? (first hit)))
       (let [[k v n]           hit
             [clean-k art-vel] (extract-articulation k def-v)]
         {:inst-id (inst-keyword->id (keyword clean-k))
          :note    (if n (parse-midi-note n) 60)
          :vel     (float (or v art-vel def-v))})

       (vector? hit)
       {:inst-id (inst-keyword->id default-inst-key)
        :note    (if (seq hit) (parse-midi-note (first hit)) -1)
        :vel     (if (seq hit) def-v 0.0)}

       (keyword? hit)
       (let [[clean-name art-vel] (extract-articulation hit def-v)
             resolved-alias       (get mini-notation-aliases clean-name)
             clean-kw             (or resolved-alias (keyword clean-name))]
         (cond
           (contains? #{:_ :- :rest :nil :none} clean-kw)
           {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}

           (drum-keyword? clean-kw)
           {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)}

           :else
           (if-let [m (note->midi clean-name)]
             {:inst-id (inst-keyword->id default-inst-key) :note (int m) :vel (float art-vel)}
             {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)})))

       (number? hit)
       (if (neg? hit)
         {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}
         {:inst-id (inst-keyword->id default-inst-key) :note (int hit) :vel def-v})

       (string? hit)
       (let [[clean-name art-vel] (extract-articulation hit def-v)
             resolved-alias       (get mini-notation-aliases clean-name)
             clean-kw             (or resolved-alias (keyword clean-name))]
         (cond
           (contains? #{:_ :- :rest :nil :none "." "0"} clean-name)
           {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}

           (drum-keyword? clean-kw)
           {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)}

           :else
           (let [m (parse-midi-note clean-name)]
             (if (neg? m)
               {:inst-id (inst-keyword->id clean-kw) :note 60 :vel (float art-vel)}
               {:inst-id (inst-keyword->id default-inst-key) :note m :vel (float art-vel)}))))

       :else
       {:inst-id (inst-keyword->id default-inst-key) :note -1 :vel 0.0}))))

(defn compile-voice-patch-msg
  "Compiles a declarative Clojure synth patch map into a WebAudio postMessage payload.
  Examples: (compile-voice-patch-msg 4 {:osc {:type :saw}}) -> #js {:type \"setVoicePatch\" ...}."
  [patch-id patch-spec]
  (let [pid       (int patch-id)
        osc       (:osc patch-spec)
        flt       (:filter patch-spec)
        amp       (:amp-env patch-spec)
        mod-e     (:mod-env patch-spec)
        pitch-e   (:pitch-env patch-spec)
        bus-id    (int (bus-key->id (or (:bus patch-spec)
                                        (case (:category patch-spec)
                                          :bass :bus/bass
                                          :pads :bus/space
                                          :drums :bus/drums
                                          :bus/lead))))
        poly      (int (if (= (:type patch-spec) :mono) 1 8))]
    #js {:type           "setVoicePatch"
         :patchId        pid
         :oscType        (osc-type->id (:type osc :saw))
         :subLevel       (float (:sub-level osc 0.0))
         :pulseWidth     (float (:pulse-width osc 0.5))
         :filterType     (filter-type->id (:type flt :lowpass))
         :cutoffBase     (float (:cutoff flt 1200.0))
         :cutoffEnvAmt   (float (:env-amount flt 3000.0))
         :cutoffKeyTrack (float (:key-track flt 2.0))
         :resonance      (float (:q flt 0.4))
         :attack         (float (:attack amp 0.005))
         :decay          (float (:decay amp 0.2))
         :sustain        (float (:sustain amp 0.3))
         :release        (float (:release amp 0.2))
         :modAttack      (float (:attack mod-e (:attack amp 0.005)))
         :modDecay       (float (:decay mod-e (:decay amp 0.2)))
         :busId          bus-id
         :polyphony      poly
         :glide          (float (:glide patch-spec 0.0))
         :filterDrive    (float (:drive flt 0.0))
         :noiseLevel     (float (:noise osc 0.0))
         :pitchEnvAmt    (float (:amount pitch-e 0.0))
         :pitchEnvDecay  (float (:decay pitch-e 0.015))
         :analogDrift    (float (:drift osc 0.0))}))

(defn compile-drum-patch-msg
  "Compiles a drum sound design map into a setDrumPatch message payload.
  Examples: (compile-drum-patch-msg :kick {:decay 0.3}) -> #js {:type \"setDrumPatch\" ...}."
  [drum-key drum-spec]
  (let [dk      (keyword drum-key)
        spec    (or drum-spec (find-instrument-spec dk))
        type    (or (:type spec) dk)
        inst-id (inst-keyword->id dk)
        mod-id  (drum-mod->id (:mod spec))]
    (when-let [[drum-id params]
               (case type
                 :kick
                 [0 [(or (:base-pitch spec) (:pitch spec) 48.0)
                     (or (:pitch-drop spec) (:snap spec) 180.0)
                     (or (:pitch-decay spec) (:sweep spec) 0.040)
                     (or (:decay spec) 0.28)
                     (or (:click spec) 0.35)
                     (or (:drive spec) 1.6)]]

                 :snare
                 [1 [(or (:base-freq spec) (:pitch spec) 185.0)
                     (or (:tone-decay spec) 0.9985)
                     (or (:noise-decay spec) 0.9991)
                     (or (:cutoff spec) 2400.0)
                     (or (:snappy spec) (:noise spec) 0.85)
                     0.0]]

                 :hat
                 [2 [(or (:cutoff spec) 7200.0)
                     (or (:decay-closed spec) (:decay spec) 0.04)
                     (or (:decay-open spec) 0.24)
                     0.0 0.0 0.0]]

                 :membrane
                 [inst-id [(or (:start-pitch spec) 180.0)
                           (or (:min-pitch spec) 105.0)
                           (or (:pitch-decay spec) 0.015)
                           (or (:decay spec) 0.40)
                           (or (:drive spec) 1.10)
                           0.0]]

                 :metallic
                 [inst-id [(or (:cutoff spec) 3500.0)
                           (or (:resonance spec) 0.35)
                           (or (:decay spec) 0.85)
                           (or (:drive spec) 1.0)
                           0.0 0.0]]

                 :clap
                 [18 [(or (:cutoff spec) 1200.0)
                      (or (:resonance spec) 0.70)
                      (or (:decay spec) 0.28)
                      (or (:drive spec) 1.0)
                      0.0 0.0]]

                 nil)]
      (let [[p0 p1 p2 p3 p4 p5] params]
        #js {:type   "setDrumPatch"
             :drumId drum-id
             :p0     (float (or p0 0.0))
             :p1     (float (or p1 0.0))
             :p2     (float (or p2 0.0))
             :p3     (float (or p3 0.0))
             :p4     (float (or p4 0.0))
             :p5     (float (or p5 0.0))
             :p6     mod-id}))))
