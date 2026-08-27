(ns app.eval.core
  "In-browser CLJS evaluation engine powered by Small Clojure Interpreter (SCI)."
  (:require [sci.core :as sci]
            [app.api :as api]
            [clojure.string :as str]))

(def ^:private api-bindings
  {;; Master Playback + Transport
   'play!               api/play!
   'jam!                api/jam!
   'toggle-play!        api/toggle-play!
   'cycle-jam!          api/cycle-jam!
   'stop!               api/stop!
   'b!                  api/b!
   'set-bpm!            api/set-bpm!
   'click!              api/click!
   'toggle-click!       api/toggle-click!

   ;; Looper, Scheduler + Multi-Track Stacking
   'loop!               api/loop!
   'l!                  api/l!
   'stop-loop!          api/stop-loop!
   'clear-loops!        api/clear-loops!
   'stack!              api/stack!
   'unstack!            api/unstack!

   ;; Harmonic Music Theory + Generative Rhythms
   '_                   api/_
   'd                   api/d
   'deg                 api/deg
   'chord               api/chord
   'progression         api/progression
   'scale               api/scale
   'sc                  api/sc
   'arp                 api/arp
   'pattern             api/pattern
   'pat                 api/pat
   'euclid              api/euclid
   'euc                 api/euc

   ;; Algorithmic Time Transforms + Probability
   'fast                api/fast
   'slow                api/slow
   'rev                 api/rev
   'rotate              api/rotate
   'rot                 api/rot
   'sometimes-by        api/sometimes-by
   'sometimes           api/sometimes
   'transpose           api/transpose
   'oct-shift           api/oct-shift

   ;; Live Harmonic Modulation + Key Context
   'current-key         api/current-key
   'set-key!            api/set-key!
   'modulate-all!       api/modulate-all!
   'mod-all!            api/mod-all!
   'transpose-all!      api/transpose-all!
   'tr-all!             api/tr-all!

   ;; Audio Mixer Bus Routing + Levels
   'mute!               api/mute!
   'm!                  api/m!
   'unmute!             api/unmute!
   'u!                  api/unmute!
   'solo!               api/solo!
   'so!                 api/solo!
   'unsolo!             api/unsolo!
   'unso!               api/unso!
   'undrum!             api/undrum!
   'redrum!             api/redrum!
   'toggle-drums!       api/toggle-drums!
   'set-volume!         api/set-volume!
   'v!                  api/v!
   'toggle-bus!         api/toggle-bus!
   'mute-bus!           api/mute-bus!
   'unmute-bus!         api/unmute-bus!

   ;; Master DSP Automations + Effects
   'f!                  api/f!
   'set-filter-cutoff!  api/set-filter-cutoff!
   'q!                  api/q!
   'set-filter-q!       api/set-filter-q!
   'sw!                 api/sw!
   'sweep-filter!       api/sweep-filter!
   'dist!               api/dist!
   'set-distortion!     api/set-distortion!
   'fb!                 api/fb!
   'set-delay-feedback! api/set-delay-feedback!
   'dt!                 api/dt!
   'set-delay-time!     api/set-delay-time!
   'wet!                api/wet!
   'set-reverb-wet!     api/set-reverb-wet!

   ;; SFX Drops + Dub One-Shots
   's!                  api/s!
   'siren!              api/siren!
   'drop!               api/drop!
   'chord!              api/chord!

   ;; Catalog Registries + Live Sound Design
   'tracks              api/tracks
   'deftrack!           api/deftrack!
   'instruments         api/instruments
   'definst!            api/definst!
   'routings            api/routings
   'defrouting!         api/defrouting!
   'demo!               api/demo!
   'demo-stop!          api/demo-stop!
   'refresh!            api/refresh!

   ;; Three.js WebGL Visual Controls + 3D Scenes
   'scenes              api/scenes
   'defscene!           api/defscene!
   'scene!              api/scene!
   'set-scene!          api/set-scene!
   'cycle-scene!        api/cycle-scene!
   'g!                  api/g!
   'set-geometry!       api/set-geometry!
   'c!                  api/c!
   'set-colors!         api/set-colors!
   'w!                  api/w!
   'toggle-wireframe!   api/toggle-wireframe!
   'pulse!              api/pulse!

   ;; Realtime Diagnostics + UI HUD Overlays
   'stat                api/stat
   'status!             api/status!
   'stats!              api/stats!
   'hud!                api/hud!})

(def ^:private sci-ctx
  (delay
    (sci/init {:bindings api-bindings
               :classes {'Math js/Math}})))

(defn eval-code
  "Safely evaluates a ClojureScript code string in the browser SCI context."
  [code-str]
  (if (str/blank? code-str)
    {:ok? true :result nil}
    (try
      (let [res (sci/eval-string* @sci-ctx code-str)]
        {:ok? true :result res})
      (catch :default e
        {:ok? false :error (or (.-message e) (str e))}))))

(defn run-code
  "Evaluates code-str and returns a formatted result map {:ok? bool, :target string, :text string}."
  [code-str]
  (let [trimmed (str/trim (or code-str ""))
        display-target (if (> (count trimmed) 45)
                         (str (subs trimmed 0 42) "...")
                         trimmed)
        {:keys [ok? result error]} (eval-code trimmed)]
    (if ok?
      {:ok?     true
       :target  display-target
       :text    (str "=> " (if (nil? result) ":ok" (pr-str result)))}
      {:ok?     false
       :target  display-target
       :text    (str "Error: " error)})))
