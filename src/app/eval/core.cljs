(ns app.eval.core
  "In-browser CLJS evaluation engine powered by Small Clojure Interpreter (SCI)."
  (:require [sci.core :as sci]
            [app.api :as api]
            [clojure.string :as str]))

(def ^:private api-bindings
  {'play!               api/play!
   'jam!                api/jam!
   'stop!               api/stop!
   'b!                  api/b!
   'set-bpm!            api/set-bpm!
   'tracks              api/tracks
   'deftrack!           api/deftrack!
   'instruments         api/instruments
   'definst!            api/definst!
   'defrouting!         api/defrouting!
   'routings            api/routings
   'demo!               api/demo!
   'demo-stop!          api/demo-stop!
   'refresh!            api/refresh!
   'loop!               api/loop!
   'l!                  api/l!
   'stop-loop!          api/stop-loop!
   'clear-loops!        api/clear-loops!
   'pattern             api/pattern
   'pat!                api/pat!
   'euclid              api/euclid
   'euc!                api/euc!
   'click!              api/click!
   'toggle-click!       api/toggle-click!
   'mute!               api/mute!
   'm!                  api/m!
   'unmute!             api/unmute!
   'u!                  api/u!
   'solo!               api/solo!
   'so!                 api/solo!
   'unsolo!             api/unsolo!
   'unso!               api/unso!
   'undrum!             api/undrum!
   'redrum!             api/redrum!
   'toggle-bus!         api/toggle-bus!
   'f!                  api/f!
   'set-filter-cutoff!  api/set-filter-cutoff!
   'sw!                 api/sw!
   'sweep-filter!       api/sweep-filter!
   'fb!                 api/fb!
   'wet!                api/wet!
   's!                  api/s!
   'drop!               api/drop!
   'chord!              api/chord!
   '_                   api/_
   'd                   api/d
   'deg                 api/deg
   'chord               api/chord
   'progression         api/progression
   'scale               api/scale
   'sc                  api/sc
   'arp                 api/arp
   'transpose           api/transpose
   'oct-shift           api/oct-shift
   'set-key!            api/set-key!
   'mod-all!            api/mod-all!
   'tr-all!             api/tr-all!
   'scenes              api/scenes
   'defscene!           api/defscene!
   'scene!              api/scene!
   'set-scene!          api/set-scene!
   'g!                  api/g!
   'set-geometry!       api/set-geometry!
   'c!                  api/c!
   'set-colors!         api/set-colors!
   'w!                  api/w!
   'toggle-wireframe!   api/toggle-wireframe!
   'pulse!              api/pulse!
   'stat                api/stat
   'stats!              api/stats!
   'hud!                api/hud!})

(def ^:private sci-ctx
  (sci/init {:bindings api-bindings
             :classes {'Math js/Math}}))

(defn eval-code
  "Safely evaluates a ClojureScript code string in the browser SCI context."
  [code-str]
  (if (str/blank? code-str)
    {:ok? true :result nil}
    (try
      (let [res (sci/eval-string* sci-ctx code-str)]
        {:ok? true :result res})
      (catch :default e
        {:ok? false :error (or (.-message e) (str e))}))))

(defn run-code
  "Evaluates code-str and returns a formatted result map {:ok? bool, :target string, :text string}."
  [code-str]
  (let [trimmed (str/trim (or code-str ""))
        display-target (if (> (count trimmed) 45)
                         (str (subs trimmed 0 42) "...")
                         trimmed)
        {:keys [ok? result error]} (eval-code trimmed)]
    (if ok?
      {:ok?     true
       :target  display-target
       :text    (str "=> " (if (nil? result) ":ok" (pr-str result)))}
      {:ok?     false
       :target  display-target
       :text    (str "Error: " error)})))
