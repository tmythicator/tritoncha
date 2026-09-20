(ns app.custom.routes
  "User custom audio routing topologies and DSP bus graphs for Tritoncha.")

;; Custom Routing Topologies
;;
;; Busses:     :bus/drums, :bus/bass, :bus/lead, :bus/space, :bus/master
;; Processors: :filter, :distort, :delay, :reverb, :limiter, :crusher, :chorus, :compressor
;; Routes:     Map of bus routes {:bus/drums [] :bus/bass [:distort] ...}

(def void-chamber
  "Deep cosmic void routing with vast modulated FDN diffusion and stereo chorus wash."
  {:title "VOID CHAMBER"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/master {}}

   :processors
   {:distort {:type :distortion :algorithm :adaa :distortion 0.04 :wet 0.25}
    :chorus  {:type :chorus :rate 0.40 :depth 0.60 :wet 0.45}
    :filter  {:type :filter :frequency 14000 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n." :feedback 0.46 :wet 0.38}
    :reverb  {:type :reverb :algorithm :fdn :roomSize 0.88 :wet 0.42}
    :limiter {:type :limiter :threshold -1.2}}

   :routes
   {:bus/drums  []
    :bus/bass   [:distort]
    :bus/lead   [:chorus :delay :reverb]
    :bus/space  [:delay :reverb]
    :bus/master [:filter :limiter]}})

(def user-routes
  {:void-chamber  void-chamber})
