(ns app.ui.track-browser.formatters
  "Display formatters for track library metadata."
  (:require [clojure.string :as str]))

(defn format-scale
  "Format root and mode scale specification to human readable label.
  Examples: (format-scale [:e :phrygian]) -> \"E Phrygian\"."
  [scale]
  (if (vector? scale)
    (let [[root mode] scale]
      (str (str/upper-case (name (or root :e))) " " (str/capitalize (name (or mode :minor)))))
    "Custom"))
