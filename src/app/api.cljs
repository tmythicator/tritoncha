(ns app.api
  "Unified public live-coding API, shortcuts, and orchestrator facade."
  (:require [app.audio.control.looper :as looper]
            [app.audio.control.mixer :as mixer]
            [app.audio.control.session :as session]
            [app.audio.control.tracker :as tracker]
            [app.audio.dsp.busses :as busses]
            [app.audio.dsp.fx :as fx]
            [app.audio.dsp.instruments :as inst]
            [app.audio.dsp.routing :as routing]
            [app.audio.dsp.telemetry :as telemetry]
            [app.audio.theory.harmony :as harmony]
            [app.audio.theory.patterns :as patterns]
            [app.state :as state]
            [app.utils.coll :as coll]
            [app.visuals.engine :as visuals]))

;; Master Playback + Transport
(def play! tracker/play-preset!)
(def jam! tracker/play-preset!)
(def toggle-play! tracker/toggle-play!)
(def cycle-jam! tracker/cycle-jam!)
(def next-jam! tracker/next-jam!)
(def prev-jam! tracker/prev-jam!)
(def jam-list tracker/jam-list)
(def stop! looper/stop!)
(def b! looper/set-bpm!)
(def set-bpm! looper/set-bpm!)
(def click! looper/click!)
(def toggle-click! looper/toggle-click!)
(def set-click! looper/set-click!)

;; Looper, Scheduler + Multi-Track Stacking
(def loop! looper/loop!)
(def l! looper/loop!)
(def stop-loop! looper/stop-loop!)
(def clear-loops! looper/clear-loops!)
(def stack! looper/stack!)
(def unstack! looper/unstack!)
(def set-drum-mode! looper/set-drum-mode!)
(def mod! looper/mod!)

;; Harmonic Music Theory + Generative Rhythms
(def _ harmony/_)
(def d session/d)
(def deg harmony/deg)
(def chord harmony/chord)
(def progression harmony/progression)
(def scale harmony/scale)
(def sc session/sc)
(def arp harmony/arp)
(def pattern patterns/pattern)
(def pat patterns/pattern)
(def euclid patterns/euclid)
(def euc patterns/euclid)

;; Algorithmic Time Transforms + Probability
(def fast patterns/fast)
(def slow patterns/slow)
(def rev patterns/rev)
(def shift patterns/shift)
(def take-steps patterns/take-steps)
(def every-n patterns/every-n)
(def rotate coll/rotate)
(def rot coll/rotate)
(def sometimes-by patterns/sometimes-by)
(def sometimes patterns/sometimes)
(def transpose harmony/transpose)
(def oct-shift harmony/oct-shift)

;; Live Harmonic Modulation + Key Context
(def current-key session/current-key)
(def set-key! session/set-key!)
(def modulate-all! session/modulate-all!)
(def mod-all! session/modulate-all!)
(def transpose-all! session/transpose-all!)
(def tr-all! session/transpose-all!)

;; Audio Mixer Bus Routing + Levels
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
(def toggle-drums! mixer/toggle-drums!)
(def unbass! mixer/unbass!)
(def rebass! mixer/rebass!)
(def toggle-bass! mixer/toggle-bass!)
(def unlead! mixer/unlead!)
(def relead! mixer/relead!)
(def toggle-leads! mixer/toggle-leads!)
(def unpad! mixer/unpad!)
(def repad! mixer/repad!)
(def toggle-pads! mixer/toggle-pads!)

;; Sound Classification and Bus Introspection Helpers
(def drum? busses/drum?)
(def bass? busses/bass?)
(def sub? busses/sub?)
(def lead? busses/lead?)
(def pad? busses/pad?)
(def fx? busses/fx?)
(def synth? busses/synth?)
(def sound-category busses/sound-category)
(def category-bus busses/category-default-bus)
(def bus busses/instrument-bus)

(def set-volume! mixer/set-volume!)
(def v! mixer/set-volume!)
(def toggle-bus! mixer/toggle-bus!)
(def mute-bus! mixer/mute-bus!)
(def unmute-bus! mixer/unmute-bus!)
(def set-send! mixer/set-send!)
(def send! mixer/set-send!)
(def bus-send! mixer/set-send!)

;; Master DSP Automations + Effects
(def f! fx/set-filter-cutoff!)
(def set-filter-cutoff! fx/set-filter-cutoff!)
(def q! fx/set-filter-q!)
(def set-filter-q! fx/set-filter-q!)
(def sw! fx/sweep-filter!)
(def sweep-filter! fx/sweep-filter!)
(def dist! fx/set-distortion!)
(def set-distortion! fx/set-distortion!)
(def bitcrush! fx/set-bitcrush!)
(def chorus! fx/set-chorus!)
(def sidechain! fx/set-sidechain!)
(def fb! fx/set-delay-feedback!)
(def set-delay-feedback! fx/set-delay-feedback!)
(def dt! fx/set-delay-time!)
(def set-delay-time! fx/set-delay-time!)
(def wet! fx/set-reverb-wet!)
(def set-reverb-wet! fx/set-reverb-wet!)
(def wf! fx/set-filter-cutoff!)
(def wq! fx/set-filter-q!)
(def worklet-cutoff! fx/set-filter-cutoff!)
(def worklet-resonance! fx/set-filter-q!)

;; SFX Drops + Dub One-Shots
(def s! fx/trigger-dub-siren!)
(def siren! fx/trigger-dub-siren!)
(def drop! fx/trigger-sub-drop!)
(def chord! fx/trigger-dark-chord!)

;; Catalog Registries + Live Sound Design
(def tracks tracker/all-tracks)
(def deftrack! tracker/register-track!)
(def instruments inst/all-instruments)
(def definst! inst/defsynth!)
(def defsynth! inst/defsynth!)
(def defdrum! inst/defdrum!)
(def patch-drum! inst/patch-drum!)
(def drum! inst/defdrum!)
(def patch! inst/patch!)
(def reset-inst! inst/reset-instrument!)
(def reset-instrument! inst/reset-instrument!)
(def routings routing/all-routings)
(def defrouting! routing/register-routing!)
(def route! routing/set-routing!)
(def set-routing! routing/set-routing!)
(def demo! tracker/demo!)
(def demo-stop! tracker/demo-stop!)
(def refresh! tracker/refresh!)

;; Three.js WebGL Visual Controls + 3D Scenes
(def scenes visuals/all-scenes)
(def defscene! visuals/register-scene!)
(def scene! visuals/load-scene!)
(def set-scene! visuals/load-scene!)
(def cycle-scene! visuals/cycle-scene!)
(def g! visuals/set-geometry!)
(def set-geometry! visuals/set-geometry!)
(def c! visuals/set-colors!)
(def set-colors! visuals/set-colors!)
(def w! visuals/toggle-wireframe!)
(def toggle-wireframe! visuals/toggle-wireframe!)
(def pulse! state/pulse!)

;; Realtime Diagnostics + UI HUD Overlays
(def stat telemetry/audio-status)
(def status! telemetry/audio-status)
(def reset-stats! telemetry/reset-telemetry-metrics!)
(defn stats! [] (swap! state/ui-state update :stats-visible? not))
(defn hud! [] (swap! state/ui-state update :hud-visible? not))
(defn instruments! [] (swap! state/ui-state update :instrument-browser-open? not))
(def inst! instruments!)
(defn jams! [] (swap! state/ui-state update :track-browser-open? not))
