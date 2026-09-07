(ns app.ui.instrument-browser.formatters
  "Declarative ClojureScript code generators (defsynth! and defdrum!) for Instrument Studio.")

(defn format-drum-spec-map
  "Generate ClojureScript defdrum! map for drum voices.
  Examples: (format-drum-spec-map :kick spec) -> \"(defdrum! :kick ...)\"."
  [inst-key spec]
  (let [bus (or (:bus spec) :bus/drums)
        mod-val (or (:mod spec) :natural)]
    (case (:type spec)
      :kick
      (str "(defdrum! " inst-key "\n"
           "  {:category    :drums\n"
           "   :type        :kick\n"
           "   :bus         " bus "\n"
           "   :mod         " mod-val "\n"
           "   :base-pitch  " (.toFixed (or (:base-pitch spec) 48.0) 1) "\n"
           "   :pitch-drop  " (.toFixed (or (:pitch-drop spec) 180.0) 1) "\n"
           "   :pitch-decay " (.toFixed (or (:pitch-decay spec) 0.040) 3) "\n"
           "   :decay       " (.toFixed (or (:decay spec) 0.28) 2) "\n"
           "   :click       " (.toFixed (or (:click spec) 0.35) 2) "\n"
           "   :drive       " (.toFixed (or (:drive spec) 1.6) 2) "})")

      :snare
      (str "(defdrum! " inst-key "\n"
           "  {:category    :drums\n"
           "   :type        :snare\n"
           "   :bus         " bus "\n"
           "   :mod         " mod-val "\n"
           "   :base-freq   " (.toFixed (or (:base-freq spec) 185.0) 1) "\n"
           "   :tone-decay  " (.toFixed (or (:tone-decay spec) 0.9985) 4) "\n"
           "   :noise-decay " (.toFixed (or (:noise-decay spec) 0.9991) 4) "\n"
           "   :cutoff      " (Math/round (or (:cutoff spec) 2400.0)) "\n"
           "   :snappy      " (.toFixed (or (:snappy spec) 0.85) 2) "})")

      :hat
      (str "(defdrum! " inst-key "\n"
           "  {:category     :drums\n"
           "   :type         :hat\n"
           "   :bus          " bus "\n"
           "   :mod          " mod-val "\n"
           "   :cutoff       " (Math/round (or (:cutoff spec) 7200.0)) "\n"
           "   :decay-closed " (.toFixed (or (:decay-closed spec) 0.04) 2) "\n"
           "   :decay-open   " (.toFixed (or (:decay-open spec) 0.24) 2) "})")

      :membrane
      (str "(defdrum! " inst-key "\n"
           "  {:category    :drums\n"
           "   :type        :membrane\n"
           "   :bus         " bus "\n"
           "   :mod         " mod-val "\n"
           "   :start-pitch " (.toFixed (or (:start-pitch spec) 180.0) 1) "\n"
           "   :min-pitch   " (.toFixed (or (:min-pitch spec) 105.0) 1) "\n"
           "   :pitch-decay " (.toFixed (or (:pitch-decay spec) 0.015) 3) "\n"
           "   :decay       " (.toFixed (or (:decay spec) 0.40) 2) "\n"
           "   :drive       " (.toFixed (or (:drive spec) 1.10) 2) "})")

      :metallic
      (str "(defdrum! " inst-key "\n"
           "  {:category  :drums\n"
           "   :type      :metallic\n"
           "   :bus       " bus "\n"
           "   :mod       " mod-val "\n"
           "   :cutoff    " (Math/round (or (:cutoff spec) 3500.0)) "\n"
           "   :resonance " (.toFixed (or (:resonance spec) 0.35) 2) "\n"
           "   :decay     " (.toFixed (or (:decay spec) 0.85) 2) "\n"
           (if (:bandpass spec)
             "   :bandpass  true\n"
             "")
           "   :drive     " (.toFixed (or (:drive spec) 1.0) 2) "})")

      :clap
      (str "(defdrum! " inst-key "\n"
           "  {:category  :drums\n"
           "   :type      :clap\n"
           "   :bus       " bus "\n"
           "   :mod       " mod-val "\n"
           "   :cutoff    " (Math/round (or (:cutoff spec) 1200.0)) "\n"
           "   :resonance " (.toFixed (or (:resonance spec) 0.70) 2) "\n"
           "   :decay     " (.toFixed (or (:decay spec) 0.28) 2) "\n"
           "   :drive     " (.toFixed (or (:drive spec) 1.0) 2) "})")

      (str "(defdrum! " inst-key "\n"
           "  " (pr-str spec) ")"))))

