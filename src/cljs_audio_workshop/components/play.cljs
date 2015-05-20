(ns cljs-audio-workshop.components.play
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-audio-workshop.state :refer [composition-duration-sec
                                          track-samples
                                          audio-look-ahead-time-sec
                                          track-width
                                          audio-context
                                          sixteenth-note-length
                                          ui]]
            [cljs-audio-workshop.utils :refer [lin-interp]])
  (:require-macros [cljs-audio-workshop.macros :refer [send!! build-button]]))

(defn schedule-samples [owner t-samples]
  (let [current-time (.-currentTime audio-context)
        track-offset-time (mod current-time composition-duration-sec)
        px-offset->secs (lin-interp 0 track-width 0 composition-duration-sec)
        window-max-time (mod (+ track-offset-time audio-look-ahead-time-sec) composition-duration-sec)
        window-min-time (- window-max-time audio-look-ahead-time-sec)
        track-samples-to-play (filter #(and (>= (px-offset->secs (:offset %)) window-min-time)
                                            (<= (px-offset->secs (:offset %)) window-max-time))
                                      t-samples)]
    (send!! owner :play-track-samples track-samples-to-play)))

(defonce next-note-time (atom 0.0))

(defn playback-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "playback-view")

    om/IInitState (init-state [_]
      {:current-time (.-currentTime audio-context)})

    om/IDidMount (did-mount [_]
      (om/set-state! owner :current-time (.-currentTime audio-context)))

    om/IDidUpdate (did-update [_ _ _]
      (let [current-time (.-currentTime audio-context)
            t-samples (om/observe owner (track-samples))
            ui (om/observe owner (ui))
            playing? (:is-playing ui)]
        (when playing?
          (while (< @next-note-time (+ current-time audio-look-ahead-time-sec))
            (reset! next-note-time (+ @next-note-time sixteenth-note-length))
            (schedule-samples owner t-samples)))
        (om/set-state! owner :current-time (.-currentTime audio-context))))

    om/IRenderState (render-state [_ _]
      (let [current-time (.-currentTime audio-context)
            track-offset-time (.toFixed (mod current-time composition-duration-sec) 2)]
        (dom/span nil track-offset-time)))))

(defn play-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "play-view")
    om/IRender (render [_]
      (let [ui (om/observe owner (ui))
            playing? (:is-playing ui)]
        (dom/div nil
          (build-button "toggle-playback-view" #(send!! owner :toggle-playback)
                        (if playing? "Stop" "Play"))
          (om/build playback-view nil))))))
