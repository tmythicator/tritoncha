(ns app.custom.routes
  "User custom audio routing topologies and DSP bus graphs for Tritoncha.")

;; Custom Routing Topologies
;;
;; Busses:     :bus/drums, :bus/bass, :bus/lead, :bus/space, :bus/direct
;; Processors: :filter, :distortion, :delay, :reverb, :limiter, :volume, :bitcrusher, :chorus
;; Routes:     Vector of signal connection chains [[:src :fx1 :fx2 :dst] ...]

(def void-chamber
  "Deep cosmic void routing with vast modulated FDN diffusion and stereo chorus wash."
  {:title "VOID CHAMBER"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :algorithm :adaa :distortion 0.04 :wet 0.25}
    :chorus        {:type :chorus :rate 0.40 :depth 0.60 :wet 0.45}
    :master-filter {:type :filter :frequency 14000 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.46 :wet 0.38}
    :reverb        {:type :reverb :algorithm :fdn :roomSize 0.88 :wet 0.42}
    :limiter       {:type :limiter :threshold -1.2}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/lead :chorus :delay :reverb :limiter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def user-routes
  {:void-chamber  void-chamber})
