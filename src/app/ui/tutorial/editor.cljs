(ns app.ui.tutorial.editor
  "Live code editor subcomponent."
  (:require [app.eval.buffer :as buffer]))

(defn editor-component [{:keys [code-atom on-eval-line on-eval-all]}]
  (let [handle-key-down
        (fn [^js e]
          (let [k (.-key e)
                target (.-target e)]
            (cond
              (and (or (.-ctrlKey e) (.-metaKey e)) (= k "Enter"))
              (do
                (.preventDefault e)
                (.stopPropagation e)
                (if (.-shiftKey e)
                  (on-eval-all @code-atom)
                  (let [sel-start (.-selectionStart target)
                        sel-end   (.-selectionEnd target)
                        expr      (buffer/get-code-at-cursor @code-atom sel-start sel-end)]
                    (on-eval-line expr))))

              (= k "Tab")
              (do
                (.preventDefault e)
                (.stopPropagation e)
                (let [start (.-selectionStart target)
                      end   (.-selectionEnd target)
                      val   (.-value target)
                      {:keys [text cursor]} (buffer/insert-tab val start end)]
                  (reset! code-atom text)
                  (js/setTimeout (fn []
                                   (set! (.-selectionStart target) cursor)
                                   (set! (.-selectionEnd target) cursor)) 0)))

              :else
              (.stopPropagation e))))]

    [:div.scratchpad-container
     [:div.scratchpad-toolbar
      [:span.scratchpad-hint "[Ctrl+Enter] Eval Line/Form | [Ctrl+Shift+Enter] Eval All"]
      [:div.scratchpad-btn-group
       [:button.neo-run-btn
        {:on-click (fn [_]
                     (let [el (.querySelector js/document ".neo-code-editor")
                           start (when el (.-selectionStart el))
                           end   (when el (.-selectionEnd el))]
                       (on-eval-line (buffer/get-code-at-cursor @code-atom start end))))
         :title "Evaluate line or form under cursor (Ctrl+Enter)"}
        "EVAL LINE"]
       [:button.neo-run-btn.btn-all
        {:on-click (fn [_] (on-eval-all @code-atom))
         :title "Evaluate full script buffer (Ctrl+Shift+Enter)"}
        "EVAL ALL"]]]

     [:textarea.neo-code-editor
      {:value       @code-atom
       :on-change   #(reset! code-atom (.. % -target -value))
       :on-key-down handle-key-down
       :placeholder "Type ClojureScript expressions here..."
       :spell-check false
       :rows 22}]]))