(defn format-spec-map
  "Generate ClojureScript defsynth! or defdrum! map matching instrument definitions.
  Examples: (format-spec-map :saw-bass spec) -> \"(defsynth! :saw-bass ...)\"."
  [inst-key spec]
  (if (= (:category spec) :drums)
    (format-drum-spec-map inst-key spec)
    (let [osc      (or (:osc spec) {:type :saw})
          flt      (or (:filter spec) {:type :lowpass :cutoff 2000 :q 0.2})
          amp      (or (:amp-env spec) {:attack 0.01 :decay 0.2 :sustain 0.5 :release 0.3})
          mod-e    (:mod-env spec)
          pitch-e  (:pitch-env spec)
          bus      (or (:bus spec) :bus/lead)
          poly?    (= (:type spec) :poly)
          glide    (:glide spec)]
      (str "(defsynth! " inst-key "\n"
           "  {:osc     {:type " (or (:type osc) :saw)
           (if (and (:sub-level osc) (> (:sub-level osc) 0.001))
             (str " :sub-level " (.toFixed (:sub-level osc) 2))
             "")
           (if (contains? #{:pulse :blade} (:type osc))
             (str " :pulse-width " (.toFixed (or (:pulse-width osc) 0.5) 2))
             "")
           (if (and (:noise osc) (> (:noise osc) 0.001))
             (str " :noise " (.toFixed (:noise osc) 2))
             "")
           (if (and (:drift osc) (> (:drift osc) 0.001))
             (str " :drift " (.toFixed (:drift osc) 2))
             "")
           "}\n"
           "   :filter  {:type " (or (:type flt) :lowpass)
           " :cutoff " (Math/round (or (:cutoff flt) 2000))
           " :q " (.toFixed (or (:q flt) 0.2) 2)
           (if (and (:drive flt) (> (:drive flt) 0.001))
             (str " :drive " (.toFixed (:drive flt) 2))
             "")
           (if (and (:env-amount flt) (not= (Math/round (:env-amount flt)) 0))
             (str " :env-amount " (Math/round (:env-amount flt)))
             "")
           (if (and (:key-track flt) (not= (:key-track flt) 0.0))
             (str " :key-track " (.toFixed (:key-track flt) 1))
             "")
           "}\n"
           "   :amp-env {:attack " (.toFixed (or (:attack amp) 0.01) 3)
           " :decay " (.toFixed (or (:decay amp) 0.2) 3)
           " :sustain " (.toFixed (or (:sustain amp) 0.5) 2)
           " :release " (.toFixed (or (:release amp) 0.3) 3) "}\n"
           (if mod-e
             (str "   :mod-env {:attack " (.toFixed (or (:attack mod-e) 0.01) 3)
                  " :decay " (.toFixed (or (:decay mod-e) 0.2) 3) "}\n")
             "")
           (if (and pitch-e (> (or (:amount pitch-e) 0) 0))
             (str "   :pitch-env {:amount " (Math/round (:amount pitch-e))
                  " :decay " (.toFixed (or (:decay pitch-e) 0.015) 3) "}\n")
             "")
           (if poly?
             "   :type    :poly\n"
             "")
           (if (and glide (> glide 0.001))
             (str "   :glide   " (.toFixed glide 3) "\n")
             "")
           "   :bus     " bus "})"))))
