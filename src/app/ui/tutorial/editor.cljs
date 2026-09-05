(ns app.ui.tutorial.editor
  "Live code editor subcomponent with state persistence and realtime Clojure syntax highlighting."
  (:require [app.eval.buffer :as buffer]
            [app.ui.tutorial.highlight :refer [highlight-clojure-hiccup]]
            [reagent.core :as r]))

(defonce editor-state
  (atom {:scroll-top  0
         :scroll-left 0
         :selection-s 0
         :selection-e 0}))

(defn editor-component [{:keys [default-content content-atom]}]
  (let [content-state (or content-atom (r/atom (or default-content "")))
        textarea-ref  (atom nil)
        pre-ref       (atom nil)

        sync-scroll!
        (fn [^js ta]
          (when ta
            (when-let [p @pre-ref]
              (set! (.-scrollTop p) (.-scrollTop ta))
              (set! (.-scrollLeft p) (.-scrollLeft ta)))))

        sync-state!
        (fn [^js ta]
          (when ta
            (reset! editor-state {:scroll-top  (.-scrollTop ta)
                                  :scroll-left (.-scrollLeft ta)
                                  :selection-s (.-selectionStart ta)
                                  :selection-e (.-selectionEnd ta)})
            (sync-scroll! ta)))

        restore-state!
        (fn [^js ta]
          (when ta
            (let [{:keys [scroll-top scroll-left selection-s selection-e]} @editor-state]
              (set! (.-scrollTop ta) scroll-top)
              (set! (.-scrollLeft ta) scroll-left)
              (set! (.-selectionStart ta) (or selection-s 0))
              (set! (.-selectionEnd ta) (or selection-e 0))
              (sync-scroll! ta)
              (.focus ta))))

        handle-key-down
        (fn [^js e eval-fn on-eval-all]
          (let [k      (.-key e)
                target (.-target e)]
            (cond
              (and (or (.-ctrlKey e) (.-metaKey e)) (= k "Enter"))
              (do
                (.preventDefault e)
                (.stopPropagation e)
                (sync-state! target)
                (if (.-shiftKey e)
                  (on-eval-all @content-state)
                  (let [expr (buffer/get-code-at-cursor @content-state (.-selectionStart target) (.-selectionEnd target))]
                    (eval-fn expr)))
                (js/requestAnimationFrame #(restore-state! target)))

              (= k "Tab")
              (do
                (.preventDefault e)
                (.stopPropagation e)
                (let [start (.-selectionStart target)
                      end   (.-selectionEnd target)
                      val   @content-state
                      {:keys [text cursor]} (buffer/insert-tab val start end)]
                  (reset! content-state text)
                  (js/requestAnimationFrame
                   (fn []
                     (set! (.-selectionStart target) cursor)
                     (set! (.-selectionEnd target) cursor)
                     (sync-state! target)))))

              :else
              (do
                (sync-state! target)
                (.stopPropagation e)))))]

    (fn [{:keys [on-eval-sexp on-eval-line on-eval-all]}]
      (let [eval-fn (or on-eval-sexp on-eval-line)]
        [:div.scratchpad-container
         [:div.scratchpad-toolbar
          [:span.scratchpad-hint "[Ctrl+Enter] Eval SEXP | [Ctrl+Shift+Enter] Eval All"]
          [:div.scratchpad-btn-group
           [:button.neo-run-btn
            {:on-click (fn [_]
                         (when-let [ta @textarea-ref]
                           (sync-state! ta)
                           (let [start (.-selectionStart ta)
                                 end   (.-selectionEnd ta)]
                             (eval-fn (buffer/get-code-at-cursor @content-state start end)))
                           (js/requestAnimationFrame #(restore-state! ta))))
             :title "Evaluate enclosing S-expression under cursor (Ctrl+Enter)"}
            "EVAL SEXP"]
           [:button.neo-run-btn.btn-all
            {:on-click (fn [_]
                         (when-let [ta @textarea-ref]
                           (sync-state! ta)
                           (on-eval-all @content-state)
                           (js/requestAnimationFrame #(restore-state! ta))))
             :title "Evaluate full script buffer (Ctrl+Shift+Enter)"}
            "EVAL ALL"]]]

         [:div.neo-editor-stage
          [:textarea.neo-code-editor
           {:id            "tutorial-editor"
            :ref           (fn [el]
                             (when el
                               (reset! textarea-ref el)
                               (restore-state! el)))
            :value         (or @content-state "")
            :on-change     (fn [^js e]
                             (let [v (.. e -target -value)]
                               (reset! content-state v)
                               (sync-state! (.-target e))))
            :on-scroll     (fn [^js e] (sync-state! (.-target e)))
            :on-click      (fn [^js e] (sync-state! (.-target e)))
            :on-key-up     (fn [^js e] (sync-state! (.-target e)))
            :on-key-down   #(handle-key-down % eval-fn on-eval-all)
            :placeholder   "Type ClojureScript expressions here..."
            :spell-check   false
            :rows          22}]

          [:pre.neo-editor-highlight
           {:ref (fn [el] (reset! pre-ref el))
            :aria-hidden "true"}
           [highlight-clojure-hiccup (or @content-state "")]]]]))))