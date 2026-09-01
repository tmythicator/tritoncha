(ns app.ui.tutorial.editor
  "Live code editor subcomponent with state persistence."
  (:require [app.eval.buffer :as buffer]))

(defonce editor-state
  (atom {:scroll-top  0
         :selection-s 0
         :selection-e 0}))

(defn editor-component [{:keys [on-eval-line on-eval-all default-content]}]
  (let [textarea-ref (atom nil)
        sync-state!
        (fn [^js ta]
          (when ta
            (reset! editor-state {:scroll-top  (.-scrollTop ta)
                                  :selection-s (.-selectionStart ta)
                                  :selection-e (.-selectionEnd ta)})))

        restore-state!
        (fn [^js ta]
          (when ta
            (let [{:keys [scroll-top selection-s selection-e]} @editor-state]
              (set! (.-scrollTop ta) scroll-top)
              (set! (.-selectionStart ta) selection-s)
              (set! (.-selectionEnd ta) selection-e)
              (.focus ta))))

        handle-key-down
        (fn [^js e]
          (let [k (.-key e)
                target (.-target e)]
            (cond
              (and (or (.-ctrlKey e) (.-metaKey e)) (= k "Enter"))
              (do
                (.preventDefault e)
                (.stopPropagation e)
                (sync-state! target)
                (if (.-shiftKey e)
                  (on-eval-all (.-value target))
                  (let [expr (buffer/get-code-at-cursor (.-value target) (.-selectionStart target) (.-selectionEnd target))]
                    (on-eval-line expr)))
                (js/requestAnimationFrame #(restore-state! target)))

              (= k "Tab")
              (do
                (.preventDefault e)
                (.stopPropagation e)
                (let [start (.-selectionStart target)
                      end   (.-selectionEnd target)
                      val   (.-value target)
                      {:keys [text cursor]} (buffer/insert-tab val start end)]
                  (set! (.-value target) text)
                  (set! (.-selectionStart target) cursor)
                  (set! (.-selectionEnd target) cursor)
                  (sync-state! target)))

              :else
              (do
                (sync-state! target)
                (.stopPropagation e)))))]

    [:div.scratchpad-container
     [:div.scratchpad-toolbar
      [:span.scratchpad-hint "[Ctrl+Enter] Eval Line/Form | [Ctrl+Shift+Enter] Eval All"]
      [:div.scratchpad-btn-group
       [:button.neo-run-btn
        {:on-click (fn [_]
                     (when-let [ta @textarea-ref]
                       (sync-state! ta)
                       (let [start (.-selectionStart ta)
                             end   (.-selectionEnd ta)]
                         (on-eval-line (buffer/get-code-at-cursor (.-value ta) start end)))
                       (js/requestAnimationFrame #(restore-state! ta))))
         :title "Evaluate line or form under cursor (Ctrl+Enter)"}
        "EVAL LINE"]
       [:button.neo-run-btn.btn-all
        {:on-click (fn [_]
                     (when-let [ta @textarea-ref]
                       (sync-state! ta)
                       (on-eval-all (.-value ta))
                       (js/requestAnimationFrame #(restore-state! ta))))
         :title "Evaluate full script buffer (Ctrl+Shift+Enter)"}
        "EVAL ALL"]]]

     [:textarea.neo-code-editor
      {:id            "tutorial-editor"
       :ref           (fn [el]
                        (when el
                          (reset! textarea-ref el)
                          (restore-state! el)))
       :default-value default-content
       :on-scroll     (fn [^js e] (sync-state! (.-target e)))
       :on-click      (fn [^js e] (sync-state! (.-target e)))
       :on-key-up     (fn [^js e] (sync-state! (.-target e)))
       :on-key-down   handle-key-down
       :placeholder   "Type ClojureScript expressions here..."
       :spell-check   false
       :rows          22}]]))