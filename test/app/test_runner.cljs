(ns app.test-runner
  (:require [cljs.test :as test]
            [app.utils-test]
            [app.audio.theory-test]
            [app.lib.tracks-test]
            [app.eval.core-test]
            [app.eval.buffer-test]))

(defn main []
  (test/run-all-tests #"app\..*-test"))
