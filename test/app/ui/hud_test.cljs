(ns app.ui.hud-test
  "Unit tests for HUD top bar, status badges, scene controls and bottom bar layout."
  (:require [cljs.test :refer [deftest is testing]]
            [app.state :refer [audio-state visual-state]]
            [app.ui.bottom-bar :refer [bottom-bar-component]]
            [app.ui.top-bar.branding :refer [branding-component]]
            [app.ui.top-bar.controls :refer [controls-component]]
            [clojure.string :as str]))

(deftest branding-component-test
  (testing "Renders TRITONCHA title without status badges in top-bar-branding"
    (let [hiccup (branding-component)]
      (is (= :div.top-bar-branding (first hiccup)))
      ;; Contains neo-brand element with title
      (is (some #(= :div.neo-brand (first %)) (rest hiccup)))
      ;; Does NOT contain top-bar-status badge group anymore
      (is (not (some #(= :div.top-bar-status (first %)) (rest hiccup)))))))

(deftest controls-component-test
  (testing "Renders BPM, KEY, and SCENE together in top-bar-tools"
    (swap! audio-state assoc :bpm 174 :key {:root :d :mode :minor})
    (swap! visual-state assoc :current-scene :acid-sphere)
    (let [hiccup (controls-component {})
          tools  (first (filter #(= :div.top-bar-tools (first %)) (rest hiccup)))]
      (is (some? tools) "top-bar-tools container must be present")
      (let [children (rest tools)
            bpm-badge    (first children)
            key-badge    (second children)
            scene-button (nth children 2)]
        (is (= :span.neo-badge (first bpm-badge)))
        (is (= "174 BPM" (second bpm-badge)))
        (is (= :span.neo-badge.badge-cyan (first key-badge)))
        (is (= "D MINOR" (second key-badge)))
        (is (= :button.neo-btn-stats (first scene-button)))
        (is (= "SCENE: ACID-SPHERE" (last scene-button)))))))

(deftest bottom-bar-component-test
  (testing "Renders hotkey hints on the left and author link (by timcha.dev) on the right"
    (let [hiccup (bottom-bar-component {})
          [_ _ & children] hiccup]
      (is (= :footer.hud-bottom (first hiccup)))
      (is (= 2 (count children)))
      (let [first-child  (first children)
            second-child (second children)]
        ;; First child is hotkey hints (left)
        (is (= :div.hotkey-hints (first first-child)))
        ;; Second child is neo-links-group with timcha.dev (right)
        (is (= :div.neo-links-group (first second-child)))
        (is (str/includes? (str second-child) "timcha.dev"))))))
