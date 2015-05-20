(ns cljs-audio-workshop.components.sample
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.state :refer [sample-width sample-height
                                          note-type->bg-color
                                          note-type->color
                                          note-type->num]])
  (:require-macros [cljs-audio-workshop.macros :refer [send!!]]))

(def fill-color "lavender")
(def stroke-color "linen")
(def circle-radius 0.5)
(def stroke-width ".08px")

(defn x-from-angle [angle r]
  (+ (* (js/Math.cos angle) r) r))

(defn y-from-angle [angle r]
  (+ (* (js/Math.sin angle) r) r))

(defn arc-path [r x y]
  (apply str
    "M " r " " r
   " l " r " " 0
   " A " r ", " r " 0, 0, 1, " x ", " y " z"))

(defn sample-view [sample owner]
  (reify
    om/IDisplayName (display-name [_] "sample-view")
    om/IRender
    (render [_]
      (let [note-type (:type sample)
            theta (/ (* 2 js/Math.PI) (note-type->num note-type))
            x (x-from-angle theta circle-radius)
            y (y-from-angle theta circle-radius)]
        (dom/div #js {:className "sample"
                      :style #js
                      {:width sample-width
                       :height sample-height
                       :color (get note-type->color note-type)
                       :background (get note-type->bg-color note-type)
                       :padding (/ sample-width 10)
                       :borderRadius (/ sample-width 10)}
                      :onClick #(send!! owner :add-sample-to-track sample)}
                 (dom/p #js {:className "name"} (:name sample))
                 (apply dom/svg #js {:viewBox "0 0 1 1" :width "20"
                                     :style #js {:position "absolute" :top "4px"
                                                 :right "4px" :padding "4px"}}
                        (if (= note-type "Whole")
                          [(dom/circle #js {:cx ".5" :cy ".5" :r circle-radius
                                            :strokeWidth stroke-width :stroke stroke-color :fill fill-color})]
                          [(dom/circle #js {:cx ".5" :cy ".5" :r circle-radius
                                            :strokeWidth stroke-width :stroke stroke-color :fill "none"})
                           (dom/path #js {:d (arc-path circle-radius x y)
                                          :fill fill-color
                                          :transform (str "rotate(270,"circle-radius","circle-radius")")})])))))))
