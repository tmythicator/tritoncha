(ns app.ui.stats.routing-graph
  "DSP audio routing topology visualization subcomponent.")

(defn routing-graph-component []
  [:div.neo-section
   [:div.neo-section-label "$ routing_topology"]
   [:div.neo-routing-box
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-drums "DRUMS"]
     [:div.neo-route-chain
      [:span.neo-node "LP-FILTER (3.4k)"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-bass "BASS"]
     [:div.neo-route-chain
      [:span.neo-node "DISTORTION"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LP-FILTER"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-space "SPACE"]
     [:div.neo-route-chain
      [:span.neo-node "DELAY (8n.)"]
      [:span.neo-arrow ">"]
      [:span.neo-node "REVERB (3.8s)"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]
    [:div.neo-route-row
     [:span.neo-bus-tag.bus-direct "DIRECT"]
     [:div.neo-route-chain
      [:span.neo-node "DIRECT PASSTHROUGH"]
      [:span.neo-arrow ">"]
      [:span.neo-node "LIMITER"]
      [:span.neo-arrow ">"]
      [:span.neo-dest "OUT"]]]]])
