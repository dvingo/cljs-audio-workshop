(ns cljs-audio-workshop.components.track-sample
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.utils :refer [lin-interp listen]]
            [cljs-audio-workshop.state :refer [track-sample->bg-color track-width
                                          note-type->width
                                          track-sample-height]])
  (:import [goog.events EventType])
  (:require-macros [cljs-audio-workshop.macros :refer [send!]]
                   [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn rel-mouse-x-pos [mouse-x {:keys [x-offset mouse-down-x]}]
  (let [x-delta (- mouse-x mouse-down-x)]
    [(+ x-offset x-delta) mouse-x]))

(defn track-sample-view [t-sample owner]
  (reify
    om/IDisplayName (display-name [_] "track-sample-view")
    om/IInitState
    (init-state [_]
      {:mouse-down false
       :mouse-down-x 0
       :x-offset 0
       :t-sample-width 40
       :mouse-over false})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [x-offset t-sample-width]} (om/get-state owner)
            max-x-pos (- track-width t-sample-width)
            mouse-move-chan (listen js/document (.-MOUSEMOVE EventType)
                                    (comp (filter #(:mouse-down (om/get-state owner)))
                                          (map #(rel-mouse-x-pos (.-clientX %)
                                                                 (om/get-state owner)))
                                          (map (fn [[new-x mouse-x]]
                                                 [(.clamp goog.math new-x 0 max-x-pos) mouse-x]))))
            mouse-up-chan (listen js/document (.-MOUSEUP EventType))]
        (go-loop []
          (alt!
            mouse-move-chan ([[new-x mouse-down-x]]
              (send! owner :set-track-sample-offset t-sample new-x)
              (om/update-state! owner #(assoc % :x-offset new-x :mouse-down-x mouse-down-x)))
            mouse-up-chan ([_]
              (om/set-state! owner :mouse-down false)))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [x-offset mouse-down-x mouse-over mouse-down]}]
      (let [offset-str  (str "translate("x-offset"px,0px)")]
        (dom/div #js {:className "track-sample"
                      :style #js {:WebkitTransform offset-str :transform offset-str
                                  :background (if (:is-playing t-sample)
                                                "black"
                                                (track-sample->bg-color t-sample))
                                  :border (cond mouse-down "1px solid dodgerblue"
                                                mouse-over "1px solid white")
                                  :width 40;;track-sample-width
                                  :height track-sample-height}
                      :onMouseDown (fn [e] (om/update-state! owner
                         #(assoc % :mouse-down true :mouse-down-x (.-clientX e))))
                      :onMouseOver #(om/set-state! owner :mouse-over true)
                      :onMouseOut  #(om/set-state! owner :mouse-over false)}
           nil)))))
