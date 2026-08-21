(ns app.audio.tracker
  "Track registry, scene orchestrator, and algorithmic preset launcher."
  (:require [app.state :refer [state]]
            [app.audio.engine :refer [init-audio!]]
            [app.audio.theory :refer [set-key! chord d]]
            [app.audio.looper :refer [loop! stop-loop! pattern]]
            [app.audio.mixer :refer [stop! set-bpm!]]
            [app.audio.fx :refer [set-filter-cutoff!]]
            [app.visuals.engine :refer [set-geometry! set-colors!]]
            [app.lib.tracks :refer [core-tracks]]
            [app.custom.tracks :refer [user-tracks]]
            [app.audio.voices :refer [reload-instruments! all-drum-keys]]))

;; Track Registry (Built-in + Custom User + REPL Live Tracks)

(defonce repl-tracks (atom {}))

(defn register-track!
  "Registers or updates a dynamic track in the REPL registry.
  Examples: (register-track! :my-jam {:bpm 170 :scale [:d :dorian 1] :tracks {...}})."
  [track-key spec]
  (swap! repl-tracks assoc track-key spec)
  track-key)

(def deftrack! register-track!)

(defn all-tracks
  "Returns a merged map of core built-in tracks, user custom tracks, and REPL tracks.
  Examples: (all-tracks)."
  []
  (merge core-tracks user-tracks @repl-tracks))

(def tracks all-tracks)

;; Track Playback Orchestrator

(defn play-preset!
  "Launches a track by keyword (from all-tracks) or custom data map.
  Examples: (play-preset! :roller), (play-preset! {:bpm 165 :scale [:f :phrygian 1] ...})."
  [preset-spec]
  (init-audio!)
  (stop!)
  (let [available (all-tracks)
        preset-map (cond
                     (map? preset-spec) preset-spec
                     (contains? available preset-spec) (get available preset-spec)
                     :else (:roller available))
        preset-key (if (keyword? preset-spec) preset-spec :custom)
        {:keys [bpm scale geom colors cutoff tracks]} preset-map
        [bg-c mesh-c] (or colors ["#080412" "#ff007f"])]

    (swap! state assoc :current-jam preset-key :active? true)
    (when scale
      (let [[r m oct] scale]
        (set-key! r m (or oct 2))))
    (when bpm (set-bpm! bpm))
    (when geom (set-geometry! geom))
    (when colors (set-colors! bg-c mesh-c))
    (when cutoff (set-filter-cutoff! cutoff))

    (doseq [[track-name track-opts] tracks]
      (loop! track-name track-opts))))

(def jam! play-preset!)
(def play! play-preset!)

(defn reload-track!
  "Reloads and restarts the currently active track preset with updated track data.
  Examples: (reload-track!)."
  []
  (let [cur-jam (:current-jam @state)]
    (when (and cur-jam (not= cur-jam :none))
      (play-preset! cur-jam))))

(def refresh-track! reload-track!)

(defn refresh!
  "Refreshes both custom sound design synth nodes and current track playback.
  Examples: (refresh!)."
  []
  (reload-instruments!)
  (reload-track!)
  :ok)

;; Instrument Auditioning + Previews

(defn demo!
  "Plays a live preview loop for any instrument in the current key.
  Examples: (demo! :acid-bass), (demo! :dark-pad), (demo! :kick)."
  [inst-key]
  (let [kw (keyword inst-key)]
    (cond
      (contains? (all-drum-keys) kw)
      (loop! :demo {:inst kw :notes (pattern "k . . .  k . . .  . . k .  . . . .") :step "16n"})

      (contains? #{:dark-pad :pad :ambient-glass} kw)
      (loop! :demo {:inst kw :notes [(chord :e :min9 3) (chord :c :maj7 3)] :step "1m" :dur "1m" :vel 0.4})

      :else
      (loop! :demo {:inst kw :notes (d [1 2 3 5 7 8 5 3 1] 2) :step "16n" :dur "16n" :vel 0.85}))))

(defn demo-stop!
  "Stops and deletes the active preview demo loop.
  Examples: (demo-stop!)."
  []
  (stop-loop! :demo))
