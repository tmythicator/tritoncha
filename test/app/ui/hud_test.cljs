(ns app.ui.hud-test
  "Unit tests for HUD top bar, status badges, scene controls and bottom bar layout."
  (:require [cljs.test :refer [deftest is testing]]
            [app.state :refer [audio-state visual-state]]
            [app.ui.bottom-bar :refer [bottom-bar-component]]
            [app.ui.top-bar.branding :refer [branding-component]]
            [app.ui.top-bar.controls :refer [controls-component master-deck-component tweaks-deck-component]]
            [clojure.string :as str]))

(deftest branding-component-test
  (testing "Renders TRITONCHA title without status badges in top-bar-branding"
    (let [hiccup (branding-component)]
      (is (= :div.top-bar-branding (first hiccup)))
      ;; Contains neo-brand element with title
      (is (some #(= :div.neo-brand (first %)) (rest hiccup)))
      ;; Does NOT contain top-bar-status badge group anymore
      (is (not (some #(= :div.top-bar-status (first %)) (rest hiccup)))))))

(deftest master-deck-component-test
  (testing "Renders play engine button, track scrubber deck with chevrons and index, and scene button"
    (swap! audio-state assoc :active? false :current-jam :orbital-roller)
    (swap! visual-state assoc :current-scene :acid-sphere)
    (let [hiccup    (master-deck-component {})
          children  (rest hiccup)
          play-btn  (first children)
          track-dk  (second children)
          scene-btn (nth children 2)]
      (is (= :div.hud-master-deck (first hiccup)))
      (is (= :button.player-play-btn (first play-btn)))
      (is (str/includes? (last play-btn) "PLAY ENGINE"))
      (is (= :div.player-track-deck (first track-dk)))
      (is (= :button.neo-btn-stats.btn-swap-scene (first scene-btn)))
      (is (str/includes? (last scene-btn) "SCENE: ACID-SPHERE")))))

(deftest tweaks-deck-component-test
  (testing "Renders BPM tempo slider, KEY and SCALE selects, and RESET button in top-bar-tools"
    (swap! audio-state assoc :bpm 174 :key {:root :d :mode :minor})
    (let [hiccup       (tweaks-deck-component {})
          children     (rest hiccup)
          tempo-box    (first children)
          key-select   (second children)
          scale-select (nth children 2)
          reset-button (nth children 3)]
      (is (= :div.top-bar-tools (first hiccup)))
      (is (= :div.tool-tempo-box (first tempo-box)))
      (is (str/includes? (str tempo-box) "174 BPM"))
      (is (some #(= :input.neo-tempo-slider (first %)) (rest tempo-box)))
      (is (= :select.neo-select.neo-select-key (first key-select)))
      (is (= "d" (get-in key-select [1 :value])))
      (is (= :select.neo-select.neo-select-scale (first scale-select)))
      (is (= "minor" (get-in scale-select [1 :value])))
      (is (= :button.neo-btn-stats.btn-reset (first reset-button)))
      (is (str/includes? (last reset-button) "RESET")))))

(deftest controls-component-test
  (testing "Renders both master and tweaks decks in top-bar-controls"
    (let [hiccup (controls-component {})]
      (is (= :div.top-bar-controls (first hiccup)))
      (is (= 2 (count (rest hiccup)))))))

(deftest bottom-bar-component-test
  (testing "Renders hotkey hints on the left and website link on the right"
    (let [hiccup (bottom-bar-component {})
          [_ _ & children] hiccup]
      (is (= :footer.hud-bottom (first hiccup)))
      (is (= 2 (count children)))
      (let [first-child  (first children)
            second-child (second children)]
        (is (= :div.hotkey-hints (first first-child)))
        (is (= :div.neo-links-group (first second-child)))
        (is (str/includes? (str second-child) "timcha.dev"))))))
