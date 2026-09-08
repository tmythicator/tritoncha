(ns app.custom.tracks
  "User custom tracks, live-coding sets, and arrangements.")

;; Custom Tracks Catalog (Same format as app.lib.tracks)
;;
;; Track parameters:
;;   :name    - Title displayed in HUD
;;   :bpm     - Master tempo (e.g. 172)
;;   :scale   - Vector [root mode octave] (e.g. [:e :hirajoshi 2])
;;   :geom    - Single 3D visual geometry fallback (:torus-knot, :icosahedron, etc.)
;;   :figures - Map of named 3D figures in scene: {:core {:geom ... :pos [...] :scale ... :colors ...}}
;;   :colors  - Vector of [background-hex mesh-hex]
;;   :cutoff  - Master filter cutoff frequency in Hz (e.g. 4200)
;;   :tracks  - Map of loops to launch simultaneously (bind :figure :core to react to loop pulses)

(def user-tracks
  {})