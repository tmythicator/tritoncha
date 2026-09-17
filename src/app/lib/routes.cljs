(ns app.lib.routes
  "Core built-in audio routing topologies and default DSP bus graph for Tritoncha.

  Architecture Overview:
  Tritoncha routes real-time sound synthesis through 5 stereo audio busses into
  dedicated send processors and a master insert chain:

  1. Audio Busses:
     - :bus/drums  Drums and percussion (membrane, acoustic, 808/909 kits)
     - :bus/bass   Bass synths, reese, 303 acid, sub-bass
     - :bus/lead   Melodic leads, arpeggiators, pluck synths
     - :bus/space  Ambient pads, chord stabs, textured soundscapes
     - :bus/direct Metronome clicks and audition voices (bypasses master)
     - :bus/master Master summing bus with master insert processing

  2. Send Processors (Parallel Bus Sends):
     - :reverb  Global spatial diffusion. Supports two selectable engines:
                - :fdn      8-channel Householder Feedback Delay Network with
                            mutually prime delay lines, T60 absorption damping,
                            and dual quadrature LFO modulation.
                - :freeverb Classic Schroeder 8-comb plus 4-allpass diffusion.
     - :delay   Stereo ping-pong delay with lowpass feedback damping.

  3. Master Insert Processors (Serial Master Chain):
     - :filter     Topology-Preserving Transform (TPT) State-Variable Lowpass Filter.
     - :compressor Stereo-linked VCA studio bus glue compressor (threshold, ratio, attack, release, makeup, mix).
     - :distort    Master analog saturation overdrive (ADAA or classic Pade).
     - :crusher    Bit depth reduction (1 to 16 bits) and sample-hold decimation.
     - :chorus     Dual-quadrature stereo chorus and flanger.
     - :limiter    Analog-style soft-clipping master output stage.")

;; Default Studio Graph (Simple, Clean Studio Routing Without Insert Coloring)
(def default-graph
  "Clean studio default routing with uncolored busses and natural track filter cutoff."
  {:title "DEFAULT"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:filter     {:type :filter :filter-type "lowpass"}
    :compressor {:type :compressor :enabled false :threshold -12.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 0.0}
    :delay      {:type :delay :time "8n." :feedback 0.35 :wet 0.25}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.75 :wet 0.35}
    :limiter    {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :bus/master]
    [:bus/bass :bus/master]
    [:bus/lead :bus/master]
    [:bus/space :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :compressor :limiter :destination]]})

;; Studio Master (Punchy VCA Glue Bus Compressor + Crisp Lowpass + FDN Reverb)
(def studio-master
  "Punchy studio mastering topology with VCA glue bus compression, natural filter dynamics, and lush FDN room diffusion."
  {:title "STUDIO MASTER"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:filter     {:type :filter :filter-type "lowpass"}
    :compressor {:type :compressor :enabled true :threshold -14.0 :ratio 4.0 :attack 0.010 :release 0.100 :makeup 2.5 :mix 1.0}
    :delay      {:type :delay :time "8n." :feedback 0.35 :wet 0.25}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.75 :wet 0.35}
    :limiter    {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :bus/master]
    [:bus/bass :bus/master]
    [:bus/lead :bus/master]
    [:bus/space :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :compressor :limiter :destination]]})

;; Liminal Prison (Infinite Subterranean FDN Diffusion + Cross-Fed Ping-Pong Hallways)
(def liminal-prison
  "Mind-bending liminal space topology with vast subterranean FDN diffusion, cross-fed ping-pong echoes, and cold eerie acoustics."
  {:title "LIMINAL PRISON"
   :busses
   {:bus/drums  {:type :volume :volume 0 :delay 0.28 :reverb 0.42}
    :bus/bass   {:type :volume :volume 0 :delay 0.05 :reverb 0.15}
    :bus/lead   {:type :volume :volume 0 :delay 0.42 :reverb 0.65}
    :bus/space  {:type :volume :volume 0 :delay 0.55 :reverb 0.85}
    :bus/direct {:type :volume :volume 0 :delay 0.0  :reverb 0.0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:chorus  {:type :chorus :rate 0.18 :depth 0.70 :wet 0.35}
    :filter  {:type :filter :frequency 11500 :q 0.22 :filter-type "lowpass"}
    :delay   {:type :delay :time "4n." :feedback 0.52 :wet 0.45}
    :reverb  {:type :reverb :algorithm :fdn :roomSize 0.96 :wet 0.58}
    :limiter {:type :limiter :threshold -1.2}}

   :routes
   [[:bus/drums :delay :reverb :bus/master]
    [:bus/bass :bus/master]
    [:bus/lead :chorus :delay :reverb :bus/master]
    [:bus/space :chorus :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :limiter :destination]]})

;; Heavy Dub Echo Chamber (Cathedral FDN Reverb + Warm ADAA Tape Saturation)
(def dub-echo-chamber
  "Heavy dub routing with tape delay feedback and deep cathedral FDN reverb."
  {:title "DUB ECHO"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:distort    {:type :distortion :algorithm :adaa :distortion 0.05 :wet 0.25}
    :chorus     {:type :chorus :rate 0.4 :depth 0.25 :wet 0.15}
    :compressor {:type :compressor :enabled true :threshold -14.0 :ratio 3.0 :attack 0.020 :release 0.150 :makeup 2.0 :mix 0.85}
    :filter     {:type :filter :frequency 4200 :filter-type "lowpass"}
    :delay      {:type :delay :time "8n." :feedback 0.44 :wet 0.36}
    :reverb     {:type :reverb :algorithm :fdn :roomSize 0.82 :wet 0.32}
    :limiter    {:type :limiter :threshold -1.5}}

   :routes
   [[:bus/drums :bus/master]
    [:bus/bass :distort :bus/master]
    [:bus/lead :chorus :bus/master]
    [:bus/space :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :compressor :limiter :destination]]})

