(ns app.lib.routes
  "Core built-in audio routing topologies and default DSP bus graph for Tritoncha.")

(def default-graph
  "Clean studio default routing with uncolored busses and natural track filter cutoff."
  {:title "DEFAULT"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:filter     {:type :filter :filter-type "lowpass"}
    :compressor {:type :compressor :enabled true :threshold -14.0 :ratio 3.0 :attack 0.012 :release 0.100 :makeup 2.0 :mix 1.0}
    :delay      {:type :delay :time "8n." :feedback 0.35 :wet 0.25}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.75 :wet 0.35}
    :limiter    {:type :limiter :threshold -1.0}}

   :routes
   {:bus/drums  []
    :bus/bass   []
    :bus/lead   [:delay]
    :bus/space  [:delay :reverb]
    :bus/direct :out
    :bus/master [:filter :compressor :limiter]}})

(def studio-master
  "Punchy studio mastering topology with VCA glue bus compression, natural filter dynamics, and lush FDN room diffusion."
  {:title "STUDIO MASTER"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:filter     {:type :filter :filter-type "lowpass"}
    :compressor {:type :compressor :enabled true :threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
    :delay      {:type :delay :time "8n." :feedback 0.35 :wet 0.25}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.75 :wet 0.35}
    :limiter    {:type :limiter :threshold -1.0}}

   :routes
   {:bus/drums  []
    :bus/bass   []
    :bus/lead   []
    :bus/space  [:delay :reverb]
    :bus/direct :out
    :bus/master [:filter :compressor :limiter]}})

(def liminal-prison
  "Mind-bending liminal space topology with vast subterranean FDN diffusion, cross-fed ping-pong echoes, and cold eerie acoustics."
  {:title "LIMINAL PRISON"
   :busses
   {:bus/drums  {:delay 0.28 :reverb 0.42}
    :bus/bass   {:delay 0.05 :reverb 0.15}
    :bus/lead   {:delay 0.42 :reverb 0.65}
    :bus/space  {:delay 0.55 :reverb 0.85}
    :bus/direct {:delay 0.0  :reverb 0.0}
    :bus/master {}}

   :processors
   {:chorus  {:type :chorus :rate 0.18 :depth 0.70 :wet 0.35}
    :filter  {:type :filter :frequency 11500 :q 0.22 :filter-type "lowpass"}
    :delay   {:type :delay :time "4n." :feedback 0.52 :wet 0.45}
    :reverb  {:type :reverb :algorithm :fdn :roomSize 0.96 :wet 0.58}
    :limiter {:type :limiter :threshold -1.2}}

   :routes
   {:bus/drums  [:delay :reverb]
    :bus/bass   []
    :bus/lead   [:chorus :delay :reverb]
    :bus/space  [:chorus :delay :reverb]
    :bus/direct :out
    :bus/master [:filter :limiter]}})

(def dub-echo-chamber
  "Heavy dub routing with tape delay feedback and deep cathedral FDN reverb."
  {:title "DUB ECHO"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:distort    {:type :distortion :algorithm :adaa :distortion 0.05 :wet 0.25}
    :chorus     {:type :chorus :rate 0.4 :depth 0.25 :wet 0.15}
    :compressor {:type :compressor :enabled true :threshold -14.0 :ratio 3.0 :attack 0.020 :release 0.150 :makeup 2.0 :mix 0.85}
    :filter     {:type :filter :frequency 4200 :filter-type "lowpass"}
    :delay      {:type :delay :time "8n." :feedback 0.44 :wet 0.36}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.82 :wet 0.32}
    :limiter    {:type :limiter :threshold -1.5}}

   :routes
   {:bus/drums  []
    :bus/bass   [:distort]
    :bus/lead   [:chorus]
    :bus/space  [:delay :reverb]
    :bus/direct :out
    :bus/master [:filter :compressor :limiter]}})

(def tape-lofi
  "Vintage cassette tape character with 10-bit quantization noise, tape saturation and wow flutter."
  {:title "TAPE LO-FI"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:crusher {:type :bitcrusher :bits 10.0 :sample-hold 1.35 :wet 0.45}
    :distort {:type :distortion :algorithm :adaa :distortion 0.12 :wet 0.45}
    :chorus  {:type :chorus :rate 0.24 :depth 0.52 :wet 0.28}
    :filter  {:type :filter :frequency 3500 :q 0.18 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n" :feedback 0.35 :wet 0.28}
    :reverb  {:type :reverb :algorithm :freeverb :roomSize 0.60 :wet 0.24}
    :limiter {:type :limiter :threshold -1.0}}

   :routes
   {:bus/drums  [:crusher :distort]
    :bus/bass   [:distort]
    :bus/lead   [:chorus :distort]
    :bus/space  [:crusher :delay :reverb]
    :bus/direct :out
    :bus/master [:filter :limiter]}})

