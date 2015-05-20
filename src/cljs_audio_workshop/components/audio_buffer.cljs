(ns cljs-audio-workshop.components.audio-buffer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [cljs-audio-workshop.components.wave-selector :refer [wave-selector-view]]
            [cljs-audio-workshop.state :refer [audio-context wave-width wave-height
                                          note-type->width note-types bpm
                                          recording-duration-sec play-buffer!]]
            [cljs-audio-workshop.utils :refer [min-arr-val max-arr-val lin-interp]]))

(defn draw-buffer! [width height canvas-context data]
  (let [step (.ceil js/Math (/ (.-length data) width))
        amp (/ height 2)
        min-val (min-arr-val data)
        max-val (max-arr-val data)
        scale (lin-interp min-val max-val (- amp) amp)]
    (aset canvas-context "fillStyle" "silver")
    (.clearRect canvas-context 0 0 width height)
    (doseq [i (range width)]
      (doseq [j (range step)]
        (let [datum (.min js/Math (aget data (+ (* i step) j)) 1)
              height (scale datum)]
          (.fillRect canvas-context i amp 1 height))))))

(defn note-type-view [sound owner]
  (reify
    om/IDisplayName (display-name [_] "note-type-view")
    om/IRender
    (render [_]
      (apply dom/select #js
        {:onChange #(put! (:action-chan (om/get-shared owner))
                         [:set-sound-note-type sound (.. % -target -value)])
         :value (:current-note-type sound)}
        (map #(dom/option #js {:value %} %) note-types)))))

(defn audio-buffer-view [sound owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffer-view")
    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :current-note-type "Quarter"})
    om/IDidMount
    (did-mount [_]
      (let [sound-name (:name sound)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! wave-width wave-height canvas-context (:audio-buffer sound))))

    om/IRenderState
    (render-state [_ {:keys [current-note-type]}]
      (let [audio-buffer (:audio-buffer sound)
            selector-width (get note-type->width (:current-note-type sound))
            selector-offset (:current-offset sound)
            recording-length (recording-duration-sec)
            play-offset ((lin-interp 0 wave-width 0 recording-length) selector-offset)
            play-duration (* (/ selector-width wave-width) recording-length)]

      (dom/div #js {:style #js {:position "relative"}}

        (dom/div nil
          (dom/h3 #js {:style #js {:display "inline-block" :marginRight "1em"}} (:name sound))
          (om/build note-type-view sound))

        (dom/canvas #js {:width  wave-width
                         :height wave-height
                         :ref    "canvas-ref"}
                    "no canvas")

        (om/build wave-selector-view sound
          {:state {:x-offset selector-offset
                   :canvas-width selector-width
                   :max-width wave-width}})
        (dom/div nil

          (dom/button #js {:onClick #(put! (:action-chan (om/get-shared owner))
                   [:new-sample sound])} "Make Sample")

          (dom/button #js {:onClick
                           #(play-buffer! audio-context audio-buffer play-offset play-duration)}
                      "Play")))))))
