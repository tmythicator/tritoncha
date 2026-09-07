(ns app.audio.control.tracker
  "Track presets registry, playback orchestrator, and instrument preview demos."
  (:require [app.audio.control.looper :refer [loop! set-bpm! set-drum-mode! stop! stop-loop!]]
            [app.audio.control.session :as session]
            [app.audio.dsp.busses :as busses]
            [app.audio.dsp.engine :refer [init-audio!]]
            [app.audio.dsp.fx :refer [set-filter-cutoff!]]
            [app.audio.dsp.instruments :refer [reload-instruments!]]
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

(def ^:private track-aliases
  {:roller :metro-roller})

(defn all-tracks
  "Returns a merged map of core built-in tracks, user custom tracks, and REPL tracks."
  []
  (merge core-tracks user-tracks (:tracks @repl-registry)))

(defn track-keys
  "Returns a vector of all available track keywords across core, custom, and REPL catalogs."
  []
  (vec (keys (all-tracks))))

(defn default-track-key
  "Returns the default track keyword."
  []
  (or (first (track-keys)) :metro-roller))

(defn play-preset!
  "Launches a track by keyword (from all-tracks) or custom data map.
  Examples: (play-preset! :metro-roller), (play-preset! {:bpm 165 :scale [:f :phrygian 1] ...})."
  [preset-spec]
  (init-audio!)
  (stop!)
  (let [available  (all-tracks)
        target-key (if (keyword? preset-spec)
                     (get track-aliases preset-spec preset-spec)
                     preset-spec)
        preset-map (cond
                     (map? target-key) target-key
                     (contains? available target-key) (get available target-key)
                     :else (or (val (first available)) (get core-tracks :metro-roller)))
        preset-key (cond
                     (keyword? target-key) target-key
                     (keyword? preset-spec) preset-spec
                     :else :custom)
        {:keys [bpm scale geom colors cutoff tracks mod kit]} preset-map
        [bg-c mesh-c] (or colors [(:bg cfg/default-scene-colors) (:mesh cfg/default-scene-colors)])]

    (swap! audio-state assoc :current-jam preset-key :active? true :track-cutoff cutoff)
    (when-let [drum-m (or mod (when (keyword? kit) kit) (:mod kit))]
      (set-drum-mode! drum-m))
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
    (play-preset! (or (:current-jam @audio-state) (default-track-key)))))

(defn- cycle-track!
  [cycle-fn]
  (let [tks (track-keys)
        cur (or (:current-jam @audio-state) (first tks))]
    (play-preset! (cycle-fn cur tks))))

(defn next-jam!
  "Cycles to the next available jam track preset across core and custom catalogs.
  Examples: (next-jam!)."
  []
  (cycle-track! coll/cycle-next))

(defn prev-jam!
  "Cycles to the previous available jam track preset across core and custom catalogs.
  Examples: (prev-jam!)."
  []
  (cycle-track! coll/cycle-prev))

(def cycle-jam! next-jam!)

(defn jam-list
  "Returns a vector of metadata maps for all available jam presets across core, custom, and REPL catalogs.
  Useful for UI dropdowns, modals, and preset preview cards."
  []
  (mapv (fn [[k t]]
          {:id    k
           :name  (or (:name t) (name k))
           :bpm   (or (:bpm t) 168)
           :scale (or (:scale t) [:e :minor 1])
           :geom  (or (:geom t) :torus-knot)})
        (all-tracks)))

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
      (busses/drum? kw)
      (loop! :demo {:inst kw :notes (pattern "k . . .  k . . .  . . k .  . . . .") :step cfg/default-step})

      (busses/pad? kw)
      (loop! :demo {:inst kw :notes [(chord root :min9 3) (chord root :maj7 3)] :step "1m" :dur "1m" :vel 0.4})

      :else
      (loop! :demo {:inst kw :notes (session/d [1 2 3 5 7 8 5 3 1] cfg/default-lead-octave) :step cfg/default-step :dur cfg/default-step :vel cfg/default-velocity}))))

(defn demo-stop!
  "Stops and deletes the active preview demo loop."
  []
  (stop-loop! :demo))
