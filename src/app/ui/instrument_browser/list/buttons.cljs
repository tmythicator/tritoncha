(ns app.ui.instrument-browser.list.buttons
  "Quick audition buttons row for instrument cards."
  (:require
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.state :as state]))

(defn- audition-btn
  "Render a single styled audition button with click isolation and state selection.
  Examples: [audition-btn :lead-pluck \"♪ NOTE\" \"Single note\" #(audition/play-test-note! :lead-pluck) false]."
  [inst-key label tooltip-title action wide?]
  [:button.neo-btn-stats.inst-audition-btn
   {:class    (when wide? "wide")
    :on-click (fn [e]
                (.stopPropagation e)
                (reset! state/selected-inst inst-key)
                (action inst-key))
    :title    tooltip-title}
   label])

(defn- drum-audition-buttons
  "Render 3 quick audition buttons for drum sound voices.
  Examples: [drum-audition-buttons :analog-kick]."
  [inst-key]
  [:<>
   [audition-btn inst-key "♪ HIT" "Audition single drum hit" audition/play-test-note! false]
   [audition-btn inst-key "♫ ROLL" "Audition drum roll" audition/play-test-run! false]
   [audition-btn inst-key "≋ FILL" "Audition 8-step drum fill" audition/play-test-arp! true]])

(defn- fx-audition-buttons
  "Render 4 quick audition buttons for FX sound effects and one-shots.
  Examples: [fx-audition-buttons :fx-laser]."
  [inst-key]
  [:<>
   [audition-btn inst-key "♪ SHOT" "Audition single one-shot FX" audition/play-test-note! false]
   [audition-btn inst-key "♫ RUN" "Audition descending pitch run" audition/play-test-run! false]
   [audition-btn inst-key "≋ ARP" "Audition rapid FX strobe" audition/play-test-arp! false]
   [audition-btn inst-key "≈ CHORD" "Audition FX burst" audition/play-test-chord! true]])

(defn- tonal-audition-buttons
  "Render 4 uniform audition buttons for musical synth voices.
  Examples: [tonal-audition-buttons :lead-pluck true]."
  [inst-key poly?]
  [:<>
   [audition-btn inst-key "♪ NOTE" "Audition single sustained note" audition/play-test-note! false]
   [audition-btn inst-key "♫ RUN" "Audition melodic 5-note scale run" audition/play-test-run! false]
   [audition-btn inst-key "≋ ARP" "Audition rolling 8-step arpeggio" audition/play-test-arp! false]
   [audition-btn inst-key "≈ CHORD"
    (if poly?
      "Audition polyphonic sustained chord"
      "Audition fast broken chord strum")
    audition/play-test-chord! true]])

(defn card-audition-buttons
  "Render unified quick audition buttons on instrument catalog cards.
  Examples: [card-audition-buttons :saw-bass :bass false]."
  [inst-key cat poly?]
  [:div.inst-audition-btn-group
   (cond
     (= cat :drums) [drum-audition-buttons inst-key]
     (= cat :fx)    [fx-audition-buttons inst-key]
     :else          [tonal-audition-buttons inst-key poly?])])
