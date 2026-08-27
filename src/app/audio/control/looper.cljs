(ns app.audio.control.looper
  "Live looper, scheduler and master transport engine."
  (:require [app.audio.control.session :as session]
            [app.audio.dsp.engine :refer [create-sequence init-audio!]]
            [app.audio.dsp.instruments :as inst]
            [app.config :as cfg]
            [app.state :refer [audio-state engine-ctx]]
            [app.utils.audio :refer [is-bass-track? is-drum-track?]]
            [app.utils.math :refer [clamp]]
            [reagent.core :as r]))

(defn- track-audible?
  "Checks whether a track should produce sound based on its mute/solo state and global solo mode."
  [{:keys [muted? solo?]}]
  (and (not @muted?)
       (or (not (:solo-mode? @audio-state)) @solo?)))

(defn- ensure-transport-running! []
  (when-let [t (:transport (:tone @engine-ctx))]
    (when (not= (.-state ^js t) "started")
      (.start ^js t "+0.05"))))

(defn- trigger-hit!
  "Dispatches a single rhythm/melodic hit to Tone.js instrument or drum voice."
  [hit inst-key synth-node dur time vel]
  (cond
    (and (vector? hit) (keyword? (first hit)))
    (let [[k v n] hit
          final-vel (or v vel)]
      (if (is-drum-track? k)
        (inst/trigger-drum! k n dur time final-vel)
        (inst/trigger-note! (or (get (:tone @engine-ctx) k) synth-node) n dur time final-vel k)))

    (and (keyword? hit) (is-drum-track? hit))
    (inst/trigger-drum! hit nil dur time vel)

    (and (true? hit) (is-drum-track? inst-key))
    (inst/trigger-drum! inst-key nil dur time vel)

    :else
    (inst/trigger-note! synth-node hit dur time vel inst-key)))

(defn- execute-step-callback!
  "Zero-allocation step callback executed on each quantization tick of Tone.Sequence."
  [track-info time step-idx synth-node inst-key]
  (when (track-audible? track-info)
    (let [{:keys [mask vel dur] :or {vel cfg/default-velocity dur cfg/default-step} :as pat} @(:pattern track-info)
          hits (or (:notes pat) (:hits-vec pat) (:pattern pat) (:hits pat) [true])
          cnt  (count hits)]
      (when (pos? cnt)
        (let [masked? (when mask (nil? (nth mask (mod step-idx (count mask)))))]
          (when-not masked?
            (when-let [hit (nth hits (mod step-idx cnt))]
              (let [step-vel (if (vector? vel) (nth vel (mod step-idx (count vel))) vel)]
                (trigger-hit! hit inst-key synth-node dur time step-vel)))))))))

(defn loop!
  "Schedules or hot-swaps an audio loop track in the live-coding session.
  Examples: (loop! :bass {:notes (d [1 2 3]) :step \"16n\"})."
  [track-name pattern-map]
  (init-audio!)
  (let [tk        (keyword track-name)
        raw-data  (if (vector? pattern-map) {:notes pattern-map} pattern-map)
        notes-in  (:notes raw-data)
        meta-info (when (vector? notes-in) (meta notes-in))
        degs      (or (:deg raw-data) (:degrees raw-data) (:degrees meta-info))
        with-degs (if (and degs (not notes-in))
                    (let [oct (or (:oct raw-data) (:octave raw-data))]
                      (assoc raw-data :notes (session/d degs (if oct {:octave oct} {}))))
                    raw-data)
        notes     (:notes with-degs)
        oct       (or (:oct with-degs)
                      (:octave with-degs)
                      (when (vector? notes) (:octave (meta notes)))
                      (:octave meta-info)
                      (if (is-bass-track? tk) cfg/default-bass-octave cfg/default-lead-octave))
        pat-data  (cond-> (assoc with-degs :oct oct :hits-vec notes :hits-count (count notes))
                    degs (assoc :deg degs))
        step      (or (:step pat-data) cfg/default-step)]
    (if-let [tr (get (:active-tracks @audio-state) tk)]
      (reset! (:pattern tr) pat-data)
      (let [inst-k   (or (:inst pat-data) (:synth pat-data) tk)
            synth    (or (get (:tone @engine-ctx) inst-k) (get (:tone @engine-ctx) :saw-bass))
            tr-info  {:pattern (atom pat-data) :muted? (r/atom false) :solo? (r/atom false) :synth synth :inst-key inst-k}
            seq-obj  (create-sequence #(execute-step-callback! tr-info %1 %2 synth inst-k) (into-array (range cfg/sequence-length)) step)]
        (swap! audio-state assoc-in [:active-tracks tk] (assoc tr-info :sequence seq-obj))))
    (ensure-transport-running!)
    (swap! audio-state assoc :active? true)
    tk))

(defn set-bpm!
  "Updates the master tempo in BPM.
  Examples: (set-bpm! 174)."
  [bpm]
  (let [clamped-bpm (clamp bpm cfg/min-bpm cfg/max-bpm)]
    (when-let [t (:transport (:tone @engine-ctx))]
      (set! (.. ^js t -bpm -value) clamped-bpm))
    (swap! audio-state assoc :bpm clamped-bpm)
    clamped-bpm))

(defn stop-loop!
  "Stops and removes active tracks by keyword(s).
  Examples: (stop-loop! :arp), (stop-loop! :kick :snare :hat)."
  [& track-keys]
  (let [tks (flatten track-keys)]
    (doseq [track-key tks]
      (let [tk (keyword track-key)]
        (when-let [tr (get (:active-tracks @audio-state) tk)]
          (when-let [s (:sequence tr)]
            (try (.dispose ^js s) (catch js/Object _)))
          (swap! audio-state update :active-tracks dissoc tk))))
    (vec tks)))

(def unstack! stop-loop!)

(defn clear-loops!
  "Stops and deletes all active loops."
  []
  (doseq [[_ tr] (:active-tracks @audio-state)]
    (when-let [s (:sequence tr)]
      (try (.dispose ^js s) (catch js/Object _))))
  (swap! audio-state assoc :active-tracks {})
  :cleared)

(defn stop!
  "Stops Tone.js Transport and cancels all active loops."
  []
  (when-let [t (:transport (:tone @engine-ctx))]
    (try
      (.stop ^js t)
      (.cancel ^js t)
      (catch js/Object _)))
  (clear-loops!)
  (swap! audio-state assoc :active? false :solo-mode? false)
  :stopped)

(defn toggle-click!
  "Toggles a studio metronome click in headphones/master.
  Examples: (toggle-click!)."
  []
  (if (contains? (:active-tracks @audio-state) :click)
    (do
      (stop-loop! :click)
      :click-off)
    (do
      (loop! :click
             {:inst :click
              :notes ["C6" "G5" "G5" "G5"]
              :step "4n"
              :dur "32n"
              :vel 0.4})
      :click-on)))

(defn stack!
  "Launches multiple live loops simultaneously from variadic vectors or a track map.
  Examples:
    (stack!
      [:kick  (pat \"k . . .  k . . .\")]
      [:snare (pat \". . . .  s . . .\")])"
  [& args]
  (let [pairs (cond
                (and (= 1 (count args)) (map? (first args))) (vec (first args))
                (and (= 1 (count args)) (vector? (first (first args)))) (first args)
                :else args)
        tks   (set (map first pairs))]
    (when (some #{:kick :snare :hat} tks) (stop-loop! :drums))
    (when (contains? tks :drums) (stop-loop! :kick :snare :hat))
    (doseq [[k spec] pairs]
      (when (and k spec) (loop! k spec)))
    (mapv first pairs)))
