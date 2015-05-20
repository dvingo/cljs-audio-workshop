(ns cljs-audio-workshop.utils)

(defn max-of-array [array-of-nums]
  (.apply js/Math.max nil array-of-nums))

(defn min-of-array [array-of-nums]
  (.apply js/Math.min nil array-of-nums))

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
