(ns app.lib.routes
  "Core built-in audio routing topologies and default DSP bus graph for Tritoncha.")

;; Core Built-in Routing Catalog
;;
;; Routing Graph DSL:
;;   :graph - Map of bus routing paths:
;;            {:drums  [:filter :delay]  ;; Drums -> Filter -> Delay -> Master
;;             :space  [:out]            ;; Space -> Out (bypasses master)
;;             :lead   [:filter :out]    ;; Lead -> Filter -> Out (bypasses master)
;;             :master [:chorus]}        ;; Master -> Chorus -> Out
;;   :processors - Custom parameters for active processors (unspecified processors use neutral defaults):
;;            :filter     - Cutoff Hz (e.g. 14000) or vector [cutoff-hz Q]
;;            :distort    - Overdrive amount (e.g. 0.25) or map {:drive 0.25 :algo :adaa :wet 0.5}
;;            :crusher    - Bitcrusher bits (e.g. 6) or vector [bits sample-hold wet]
;;            :chorus     - Chorus mix (e.g. 0.45) or map {:rate 0.40 :depth 0.60 :wet 0.45}
;;            :delay      - Vector [time feedback wet] (e.g. ["8n." 0.46 0.38])
;;            :reverb     - Map {:size 0.88 :wet 0.42 :algo :fdn}
;;            :compressor - Map {:threshold -14.0 :ratio 4.0} or boolean

(def default-graph
  "Transparent reference routing with uncolored busses, gentle bus compression, and open dynamics."
  {:title "DEFAULT"
   :graph {:master [:compressor :limiter]}
   :processors {:compressor {:threshold -14.0 :ratio 3.0 :attack 0.012 :release 0.100 :makeup 2.0 :mix 1.0}}})

