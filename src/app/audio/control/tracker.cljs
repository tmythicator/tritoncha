(ns app.audio.control.tracker
  "Track presets registry, playback orchestrator, and instrument preview demos."
  (:require [app.audio.control.looper :refer [loop! set-bpm! stop! stop-loop!]]
            [app.audio.control.session :as session]
            [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.fx :refer [set-filter-cutoff!]]
            [app.audio.dsp.instruments :refer [all-drum-keys reload-instruments!]]
            [app.audio.theory.harmony :refer [chord]]
            [app.audio.theory.patterns :refer [pattern]]
            [app.config :as cfg]
            [app.custom.tracks :refer [user-tracks]]
            [app.lib.tracks :refer [core-tracks]]
            [app.state :refer [audio-state repl-registry]]
            [app.utils.coll :as coll]
            [app.visuals.engine :refer [set-colors! set-geometry!]]))

(defn register-track!
  "Registers or updates a dynamic track in the REPL registry."
  [track-key spec]
  (swap! repl-registry assoc-in [:tracks track-key] spec)
  track-key)

(defn all-tracks
  "Returns a merged map of core built-in tracks, user custom tracks, and REPL tracks."
  []
  (merge core-tracks user-tracks (:tracks @repl-registry)))

(defn play-preset!
  "Launches a track by keyword (from all-tracks) or custom data map.
  Examples: (play-preset! :roller), (play-preset! {:bpm 165 :scale [:f :phrygian 1] ...})."
  [preset-spec]
  (init-audio!)
  (stop!)
  (let [available  (all-tracks)
        preset-map (cond
                     (map? preset-spec) preset-spec
                     (contains? available preset-spec) (get available preset-spec)
                     :else (get available (first cfg/jam-presets) (:roller core-tracks)))
        preset-key (if (keyword? preset-spec) preset-spec :custom)
        {:keys [bpm scale geom colors cutoff tracks]} preset-map
        [bg-c mesh-c] (or colors [(:bg cfg/default-scene-colors) (:mesh cfg/default-scene-colors)])]

    (swap! audio-state assoc :current-jam preset-key :active? true)
    (when scale
      (let [[r m oct] scale]
        (session/set-key! r m (or oct (:octave cfg/default-key 1)))))
    (when bpm (set-bpm! bpm))
    (when geom (set-geometry! geom))
    (when colors (set-colors! bg-c mesh-c))
    (when cutoff (set-filter-cutoff! cutoff))

    (doseq [[track-name track-opts] tracks]
      (loop! track-name track-opts))))

(defn toggle-play!
  "Toggles playback between active jam preset and stop.
  Examples: (toggle-play!)."
  []
  (if (:active? @audio-state)
    (stop!)
    (play-preset! (:current-jam @audio-state :roller))))

(defn cycle-jam!
  "Cycles to the next built-in jam track preset.
  Examples: (cycle-jam!)."
  []
  (let [next-jam (coll/cycle-next (:current-jam @audio-state :roller) cfg/jam-presets)]
    (play-preset! next-jam)))

(defn reload-track!
  "Reloads and restarts the currently active track preset with updated track data."
  []
  (let [cur-jam (:current-jam @audio-state)]
    (when (and cur-jam (not= cur-jam :none))
      (play-preset! cur-jam))))

(defn refresh!
  "Refreshes both custom sound design synth nodes and current track playback."
  []
  (reload-instruments!)
  (reload-track!)
  :ok)

(defn demo!
  "Plays a live preview loop for any instrument in the current key.
  Examples: (demo! :acid-bass), (demo! :dark-pad), (demo! :kick)."
  [inst-key]
  (let [kw   (keyword inst-key)
        root (:root (session/current-key) :e)]
    (cond
      (contains? (all-drum-keys) kw)
      (loop! :demo {:inst kw :notes (pattern "k . . .  k . . .  . . k .  . . . .") :step cfg/default-step})

      (contains? #{:dark-pad :pad :ambient-glass} kw)
      (loop! :demo {:inst kw :notes [(chord root :min9 3) (chord root :maj7 3)] :step "1m" :dur "1m" :vel 0.4})

      :else
      (loop! :demo {:inst kw :notes (session/d [1 2 3 5 7 8 5 3 1] cfg/default-lead-octave) :step cfg/default-step :dur cfg/default-step :vel cfg/default-velocity}))))

(defn demo-stop!
  "Stops and deletes the active preview demo loop."
  []
  (stop-loop! :demo))