;; Tape Lo-Fi Cassette (Analog Tape Hiss, Quantization Grain and Wow Flutter)
(def tape-lofi
  "Vintage cassette tape character with 10-bit quantization noise, tape saturation and wow flutter."
  {:title "TAPE LO-FI"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:crusher {:type :bitcrusher :bits 10.0 :sample-hold 1.35 :wet 0.45}
    :distort {:type :distortion :algorithm :adaa :distortion 0.12 :wet 0.45}
    :chorus  {:type :chorus :rate 0.24 :depth 0.52 :wet 0.28}
    :filter  {:type :filter :frequency 3500 :q 0.18 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n" :feedback 0.35 :wet 0.28}
    :reverb  {:type :reverb :algorithm :freeverb :roomSize 0.60 :wet 0.24}
    :limiter {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :crusher :distort :bus/master]
    [:bus/bass :distort :bus/master]
    [:bus/lead :chorus :distort :bus/master]
    [:bus/space :crusher :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :limiter :destination]]})

;; Crematorium (Searing Incineration Matrix with Overdriven Bass and Resonant Scorch Filter)
(def crematorium
  "Searing incineration matrix with overdriven burning bass into resonant scorch filter and dry drums."
  {:title "CREMATORIUM"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:distort {:type :distortion :algorithm :adaa :distortion 0.20 :wet 0.75}
    :filter  {:type :filter :frequency 2800 :q 0.72 :filter-type "lowpass"}
    :delay   {:type :delay :time "16n" :feedback 0.44 :wet 0.30}
    :reverb  {:type :freeverb :algorithm :freeverb :roomSize 0.52 :wet 0.22}
    :limiter {:type :limiter :threshold -1.5}}

   :routes
   [[:bus/drums :destination]
    [:bus/bass :distort :bus/master]
    [:bus/lead :delay :bus/master]
    [:bus/space :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :limiter :destination]]})

;; Ambient Prism (Crystalline Spatial Diffusion + 8-Channel Cathedral FDN)
(def ambient-prism
  "Ultra-clean crystalline spatial diffusion with cathedral FDN reverb and zero saturation."
  {:title "AMBIENT PRISM"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:chorus  {:type :chorus :rate 0.32 :depth 0.65 :wet 0.40}
    :filter  {:type :filter :frequency 16000 :q 0.0 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n." :feedback 0.48 :wet 0.38}
    :reverb  {:type :reverb :algorithm :fdn :roomSize 0.86 :wet 0.42}
    :limiter {:type :limiter :threshold -0.8}}

   :routes
   [[:bus/drums :bus/master]
    [:bus/bass :bus/master]
    [:bus/lead :chorus :delay :reverb :bus/master]
    [:bus/space :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :limiter :destination]]})

;; Industrial Crush (Cyberpunk EBM + Classic Pade Tanh Clipping Overdrive)
(def industrial-crush
  "Aggressive industrial dance matrix with 5-bit decimation and classic Pade clipping overdrive."
  {:title "INDUSTRIAL CRUSH"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:crusher    {:type :bitcrusher :bits 5.0 :sample-hold 2.2 :wet 0.65}
    :distort    {:type :distortion :algorithm :classic :distortion 0.28 :wet 0.75}
    :compressor {:type :compressor :enabled true :threshold -16.0 :ratio 6.0 :attack 0.005 :release 0.080 :makeup 3.5 :mix 1.0}
    :filter     {:type :filter :frequency 4600 :q 0.45 :filter-type "lowpass"}
    :delay      {:type :delay :time "16n" :feedback 0.28 :wet 0.22}
    :reverb     {:type :freeverb :algorithm :freeverb :roomSize 0.48 :wet 0.20}
    :limiter    {:type :limiter :threshold -2.0}}

   :routes
   [[:bus/drums :crusher :bus/master]
    [:bus/bass :distort :bus/master]
    [:bus/lead :distort :delay :bus/master]
    [:bus/space :crusher :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :compressor :limiter :destination]]})

;; Cyber Glitch Industrial Graph (Aggressive Crusher + ADAA Drive)
(def cyber-glitch
  "Aggressive industrial DSP graph with bitcrusher and resonance filtering."
  {:title "CYBER GLITCH"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

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
   [[:bus/drums :crusher :bus/master]
    [:bus/bass :distort :bus/master]
    [:bus/lead :chorus :bus/master]
    [:bus/space :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :compressor :limiter :destination]]})

;; Vintage Schroeder Graph (Classic 90s Freeverb + Classic Pade Tanh Drive)
(def vintage-schroeder
  "Classic early digital DSP graph with Schroeder comb reverb and Pade drive."
  {:title "VINTAGE SCHROEDER"
   :busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}
    :bus/master {:type :volume :volume 0}}

   :processors
   {:distort {:type :distortion :algorithm :classic :distortion 0.35 :wet 0.8}
    :chorus  {:type :chorus :rate 0.8 :depth 0.4 :wet 0.35}
    :filter  {:type :filter :frequency 3400 :filter-type "lowpass"}
    :delay   {:type :delay :time "8n." :feedback 0.38 :wet 0.35}
    :reverb  {:type :freeverb :algorithm :freeverb :roomSize 0.75 :wet 0.35}
    :limiter {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :bus/master]
    [:bus/bass :distort :bus/master]
    [:bus/lead :chorus :bus/master]
    [:bus/space :delay :reverb :bus/master]
    [:bus/direct :destination]
    [:bus/master :filter :limiter :destination]]})

;; Catalog of Core Routing Topologies
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
