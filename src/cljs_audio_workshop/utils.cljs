(ns cljs-audio-workshop.utils)

(defn max-of-array [array-of-nums]
  (.apply js/Math.max nil array-of-nums))

(defn min-of-array [array-of-nums]
  (.apply js/Math.min nil array-of-nums))
