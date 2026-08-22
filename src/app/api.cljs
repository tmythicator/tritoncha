(ns app.api
  "Unified public live-coding API and shortcuts."
  (:require [app.utils :as utils]
            [app.state :as state]
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
(def set-filter-cutoff! fx/set-filter-cutoff!)
(def f! fx/set-filter-cutoff!)
(def set-filter-q! fx/set-filter-q!)
(def q! fx/set-filter-q!)
(def sweep-filter! fx/sweep-filter!)
(def sw! fx/sweep-filter!)
(def set-distortion! fx/set-distortion!)
(def dist! fx/set-distortion!)
(def set-delay-feedback! fx/set-delay-feedback!)
(def fb! fx/set-delay-feedback!)
(def set-reverb-wet! fx/set-reverb-wet!)
(def wet! fx/set-reverb-wet!)
(def trigger-dub-siren! fx/trigger-dub-siren!)
(def s! fx/trigger-dub-siren!)
(def trigger-sub-drop! fx/trigger-sub-drop!)
(def drop! fx/trigger-sub-drop!)
(def trigger-dark-chord! fx/trigger-dark-chord!)
(def chord! fx/trigger-dark-chord!)
(def hit! fx/drum-hit!)

;; Bus + Track Mixer Controls
(def set-bpm! mixer/set-bpm!)
(def b! mixer/set-bpm!)
(def bpm! mixer/set-bpm!)
(def bvol! mixer/set-bus-volume!)
(def mute-bus! mixer/mute-bus!)
(def mb! mixer/mute-bus!)
(def unmute-bus! mixer/unmute-bus!)
(def ub! mixer/unmute-bus!)
(def toggle-bus! mixer/toggle-bus!)
(def tb! mixer/toggle-bus!)
(def mute! mixer/mute!)
(def m! mixer/mute!)
(def unmute! mixer/unmute!)
(def u! mixer/unmute!)
(def toggle-mute! mixer/toggle-mute!)
(def tm! mixer/toggle-mute!)
(def solo! mixer/solo!)
(def so! mixer/solo!)
(def unsolo! mixer/unsolo!)
(def unso! mixer/unsolo!)
(def mute-type! mixer/mute-type!)
(def mt! mixer/mute-type!)
(def unmute-type! mixer/unmute-type!)
(def ut! mixer/unmute-type!)
(def undrum! mixer/undrum!)
(def redrum! mixer/redrum!)
(def all-mute! mixer/all-mute!)
(def all-unmute! mixer/all-unmute!)
(def flip-mute! mixer/flip-mute!)
(def stop-loop! mixer/stop-loop!)
(def clear-loops! mixer/clear-loops!)

;; Visuals + 3D WebGL
(def set-geometry! visuals/set-geometry!)
(def g! visuals/set-geometry!)
(def set-colors! visuals/set-colors!)
(def c! visuals/set-colors!)
(def toggle-wireframe! visuals/toggle-wireframe!)
(def w! visuals/toggle-wireframe!)
(def pulse! state/pulse!)

;; Music Theory + Live Modulation
(def _ theory/_)
(def d theory/d)
(def deg theory/deg)
(def deg! theory/deg)
(def chord theory/chord)
(def ch! theory/chord)
(def scale theory/scale)
(def sc theory/sc)
(def sc! theory/scale)
(def progression theory/progression)
(def prog theory/progression)
(def arp theory/arp)
(def arp! theory/arp)
(def transpose theory/transpose)
(def tr! theory/transpose)
(def oct-shift theory/oct-shift)
(def oct! theory/oct-shift)
(def set-key! theory/set-key!)
(def key! theory/set-key!)
(def mod-all! looper/modulate-all!)
(def tr-all! looper/transpose-all!)

;; Live Looper + Rhythms
(def loop! looper/loop!)
(def l! looper/loop!)
(def euclid looper/euclid)
(def euc! looper/euclid)
(def pattern looper/pattern)
(def pat! looper/pattern)
(def toggle-click! looper/toggle-click!)
(def click! looper/toggle-click!)
