(ns cljs-audio-workshop.utils
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [cljs.core.async :refer [put! chan]]
            [goog.events :as events]))

(defn max-of-array [array-of-nums]
  (.apply js/Math.max nil array-of-nums))

(defn min-of-array [array-of-nums]
  (.apply js/Math.min nil array-of-nums))

(defn listen
  ([el type]
   (let [out (chan)]
     (events/listen el type
       (fn [e] (put! out e)))
     out))
  ([el type tx]
   (let [out (chan 1 tx)]
     (events/listen el type
       (fn [e] (put! out e)))
     out)))

(defn lin-interp [x0 x1 y0 y1]
  (fn [x]
    (+ y0
       (* (/ (- x x0)
             (- x1 x0))
          (- y1 y0)))))

(defn superlative-of [arr compar]
  (loop [i 0 min-val (aget arr 0)]
    (if (= i (.-length arr))
      min-val
      (let [cur-val (aget arr i)]
        (if (apply compar [cur-val min-val])
          (recur (inc i) cur-val)
          (recur (inc i) min-val))))))

(defn min-arr-val [arr]
  (superlative-of arr <))

(defn max-arr-val [arr]
  (superlative-of arr >))

(defn make-button [disp-name on-click btn-label]
  (fn [data owner]
    (reify
      om/IDisplayName (display-name [_] disp-name)
      om/IRender
      (render [_] (dom/button #js {:onClick on-click} btn-label)))))
