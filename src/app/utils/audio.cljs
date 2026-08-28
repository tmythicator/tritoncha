(ns app.utils.audio
  "Pitch parsing, note and MIDI conversions, WebAudio safety, and parameter ramping."
  (:require [clojure.string :as str]))

(def ^:private note-offsets
  {"C" 0 "C#" 1 "DB" 1 "D" 2 "D#" 3 "EB" 3 "E" 4 "F" 5
   "F#" 6 "GB" 6 "G" 7 "G#" 8 "AB" 8 "A" 9 "A#" 10 "BB" 10 "B" 11})

(def ^:private midi-names
  ["C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"])

(def ^:private note-regex
  #"([A-G][#B]?)(-?\d+)?")

(defn parse-note
  "Parses a note string or keyword into pitch name and octave.
  Examples: (parse-note \"C#4\") -> {:pitch \"C#\", :octave 4}, (parse-note :eb) -> {:pitch \"EB\", :octave 3}."
  [n-val & [default-oct]]
  (when n-val
    (let [s (-> (name n-val) str/trim str/upper-case)]
      (when-let [[_ pitch oct-str] (re-matches note-regex s)]
        {:pitch pitch
         :octave (if oct-str (js/parseInt oct-str 10) (or default-oct 3))}))))

(defn note->midi
  "Converts a note string or keyword to MIDI pitch number.
  Examples: (note->midi \"C4\") -> 60, (note->midi :eb2) -> 39, (note->midi :a4) -> 69."
  [n-val]
  (when n-val
    (if (number? n-val)
      n-val
      (when-let [{:keys [pitch octave]} (parse-note n-val)]
        (let [offset (get note-offsets pitch)]
          (when offset
            (+ (* (inc octave) 12) offset)))))))

(defn midi->note
  "Converts MIDI pitch number back to pitch string.
  Examples: (midi->note 60) -> \"C4\", (midi->note 39) -> \"D#2\", (midi->note 69) -> \"A4\"."
  [midi-num]
  (when (number? midi-num)
    (let [m (js/Math.round midi-num)
          pitch (get midi-names (mod m 12))
          oct (- (quot m 12) 1)]
      (str pitch oct))))

(defn format-key
  "Formats a musical key map into a clean uppercase string.
  Examples: (format-key {:root :e :mode :phrygian}) -> \"E PHRYGIAN\"."
  [{:keys [root mode]}]
  (str (str/upper-case (name (or root :e))) " " (str/upper-case (name (or mode :phrygian)))))

(defn normalize-opts
  "Normalizes octave numbers or option maps into a standard options map.
  Examples: (normalize-opts 2 1) -> {:octave 2}, (normalize-opts {:octave 3} 1) -> {:octave 3}, (normalize-opts nil 1) -> {:octave 1}."
  ([opts-or-oct] (normalize-opts opts-or-oct 3))
  ([opts-or-oct default-oct]
   (cond
     (map? opts-or-oct) opts-or-oct
     (number? opts-or-oct) {:octave opts-or-oct}
     :else {:octave default-oct})))

(defn enforce-stereo-mode!
  "Enforces explicit 2-channel stereo routing on WebAudio/Tone.js nodes to prevent dynamic allocation glitches."
  [^js node]
  (when node
    (try
      (doseq [field ["input" "output" "_filter" "_gainNode" "_node"]]
        (when-let [^js sub (aget node field)]
          (when (exists? (.-channelCount sub))
            (set! (.-channelCount sub) 2)
            (set! (.-channelCountMode sub) "explicit")
            (set! (.-channelInterpretation sub) "speakers"))))
      (when (exists? (.-channelCount node))
        (set! (.-channelCount node) 2)
        (set! (.-channelCountMode node) "explicit")
        (set! (.-channelInterpretation node) "speakers"))
      (catch js/Object _)))
  node)

(defn safe-ramp!
  "Safely ramps a WebAudio/Tone AudioParam with fallback to direct value assignment.
  Examples: (safe-ramp! (.-volume bus) -6 0.05)."
  [^js audio-param target-val ramp-time]
  (when audio-param
    (let [v (or target-val 0)
          t (or ramp-time 0.05)]
      (try
        (.rampTo audio-param v t)
        (catch js/Object _
          (set! (.-value audio-param) v))))))

(def ^:private drum-voice-set
  #{:kick :snare :sn-rs :sn-clk :sn-gh :sn-roll :hh-c :hh-o :hh-clk :click :drums :drum})

(def ^:private bass-track-set
  #{:bass :sub :saw-bass :acid :sub-sine :fm-growl :bass-lead :sub-bass})

(defn is-drum-track?
  "Returns true if the track key represents a drum/percussion track.
  Examples: (is-drum-track? :kick) -> true, (is-drum-track? :drums) -> true, (is-drum-track? :bass) -> false."
  [track-key]
  (contains? drum-voice-set (keyword track-key)))

(defn is-bass-track?
  "Returns true if the track key represents a bass or sub-bass track.
  Examples: (is-bass-track? :bass) -> true, (is-bass-track? :lead) -> false."
  [track-key]
  (contains? bass-track-set (keyword track-key)))
