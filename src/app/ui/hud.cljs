(ns app.ui.hud
  (:require [clojure.string :as str]
            [reagent.dom.client :as rdom]
            [app.state :refer [state]]
            [app.audio.theory :as theory]
            [app.audio.looper :as looper]))

(defn toggle-hud! []
  (swap! state update :hud-visible? not))

(defn hud-component []
  (let [{:keys [active? bpm current-jam hud-visible? mesh-color]} @state
        jam-name (-> (or current-jam :roller) name str/upper-case)
        {:keys [root mode]} @theory/global-key
        active-loops-map @looper/active-tracks
        active-loops (keys active-loops-map)]
    [:div#hud {:class (str "minimal-hud" (when-not hud-visible? " hidden"))}
     [:div.hud-top
      [:div.hud-brand
       [:span.pulse-dot {:class (when active? "active")}]
       [:span.brand-text "TRITONCHA"]
       [:span#bpm-display.hud-badge (str bpm " BPM")]
       [:span#key-display.hud-badge {:style {:border-color "rgba(0, 255, 204, 0.4)"}}
        (str (str/upper-case (name (or root :e))) " " (str/upper-case (name (or mode :dorian))))]
       [:span#jam-display.hud-badge {:style {:border-color mesh-color}} jam-name]]
      [:div.hud-info
       [:div.hud-tracks
        (when (seq active-loops)
          [:div.loop-tags
           (for [lk active-loops
                 :let [tr (get active-loops-map lk)
                       muted? @(:muted? tr)]]
             ^{:key lk}
             [:span.loop-tag {:class (if muted? "muted" "active")}
              (str (if muted? "[M] " "") (name lk))])])
        [:span#status-hint (if active?
                             "LIVE ENGINE ACTIVE"
                             "READY (REPL / SPACE)")]]]]

     [:div.hud-bottom
      [:div.hud-links
       [:span.hud-by "by "]
       [:a.hud-link {:href "https://timcha.dev" :target "_blank" :rel "noreferrer"} "timcha.dev"]]
      [:div.hotkey-hints
       [:span "[Space] Jam"]
       [:span "[1-3] Presets"]
       [:span "[U] Undrum"]
       [:span "[R] Redrum"]
       [:span "[G] Geom"]
       [:span "[W] Wire"]
       [:span "[H] HUD"]]]]))

(defonce root-container (atom nil))

(defn render-ui! []
  (when-let [el (.getElementById js/document "app")]
    (when-not @root-container
      (reset! root-container (rdom/create-root el)))
    (rdom/render @root-container [hud-component])))
