# Tritoncha

In-browser live-coding studio for synthesized music ([`Tone.js`](https://github.com/Tonejs/Tone.js/)) and reactive 3D WebGL visuals ([`Three.js`](https://github.com/mrdoob/three.js/)), controlled live from your editor via a ClojureScript REPL (e.g. Emacs + [`CIDER`](https://github.com/clojure-emacs/cider)).

---

## What is Tritoncha?

Tritoncha is a self-contained live-coding instrument for Algorave performances and jamming.

You write ClojureScript in your editor, send lines to the REPL and the browser makes real-time sound and 3D graphics at the same time.

### Q&A

Q: **Is it like TidalCycles, Sonic Pi or Overtone?**  
A: Similar in spirit, but runs entirely in the browser. Tools like TidalCycles and Sonic Pi require installing SuperCollider (`scsynth`), configuring audio servers and connecting external visualizers. With Tritoncha, there is no SuperCollider or background audio daemon setup. You just open the page, connect your REPL and start playing.

Q: **Is it a WebDAW?**  
A: It has synths, effects, and mixing buses like a DAW, but you control them with code instead of mouse clicks on a static timeline. You sequence notes with scale degrees (`d`), chords, drum patterns (`pat!`) and Euclidean rhythms (`euc!`) live in the REPL while the music is running.

Q: **How do the 3D visuals react to the audio?**  
A: Direct state sharing with zero delay. It does not just listen to microphone input. Every kick hit, snare, and bass envelope directly triggers visual pulses in Three.js on every animation frame.

Q: **Does it work offline?**  
A: Yes, 100%. No internet connection, cloud services or external plugins are required. Everything is compiled into static JavaScript.

---

## Quick Start

```bash
# 1. Enter the dev environment
nix develop
# (or simply `direnv allow` if you use direnv)

# 2. Start the dev server
pnpm run dev
```

Open [http://localhost:3000](http://localhost:3000) and click anywhere on the page to unlock the browser's WebAudio context.

---

## Interactive Tutorial & REPL Workflow

The fastest way to learn Tritoncha is through the interactive tutorial:

**Open [`src/app/demo/tutorial.cljs`](src/app/demo/tutorial.cljs)**

It guides you step-by-step through:

- Making basslines with scale degrees (`d`) and live drums (`pat!`, `euc!`)
- Sound design and auditioning custom synthesizers (`definst!`, `demo!`)
- Custom audio routing and effects chains (`defrouting!`)
- Modal harmony, progressions and live key modulation (`mod-all!`, `tr-all!`)
- Composing full track arrangements and live performance tricks

### Recommended Setup

For the best experience, use an editor with strong REPL-Driven Development (RDD) support:

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
(f! 450)                  ;; Set lowpass filter cutoff
(sw! 400 5500 4)          ;; 4-second opening filter sweep into the drop
(s!)                      ;; Dub laser siren
(drop!)                   ;; Seismic sub-bass drop

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

- `src/app/demo/tutorial.cljs` Interactive masterclass tutorial
- `src/app/live/jam.cljs` Live performance scratchpad for fast jams
- `src/app/custom/` Your sandbox for custom synths, tracks and audio routings
- `src/app/lib/` Built-in synths, tracks catalog and default bus routing
- `src/app/audio/` + `visuals/` WebAudio synthesis engine and Three.js WebGL visual engine

---

## License

Copyright © 2026 Alexandr Timchenko.
Licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
