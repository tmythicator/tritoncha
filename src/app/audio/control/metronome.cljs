(ns app.audio.control.metronome
  "Metronome click pattern compiler, accent configuration, and audio monitoring toggle."
  (:require [app.audio.control.looper :as looper]
            [app.state :refer [audio-state]]
            [clojure.string :as str]))

(defonce click-config
  (atom {:pattern [1 0 0 0]
         :step    "4n"
         :dur     "32n"
         :accent  {:note "C6" :vel 1.0}
         :beat    {:note "G5" :vel 0.55}}))

(defn- compile-click-hits
  "Compiles high-level rhythm symbols into worklet click hit triples [inst vel pitch]."
  [pat-seq {:keys [accent beat]}]
  (mapv (fn [hit]
          (cond
            (or (nil? hit) (= hit :_) (= hit :-) (= hit "."))
            [:click 0.0 -1]

            (or (= hit 1) (= hit true) (= hit :acc) (= hit :accent) (= hit "X") (= hit "!"))
            [:click (float (:vel accent 1.0)) (:note accent "C6")]

            (or (= hit 0) (= hit false) (= hit :beat) (= hit "x") (= hit "o"))
            [:click (float (:vel beat 0.55)) (:note beat "G5")]

            (vector? hit)
            hit

            (string? hit)
            [:click (float (:vel beat 0.55)) hit]

            :else
            [:click (float (:vel beat 0.55)) (:note beat "G5")]))
        pat-seq))

(defn- parse-click-pattern
  "Parses string notation or vectors into click hit tokens."
  [raw-pat]
  (cond
    (vector? raw-pat) raw-pat
    (string? raw-pat) (vec (remove #{" "} (str/split raw-pat #"\s+")))
    (sequential? raw-pat) (vec raw-pat)
    :else [1 0 0 0]))

(defn set-click!
  "Configures the metronome click pattern, accents, and step subdivision.
  Accented hits (1, true, :acc, \"X\") play a high pitch (C6 at vel 1.0),
  while regular beats (0, false, :beat, \"x\") play a lower pitch (G5 at vel 0.55).
  Examples: (set-click! [1 0 0 0]), (set-click! \"X . x .\")."
  [arg]
  (if (map? arg)
    (swap! click-config merge arg)
    (swap! click-config assoc :pattern (parse-click-pattern arg)))
  (let [cfg  @click-config
        hits (compile-click-hits (:pattern cfg) cfg)
        pat  {:inst  :click
              :notes hits
              :step  (:step cfg "4n")
              :dur   (:dur cfg "32n")}]
    (when (contains? (:active-tracks @audio-state) :click)
      (looper/loop! :click pat))
    pat))

(defn toggle-click!
  "Toggles a studio metronome click in headphones or master."
  []
  (if (contains? (:active-tracks @audio-state) :click)
    (do
      (looper/stop-loop! :click)
      :click-off)
    (let [cfg  @click-config
          hits (compile-click-hits (:pattern cfg) cfg)]
      (looper/loop! :click {:inst :click :notes hits :step (:step cfg "4n") :dur (:dur cfg "32n")})
      :click-on)))

(defn click!
  "Toggles or sets the metronome click.
  Examples: (click!), (click! [1 0 0 0]), (click! \"X x x x\")."
  ([] (toggle-click!))
  ([pattern-or-config]
   (set-click! pattern-or-config)
   (when-not (contains? (:active-tracks @audio-state) :click)
     (let [cfg  @click-config
           hits (compile-click-hits (:pattern cfg) cfg)]
       (looper/loop! :click {:inst :click :notes hits :step (:step cfg "4n") :dur (:dur cfg "32n")})))
   :click-on))
