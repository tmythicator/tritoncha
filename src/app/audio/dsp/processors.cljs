(ns app.audio.dsp.processors
  "DSP audio processor specifications, parameter compilation, Worklet insert serialization, and engine automation."
  (:require [app.audio.dsp.fx :as fx]))

;; Neutral and Default Baseline Specifications

(def neutral-processors
  "Neutral DSP processor parameter map with zero distortion, full open filter, and bypass levels."
  {:distort    {:distortion 0.0 :algorithm :adaa}
   :crusher    {:bits 16.0 :sample-hold 1.0}
   :chorus     {:rate 0.8 :depth 0.4 :wet 0.0}
   :filter     {:frequency 18000.0 :q 0.0}
   :delay      {:time "8n." :feedback 0.35 :wet 0.0}
   :reverb     {:room-size 0.75 :wet 0.0 :algorithm :fdn}
   :compressor {:enabled false :threshold -12.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 0.0}})

(def default-active-specs
  "Default parameters applied when a processor is activated in a routing graph without custom arguments."
  {:filter     {:frequency 12000.0 :q 0.0}
   :distort    {:drive 0.20 :algo :adaa :wet 0.5}
   :crusher    {:bits 8.0 :sample-hold 1.0 :wet 0.5}
   :chorus     {:rate 0.8 :depth 0.4 :wet 0.3}
   :compressor {:threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
   :delay      ["8n." 0.35 0.25]
   :reverb     {:algo :fdn :size 0.75 :wet 0.35}})

;; Individual Processor Parameter Compilers

(defn compile-filter-spec
  "Compiles filter parameters from number, vector, or map into canonical spec map."
  [v]
  (cond
    (number? v)
    {:type :filter :frequency (float v) :q 0.0 :filter-type "lowpass"}

    (vector? v)
    {:type :filter :frequency (float (first v)) :q (float (or (second v) 0.0)) :filter-type "lowpass"}

    (map? v)
    {:type        :filter
     :filter-type "lowpass"
     :frequency   (float (or (:frequency v) (:cutoff v) (:cutoff-hz v) (:freq v) 18000.0))
     :q           (float (or (:q v) (:resonance v) 0.0))}

    :else nil))

(defn compile-distort-spec
  "Compiles distortion parameters from number or map into canonical spec map."
  [v]
  (cond
    (number? v)
    {:type :distortion :distortion (float v) :algorithm :adaa :bits 16.0 :sample-hold 1.0 :wet 1.0}

    (map? v)
    {:type        :distortion
     :distortion  (float (or (:drive v) (:distortion v) 0.0))
     :algorithm   (or (:algo v) (:algorithm v) :adaa)
     :bits        (float (or (:bits v) 16.0))
     :sample-hold (float (or (:sample-hold v) (:sampleHold v) 1.0))
     :wet         (float (or (:wet v) 1.0))}

    :else nil))

(defn compile-crusher-spec
  "Compiles bitcrusher parameters from number, vector, or map into canonical spec map."
  [v]
  (cond
    (number? v)
    {:type :bitcrusher :bits (float v) :sample-hold 1.0 :wet 1.0}

    (vector? v)
    {:type        :bitcrusher
     :bits        (float (or (first v) 16.0))
     :sample-hold (float (or (second v) 1.0))
     :wet         (float (or (nth v 2 nil) 1.0))}

    (map? v)
    {:type        :bitcrusher
     :bits        (float (or (:bits v) 16.0))
     :sample-hold (float (or (:sample-hold v) (:sampleHold v) 1.0))
     :drive       (float (or (:drive v) (:distortion v) 0.0))
     :wet         (float (or (:wet v) 1.0))}

    :else nil))

(defn compile-chorus-spec
  "Compiles chorus parameters from number or map into canonical spec map."
  [v]
  (cond
    (number? v)
    {:type :chorus :rate 0.8 :depth 0.4 :wet (float v)}

    (map? v)
    {:type  :chorus
     :rate  (float (or (:rate v) (:rate-hz v) 0.8))
     :depth (float (or (:depth v) 0.4))
     :wet   (float (or (:wet v) (:mix v) 0.0))}

    :else nil))

(defn compile-delay-spec
  "Compiles stereo delay parameters from number, vector, or map into canonical spec map."
  [v]
  (cond
    (number? v)
    {:type     :delay
     :time     (float v)
     :feedback 0.35
     :wet      0.25}

    (vector? v)
    {:type     :delay
     :time     (str (or (first v) "8n."))
     :feedback (float (or (second v) 0.35))
     :wet      (float (or (nth v 2 nil) 0.25))}

    (map? v)
    {:type     :delay
     :time     (str (or (:time v) "8n."))
     :feedback (float (or (:feedback v) (:fb v) 0.35))
     :wet      (float (or (:wet v) 0.0))}

    :else nil))

(defn compile-reverb-spec
  "Compiles reverb parameters from number or map into canonical spec map."
  [v]
  (cond
    (number? v)
    {:type :reverb :algorithm :fdn :roomSize 0.75 :wet (float v)}

    (map? v)
    (let [algo (or (:algo v) (:algorithm v) :fdn)]
      {:type      (if (= algo :freeverb) :freeverb :reverb)
       :algorithm algo
       :roomSize  (float (or (:size v) (:room-size v) 0.75))
       :wet       (float (or (:wet v) 0.35))})

    :else nil))

(defn compile-compressor-spec
  "Compiles compressor parameters from boolean or map into canonical spec map."
  [v]
  (cond
    (boolean? v)
    {:type :compressor :enabled v :threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}

    (map? v)
    (merge {:type :compressor :enabled (get v :enabled true)
            :threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
           v)

    :else nil))

;; Bus Insert Serialization for Rust WASM Worklet

(defn compile-bus-insert
  "Compiles a processor keyword and its parameters into the map format expected by the Rust WASM worklet."
  [proc-key proc-specs]
  (let [spec (get proc-specs proc-key)]
    (case proc-key
      :filter
      {:type      "filter"
       :cutoffHz  (float (or (:frequency spec) (:cutoff spec) (:cutoff-hz spec) 18000.0))
       :resonance (float (or (:resonance spec) (:q spec) 0.0))}

      :delay
      {:type     "delay"
       :timeS    (float (or (when-let [t (or (:time spec) (:time-s spec))]
                              (fx/parse-delay-time-s t))
                            0.35))
       :feedback (float (or (:feedback spec) (:fb spec) 0.35))
       :wet      (float (or (:wet spec) 0.30))}

      :distort
      {:type       "distort"
       :drive      (float (or (:drive spec) (:distortion spec) 0.0))
       :bits       (float (or (:bits spec) 16.0))
       :sampleHold (float (or (:sample-hold spec) (:sampleHold spec) 1.0))}

      (:crusher :bitcrusher)
      {:type       "distort"
       :drive      (float (or (:drive spec) 0.0))
       :bits       (float (or (:bits spec) 8.0))
       :sampleHold (float (or (:sample-hold spec) (:sampleHold spec) 1.0))}

      :chorus
      {:type   "chorus"
       :rateHz (float (or (:rate spec) (:rate-hz spec) 0.8))
       :depth  (float (or (:depth spec) 0.4))
       :mix    (float (or (:mix spec) (:wet spec) 0.30))}

      :reverb
      {:type     "reverb"
       :roomSize (float (or (:roomSize spec) (:room-size spec) (:size spec) 0.75))
       :wet      (float (or (:wet spec) 0.35))}

      :compressor
      {:type        "compressor"
       :thresholdDb (float (or (:threshold spec) (:threshold-db spec) -12.0))
       :ratio       (float (or (:ratio spec) 4.0))
       :attackS     (float (or (:attack spec) (:attack-s spec) 0.010))
       :releaseS    (float (or (:release spec) (:release-s spec) 0.100))
       :makeupDb    (float (or (:makeup spec) (:makeup-db spec) 2.5))
       :mix         (float (or (:mix spec) 1.0))}

      (:limiter :limitter)
      {:type        "compressor"
       :thresholdDb (float (or (:threshold spec) (:threshold-db spec) -1.0))
       :ratio       20.0
       :attackS     0.001
       :releaseS    0.050
       :makeupDb    0.0
       :mix         1.0}

      nil)))

;; Batch Compilation and Engine Automation

(defn compile-processors
  "Compiles a map of declared and fallback processor specifications for a routing graph."
  [active-proc-keys proc-defs]
  (let [f-spec  (when (some #{:filter} active-proc-keys)
                  (compile-filter-spec (or (:filter proc-defs) (:filter default-active-specs))))
        d-spec  (when (some #{:distort} active-proc-keys)
                  (compile-distort-spec (or (:distort proc-defs) (:distort default-active-specs))))
        c-spec  (when (some #{:crusher} active-proc-keys)
                  (compile-crusher-spec (or (:crusher proc-defs) (:crusher default-active-specs))))
        ch-spec (when (some #{:chorus} active-proc-keys)
                  (compile-chorus-spec (or (:chorus proc-defs) (:chorus default-active-specs))))
        dl-spec (when (some #{:delay} active-proc-keys)
                  (compile-delay-spec (or (:delay proc-defs) (:delay default-active-specs))))
        rv-spec (when (some #{:reverb} active-proc-keys)
                  (compile-reverb-spec (or (:reverb proc-defs) (:reverb default-active-specs))))
        cp-spec (when (some #{:compressor} active-proc-keys)
                  (compile-compressor-spec (or (:compressor proc-defs) (:compressor default-active-specs))))]
    (cond-> {:limiter {:type :limiter :threshold -1.0}}
      f-spec  (assoc :filter f-spec)
      d-spec  (assoc :distort d-spec)
      c-spec  (assoc :crusher c-spec)
      ch-spec (assoc :chorus ch-spec)
      dl-spec (assoc :delay dl-spec)
      rv-spec (assoc :reverb rv-spec)
      cp-spec (assoc :compressor cp-spec))))

(defn apply-processor!
  "Applies a declared DSP processor configuration map into the Rust WASM engine."
  [p-type spec]
  (case p-type
    :distort
    (do
      (fx/set-distortion! (or (:distortion spec) (:drive spec) 0.0))
      (fx/set-drive-mode! (or (:algorithm spec) :adaa)))

    :crusher
    (fx/set-bitcrush! (or (:bits spec) 16.0) (or (:sample-hold spec) 1.0))

    :chorus
    (fx/set-chorus! (or (:rate spec) 0.8) (or (:depth spec) 0.4) (or (:wet spec) 0.0))

    :filter
    (let [freq (or (:frequency spec) (:cutoff spec) 18000.0)
          q    (or (:q spec) (:resonance spec) 0.0)]
      (fx/set-filter-cutoff! freq)
      (fx/set-filter-q! q))

    :delay
    (fx/set-delay! (or (:time spec) "8n.") (or (:feedback spec) 0.35) (or (:wet spec) 0.0))

    :reverb
    (do
      (fx/set-reverb! (or (:roomSize spec) (:room-size spec) 0.75) (or (:wet spec) 0.0))
      (fx/set-reverb-mode! (or (:algorithm spec) :fdn)))

    :compressor
    (fx/set-compressor! spec)

    nil))

(defn apply-processors!
  "Applies all processor parameters in proc-specs (merged with neutral fallbacks) into the Rust WASM engine."
  [proc-specs]
  (let [dsp (merge-with merge neutral-processors proc-specs)]
    (doseq [[p-type p-spec] dsp]
      (apply-processor! p-type p-spec))))