(def crematorium
  "Searing incineration matrix with overdriven burning bass into resonant scorch filter and dry drums."
  {:title "CREMATORIUM"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:distort {:type :distortion :algorithm :adaa :distortion 0.20 :wet 0.75}
    :filter  {:type :filter :frequency 2800 :q 0.72 :filter-type "lowpass"}
    :delay   {:type :delay :time "16n" :feedback 0.44 :wet 0.30}
    :reverb  {:type :freeverb :algorithm :freeverb :roomSize 0.52 :wet 0.22}
    :limiter {:type :limiter :threshold -1.5}}

   :routes
   {:bus/drums  :out
    :bus/bass   [:distort]
    :bus/lead   [:delay]
    :bus/space  [:reverb]
    :bus/direct :out
    :bus/master [:filter :limiter]}})

(def ambient-prism
  "Ultra-clean crystalline spatial diffusion with cathedral FDN reverb and zero saturation."
  {:title "AMBIENT PRISM"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:chorus  {:type :chorus :rate 0.32 :depth 0.65 :wet 0.40}
    :filter  {:type :filter :frequency 16000 :q 0.0 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n." :feedback 0.48 :wet 0.38}
    :reverb  {:type :reverb :algorithm :fdn :roomSize 0.86 :wet 0.42}
    :limiter {:type :limiter :threshold -0.8}}

   :routes
   {:bus/drums  []
    :bus/bass   []
    :bus/lead   [:chorus :delay :reverb]
    :bus/space  [:delay :reverb]
    :bus/direct :out
    :bus/master [:filter :limiter]}})

(def industrial-crush
  "Aggressive industrial dance matrix with 5-bit decimation and classic Pade clipping overdrive."
  {:title "INDUSTRIAL CRUSH"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:crusher    {:type :bitcrusher :bits 5.0 :sample-hold 2.2 :wet 0.65}
    :distort    {:type :distortion :algorithm :classic :distortion 0.28 :wet 0.75}
    :compressor {:type :compressor :enabled true :threshold -16.0 :ratio 6.0 :attack 0.005 :release 0.080 :makeup 3.5 :mix 1.0}
    :filter     {:type :filter :frequency 4600 :q 0.45 :filter-type "lowpass"}
    :delay      {:type :delay :time "16n" :feedback 0.28 :wet 0.22}
    :reverb     {:type :freeverb :algorithm :freeverb :roomSize 0.48 :wet 0.20}
    :limiter    {:type :limiter :threshold -2.0}}

   :routes
   {:bus/drums  [:crusher]
    :bus/bass   [:distort]
    :bus/lead   [:distort :delay]
    :bus/space  [:crusher :reverb]
    :bus/direct :out
    :bus/master [:filter :compressor :limiter]}})

(def cyber-glitch
  "Aggressive industrial DSP graph with bitcrusher and resonance filtering."
  {:title "CYBER GLITCH"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:crusher    {:type :bitcrusher :bits 6 :sample-hold 1.5 :wet 0.7}
    :distort    {:type :distortion :algorithm :adaa :distortion 0.45 :wet 0.8}
    :chorus     {:type :chorus :rate 1.2 :depth 0.6 :wet 0.5}
    :compressor {:type :compressor :enabled true :threshold -12.0 :ratio 4.0 :attack 0.008 :release 0.070 :makeup 2.0 :mix 0.9}
    :filter     {:type :filter :frequency 5000 :filter-type "lowpass"}
    :delay      {:type :delay :time "16n" :feedback 0.4 :wet 0.3}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.65 :wet 0.28}
    :limiter    {:type :limiter :threshold -2.0}}

   :routes
   {:bus/drums  [:crusher]
    :bus/bass   [:distort]
    :bus/lead   [:chorus]
    :bus/space  [:delay :reverb]
    :bus/direct :out
    :bus/master [:filter :compressor :limiter]}})

(def vintage-schroeder
  "Classic early digital DSP graph with Schroeder comb reverb and Pade drive."
  {:title "VINTAGE SCHROEDER"
   :busses
   {:bus/drums  {}
    :bus/bass   {}
    :bus/lead   {}
    :bus/space  {}
    :bus/direct {}
    :bus/master {}}

   :processors
   {:distort {:type :distortion :algorithm :classic :distortion 0.35 :wet 0.8}
    :chorus  {:type :chorus :rate 0.8 :depth 0.4 :wet 0.35}
    :filter  {:type :filter :frequency 3400 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n." :feedback 0.38 :wet 0.35}
    :reverb  {:type :freeverb :algorithm :freeverb :roomSize 0.75 :wet 0.35}
    :limiter {:type :limiter :threshold -1.0}}

   :routes
   {:bus/drums  []
    :bus/bass   [:distort]
    :bus/lead   [:chorus]
    :bus/space  [:delay :reverb]
    :bus/direct :out
    :bus/master [:filter :limiter]}})

(def core-routes
  {:default           default-graph
   :studio-master     studio-master
   :liminal-prison    liminal-prison
   :dub-echo          dub-echo-chamber
   :tape-lofi         tape-lofi
   :crematorium       crematorium
   :ambient-prism     ambient-prism
   :industrial-crush  industrial-crush
   :cyber-glitch      cyber-glitch
   :vintage-schroeder vintage-schroeder})
