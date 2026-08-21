# Tritoncha

Live-coding music and 3D WebGL visuals studio built with ClojureScript, Tone.js, and Three.js directly in your browser.

> **What is Tritoncha?**
> **Tritoncha** is the synergy of 3D visual rendering (**Three.js**) and real-time sound synthesis (**Tone.js**).

---

## Quick Start

```bash
# 1. Enter the environment (Node.js, Clojure, pnpm)
nix develop
# (or simply `direnv allow` if you use direnv)

# 2. Start the dev server
pnpm run dev
```

Open [http://localhost:3000](http://localhost:3000) and click anywhere on the page to unlock the browser's WebAudio context.

---

## Interactive Tutorial & REPL Workflow

The fastest and most fun way to explore Tritoncha is through the interactive tutorial:

**Open [`src/app/demo/tutorial.cljs`](src/app/demo/tutorial.cljs)**

It guides you step-by-step through:

- Building basslines with scale degrees (`d`) and live drums (`pat!`, `euc!`)
- Sound design and auditioning custom synthesizers (`definst!`, `demo!`)
- Custom audio routing and effects chains (`defrouting!`)
- Modal harmony, progressions, live key modulation (`mod-all!`, `tr-all!`)
- Composing full track arrangements and live performance tricks

### Recommended Setup

For the best experience, use an editor with strong **REPL-Driven Development (RDD)** support:

- **Emacs + CIDER** (Recommended): Open `src/app/live/jam.cljs` or `src/app/demo/tutorial.cljs` and run `M-x cider-connect-cljs` (select `shadow` -> `:app`). Evaluate expressions directly with `C-c C-e` or `C-c C-c`.
- **VSCode + Calva**: Connect to the running Shadow-CLJS build `:app`.

---

## Quick Cheatsheet

```clojure
;; Start / Stop playback
(play!)                   ;; Start default Phrygian Roller
(jam! :acid-roller)       ;; Launch track (:roller, :sub-roller, :acid-roller, :ambient-drift)
(stop!)                   ;; Stop all tracks and audio loops

;; Live Looper + Drum Patterns
(l! :bass  {:inst :bass  :notes (d [1 _ 1 2 _ 1 4 3]) :step "16n"})
(l! :kick  {:inst :kick  :pattern (pat! "k . . .  k . . .") :step "16n"})
(l! :hat   {:inst :hh-c  :mask (euc! 11 16) :step "16n" :dur "32n"})
(l! :drums {:hits [[:kick 1.0 "D1"] [:hh-c 0.4] [[:snare 1.0 "G3"] [:hh-c 0.45]] [:hh-c 0.4]]})

;; Live Key Modulation + Transposition
(mod-all! :f :phrygian 1) ;; Modulate all active loops to F Phrygian on the fly
(tr-all! 2)               ;; Transpose all active loops +2 semitones

;; Sound Design + Previews
(demo! :saw-bass)         ;; Audition instrument in active key
(demo-stop!)              ;; Stop auditioning
(refresh!)                ;; Hot-reload updated synth parameters and track definitions

;; Mixer + Transitions
(m! :kick :snare)         ;; Mute drums
(u! :kick :snare)         ;; Unmute drums
(sw! 400 5500 4)          ;; 4s opening lowpass filter sweep
(s!)                      ;; Dub laser siren
(drop!)                   ;; Seismic sub drop

;; Live Drummer Controls
(undrum!)                 ;; Mute all drum loops, keep bass, chords + click
(click!)                  ;; Metronome click in headphones
(redrum!)                 ;; Drop electronic drums back in

;; 3D Visuals
(g! :torus-knot)          ;; Morph 3D geometry (:icosahedron, :torus-knot, :octahedron, :sphere)
(w!)                      ;; Toggle wireframe mode
(c! "#080412" "#ff007f")  ;; Background and mesh shader colors
```

---

## Project Layout

- **`src/app/demo/tutorial.cljs`** — Interactive masterclass tutorial
- **`src/app/live/jam.cljs`** — Live performance scratchpad for fast jams
- **`src/app/custom/`** — Your sandbox for custom synths, tracks, and audio routing topologies
- **`src/app/lib/`** — Built-in synths, tracks catalog, and default DSP bus graph
- **`src/app/audio/` + `visuals/`** — WebAudio synthesis engine and Three.js WebGL visual engine

---

## License

Copyright © 2026 Alexandr Timchenko.
Licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