(def studio-master
  "Studio mix bus with VCA glue compression, clean delay and natural room reverb on master."
  {:title "STUDIO MASTER"
   :graph {:space  [:reverb :out]
           :lead   [:delay]
           :master [:compressor :limiter]}
   :processors {:compressor {:threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
                :delay      ["8n." 0.35 0.25]
                :reverb     {:algo :fdn :size 0.75 :wet 0.35}}})

(def liminal-prison
  "Spacious atmospheric chain with isolated lead and space busses, long dotted delay, and slow chorus."
  {:title "LIMINAL PRISON"
   :graph {:drums  [:filter :delay]
           :space  [:reverb :delay :out]
           :lead   [:filter :out]
           :master [:chorus :compressor :limiter]}
   :processors {:filter     {:cutoff 10500 :resonance 0.15}
                :chorus     {:rate 0.12 :depth 0.50 :wet 0.25}
                :delay      ["4n." 0.46 0.22]
                :reverb     {:algo :fdn :size 0.94 :wet 0.42}
                :compressor {:threshold -14.0 :ratio 3.0 :attack 0.015 :release 0.100 :makeup 1.5 :mix 0.95}}})

(def dub-echo-chamber
  "Classic dub setup with warm delay feedback, dark lowpass filtering, and tape drive."
  {:title "DUB ECHO"
   :graph {:drums  [:compressor]
           :lead   [:filter :delay :reverb]
           :space  [:reverb :out]
           :master [:distort :compressor :limiter]}
   :processors {:filter     4200
                :distort    {:drive 0.18 :algo :adaa :wet 0.50}
                :compressor {:threshold -15.0 :ratio 3.5 :attack 0.020 :release 0.160 :makeup 2.2 :mix 0.90}
                :delay      ["8n." 0.44 0.42]
                :reverb     {:algo :freeverb :size 0.85 :wet 0.38}}})

(def tape-lofi
  "Warm cassette and vinyl routing with slow tape wow flutter, dusty vinyl noise bed, vintage Schroeder reverb, and magnetic master saturation."
  {:title "TAPE LO-FI"
   :graph {:drums  [:crusher]
           :space  [:chorus :delay :reverb]
           :lead   [:chorus :delay]
           :master [:distort :filter :compressor :limiter]}
   :processors {:filter     [4200 0.0]
                :crusher    {:bits 9.5 :sample-hold 1.55 :drive 0.24}
                :distort    {:drive 0.18 :algo :adaa :wet 0.50}
                :chorus     {:rate 0.16 :depth 0.70 :wet 0.40}
                :delay      ["8n" 0.38 0.32]
                :reverb     {:algo :freeverb :size 0.68 :wet 0.28}
                :compressor {:threshold -14.0 :ratio 3.0 :attack 0.015 :release 0.120 :makeup 2.0 :mix 0.95}}})

(def crematorium
  "Blistering industrial routing with crushed, compressed, and limited drums, heavily saturated acid bass, and glued master dynamics."
  {:title "CREMATORIUM"
   :graph {:drums  [:crusher :compressor :limiter :out]
           :bass   [:distort :filter]
           :lead   [:distort :delay]
           :space  [:reverb :out]
           :master [:compressor :limiter]}
   :processors {:filter     [2800 0.85]
                :crusher    {:bits 8.0 :sample-hold 1.4 :drive 0.25}
                :distort    {:drive 0.65 :algo :classic :wet 0.95 :bits 14.0}
                :delay      ["16n" 0.45 0.30]
                :reverb     {:algo :freeverb :size 0.55 :wet 0.22}
                :compressor {:threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.090 :makeup 2.0 :mix 1.0}}})

(def ambient-prism
  "Pristine ambient space with wide FDN reverb, shimmering chorus, and ping-pong delay."
  {:title "AMBIENT PRISM"
   :graph {:drums  [:filter]
           :lead   [:delay :reverb]
           :space  [:reverb :out]
           :master [:chorus :compressor :limiter]}
   :processors {:filter 14000
                :chorus {:rate 0.32 :depth 0.65 :wet 0.40}
                :delay  ["8n." 0.48 0.38]
                :reverb {:algo :fdn :size 0.90 :wet 0.50}}})

(def industrial-crush
  "Heavy industrial bus with 5-bit decimation, hard clipping overdrive, and punchy compression."
  {:title "INDUSTRIAL CRUSH"
   :graph {:drums  [:crusher]
           :bass   [:distort]
           :lead   [:distort]
           :master [:filter :compressor :limiter]}
   :processors {:filter     [4600 0.45]
                :crusher    [5.0 2.2 0.65]
                :distort    {:drive 0.28 :algo :classic :wet 0.75}
                :compressor {:threshold -16.0 :ratio 6.0 :attack 0.005 :release 0.080 :makeup 3.5 :mix 1.0}
                :delay      ["16n" 0.28 0.22]
                :reverb     {:algo :freeverb :size 0.48 :wet 0.20}}})

(def cyber-glitch
  "Glitch-hop and IDM routing with 6-bit aliasing drum decimation, fast 16th stutter delay, flutter chorus, and FDN cyber chamber."
  {:title "CYBER GLITCH"
   :graph {:drums  [:crusher :filter]
           :lead   [:chorus :delay]
           :space  [:reverb :out]
           :master [:compressor :limiter]}
   :processors {:filter     [5000 0.78]
                :crusher    {:bits 10.0 :sample-hold 2.2 :drive 0.0}
                :chorus     {:rate 2.2 :depth 0.78 :wet 0.55}
                :delay      ["16n" 0.55 0.35]
                :reverb     {:algo :fdn :size 0.55 :wet 0.35}
                :compressor {:threshold -13.0 :ratio 3.5 :attack 0.008 :release 0.075 :makeup 1.5 :mix 1.0}}})

(def vintage-schroeder
  "Early digital audio character with Schroeder reverb, warm overdrive and 3.4 kHz lowpass rolloff."
  {:title "VINTAGE SCHROEDER"
   :graph {:drums  [:distort]
           :space  [:reverb :out]
           :lead   [:delay]
           :master [:chorus :filter :compressor :limiter]}
   :processors {:filter  3400
                :distort {:drive 0.35 :algo :classic :wet 0.8}
                :chorus  {:rate 0.8 :depth 0.4 :wet 0.35}
                :delay   ["8n." 0.38 0.35]
                :reverb  {:algo :freeverb :size 0.75 :wet 0.35}}})

(def void-chamber
  "Deep cosmic void routing with vast modulated FDN diffusion and stereo chorus wash."
  {:title "VOID CHAMBER"
   :graph {:drums  [:filter]
           :space  [:reverb :out]
           :lead   [:delay :reverb]
           :master [:chorus :distort :compressor :limiter]}
   :processors {:filter  14000
                :distort {:drive 0.04 :algo :adaa :wet 0.25}
                :chorus  {:rate 0.40 :depth 0.60 :wet 0.45}
                :delay   ["8n." 0.46 0.38]
                :reverb  {:algo :fdn :size 0.88 :wet 0.42}}})

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
   :vintage-schroeder vintage-schroeder
   :void-chamber      void-chamber})
