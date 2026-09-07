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

(defn midi->freq
  "Converts a MIDI pitch number (0..127) to frequency in Hertz.
  Examples: (midi->freq 69) -> 440.0, (midi->freq 60) -> 261.6256."
  [midi-num]
  (when (number? midi-num)
    (* 440.0 (js/Math.pow 2.0 (/ (- midi-num 69.0) 12.0)))))

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

(defn dur->seconds
  "Converts a musical duration notation string, keyword, or number to seconds at given BPM.
  Supports standard musical notations: '1m', '1n', '2n', '4n', '8n', '16n', '32n', '8n.', '16t', etc.
  Examples: (dur->seconds \"16n\" 168) -> 0.089, (dur->seconds \"1m\" 168) -> 1.428."
  ([dur] (dur->seconds dur 168))
  ([dur bpm]
   (let [b (or bpm 168)
         beat-s (/ 60.0 b)]
     (cond
       (number? dur) (double dur)
       (nil? dur) (* beat-s 0.25)
       :else
       (let [s (-> (name dur) str/trim str/lower-case)]
         (case s
           ("1m" "measure" "bar") (* beat-s 4.0)
           ("1n" "whole")         (* beat-s 4.0)
           ("2n" "half")          (* beat-s 2.0)
           ("2n.")                (* beat-s 3.0)
           ("2t")                 (* beat-s (/ 4.0 3.0))
           ("4n" "quarter")       beat-s
           ("4n.")                (* beat-s 1.5)
           ("4t")                 (* beat-s (/ 2.0 3.0))
           ("8n" "eighth")        (* beat-s 0.5)
           ("8n.")                (* beat-s 0.75)
           ("8t")                 (* beat-s (/ 1.0 3.0))
           ("16n" "sixteenth")    (* beat-s 0.25)
           ("16n.")               (* beat-s 0.375)
           ("16t")                (* beat-s (/ 0.5 3.0))
           ("32n" "thirtysecond") (* beat-s 0.125)
           (* beat-s 0.25)))))))

(defn step->mult
  "Converts step string notation to step multiplier relative to base clock ticks (64th notes).
  Examples: (step->mult \"64n\") -> 1, (step->mult \"32n\") -> 2, (step->mult \"16n\") -> 4, (step->mult \"8n\") -> 8, (step->mult \"4n\") -> 16, (step->mult \"1m\") -> 64."
  [step-val]
  (case (str step-val)
    ("64n" "64" "64th") 1
    ("32n" "32" "32nd") 2
    ("16n" "16" "16th") 4
    ("8n" "8" "8th") 8
    ("4n" "4" "quarter" "1n") 16
    ("2n" "half") 32
    ("1m" "measure" "bar") 64
    4))
