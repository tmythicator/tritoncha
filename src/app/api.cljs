(ns app.api
  "Unified public live-coding API and shortcuts."
  (:require [app.utils :as utils]
            [app.audio.theory :as theory]
            [app.audio.voices :as voices]
            [app.audio.engine :as engine]
            [app.audio.mixer :as mixer]
            [app.audio.looper :as looper]
            [app.audio.routing :as routing]
            [app.audio.tracker :as tracker]
            [app.audio.fx :as fx]
            [app.visuals.engine :as visuals]))

;; Generative + Math Utilities
(def rot utils/rotate)
(def pick utils/pick-one)
(def prob? utils/prob)
(def lerp utils/lerp)
(def clamp utils/clamp)

;; Transport + Track Orchestration
(def tracks tracker/all-tracks)
(def jam! tracker/play-preset!)
(def play! tracker/play-preset!)
(def stop! mixer/stop!)
(def deftrack! tracker/deftrack!)

;; Sound Design + Instruments
(def instruments voices/all-instruments)
(def definst! voices/register-instrument!)
(def make-instrument voices/create-instrument)
(def reload-instruments! voices/reload-instruments!)
(def refresh-instruments! voices/reload-instruments!)
(def reload-track! tracker/reload-track!)
(def refresh-track! tracker/reload-track!)
(def demo! tracker/demo!)
(def demo-stop! tracker/demo-stop!)

(def refresh! tracker/refresh!)

;; Audio Graph + Routing Topologies
(def routings routing/all-routings)
(def defrouting! routing/register-routing!)
(def defgraph! routing/register-routing!)

;; Engine + Diagnostics
(def stat engine/audio-status)

;; DSP FX + Audio Automations
(def f! fx/set-filter-cutoff!)
(def q! fx/set-filter-q!)
(def sw! fx/sweep-filter!)
(def dist! fx/set-distortion!)
(def fb! fx/set-delay-feedback!)
(def wet! fx/set-reverb-wet!)
(def s! fx/trigger-dub-siren!)
(def drop! fx/trigger-sub-drop!)
(def chord! fx/trigger-dark-chord!)
(def hit! fx/drum-hit!)

;; Bus + Track Mixer Controls
(def b! mixer/set-bpm!)
(def bpm! mixer/set-bpm!)
(def bvol! mixer/set-bus-volume!)
(def mb! mixer/mute-bus!)
(def ub! mixer/unmute-bus!)
(def tb! mixer/toggle-bus!)
(def m! mixer/mute!)
(def u! mixer/unmute!)
(def tm! mixer/toggle-mute!)
(def so! mixer/solo!)
(def unso! mixer/unsolo!)
(def mt! mixer/mute-type!)
(def ut! mixer/unmute-type!)
(def undrum! mixer/undrum!)
(def redrum! mixer/redrum!)
(def all-mute! mixer/all-mute!)
(def all-unmute! mixer/all-unmute!)
(def flip-mute! mixer/flip-mute!)
(def stop-loop! mixer/stop-loop!)
(def clear-loops! mixer/clear-loops!)

;; Visuals + 3D WebGL
(def g! visuals/set-geometry!)
(def c! visuals/set-colors!)
(def w! visuals/toggle-wireframe!)

;; Music Theory + Live Modulation
(def key! theory/set-key!)
(def mod-all! looper/modulate-all!)
(def tr-all! looper/transpose-all!)
(def sc! theory/scale)
(def ch! theory/chord)
(def deg! theory/deg)
(def prog theory/progression)
(def arp! theory/arp)
(def tr! theory/transpose)
(def oct! theory/oct-shift)

;; Live Looper + Rhythms
(def l! looper/loop!)
(def loop! looper/loop!)
(def euc! looper/euclid)
(def pat! looper/pattern)
(def click! looper/toggle-click!)
