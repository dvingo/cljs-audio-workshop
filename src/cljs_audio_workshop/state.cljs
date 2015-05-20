(ns cljs-audio-workshop.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [<!]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(let [db {:compositions []
          :tracks []
          :track-samples []
          :samples []
          :sounds []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :ui {:buffers-visible true
               :selected-track-id nil
               :selected-track-idx nil
               :is-playing false}}]

  (defonce app-state (atom db)))

(defonce audio-context (js/window.AudioContext.))

(defn start-actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
    (match [action-vec]
      [[:some-action]] (.log js/console "got an action")
      :else (.error js/console "Unknown handler: " (clj->js action-vec)))
    (recur (<! actions-chan))))
