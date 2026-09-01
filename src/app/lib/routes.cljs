(ns app.lib.routes
  "Core built-in audio routing topologies and default DSP bus graph for Tritoncha.")

(def default-graph
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :distortion 0.35 :wet 0.8}
    :master-filter {:type :filter :frequency 3400 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.38 :wet 0.35}
    :reverb        {:type :reverb :roomSize 0.75 :dampening 3000 :wet 0.35}
    :limiter       {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def dub-echo-chamber
  "Heavy dub routing with tape delay feedback and cathedral reverb."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :distortion 0.28 :wet 0.65}
    :master-filter {:type :filter :frequency 4200 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.55 :wet 0.6}
    :reverb        {:type :reverb :roomSize 0.9 :dampening 2400 :wet 0.5}
    :limiter       {:type :limiter :threshold -1.5}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def cyber-glitch
  "Aggressive industrial DSP graph with bitcrusher and resonance filtering."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:crusher       {:type :bitcrusher :bits 6 :wet 0.7}
    :distort       {:type :distortion :distortion 0.45 :wet 0.8}
    :master-filter {:type :filter :frequency 5000 :filter-type "lowpass"}
    :delay         {:type :delay :time "16n" :feedback 0.4 :wet 0.3}
    :reverb        {:type :reverb :roomSize 0.6 :dampening 4000 :wet 0.25}
    :limiter       {:type :limiter :threshold -2.0}}

   :routes
   [[:bus/drums :crusher :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def core-routes
  {:default      default-graph
   :dub-echo     dub-echo-chamber
   :cyber-glitch cyber-glitch})
