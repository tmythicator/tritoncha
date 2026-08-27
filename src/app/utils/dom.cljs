(ns app.utils.dom
  "Browser DOM utilities, viewport metrics, and device class detection."
  (:require [app.config :as cfg]))

(defn mobile?
  "Returns true if running on a mobile browser or touch device."
  []
  (and (exists? js/navigator)
       (or (boolean (re-find #"(?i)android|iphone|ipad|ipod|mobile" (or (.-userAgent js/navigator) "")))
           (pos? (or (.-maxTouchPoints js/navigator) 0)))))

(defn active-lookahead
  "Returns optimal lookahead buffer duration based on device class."
  []
  (if (mobile?) cfg/lookahead-mobile cfg/lookahead-desktop))

(defn max-dpr
  "Returns optimal maximum devicePixelRatio based on device class."
  []
  (if (mobile?) cfg/max-dpr-mobile cfg/max-dpr-desktop))
