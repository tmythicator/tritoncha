(ns app.ui.tutorial-modal
  "In-browser Live REPL tutorial modal."
  (:require-macros [app.macros :refer [load-tutorial-content]])
  (:require [app.eval.core :as eval-engine]
            [app.ui.common :as common]
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

(defn tutorial-modal-component
  "Render interactive tutorial and in-browser live REPL modal dialog."
  [{:keys [on-close]}]
  [:div.neo-tutorial-card {:role "dialog" :aria-modal true :aria-label "Interactive Livecoding Tutorial and REPL"}
   [common/modal-header
    {:title         "LIVE REPL + TUTORIAL"
     :on-close      on-close
     :close-label   "Close tutorial"
     :right-class   "neo-modal-tabs"
     :right-content [:button.tab-btn {:on-click handle-reset!
                                      :title    "Reset editor to tutorial masterclass"}
                     "RESET"]}]

   [:div.neo-body
    [editor-component {:content-atom    editor-content-atom
                       :default-content tutorial-source-code
                       :on-eval-sexp    handle-eval!
                       :on-eval-line    handle-eval!
                       :on-eval-all     handle-eval!}]

    [output-component {:output-atom eval-output}]]

   [common/modal-footer
    {:cmd  "> ./tritoncha --repl [SCI In-Browser]"
     :hint "[Press T to toggle modal]"}]])
