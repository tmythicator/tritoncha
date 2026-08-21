(ns app.lib.routes
  "Core built-in audio routing topologies and default DSP bus graph for Tritoncha.")

(def default-graph
  {:busses
   {:drum-bus   {:type :volume :volume 0}
    :bass-bus   {:type :volume :volume 0}
    :space-bus  {:type :volume :volume -3}
    :direct-bus {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :distortion 0.35}
    :master-filter {:type :filter :frequency 3400 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.38}
    :reverb        {:type :reverb :decay 3.8 :wet 0.32}
    :limiter       {:type :limiter :threshold -2.0}}

   :routes
   [[:drum-bus :master-filter]
    [:bass-bus :distort :master-filter]
    [:space-bus :delay :reverb :limiter]
    [:direct-bus :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

(def core-routes
  {:default default-graph})
