(ns cljs-audio-workshop.components.audio-buffers
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.components.audio-buffer :refer [audio-buffer-view]]
            [cljs-audio-workshop.state :refer [sounds]]))

(defn audio-buffers-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-view")
    om/IRender
    (render [_]
      (let [snds (om/observe owner (sounds))]
        (dom/div #js {:style #js {:float "left"}}
          (apply dom/div #js {:style #js {:float "left"}}
            (map #(om/build audio-buffer-view %) snds)))))))
