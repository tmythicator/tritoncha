(ns app.ui.instrument-browser.audition
  "Audio audition engine, preview triggers, phrase runs, and looper playback for Instrument Studio."
  (:require
   [app.audio.control.looper :as looper]
   [app.audio.dsp.busses :as busses]
   [app.audio.dsp.instruments :as instruments]
   [reagent.core :as r]))

(defonce audition-loop-active? (r/atom false))
(defonce ^:private last-slider-preview-time (atom 0))

(defn sound-family
  "Resolve intrinsic sound family (:drums, :fx, :bass, :pads, :leads).
  Examples: (sound-family :fx-laser spec) -> :fx."
  [inst-key spec]
  (let [resolved (or spec (instruments/resolve-instrument-spec inst-key))]
    (busses/sound-category (or resolved inst-key))))

(defn stop-audition-loop!
  "Halt active audition looper playback.
  Examples: (stop-audition-loop!) -> nil."
  []
  (when @audition-loop-active?
    (looper/stop-loop! :inst-audition)
    (reset! audition-loop-active? false)))

(defn start-audition-loop!
  "Start continuous audition looper for the given instrument.
  Examples: (start-audition-loop! :saw-bass) -> nil."
  [inst-key]
  (let [spec   (instruments/resolve-instrument-spec inst-key)
        family (sound-family inst-key spec)]
    (reset! audition-loop-active? true)
    (case family
      :drums
      (looper/loop! :inst-audition
                    {:inst inst-key
                     :notes [inst-key nil inst-key nil]
                     :step "8n"
                     :dur "16n"
                     :vel 0.9})

      :fx
      (looper/loop! :inst-audition
                    {:inst inst-key
                     :notes ["E4" nil nil nil "E4" nil nil nil]
                     :step "8n"
                     :dur "16n"
                     :vel 0.85})

      :bass
      (looper/loop! :inst-audition
                    {:inst inst-key
                     :notes ["E1" nil "E1" "G1" nil "A1" "Bb1" "B1"]
                     :step "16n"
                     :dur "16n"
                     :vel 0.9})

      :pads
      (looper/loop! :inst-audition
                    {:inst inst-key
                     :notes [["E3" "G3" "B3" "D4"]]
                     :step "1m"
                     :dur "1m"
                     :vel 0.45})

      :leads
      (looper/loop! :inst-audition
                    {:inst inst-key
                     :notes ["E4" "G4" "B4" "D5" "E5" "D5" "B4" "G4"]
                     :step "16n"
                     :dur "16n"
                     :vel 0.45}))))

(defn toggle-audition-loop!
  "Toggle audition looper for active instrument.
  Examples: (toggle-audition-loop! :saw-bass) -> nil."
  [inst-key]
  (if @audition-loop-active?
    (stop-audition-loop!)
    (start-audition-loop! inst-key)))

(defn touch-preview-note!
  "Trigger quick throttled audition note when adjusting sliders.
  Examples: (touch-preview-note! :bass) -> nil."
  [inst-key]
  (when-not @audition-loop-active?
    (let [now (.now js/Date)]
      (when (> (- now @last-slider-preview-time) 1400)
        (reset! last-slider-preview-time now)
        (let [spec   (instruments/resolve-instrument-spec inst-key)
              family (sound-family inst-key spec)]
          (case family
            :drums (instruments/trigger-drum! inst-key 0.9)
            :fx    (instruments/trigger-note! inst-key "E4" "16n" 0.9)
            :bass  (instruments/trigger-note! inst-key "E1" "2n" 0.85)
            :pads  (instruments/trigger-note! inst-key ["E3" "G3" "B3" "D4"] "1m" 0.5)
            :leads (instruments/trigger-note! inst-key "E4" "2n" 0.75)))))))

(defn play-test-note!
  "Audition single note for selected instrument.
  Examples: (play-test-note! :kick) -> nil."
  [inst-key]
  (let [spec   (instruments/resolve-instrument-spec inst-key)
        family (sound-family inst-key spec)]
    (case family
      :drums (instruments/trigger-drum! inst-key 0.95)
      :fx    (instruments/trigger-note! inst-key "E4" "8n" 0.95)
      :bass  (instruments/trigger-note! inst-key "E1" "4n" 0.92)
      :pads  (instruments/trigger-note! inst-key ["E3" "G3" "B3" "D4"] "2n" 0.85)
      :leads (instruments/trigger-note! inst-key "E4" "16n" 0.92))))

