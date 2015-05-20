(ns cljs-audio-workshop.components.wave-selector
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.utils :refer [listen]]
            [cljs-audio-workshop.state :refer [wave-width]])
  (:import [goog.events EventType])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [cljs-audio-workshop.macros :refer [send!]]))

(defn rel-mouse-pos [e {:keys [x-offset mouse-down-pos]}]
  "Function to get updated position of overlay rectangle x position
  and where the mouse down is. These two values are different because
  you can drag from anywhere 'inside' the overlay rectangle.
  e is a mouse-move event.
  x-offset is the current position of the left edge of
  the selector canvas element.
  mouse-down-pos is the previous location of where the mouse way dragged from.
  The new mouse-down position will become current-mouse-x.
  new-x will be where the overlay rectangle's left edge is."
  (let [current-mouse-x (.-clientX e)
        [previous-x previous-y] mouse-down-pos
        x-delta (- current-mouse-x previous-x)
        new-x (+ x-offset x-delta)]
    [new-x current-mouse-x previous-x previous-y]))

(defn draw-select-rect! [canvas-width canvas-height canvas-context mouse-down? mouse-over?]
  (.clearRect canvas-context 0 0 canvas-width canvas-height)
  (.fillRect canvas-context 0 0 canvas-width canvas-height)
  (aset canvas-context "strokeStyle" "aliceblue")
  (cond
    mouse-down?
    (do (aset canvas-context "lineWidth" 6)
        (.strokeRect canvas-context 2 2 (- canvas-width 4) (- canvas-height 4)))
    mouse-over?
    (do (aset canvas-context "lineWidth" 2)
        (.strokeRect canvas-context 0 0 canvas-width canvas-height))))

(defn wave-selector-view [sound owner]
  (reify
    om/IDisplayName (display-name [_] "wave-selector-view")

    om/IInitState
    (init-state [_]
      {:canvas nil
       :canvas-context nil
       :canvas-height 100
       :mouse-down false
       :mouse-down-pos []
       :mouse-over false})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [canvas-height left-x]} (om/get-state owner)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")
            mouse-move-chan (listen js/document (.-MOUSEMOVE EventType)
                                    (comp (filter #(:mouse-down (om/get-state owner)))
                                          (map #(rel-mouse-pos % (om/get-state owner)))))
            mouse-up-chan (listen js/document (.-MOUSEUP EventType))]
        (go-loop []
          (alt!
            mouse-move-chan
            ([[new-x mouse-down-x _ old-y]]
             (let [canvas-width (:canvas-width (om/get-state owner))
                   clamp-x (.clamp goog.math new-x 0 (- wave-width canvas-width))]
               (send! owner :set-sound-offset (last (om/path sound)) clamp-x)
               (om/update-state! owner #(assoc % :x-offset clamp-x :mouse-down-pos [mouse-down-x old-y]))))
            mouse-up-chan
            ([_] (om/set-state! owner :mouse-down false)))
          (recur))
        (om/update-state! owner #(assoc % :canvas-context canvas-context :canvas canvas))))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [canvas-height canvas-context mouse-down
                    mouse-over canvas-width]} (om/get-state owner)]
        (if canvas-context
          (draw-select-rect! canvas-width canvas-height canvas-context mouse-down mouse-over)
          (om/refresh! owner))))

    om/IRenderState
    (render-state [_ {:keys [canvas-height mouse-down x-offset canvas-width max-width]}]
      (dom/canvas #js {:width       canvas-width
                       :height      canvas-height
                       :style       #js {:opacity 0.3
                                         :position "absolute"
                                         :left x-offset
                                         :cursor (if mouse-down "move" "default")}
                       :ref         "canvas-ref"
                       :onMouseDown (fn [e] (om/update-state! owner
                                              #(assoc % :mouse-down true
                                                        :mouse-down-pos [(.-clientX e) (.-clientY e)])))
                       :onMouseOver #(om/set-state! owner :mouse-over true)
                       :onMouseOut  #(om/set-state! owner :mouse-over false)}
                  "no canvas"))))
