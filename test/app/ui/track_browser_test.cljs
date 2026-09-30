(ns app.ui.track-browser-test
  "Unit tests for track preset browser decomposition, formatters, and item views."
  (:require
   [app.ui.track-browser :refer [track-browser-component]]
   [app.ui.track-browser.formatters :as fmt]
   [app.ui.track-browser.item :refer [track-item]]
   [app.ui.track-browser.list :refer [track-list]]
   [cljs.test :refer [deftest is testing]]
   [clojure.string :as str]))

(deftest format-scale-test
  (testing "Formats root and mode into capitalized title string"
    (is (= "E Phrygian" (fmt/format-scale [:e :phrygian])))
    (is (= "D Dorian" (fmt/format-scale [:d :dorian])))
    (is (= "A Hungarian-minor" (fmt/format-scale [:a :hungarian-minor])))
    (is (= "Custom" (fmt/format-scale nil)))))

(deftest track-item-test
  (testing "Renders track item with index, title, badges, and load action"
    (let [sample-track {:id :roller :name "Phrygian Roller" :bpm 168 :scale [:e :phrygian] :geom :torus}
          hiccup       (track-item {:idx       0
                                    :item      sample-track
                                    :active?   true
                                    :on-select (fn [_] nil)})]
      (is (= :div.track-browser-item (first hiccup)))
      (is (= "active" (get-in hiccup [1 :class])))
      (is (str/includes? (str hiccup) "01"))
      (is (str/includes? (str hiccup) "Phrygian Roller"))
      (is (str/includes? (str hiccup) "168 BPM"))
      (is (str/includes? (str hiccup) "E Phrygian"))
      (is (str/includes? (str hiccup) "PLAYING")))))

(deftest track-list-test
  (testing "Renders list containing track items"
    (let [jams   [{:id :roller :name "Track 1" :bpm 168 :scale [:e :phrygian]}
                  {:id :sub :name "Track 2" :bpm 172 :scale [:e :phrygian]}]
          hiccup (track-list jams :roller (fn [_] nil))]
      (is (= :div.track-browser-list (first hiccup)))
      (is (= 2 (count (rest hiccup)))))))

(deftest track-browser-component-test
  (testing "Renders full track browser modal dialog with header, list, and footer"
    (let [hiccup (track-browser-component {:on-close (fn [] nil)})]
      (is (= :div.track-browser-modal (first hiccup)))
      (is (= "dialog" (get-in hiccup [1 :role])))
      (is (str/includes? (str hiccup) "TRACK PRESETS LIBRARY"))
      (is (str/includes? (str hiccup) "track-browser-footer")))))
