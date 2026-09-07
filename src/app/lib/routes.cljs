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
     - :bus/direct Metronome clicks and audition voices (bypasses sends)

  2. Send Processors (Parallel Bus Sends):
     - :reverb  Global spatial diffusion. Supports two selectable engines:
                - :fdn      8-channel Householder Feedback Delay Network with
                            mutually prime delay lines, T60 absorption damping,
                            and dual quadrature LFO modulation.
                - :freeverb Classic Schroeder 8-comb plus 4-allpass diffusion.
     - :delay   Stereo ping-pong delay with lowpass feedback damping.

  3. Master Insert Processors (Serial Master Chain):
     - :distort Master analog saturation overdrive. Supports two engines:
                - :adaa    First-order Antiderivative Antialiased soft-clipper
                           for zero-aliasing analog warmth.
                - :classic Fast analog Pade tanh approximation.
     - :crusher Bit depth reduction (1 to 16 bits) and sample-hold decimation.
     - :chorus  Dual-quadrature stereo chorus and flanger.
     - :filter  Topology-Preserving Transform (TPT) State-Variable Lowpass Filter.
     - :limiter Analog-style soft-clipping master output stage.")

;; Default Studio Graph (8-Channel FDN Reverb + ADAA Antialiased Saturation)
(def default-graph
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :algorithm :adaa :distortion 0.30 :wet 0.8}
    :chorus        {:type :chorus :rate 0.8 :depth 0.4 :wet 0.35}
    :master-filter {:type :filter :frequency 18000 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.38 :wet 0.35}
    :reverb        {:type :reverb :algorithm :fdn :roomSize 0.78 :wet 0.35}
    :limiter       {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/lead :chorus :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

;; Heavy Dub Echo Chamber (Cathedral FDN Reverb + Warm ADAA Tape Saturation)
(def dub-echo-chamber
  "Heavy dub routing with tape delay feedback and deep cathedral FDN reverb."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :algorithm :adaa :distortion 0.25 :wet 0.65}
    :chorus        {:type :chorus :rate 0.6 :depth 0.5 :wet 0.4}
    :master-filter {:type :filter :frequency 4200 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.55 :wet 0.6}
    :reverb        {:type :reverb :algorithm :fdn :roomSize 0.92 :wet 0.55}
    :limiter       {:type :limiter :threshold -1.5}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/lead :chorus :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

;; Cyber Glitch Industrial Graph (Aggressive Crusher + ADAA Drive)
(def cyber-glitch
  "Aggressive industrial DSP graph with bitcrusher and resonance filtering."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:crusher       {:type :bitcrusher :bits 6 :sample-hold 1.5 :wet 0.7}
    :distort       {:type :distortion :algorithm :adaa :distortion 0.45 :wet 0.8}
    :chorus        {:type :chorus :rate 1.2 :depth 0.6 :wet 0.5}
    :master-filter {:type :filter :frequency 5000 :filter-type "lowpass"}
    :delay         {:type :delay :time "16n" :feedback 0.4 :wet 0.3}
    :reverb        {:type :reverb :algorithm :fdn :roomSize 0.65 :wet 0.28}
    :limiter       {:type :limiter :threshold -2.0}}

   :routes
   [[:bus/drums :crusher :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/lead :chorus :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

;; Vintage Schroeder Graph (Classic 90s Freeverb + Classic Pade Tanh Drive)
(def vintage-schroeder
  "Classic early digital DSP graph with Schroeder comb reverb and Pade drive."
  {:busses
   {:bus/drums  {:type :volume :volume 0}
    :bus/bass   {:type :volume :volume 0}
    :bus/lead   {:type :volume :volume 0}
    :bus/space  {:type :volume :volume 0}
    :bus/direct {:type :volume :volume 0}}

   :processors
   {:distort       {:type :distortion :algorithm :classic :distortion 0.35 :wet 0.8}
    :chorus        {:type :chorus :rate 0.8 :depth 0.4 :wet 0.35}
    :master-filter {:type :filter :frequency 3400 :filter-type "lowpass"}
    :delay         {:type :delay :time "8n." :feedback 0.38 :wet 0.35}
    :reverb        {:type :reverb :algorithm :freeverb :roomSize 0.75 :wet 0.35}
    :limiter       {:type :limiter :threshold -1.0}}

   :routes
   [[:bus/drums :master-filter]
    [:bus/bass :distort :master-filter]
    [:bus/lead :chorus :master-filter]
    [:bus/space :delay :reverb :limiter]
    [:bus/direct :limiter]
    [:master-filter :limiter]
    [:limiter :destination]]})

;; Catalog of Core Routing Topologies
(def core-routes
  {:default           default-graph
   :dub-echo          dub-echo-chamber
   :cyber-glitch      cyber-glitch
   :vintage-schroeder vintage-schroeder})
