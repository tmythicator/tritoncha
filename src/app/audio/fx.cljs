(ns app.audio.fx
  "Audio effects, transitions and SFX triggers."
  (:require ["tone" :as tone]
            [app.state :refer [engine-ctx pulse!]]
            [app.audio.engine :refer [init-audio!]]))

(defn- tone-node [k]
  (get (:tone @engine-ctx) k))

(defn- ramp-node-param! [node-key getter-fn target-val ramp-time]
  (when-let [node (tone-node node-key)]
    (.rampTo ^js (getter-fn node) target-val (or ramp-time 0.05))))

(defn set-filter-cutoff!
  "Sets the master lowpass filter cutoff frequency in Hz with smooth ramp."
  [hz]
  (ramp-node-param! :filter #(.-frequency ^js %) hz 0.05))

(defn set-filter-q!
  "Sets filter resonance (Q factor)."
  [q]
  (ramp-node-param! :filter #(.-Q ^js %) q 0.05))

(defn sweep-filter!
  "Smoothly sweeps master filter cutoff from one frequency to another over seconds."
  [from-hz to-hz duration-secs]
  (when-let [{:keys [^js filter]} (:tone @engine-ctx)]
    (let [now (tone/now)
          freq (.-frequency filter)]
      (.cancelScheduledValues freq now)
      (.setValueAtTime freq from-hz now)
      (.linearRampToValueAtTime freq to-hz (+ now duration-secs)))))

(defn set-distortion!
  "Sets overdrive/distortion amount (0.0 to 1.0)."
  [amt]
  (when-let [^js distort (:distort (:tone @engine-ctx))]
    (set! (.-distortion distort) amt)))

(defn set-delay-feedback!
  "Sets delay feedback amount (0.0 to 0.9)."
  [fb]
  (ramp-node-param! :delay #(.-feedback ^js %) fb 0.05))

(defn set-delay-time!
  "Sets delay tempo subdivision ('8n.', '16n', '4n')."
  [time-val]
  (when-let [^js delay (:delay (:tone @engine-ctx))]
    (set! (.. delay -delayTime -value) time-val)))

(defn set-reverb-wet!
  "Sets reverb wet mix (0.0 to 1.0)."
  [w]
  (ramp-node-param! :reverb #(.-wet ^js %) w 0.05))

(defn drum-hit!
  "Triggers a visual geometry pulse (default intensity 2.0)."
  ([] (drum-hit! 2.0))
  ([intensity] (pulse! intensity)))

(defn trigger-dub-siren!
  "Triggers a classic one-shot dub laser siren FX."
  []
  (init-audio!)
  (when-let [{:keys [^js siren]} (:tone @engine-ctx)]
    (let [now (tone/now)
          freq (.-frequency siren)]
      (.cancelScheduledValues freq now)
      (.setValueAtTime freq 350 now)
      (.linearRampToValueAtTime freq 1400 (+ now 0.35))
      (.linearRampToValueAtTime freq 250 (+ now 0.8))
      (.triggerAttackRelease siren 300 "1n" now)
      (pulse! 2.8))))

(defn trigger-sub-drop!
  "Triggers a seismic sub-bass drop."
  []
  (init-audio!)
  (when-let [{:keys [^js kick]} (:tone @engine-ctx)]
    (let [now (tone/now)]
      (.triggerAttackRelease kick "F1" "1n" now 1.0)
      (pulse! 3.0))))

(defn trigger-dark-chord!
  "Triggers a dark minor 9th pad chord stab."
  ([] (trigger-dark-chord! ["E3" "G3" "B3" "D4" "F#4"]))
  ([chord]
   (init-audio!)
   (when-let [{:keys [^js pad]} (:tone @engine-ctx)]
     (let [now (tone/now)]
       (doseq [n chord]
         (.triggerAttackRelease pad n "2n" now 0.4)))
     (pulse! 1.8))))
