(ns ^:figwheel-always cljs-audio-workshop.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(defn set-prop-if-undefined! [prop obj options]
  (when-not (aget obj prop)
    (let [opts (map #(aget obj %) options)
          prop-to-use (first (filter #(not (nil? %)) opts))]
      (aset obj prop prop-to-use))))

(set-prop-if-undefined! "AudioContext" js/window ["AudioContext" "webkitAudioContext" "mozAudioContext"])
(set-prop-if-undefined! "getUserMedia" js/navigator ["webkitGetUserMedia" "mozGetUserMedia"])

(defonce app-state (atom {:text "Hello there" :analyzer-node nil}))

;; {:audio-recorder #<[object Object]>
;;    :analyser-node #<[object AnalyserNode]>
;;    :compositions []
;;    :tracks [{:id #uuid "30bed551-f857-4fad-aab7-9bd083dcee36"
;;              :name "new track"
;;              :track-samples [#uuid "78b49f91-12bb-495e-97da-0ba771649227"
;;                              #uuid "8ddd7523-c3fb-4137-a667-1b7428d6fde2"
;;                              #uuid "687e4a9d-4da7-41f6-a854-7801dfac6fd5"
;;                              #uuid "98e85787-b161-4fae-972c-99332632e02c"]}]
;;    :track-samples [{:id #uuid "78b49f91-12bb-495e-97da-0ba771649227"
;;                     :sample #uuid "27761110-bb7b-449c-b21d-96fcffc5c502"
;;                     :is-playing false
;;                     :offset 0}
;;                    {:id #uuid "8ddd7523-c3fb-4137-a667-1b7428d6fde2"
;;                     :sample #uuid "27761110-bb7b-449c-b21d-96fcffc5c502"
;;                     :is-playing false
;;                     :offset 0}
;;                    {:id #uuid "687e4a9d-4da7-41f6-a854-7801dfac6fd5"
;;                     :sample #uuid "27761110-bb7b-449c-b21d-96fcffc5c502"
;;                     :is-playing false
;;                     :offset 0}
;;                    {:id #uuid "98e85787-b161-4fae-972c-99332632e02c"
;;                     :sample #uuid "27761110-bb7b-449c-b21d-96fcffc5c502"
;;                     :is-playing false
;;                     :offset 0}]
;;    :samples [{:id #uuid "27761110-bb7b-449c-b21d-96fcffc5c502"
;;               :name "Name..."
;;               :audio-buffer #<[object Float32Array]>
;;               :offset 89
;;               :type "Quarter"
;;               :sound #uuid "6b6d3655-e0d4-436d-a0b2-4eeefa5f7e5f"}]
;;    :sounds [{:id #uuid "6b6d3655-e0d4-436d-a0b2-4eeefa5f7e5f"
;;              :name "Name..."
;;              :audio-buffer #<[object Float32Array]>
;;              :current-offset 89
;;              :current-note-type "Quarter"}
;;             {:id #uuid "f44f8969-820f-473c-b1c2-3da20f05dd59"
;;              :name "Name..."
;;              :audio-buffer #<[object Float32Array]>
;;              :current-offset 257
;;              :current-note-type "Quarter"}]
;;    :ui {:buffers-visible true
;;         :selected-track-id #uuid "30bed551-f857-4fad-aab7-9bd083dcee36"
;;         :selected-track-idx 0
;;         :is-playing false}
;;    :is-recording false}

(defonce audio-context (js/window.AudioContext.))
(defonce got-audio? (atom false))

(defn got-stream [stream]
  (reset! got-audio? true)
  (let [audio-input (.createMediaStreamSource audio-context stream)
        analyser-node (.createAnalyser audio-context)]
    (set! (.-fftSize analyser-node) 2048)
    (.connect audio-input analyser-node)
    (swap! app-state assoc :analyser-node analyser-node)
    (om/root
      (fn [data owner]
        (reify om/IRender
          (render [_] (dom/h1 nil (:text data)))))
      app-state
      {:target (. js/document (getElementById "app"))})))

(when-not @got-audio?
  (let [audio-constraints (clj->js {"audio" {"mandatory" {"googEchoCancellation" "false"
                                                          "googAutoGainControl"  "false"
                                                          "googNoiseSuppression" "false"
                                                          "googHighpassFilter"   "false"}
                                               "optional" []}})]
    (.getUserMedia js/navigator audio-constraints
                   got-stream
                   #(.log js/console "ERROR getting user media"))))
