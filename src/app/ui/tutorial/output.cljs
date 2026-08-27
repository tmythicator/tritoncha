(ns app.ui.tutorial.output
  "Evaluation output console subcomponent.")

(defn output-component [{:keys [output-atom]}]
  (let [{:keys [ok? target text]} @output-atom]
    [:div.neo-output-container
     [:div.neo-section-label (str "$ eval_output" (when target (str " [" target "]")))]
     [:div.neo-output-console {:class (if ok? "output-ok" "output-err")}
      [:span.output-text (or text "")]]]))
