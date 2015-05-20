(ns cljs-audio-workshop.components.samples
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.state :refer [samples]]
            [cljs-audio-workshop.components.sample :refer [sample-view]]))

(defn samples-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "samples-view")
    om/IRender
    (render [_]
      (let [smples (om/observe owner (samples))]
        (apply dom/div #js {:className "samples"}
               (map #(om/build sample-view %) smples))))))

