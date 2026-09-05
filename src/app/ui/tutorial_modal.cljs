(ns app.ui.tutorial-modal
  "In-browser Live REPL tutorial modal."
  (:require-macros [app.macros :refer [load-tutorial-content]])
  (:require [app.eval.core :as eval-engine]
            [app.ui.tutorial.editor :refer [editor-component]]
            [app.ui.tutorial.output :refer [output-component]]
            [reagent.core :as r]))

(def ^:private tutorial-source-code (load-tutorial-content))
(defonce ^:private eval-output (r/atom {:ok? true :text "Ready. Place cursor on any line and press Ctrl+Enter."}))
(defonce ^:private editor-content-atom (r/atom tutorial-source-code))

(defn- handle-eval! [code-str]
  (reset! eval-output (eval-engine/run-code code-str)))

(defn- handle-reset! []
  (reset! editor-content-atom tutorial-source-code)
  (when-let [el (.querySelector js/document ".neo-code-editor")]
    (set! (.-value el) tutorial-source-code)
    (set! (.-scrollTop el) 0)
    (set! (.-selectionStart el) 0)
    (set! (.-selectionEnd el) 0)))

(defn tutorial-modal-component [{:keys [on-close]}]
  [:div.neo-tutorial-card {:role "dialog" :aria-modal true :aria-label "Interactive Livecoding Tutorial and REPL"}
   [:div.neo-header
    [:div.neo-title
     [:span.neo-prompt "> "]
     [:span "LIVE REPL + TUTORIAL"]]
    [:div.neo-modal-tabs
     [:button.tab-btn {:on-click handle-reset!
                       :title "Reset editor to tutorial masterclass"}
      "RESET"]
     [:button.neo-btn-close {:on-click on-close
                             :aria-label "Close tutorial"}
      "[X]"]]]

   [:div.neo-body
    [editor-component {:content-atom   editor-content-atom
                       :default-content tutorial-source-code
                       :on-eval-sexp   handle-eval!
                       :on-eval-line   handle-eval!
                       :on-eval-all    handle-eval!}]

    [output-component {:output-atom eval-output}]]

   [:div.neo-footer
    [:span.neo-foot-cmd "> ./tritoncha --repl [SCI In-Browser]"]
    [:span.neo-foot-hint "[Press T to toggle modal]"]]])
