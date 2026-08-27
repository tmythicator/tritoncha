(ns app.audio.control.session
  "Live session musical key context, scale degree resolution, and modal transposition."
  (:require [app.audio.theory.harmony :as harmony]
            [app.audio.theory.patterns :refer [map-notes]]
            [app.config :as cfg]
            [app.state :refer [audio-state]]
            [app.utils.audio :refer [is-bass-track? is-drum-track? note->midi]]))

(defn current-key
  "Returns the active musical key map from application state.
  Examples: (current-key) -> {:root :e, :mode :phrygian, :octave 1}."
  []
  (:key @audio-state cfg/default-key))

(defn set-key!
  "Sets the session musical key, mode, and octave.
  Examples: (set-key! :e :phrygian 1) -> {:root :e, :mode :phrygian, :octave 1}."
  ([root mode] (set-key! root mode (:octave cfg/default-key 1)))
  ([root mode octave]
   (let [new-k {:root (keyword root) :mode (keyword mode) :octave octave}]
     (swap! audio-state assoc :key new-k)
     new-k)))

(defn d
  "Resolves degree numbers using the current session key.
  Examples: (d [1 _ 1 2]) -> ['E2' nil 'E2' 'F#2'], (d [1 3 5] 1) -> ['E1' 'G1' 'B1']."
  ([degrees] (d degrees {}))
  ([degrees opts-or-oct]
   (let [{:keys [root mode octave]} (current-key)
         opt-map (cond
                   (map? opts-or-oct) opts-or-oct
                   (number? opts-or-oct) {:octave opts-or-oct}
                   :else {:octave octave})
         effective-oct (get opt-map :octave octave)
         res (harmony/deg root mode degrees {:octave effective-oct})]
     (with-meta res {:degrees degrees :octave effective-oct :root (keyword root) :mode (keyword mode)}))))

(defn sc
  "Returns pitch strings for the current active scale.
  Examples: (sc 1) -> ['E2' 'F#2' 'G2' 'A2' 'B2' 'C#3' 'D3']."
  ([]
   (let [{:keys [root mode octave]} (current-key)]
     (harmony/scale root mode {:octave octave :octaves 2})))
  ([octaves]
   (let [{:keys [root mode octave]} (current-key)]
     (harmony/scale root mode {:octave octave :octaves octaves}))))

(defn transpose-all!
  "Transposes all active melodic loops by N semitones live.
  Examples: (transpose-all! 2), (transpose-all! -1)."
  [semitones]
  (let [delta (or semitones 0)]
    (when-not (zero? delta)
      (doseq [[kw tr] (:active-tracks @audio-state)]
        (let [cur-pat @(:pattern tr)]
          (when-not (is-drum-track? kw)
            (when-let [notes (or (:notes cur-pat) (:pattern cur-pat))]
              (let [tr-notes (map-notes #(harmony/transpose % delta) notes)]
                (swap! (:pattern tr) assoc
                       :notes tr-notes
                       :hits-vec tr-notes
                       :hits-count (count tr-notes))))))))
    delta))

(defn modulate-all!
  "Changes session key/mode and modulates all active melodic loops on the fly.
  Examples: (modulate-all! :d :dorian), (modulate-all! :b :arabic 1)."
  ([root mode] (modulate-all! root mode nil))
  ([root mode octave]
   (let [old-root   (get-in @audio-state [:key :root] (:root cfg/default-key :e))
         old-oct    (get-in @audio-state [:key :octave] (:octave cfg/default-key 1))
         target-oct (or octave old-oct)
         oct-shift  (- target-oct old-oct)
         old-midi   (note->midi (str (name old-root) "3"))
         new-midi   (note->midi (str (name root) "3"))
         delta-st   (+ (- new-midi old-midi) (* 12 oct-shift))
         new-k      (set-key! root mode target-oct)]
     (doseq [[kw tr] (:active-tracks @audio-state)]
       (let [cur-pat @(:pattern tr)]
         (when-not (is-drum-track? kw)
           (let [notes (or (:notes cur-pat) (:pattern cur-pat))
                 degs  (or (:deg cur-pat) (:degrees cur-pat) (when (vector? notes) (:degrees (meta notes))))]
             (if degs
               (let [base-oct  (or (:oct cur-pat) (:octave cur-pat) (when (vector? notes) (:octave (meta notes))) (if (is-bass-track? kw) cfg/default-bass-octave cfg/default-lead-octave))
                     track-oct (+ base-oct oct-shift)
                     new-notes (harmony/deg root mode degs {:octave track-oct})]
                 (swap! (:pattern tr) assoc
                        :notes new-notes
                        :hits-vec new-notes
                        :hits-count (count new-notes)
                        :deg degs
                        :oct track-oct))
               (when notes
                 (let [tr-notes (map-notes #(harmony/transpose % delta-st) notes)]
                   (swap! (:pattern tr) assoc
                          :notes tr-notes
                          :hits-vec tr-notes
                          :hits-count (count tr-notes)))))))))
     new-k)))
