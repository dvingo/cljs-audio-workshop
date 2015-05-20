(ns cljs-audio-workshop.components.tracks
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.state :refer [tracks]]
            [cljs-audio-workshop.utils :refer [make-button]]
            [cljs-audio-workshop.components.track :refer [track-view]])
  (:require-macros [cljs-audio-workshop.macros :refer [send!! build-button]]))

(defn tracks-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "tracks-view")
    om/IRender
    (render [_]
      (let [trcks (om/observe owner (tracks))]
        (dom/div nil
          (build-button "new-track-button" #(send!! owner :make-new-track) "New Track")
          (apply dom/div #js {:className "tracks"}
            (map #(om/build track-view %) trcks)))))))
