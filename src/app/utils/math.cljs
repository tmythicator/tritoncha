(ns app.utils.math
  "Audio mathematics, interpolation, decibel/gain conversions, and range scaling.")

(defn sec->ms
  "Converts seconds to milliseconds.
  Examples: (sec->ms 0.25) -> 250.0."
  [s]
  (when (number? s)
    (* s 1000.0)))

(defn ms->sec
  "Converts milliseconds to seconds.
  Examples: (ms->sec 250) -> 0.25."
  [ms]
  (when (number? ms)
    (/ ms 1000.0)))

(defn clamp
  "Clamps a numeric value between min and max bounds.
  Examples: (clamp 150 0 100) -> 100, (clamp -5 0 10) -> 0."
  [val min-val max-val]
  (js/Math.max min-val (js/Math.min max-val val)))

(defn lerp
  "Linear interpolation between a and b by factor t (0.0 to 1.0).
  Examples: (lerp 0 100 0.5) -> 50.0."
  [a b t]
  (+ a (* (- b a) t)))

(defn scale-range
  "Maps a value from an input range to an output range.
  Examples: (scale-range 5 0 10 0 100) -> 50.0."
  [val in-min in-max out-min out-max]
  (let [norm (/ (- val in-min) (- in-max in-min))]
    (+ out-min (* norm (- out-max out-min)))))

(defn db->gain
  "Converts decibels to linear amplitude gain factor (0 dB = 1.0, -6 dB = ~0.5).
  Examples: (db->gain 0) -> 1.0, (db->gain -6) -> ~0.5."
  [db]
  (if (number? db)
    (js/Math.pow 10 (/ db 20))
    1.0))

(defn gain->db
  "Converts linear amplitude gain factor to decibels (1.0 = 0 dB, 0.5 = ~-6 dB).
  Examples: (gain->db 1.0) -> 0.0, (gain->db 0.5) -> ~-6.02."
  [gain]
  (if (number? gain)
    (* 20 (js/Math.log10 (max 0.00001 gain)))
    0.0))
