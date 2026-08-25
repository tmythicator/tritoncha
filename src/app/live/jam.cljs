(ns app.live.jam
  "Live performance scratchpad for Emacs CIDER (C-c C-e / C-c C-k)."
  (:require [app.api :as a :refer [_ arp b! c! chord click! d drop! euc! f! g! jam! l! m! mod-all!
                                   pat! play! redrum! refresh! s! set-key! so! stat stop! sw! tr-all!
                                   u! undrum! unso! w!]]))

(comment
  (play!)
  (jam! :sub-roller)
  (jam! :acid-roller)
  (jam! :ambient-drift)
  (jam! :liquid-roller)
  (refresh!)
  (stop!)

  (set-key! :d :minor 2)
  (l! :bass-1 {:inst :bass :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "16n" :vel 0.95})
  (l! :bass-2 {:inst :bass :notes (d [1 _ 1 2 _ 1 4 3  1 _ 5 4 _ 2 1 _]) :step "8n" :vel 0.95})
  (l! :bass {:inst :bass :notes (d [1 1 _ 2 3 _ 1 5  1 _ 4 3 2 _ 1 _]) :step "16n" :vel 1.0})
  (l! :sub  {:inst :sub  :notes (d [1 _ _ _ 2 _ _ _  1 _ _ _ 4 _ 3 _]) :step "16n" :dur "8n"})

  (l! :pad {:inst :pad :notes [(chord :e :dark-m9) (chord :c :maj9) (chord :d :sus4) (chord :b :min7 2)] :step "1m" :dur "1m" :vel 0.35})
  (l! :arp {:inst :pad :notes (arp (chord :e :min9 3) :up-down) :mask (euc! 7 16) :step "16n" :vel 0.35})

  (l! :kick  {:inst :kick  :notes (pat! "k . . .  k . . .  . . k .  . . . .") :step "16n"})
  (l! :snare {:inst :snare :notes (pat! ". . . .  s . . .  . . . .  s . . g") :step "16n"})
  (l! :snare {:inst :snare :notes (pat! ". . . .  s . . .  . . s .  r r r r") :step "16n"})
  (l! :hat   {:inst :hh-c  :mask (euc! 11 16) :step "16n" :dur "32n" :vel [0.4 0.7 0.3 0.8]})

;; FX + Transitions
  (m! :bass :bass-1 :bass-2 :sub)
  (u! :bass :bass-1 :bass-2 :sub)
  (m! :kick :snare :hat)
  (f! 450)
  (sw! 400 5500 4)
  (s!)
  (u! :kick :snare :hat)
  (so! :kick :snare :hat)
  (drop!)
  (so! :bass :sub)
  (unso!)

  ;; Live Key Modulations + Transpositions
  (mod-all! :f :phrygian 1)
  (mod-all! :d :dorian 2)
  (tr-all! 2)
  (tr-all! -2)

  ;; Drumming control
  (undrum!)
  (click!)
  (b! 174)
  (redrum!)

  ;; Visuals
  (g! :torus-knot)
  (g! :icosahedron)
  (g! :sphere)
  (w!)
  (c! "#080412" "#ff007f")
  (c! "#020b14" "#00ffcc")
  (stat))
