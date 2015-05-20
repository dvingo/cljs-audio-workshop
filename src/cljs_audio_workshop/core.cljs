(ns ^:figwheel-always cljs-audio-workshop.core
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs.core.async :refer [chan]]
              [cljs-audio-workshop.state :refer [start-actions-handler app-state audio-context]]
              [cljs-audio-workshop.components.main :refer [main-view]]))

(enable-console-print!)

(defn set-prop-if-undefined! [prop obj options]
  (when-not (aget obj prop)
    (let [opts (map #(aget obj %) options)
          prop-to-use (first (filter #(not (nil? %)) opts))]
      (aset obj prop prop-to-use))))

(set-prop-if-undefined! "AudioContext" js/window ["AudioContext" "webkitAudioContext" "mozAudioContext"])
(set-prop-if-undefined! "getUserMedia" js/navigator ["webkitGetUserMedia" "mozGetUserMedia"])

(defonce got-audio? (atom false))

(defn got-stream [stream]
  (reset! got-audio? true)
  (let [audio-input (.createMediaStreamSource audio-context stream)
        analyser-node (.createAnalyser audio-context)]
    (set! (.-fftSize analyser-node) 2048)
    (.connect audio-input analyser-node)
    (swap! app-state assoc :audio-recorder (js/Recorder. audio-input
                                                         #js {:workerPath "js/recorderWorker.js"})
                           :analyser-node analyser-node)

    (om/root main-view app-state
      {:shared {:action-chan (chan)}
       :target (. js/document (getElementById "app"))})))

(when-not @got-audio?
  (let [audio-constraints (clj->js {"audio" {"mandatory" {"googEchoCancellation" "false"
                                                          "googAutoGainControl"  "false"
                                                          "googNoiseSuppression" "false"
                                                          "googHighpassFilter"   "false"}
                                               "optional" []}})]
    (.getUserMedia js/navigator audio-constraints
                   got-stream
                   #(.log js/console "ERROR getting user media"))))

(defn on-js-reload []
  (swap! app-state update-in [:__figwheel_counter] inc))
