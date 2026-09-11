(ns app.audio.theory.tables
  "Static music theory interval definitions for scales, modes, and electronic chord structures.")

(def scale-intervals
  "Mapping of scale/mode names to pitch interval offsets relative to root."
  {:major             [0 2 4 5 7 9 11]
   :ionian            [0 2 4 5 7 9 11]
   :dorian            [0 2 3 5 7 9 10]
   :phrygian          [0 1 3 5 7 8 10]
   :lydian            [0 2 4 6 7 9 11]
   :mixolydian        [0 2 4 5 7 9 10]
   :minor             [0 2 3 5 7 8 10]
   :aeolian           [0 2 3 5 7 8 10]
   :locrian           [0 1 3 5 6 8 10]

   :harmonic-minor    [0 2 3 5 7 8 11]
   :melodic-minor     [0 2 3 5 7 9 11]
   :hungarian-minor   [0 2 3 6 7 8 11]
   :neapolitan-minor  [0 1 3 5 7 8 11]

   :pentatonic-minor  [0 3 5 7 10]
   :pentatonic-major  [0 2 4 7 9]
   :blues             [0 3 5 6 7 10]
   :major-blues       [0 2 3 4 7 9]

   :hirajoshi         [0 2 3 7 8]
   :insen             [0 1 5 7 10]
   :iwato             [0 1 5 6 10]
   :kumoi             [0 2 3 7 9]
   :arabic            [0 1 4 5 7 8 11]
   :double-harmonic   [0 1 4 5 7 8 11]
   :persian           [0 1 4 5 6 8 11]

   :whole-tone        [0 2 4 6 8 10]
   :diminished        [0 2 3 5 6 8 9 11]
   :bebop-dominant    [0 2 4 5 7 9 10 11]})

(def chord-intervals
  "Mapping of chord quality names to pitch interval offsets relative to root."
  {;; Standard chords
   :maj         [0 4 7]
   :min         [0 3 7]
   :dim         [0 3 6]
   :aug         [0 4 8]
   :sus2        [0 2 7]
   :sus4        [0 5 7]
   :5           [0 7]
   :power       [0 7 12]
   :7           [0 4 7 10]
   :dom7        [0 4 7 10]
   :maj7        [0 4 7 11]
   :min7        [0 3 7 10]
   :m7          [0 3 7 10]
   :m7b5        [0 3 6 10]
   :dim7        [0 3 6 9]
   :9           [0 4 7 10 14]
   :maj9        [0 4 7 11 14]
   :min9        [0 3 7 10 14]
   :m9          [0 3 7 10 14]
   :m11         [0 3 7 10 14 17]

   ;; Electronic chords
   :dark-m9     [0 3 7 10 14]
   :dark-sus    [0 5 7 10 15]
   :saw-fifth   [0 7 12 19]})
