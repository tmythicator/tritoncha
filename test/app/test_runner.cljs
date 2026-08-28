(ns app.test-runner
  (:require
   [app.audio.control.looper-test]
   [app.audio.control.mixer-test]
   [app.audio.control.session-test]
   [app.audio.dsp.busses-test]
   [app.audio.dsp.telemetry-test]
   [app.audio.theory.harmony-test]
   [app.audio.theory.patterns-test]
   [app.eval.buffer-test]
   [app.eval.core-test]
   [app.lib.instruments-test]
   [app.lib.routes-test]
   [app.lib.scenes-test]
   [app.lib.tracks-test]
   [app.utils.audio-test]
   [app.utils.coll-test]
   [app.utils.dom-test]
   [app.utils.math-test]
   [cljs.test :as test]))

(defn main []
  (test/run-all-tests #"app\..*-test"))
