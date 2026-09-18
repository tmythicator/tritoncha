(ns app.live.jam
  "Live performance scratchpad and Algorave cockpit for Emacs CIDER."
  (:require [app.api :as a :refer [b! c! clear-loops! click! drive-mode! f!
                                   g! hud! inst! jam! jams! loop! m! play!
                                   q! rebass! redrum! relead! repad!
                                   reverb-mode! scene! send! sidechain! so!
                                   stat stats! stop! sw! u! unbass! undrum!
                                   unlead! unpad! unso! v! vel! w! wet!]]))

;; Live Performance Controls
;; Note: For music theory guides, Euclidean patterns and synth design, see: src/app/demo/tutorial.cljs

(comment
  ;; Transport and Preset Launcher
  (play!)
  (stop!)
  (b! 172)
  (click!)
  (jam! :street-roller)
  (jam! :orbital-roller)
  (jam! :downtempo-chill)
  (jam! :cyber-dub)
  (jam! :hardcore-rave)
  (jam! :synthwave-run)

  ;; Bus Volume and FX Send Controls
  (v! :bus/drums 0.85)
  (v! :bus/bass 0.90)
  (v! :bus/space 0.70)
  (v! :bus/lead 0.80)
  (send! :bus/lead :reverb 0.35)
  (send! :bus/space :delay 0.40)

  ;; Track Mute and Solo Controls
  (m! :kick :snare :hats)
  (u! :kick :snare :hats)
  (so! :bass :sub)
  (unso!)
  (vel! :bass 0.85)

  ;; Section Drops 
  (undrum!)
  (redrum!)
  (unbass!)
  (rebass!)
  (unlead!)
  (relead!)
  (unpad!)
  (repad!)

  ;; DSP Automations and Filter Sweeps
  (sw! 300 6000 4)
  (f! 3400)
  (q! 0.6)
  (sidechain! 0.65)
  (drive-mode! :adaa)
  (reverb-mode! :fdn)
  (wet! 0.35)

  ;; Quick Live Looping
  (loop! :acid {:inst :bass-303 :notes ["C2" nil "D#2" "F2" nil "G2" "A#2" nil] :step "16n" :dur "16n" :vel 0.85})
  (clear-loops!)

  ;; Three.js WebGL Visual Controls
  (scene! :cyber-torus)
  (g! :torus-knot)
  (c! "#030814" "#00e5ff")
  (w!)

  ;; HUD, Diagnostics and Browser Windows
  (stat)
  (stats!)
  (hud!)
  (inst!)
  (jams!))
