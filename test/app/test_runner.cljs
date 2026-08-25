(ns app.test-runner
  (:require
   [app.audio.theory-test]
   [app.eval.buffer-test]
   [app.eval.core-test]
   [app.lib.instruments-test]
   [app.lib.routes-test]
   [app.lib.scenes-test]
   [app.lib.tracks-test]
   [app.utils-test]
   [cljs.test :as test]))

(defn main []
  (test/run-all-tests #"app\..*-test"))