(defn play-test-run!
  "Audition melodic 5-note phrase or drum roll.
  Examples: (play-test-run! :acid-bass) -> nil."
  [inst-key]
  (let [spec   (instruments/resolve-instrument-spec inst-key)
        family (sound-family inst-key spec)]
    (case family
      :drums
      (doseq [[idx v] (map-indexed vector [0.9 0.5 0.85 0.6 1.0])]
        (js/setTimeout
         (fn []
           (instruments/trigger-drum! inst-key v))
         (* idx 110)))

      :fx
      (let [notes ["E5" "B4" "E4" "B3" "E3"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "16n" 0.95))
           (* idx 70))))

      :bass
      (let [notes ["E1" "G1" "A1" "Bb1" "B1"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "16n" 0.92))
           (* idx 110))))

      :pads
      (let [notes ["E3" "G3" "A3" "B3" "D4"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "8n" 0.85))
           (* idx 150))))

      :leads
      (let [notes ["E4" "G4" "A4" "B4" "D5"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "16n" 0.92))
           (* idx 95)))))))

(defn play-test-arp!
  "Audition 8-step rhythmic arpeggio or drum fill across instrument families.
  Examples: (play-test-arp! :bass-analog) -> nil."
  [inst-key]
  (let [spec   (instruments/resolve-instrument-spec inst-key)
        family (sound-family inst-key spec)]
    (case family
      :drums
      (let [velocities [0.9 0.4 0.8 0.5 0.95 0.6 0.85 1.0]]
        (doseq [[idx v] (map-indexed vector velocities)]
          (js/setTimeout
           (fn []
             (instruments/trigger-drum! inst-key v))
           (* idx 85))))

      :fx
      (let [notes ["E4" "G4" "B4" "E5" "E4" "G4" "B4" "E5"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "16n" 0.95))
           (* idx 80))))

      :bass
      (let [notes ["E1" "G1" "A1" "Bb1" "B1" "D2" "B1" "E1"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "16n" 0.92))
           (* idx 85))))

      :pads
      (let [notes ["E3" "G3" "B3" "D4" "E4" "D4" "B3" "G3"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "8n" 0.85))
           (* idx 110))))

      :leads
      (let [notes ["E4" "G4" "B4" "D5" "E5" "D5" "B4" "G4"]]
        (doseq [[idx n] (map-indexed vector notes)]
          (js/setTimeout
           (fn []
             (instruments/trigger-note! inst-key n "16n" 0.9))
           (* idx 75)))))))

(defn play-test-chord!
  "Audition harmonic chord: simultaneous polyphonic chord or fast broken strum for mono synths.
  Examples: (play-test-chord! :pad-cinema) -> nil."
  [inst-key]
  (let [spec   (instruments/resolve-instrument-spec inst-key)
        family (sound-family inst-key spec)
        poly?  (= (:type spec) :poly)]
    (case family
      :drums
      (let [velocities [0.7 0.9 1.0]]
        (doseq [[idx v] (map-indexed vector velocities)]
          (js/setTimeout
           (fn []
             (instruments/trigger-drum! inst-key v))
           (* idx 60))))

      :fx
      (if poly?
        (instruments/trigger-note! inst-key ["E4" "B4" "E5"] "2n" 0.85)
        (let [notes ["E4" "B4" "E5"]]
          (doseq [[idx n] (map-indexed vector notes)]
            (js/setTimeout
             (fn []
               (instruments/trigger-note! inst-key n "16n" 0.95))
             (* idx 35)))))

      :bass
      (if poly?
        (instruments/trigger-note! inst-key ["E1" "B1" "G2"] "2n" 0.88)
        (let [notes ["E1" "B1" "E2" "G2"]]
          (doseq [[idx n] (map-indexed vector notes)]
            (js/setTimeout
             (fn []
               (instruments/trigger-note! inst-key n "8n" 0.9))
             (* idx 45)))))

      :pads
      (if poly?
        (instruments/trigger-note! inst-key ["E3" "G3" "B3" "D4" "F#4"] "1m" 0.82)
        (let [notes ["E3" "G3" "B3" "D4" "F#4"]]
          (doseq [[idx n] (map-indexed vector notes)]
            (js/setTimeout
             (fn []
               (instruments/trigger-note! inst-key n "8n" 0.82))
             (* idx 50)))))

      :leads
      (if poly?
        (instruments/trigger-note! inst-key ["E4" "G4" "B4" "D5"] "2n" 0.85)
        (let [notes ["E4" "G4" "B4" "E5"]]
          (doseq [[idx n] (map-indexed vector notes)]
            (js/setTimeout
             (fn []
               (instruments/trigger-note! inst-key n "16n" 0.9))
             (* idx 40))))))))
