(ns app.custom.routes
  "User custom audio routing topologies and DSP bus graphs.")

;; Custom Routing Topologies (Same format as app.lib.routes)
;;
;; Busses:     :bus/drums, :bus/bass, :bus/space, :bus/direct, :bus/master
;; Processors: :filter, :distortion, :delay, :reverb, :limiter, :volume
;; Routes:     Vector of signal connection chains [[:src :fx1 :fx2 :dst] ...]

(def user-routes
  {})
