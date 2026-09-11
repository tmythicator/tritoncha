(ns app.ui.instrument-browser.inspector.header
  "Inspector header badges, routing information, and interactive sound audition bar."
  (:require
   [app.ui.instrument-browser.audition :as audition]
   [app.ui.instrument-browser.components :as comps]
   [app.ui.instrument-browser.state :as state]
   [clojure.string :as str]))

(defn inspector-header
  "Render inspector header with title, badges and FX routing summary.
  Examples: [inspector-header :saw-bass spec drum?]."
  [cur-sel-key cur-spec drum?]
  [:div.inst-inspector-header
   [:div.inst-inspector-title-row
    [:span.inst-inspector-title (str cur-sel-key)]
    [:div.inst-inspector-badges
     (when (state/custom-file-inst? cur-sel-key)
       [:span.neo-badge.badge-custom
        {:title "User custom instrument defined in custom/instruments.cljs (persisted on disk)"}
        "CUSTOM"])
     (when (state/session-inst? cur-sel-key)
       [:span.neo-badge.badge-session
        {:title "Session instrument / on-the-fly REPL modification (ephemeral, resets on F5)"}
        "SESSION"])
     [:span.neo-bus-tag.inspector-bus {:class (comps/bus-badge-class (:bus cur-spec))}
      (str "BUS: " (str/upper-case (str/replace (name (or (:bus cur-spec) :direct)) #"bus/" "")))]
     [:span.neo-badge.inspector-badge
      (cond
        drum? "ANALOG DRUM"
        (= (:type cur-spec) :poly)
        (let [poly (or (:polyphony cur-spec) (:maxPolyphony cur-spec) (:max-polyphony cur-spec) 16)]
          (str "POLY (" poly "x)"))
        :else "MONOPHONIC")]]]])

(defn audition-bar
  "Render quick test trigger buttons and continuous audition loop toggle.
  Examples: [audition-bar :saw-bass cur-spec drum? looping?]."
  [cur-sel-key cur-spec drum? looping?]
  (let [cat (audition/sound-family cur-sel-key cur-spec)
        fx? (= cat :fx)]
    [:div.inst-audition-bar
     [:button.neo-btn-stats
      {:on-click #(audition/play-test-note! cur-sel-key)
       :title    (cond drum? "Audition single drum hit" fx? "Audition single one-shot FX" :else "Audition single sustained note")}
      (cond drum? "♪ HIT" fx? "♪ SHOT" :else "♪ NOTE")]
     [:button.neo-btn-stats
      {:on-click #(audition/play-test-run! cur-sel-key)
       :title    (cond drum? "Audition drum roll" fx? "Audition descending pitch run" :else "Audition melodic 5-note scale run")}
      (if drum? "♫ ROLL" "♫ RUN")]
     (cond
       drum?
       [:button.neo-btn-stats
        {:on-click #(audition/play-test-arp! cur-sel-key)
         :title    "Audition 8-step drum fill"}
        "≋ FILL"]

       fx?
       [:<>
        [:button.neo-btn-stats
         {:on-click #(audition/play-test-arp! cur-sel-key)
          :title    "Audition rapid FX strobe"}
         "≋ ARP"]
        [:button.neo-btn-stats
         {:on-click #(audition/play-test-chord! cur-sel-key)
          :title    "Audition FX burst"}
         "≈ CHORD"]]

       :else
       [:<>
        [:button.neo-btn-stats
         {:on-click #(audition/play-test-arp! cur-sel-key)
          :title    "Audition 8-step rhythmic arpeggio"}
         "≋ ARP"]
        [:button.neo-btn-stats
         {:on-click #(audition/play-test-chord! cur-sel-key)
          :title    (if (= (:type cur-spec) :poly)
                      "Audition polyphonic sustained chord"
                      "Audition fast broken chord strum")}
         "≈ CHORD"]])
     [:button.neo-btn-stats.audition-loop-btn
      {:class    (when looping? "active")
       :on-click #(audition/toggle-audition-loop! cur-sel-key)
       :title    "Continuously loop the sound so you can tweak parameters live"}
      (if looping? "■ STOP" "⟳ LOOP")]]))
