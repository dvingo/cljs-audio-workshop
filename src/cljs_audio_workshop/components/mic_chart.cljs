(ns cljs-audio-workshop.components.mic-chart
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [cljs-audio-workshop.utils :refer [max-of-array min-of-array]]))

(defn clear-canvas! [canvas-context canvas-width canvas-height bg-color]
  (if (nil? bg-color)
    (.clearRect canvas-context 0 0 canvas-width canvas-height)
    (do
      (set! (.-fillStyle canvas-context) bg-color)
      (.fillRect canvas-context 0 0 canvas-width canvas-height)
      (set! (.-fillStyle canvas-context) "#F6D565")
      (set! (.-lineCap canvas-context) "round"))))

(defn draw-line-on-canvas!
  [canvas-context canvas-height i spacing num-bars bar-width magnitude]
  (set! (.-fillStyle canvas-context)
        (str "hsl(" (string/join "," [(.round js/Math (/ (* i 360) num-bars)) "100%" "50%"]) ")"))
  (.fillRect canvas-context (* i spacing) canvas-height bar-width (- magnitude)))

(defn get-time-domain-data [analyser-node num-bars]
  (let [Uint8Array (.-Uint8Array js/window)
        freq-bin-count (.-frequencyBinCount analyser-node)]
    {:freq-byte-data (Uint8Array. freq-bin-count)
     :multiplier (/ (.-frequencyBinCount analyser-node) num-bars)}))

(defn draw-bars!
  [canvas-context canvas-width canvas-height spacing num-bars multiplier freq-byte-data bar-width]
  (clear-canvas! canvas-context canvas-width canvas-height "#000000")
  (doseq [i (range num-bars)]
    (let [offset (.floor js/Math (* i multiplier))
          magnitude (/ (reduce #(+ (aget freq-byte-data (+ offset %2)) %1) (range multiplier))
                       multiplier)]
      (draw-line-on-canvas! canvas-context canvas-height i spacing num-bars bar-width magnitude))))

(defn draw-circle! [canvas-context canvas-width canvas-height freq-byte-data]
  (let [max-val (max-of-array freq-byte-data)
        r (* (/ canvas-width 2) (/ max-val 256))
        center-x (/ canvas-width 2)
        center-y center-x]
    (clear-canvas! canvas-context canvas-width canvas-height nil)
    (.beginPath canvas-context)
    (.arc canvas-context center-x center-y r 0 (* 2 (.-PI js/Math)) false)
    (aset canvas-context "fillStyle" "red")
    (.fill canvas-context)))

(defn mic-chart-view [{:keys [analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "mic-chart-view")

    om/IInitState
    (init-state [_]
      {:bars-canvas nil
       :circle-canvas nil
       :bars-canvas-context nil
       :circle-canvas-context nil
       :spacing 3
       :bar-width 1
       :num-bars nil
       :freq-byte-data nil
       :multiplier nil})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [spacing]} (om/get-state owner)
            bars-canvas (om/get-node owner "bars-canvas-ref")
            circle-canvas (om/get-node owner "circle-canvas-ref")
            num-bars (.round js/Math (/ (.-width bars-canvas) spacing))
            {:keys [freq-byte-data multiplier]} (get-time-domain-data analyser-node num-bars)]
        (om/update-state! owner #(assoc %
                                        :bars-canvas bars-canvas
                                        :circle-canvas circle-canvas
                                        :bars-canvas-context (.getContext bars-canvas "2d")
                                        :circle-canvas-context (.getContext circle-canvas "2d")
                                        :num-bars num-bars
                                        :freq-byte-data freq-byte-data
                                        :multiplier multiplier))))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [bars-canvas bars-canvas-context circle-canvas circle-canvas-context
                    num-bars spacing bar-width]} (om/get-state owner)
            {:keys [freq-byte-data multiplier]} (get-time-domain-data analyser-node num-bars)]
        (.getByteFrequencyData analyser-node freq-byte-data)
        (when bars-canvas-context
          (draw-circle! circle-canvas-context (.-width circle-canvas) (.-height circle-canvas) freq-byte-data)
          (draw-bars! bars-canvas-context (.-width bars-canvas) (.-height bars-canvas) spacing
                      num-bars multiplier freq-byte-data bar-width))
        (om/update-state! owner #(assoc % :freq-byte-data freq-byte-data :multiplier multiplier))))

    om/IRender
    (render [_]
      (dom/div nil
        (dom/canvas #js {:width  600
                         :height 150
                         :ref    "bars-canvas-ref"} "no canvas")
        (dom/canvas #js {:width  100
                         :height 100
                         :ref    "circle-canvas-ref"} "no canvas")))))
