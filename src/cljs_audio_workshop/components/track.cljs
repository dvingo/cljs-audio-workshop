(ns cljs-audio-workshop.components.track
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.utils :refer [lin-interp]]
            [cljs-audio-workshop.state :refer [ui track-samples-for-track track-width
                                          track-sample-height
                                          composition-duration-sec audio-context]]
            [cljs-audio-workshop.components.track-sample :refer [track-sample-view]])
  (:require-macros [cljs-audio-workshop.macros :refer [send!!]]))

(defn play-head-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "play-head-view")
    om/IDidMount (did-mount [_] (om/refresh! owner))
    om/IDidUpdate (did-update [_ _ _] (om/refresh! owner))
    om/IRender
    (render [_]
      (let [current-time (.-currentTime audio-context)
            track-offset-time (mod current-time composition-duration-sec)
            x-offset ((lin-interp 0 composition-duration-sec 0 track-width) track-offset-time)
            offset-str (str "translate("x-offset"px,0px)")]
      (dom/div #js {:className "play-head" :style #js
        {:height track-sample-height :WebkitTransform offset-str :transform offset-str}}
        nil)))))

(defn track-view [track owner]
  (reify
    om/IDisplayName (display-name [_] "track-view")
    om/IRender
    (render [_]
      (let [ui (om/observe owner (ui))
            selected? (= (:selected-track-id ui) (:id track))]

        (dom/div #js {:className "track"}

          (dom/input #js
            {:value (:name track)
             :onChange #(send!! owner :set-track-name track (.. % -target -value))})

          (dom/span nil (:name track))

          (apply dom/div #js
            {:className "container"
             :style #js {:width track-width
                         :border
                         (if selected? "3px dashed" "1px solid black")}
             :onClick #(when-not selected? (send!! owner :select-track track))}
            (concat [(om/build play-head-view nil)]
              (map #(om/build track-sample-view %) (track-samples-for-track track)))))))))
