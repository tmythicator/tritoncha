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
            [app.ui.hud :as hud]
            [app.visuals.engine :as visuals]))

;; --- Transport & Playback ---
(def play! tracker/play-preset!)
(def jam! tracker/play-preset!)
(def stop! mixer/stop!)
(def b! mixer/set-bpm!)
(def set-bpm! mixer/set-bpm!)

;; --- Track Orchestration & Sound Design ---
(def tracks tracker/all-tracks)
(def deftrack! tracker/deftrack!)
(def instruments voices/all-instruments)
(def definst! voices/register-instrument!)
(def defrouting! routing/register-routing!)
(def routings routing/all-routings)
(def demo! tracker/demo!)
(def demo-stop! tracker/demo-stop!)
(def refresh! tracker/refresh!)

;; --- Live Looper & Rhythms ---
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

;; --- Mixer & Transitions ---
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

;; --- FX One-Shots ---
(def s! fx/trigger-dub-siren!)
(def drop! fx/trigger-sub-drop!)
(def chord! fx/trigger-dark-chord!)

;; --- Music Theory & Live Modulation ---
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

;; --- Visuals & Aesthetics ---
(def g! visuals/set-geometry!)
(def set-geometry! visuals/set-geometry!)
(def c! visuals/set-colors!)
(def set-colors! visuals/set-colors!)
(def w! visuals/toggle-wireframe!)
(def toggle-wireframe! visuals/toggle-wireframe!)
(def pulse! state/pulse!)
(def colors utils/colors)
(def css-var utils/css-var)

;; --- Diagnostics & HUD ---
(def stat engine/audio-status)
(def stats! hud/toggle-stats!)
(def hud! hud/toggle-hud!)
