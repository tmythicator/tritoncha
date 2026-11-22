(ns app.ui.instrument-browser.formatters
  "Declarative ClojureScript code generator (definst!) for Instrument Studio.")

(defn- title-line [spec]
  (if-let [t (:title spec)]
    (str "   :title       " (pr-str t) "\n")
    ""))

(def ^:private drum-export-keys
  "Canonical display order and formatting for 24-param PercPatch fields in definst! export."
  [[:body          :kw]
   [:pitch         1]
   [:pitch-drop    1]
   [:pitch-decay   3]
   [:decay         2]
   [:click         2]
   [:click-freq    0]
   [:burst-count   0]
   [:burst-spacing 3]
   [:fm-depth      2]
   [:fm-ratio      3]
   [:noise-mix     2]
   [:noise-decay   2]
   [:noise-cutoff  0]
   [:noise-q       2]
   [:noise-filter  :kw]
   [:modal-spread  2]
   [:drive         2]
   [:fold          2]
   [:crush         2]
   [:vel->pitch    2]
   [:vel->tone     2]
   [:variation     2]
   [:choke-group   0]])

(defn- format-param-val [v fmt]
  (cond
    (= fmt :kw)   (str v)
    (zero? fmt)   (str (Math/round v))
    (number? v)   (.toFixed (js/parseFloat v) fmt)
    :else         (pr-str v)))

(defn format-drum-spec-map
  "Generate ClojureScript definst! map for layered parametric percussion voices."
  [inst-key spec]
  (let [bus      (or (:bus spec) :bus/drums)
        mod-val  (:mod spec)
        header   (cond-> []
                   (:title spec) (conj (str "   :title         " (pr-str (:title spec))))
                   true          (conj "   :category      :drums")
                   (:role spec)  (conj (str "   :role          " (:role spec)))
                   (:type spec)  (conj (str "   :type          " (:type spec)))
                   true          (conj (str "   :bus           " bus))
                   mod-val       (conj (str "   :mod           " mod-val)))
        params   (keep (fn [[k fmt]]
                         (when-some [v (get spec k)]
                           (let [k-str (str k)
                                 pad   (apply str (repeat (max 1 (- 14 (count k-str))) " "))]
                             (str "   " k-str pad (format-param-val v fmt)))))
                       drum-export-keys)
        all-lines (concat header params)]
    (str "(definst! " inst-key "\n"
         "  {" (subs (first all-lines) 3) "\n"
         (apply str (interpose "\n" (rest all-lines)))
         "})")))

(defn format-spec-map
  "Generate ClojureScript definst! map matching instrument definitions."
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
          glide    (:glide spec)
          t-line   (title-line spec)]
      (str "(definst! " inst-key "\n"
           (when (seq t-line) (str "  {" (subs t-line 3)))
           (if (seq t-line) "   :osc     {:type " "  {:osc     {:type ") (or (:type osc) :saw)
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
             (str "   :type    :poly\n"
                  (if-let [p (:polyphony spec)]
                    (str "   :polyphony " p "\n")
                    ""))
             "")
           (if (and glide (> glide 0.001))
             (str "   :glide   " (.toFixed glide 3) "\n")
             "")
           "   :bus     " bus "})"))))
