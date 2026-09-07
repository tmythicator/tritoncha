(ns app.custom.routes
  "User custom audio routing topologies and DSP bus graphs.")

;; Custom Routing Topologies
;;
;; Busses:     :bus/drums, :bus/bass, :bus/lead, :bus/space, :bus/direct
;; Processors: :filter, :distortion, :delay, :reverb, :limiter, :volume, :bitcrusher, :chorus
;; Routes:     Vector of signal connection chains [[:src :fx1 :fx2 :dst] ...]

(def void-chamber
  "Deep spatial void routing with modulated FDN diffusion and stereo chorus wash."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :algorithm :adaa :distortion 0.20 :wet 0.70}
    :chorus        {:type :chorus :rate 0.45 :depth 0.65 :wet 0.55}
    :master-filter {:type :filter :frequency 14000 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.48 :wet 0.45}
    :reverb        {:type :reverb :algorithm :fdn :roomSize 0.96 :wet 0.65}
    :limiter       {:type :limiter :threshold -1.2}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/lead :chorus :delay :reverb :limiter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def neuro-roller
  "Heavy neurofunk and drum and bass roller topology with saturated sub-bass and snappy direct percussion."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :algorithm :adaa :distortion 0.42 :wet 0.85}
    :chorus        {:type :chorus :rate 0.9 :depth 0.35 :wet 0.25}
    :master-filter {:type :filter :frequency 18000 :filter-type "lowpass"}
    :delay         {:type :delay :time "16n" :feedback 0.35 :wet 0.25}
    :reverb        {:type :reverb :algorithm :fdn :roomSize 0.70 :wet 0.25}
    :limiter       {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :limiter]
    [:bus/lead :chorus :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def user-routes
  {:void-chamber void-chamber
   :neuro-roller neuro-roller})
