(ns cljs-audio-workshop.components.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [cljs-audio-workshop.state :refer [start-actions-handler]]
            [cljs-audio-workshop.components.mic-chart :refer [mic-chart-view]]
            [cljs-audio-workshop.components.audio-buffers :refer [audio-buffers-view]]
            [cljs-audio-workshop.components.recorder :refer [recorder-view]]))

(defn main-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (start-actions-handler (:action-chan (om/get-shared owner)) data))
    om/IRender
    (render [_]
      (dom/div nil
        (om/build recorder-view data)
        (om/build audio-buffers-view data)
        (om/build mic-chart-view data)))))
