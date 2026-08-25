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

;; Playback
(def play! tracker/play-preset!)
(def jam! tracker/play-preset!)
(def stop! mixer/stop!)
(def b! mixer/set-bpm!)
(def set-bpm! mixer/set-bpm!)

;; Track Orchestration
(def tracks tracker/all-tracks)
(def deftrack! tracker/register-track!)
(def instruments voices/all-instruments)
(def definst! voices/register-instrument!)
(def defrouting! routing/register-routing!)
(def routings routing/all-routings)
(def demo! tracker/demo!)
(def demo-stop! tracker/demo-stop!)
(def refresh! tracker/refresh!)

;; Live Looper
(def loop! looper/loop!)
(def l! looper/loop!)
(def stop-loop! mixer/stop-loop!)
(def clear-loops! mixer/clear-loops!)
(def pattern utils/pattern)
(def pat! utils/pattern)
(def euclid utils/euclid)
(def euc! utils/euclid)
(def click! looper/toggle-click!)
(def toggle-click! looper/toggle-click!)

;; Mixer + FX
(def mute! mixer/mute!)
(def m! mixer/mute!)
(def unmute! mixer/unmute!)
(def u! mixer/unmute!)
(def solo! mixer/solo!)
(def so! mixer/solo!)
(def unsolo! mixer/unsolo!)
(def unso! mixer/unsolo!)
(def undrum! mixer/undrum!)
(def redrum! mixer/redrum!)
(def toggle-bus! mixer/toggle-bus!)
(def f! fx/set-filter-cutoff!)
(def set-filter-cutoff! fx/set-filter-cutoff!)
(def sw! fx/sweep-filter!)
(def sweep-filter! fx/sweep-filter!)
(def fb! fx/set-delay-feedback!)
(def wet! fx/set-reverb-wet!)

;; FX shots
(def s! fx/trigger-dub-siren!)
(def drop! fx/trigger-sub-drop!)
(def chord! fx/trigger-dark-chord!)

;; Live Modulation ---
(def _ theory/_)
(def d theory/d)
(def deg theory/deg)
(def chord theory/chord)
(def progression theory/progression)
(def scale theory/scale)
(def sc theory/sc)
(def arp theory/arp)
(def transpose theory/transpose)
(def oct-shift theory/oct-shift)
(def set-key! theory/set-key!)
(def mod-all! looper/modulate-all!)
(def tr-all! looper/transpose-all!)

;; Visuals
(def scenes visuals/all-scenes)
(def defscene! visuals/register-scene!)
(def scene! visuals/load-scene!)
(def set-scene! visuals/load-scene!)
(def g! visuals/set-geometry!)
(def set-geometry! visuals/set-geometry!)
(def c! visuals/set-colors!)
(def set-colors! visuals/set-colors!)
(def w! visuals/toggle-wireframe!)
(def toggle-wireframe! visuals/toggle-wireframe!)
(def pulse! state/pulse!)
(def colors utils/colors)
(def css-var utils/css-var)

;; Diagnostics/HUD
(def stat engine/audio-status)
(defn stats! [] (swap! state/ui-state update :stats-visible? not))
(defn hud! [] (swap! state/ui-state update :hud-visible? not))
