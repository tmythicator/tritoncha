(ns app.audio.dsp.styles
  "Composable genre style transforms for layered parametric percussion.
  Styles are pure ClojureScript data transforms over drum roles (:kick, :snare, :hat, :cymbal, :tom, :clap, :perc)."
  (:require [app.state :refer [repl-registry]]))

(def style-aliases
  {:classic    :analog
   :808        :analog
   :909        :analog
   :acoustic   :natural
   :organic    :natural
   :wood       :natural
   :glitch     :idm
   :laser      :idm
   :chirp      :idm
   :distort    :industrial
   :hard       :industrial
   :crush      :industrial})

(def builtin-styles
  "Built-in genre style transforms targeting drum roles."
  {:analog
   {:all   {:variation 0.08 :vel->tone 0.30 :vel->pitch 0.10}
    :kick  {:body :membrane :drive [:* 1.05] :fold 0.0}
    :snare {:drive [:* 1.0] :fold 0.0}
    :hat   {:body :cymbal :crush 0.0}}

   :natural
   {:all    {:variation 0.32 :vel->tone 0.65 :vel->pitch 0.25 :fold 0.0 :crush 0.0}
    :kick   {:body :membrane :fm-depth [:+ 0.14] :fm-ratio 1.594 :pitch-drop [:* 0.72] :decay [:* 0.85] :drive [:* 0.72] :click [:* 0.80]}
    :snare  {:body :shell :modal-spread 1.15 :noise-mix [:* 0.95] :drive [:* 0.80]}
    :hat    {:noise-mix [:* 1.15] :drive [:* 0.80]}
    :cymbal {:body :cymbal :modal-spread 1.65 :drive [:* 0.85]}
    :tom    {:body :shell :modal-spread 1.05 :drive [:* 0.85]}}

   :idm
   {:all    {:crush 0.22 :vel->pitch 0.35 :variation 0.22}
    :kick   {:fm-depth [:+ 0.65] :fm-ratio 3.8 :pitch-drop [:* 1.75] :click [:* 1.40] :drive [:* 1.30]}
    :snare  {:fm-depth [:+ 0.55] :fm-ratio 3.1 :noise-cutoff [:* 1.35] :noise-q 0.80 :decay [:* 0.75]}
    :hat    {:crush 0.42 :decay [:* 0.70] :noise-decay [:* 0.70] :noise-cutoff [:* 1.25]}
    :cymbal {:crush 0.30 :fm-depth [:+ 0.40] :decay [:* 0.65]}
    :clap   {:crush 0.35 :burst-spacing 0.008}}

   :industrial
   {:all    {:fold 0.45 :crush 0.12 :drive [:* 1.65] :variation 0.14}
    :kick   {:fold 0.65 :drive [:* 2.0] :click [:* 1.25]}
    :snare  {:fold 0.50 :drive [:* 1.8] :noise-mix [:* 1.2]}
    :hat    {:fold 0.08 :crush 0.16 :drive [:* 1.25] :noise-cutoff [:* 1.08] :noise-q 0.52}
    :cymbal {:fold 0.35 :drive [:* 1.45] :decay [:* 1.15]}
    :tom    {:fold 0.60 :drive [:* 1.8]}}})

(defn normalize-style-key
  "Resolves style aliases to their canonical keyword."
  [style-key]
  (let [k (keyword style-key)]
    (get style-aliases k k)))

(defn defstyle!
  "Registers a custom genre style transform in the live REPL registry.
  Examples: (defstyle! :techno {:all {:drive [:* 1.4]} :kick {:fold 0.35}})."
  [style-key rules]
  (let [k (keyword style-key)]
    (swap! repl-registry assoc-in [:styles k] rules)
    k))

(defn resolve-style
  "Looks up a style definition by keyword across user REPL styles and built-in styles."
  [style-key]
  (when style-key
    (let [k (normalize-style-key style-key)]
      (or (get-in @repl-registry [:styles k])
          (get builtin-styles k)))))

(defn- apply-rule-val
  "Applies a direct value, multiplicative [:* factor], or additive [:+ delta] rule to a base value."
  [base-val rule-val]
  (if (and (vector? rule-val) (= 2 (count rule-val)))
    (let [[op arg] rule-val
          b        (float (or base-val 0.0))]
      (case op
        :* (* b (float arg))
        :+ (+ b (float arg))
        rule-val))
    rule-val))

(defn- apply-single-style
  "Applies one named style transform map onto a drum spec."
  [drum-spec style-key]
  (if-let [rules (resolve-style style-key)]
    (let [role       (or (:role drum-spec) (:type drum-spec) :perc)
          merged-ops (merge (:all rules) (get rules role))]
      (reduce-kv (fn [acc k rule-val]
                   (assoc acc k (apply-rule-val (get acc k) rule-val)))
                 drum-spec
                 merged-ops))
    drum-spec))

(defn apply-style
  "Applies a genre style keyword (e.g. :idm) or weighted blend map (e.g. {:natural 0.7 :industrial 0.3})
  onto a drum specification map. Numeric fields blend linearly; discrete fields come from the highest weight."
  [drum-spec style-spec]
  (cond
    (nil? style-spec)
    drum-spec

    (keyword? style-spec)
    (apply-single-style drum-spec style-spec)

    (string? style-spec)
    (apply-single-style drum-spec (keyword style-spec))

    (map? style-spec)
    (let [entries (filter (fn [[_ w]] (and (number? w) (pos? w))) style-spec)
          total-w (reduce + 0.0 (map second entries))]
      (if (or (empty? entries) (<= total-w 0.0))
        drum-spec
        (let [norm-entries (map (fn [[sk w]] [sk (/ (float w) total-w)]) entries)
              top-style    (first (apply max-key second norm-entries))
              top-spec     (apply-single-style drum-spec top-style)
              styled-specs (map (fn [[sk w]] [(apply-single-style drum-spec sk) w]) norm-entries)
              all-keys     (into #{} (mapcat (comp keys first) styled-specs))]
          (reduce (fn [acc k]
                    (let [vals-w (map (fn [[sp w]] [(get sp k) w]) styled-specs)]
                      (if (every? #(number? (first %)) vals-w)
                        (assoc acc k (reduce + 0.0 (map (fn [[v w]] (* (float v) w)) vals-w)))
                        (assoc acc k (get top-spec k)))))
                  top-spec
                  all-keys))))

    :else
    drum-spec))
