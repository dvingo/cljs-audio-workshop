(ns cljs-audio-workshop.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [<!]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def wave-width 400)
(def wave-height 100)
(def bpm 120)

(defn recording-duration []
  "Length of a quarter note in milliseconds."
  (* (/ bpm 60) 1000))

(defn recording-duration-sec []
  "Length of a quarter note in seconds"
  (/ bpm 60))

(def note-type->num
  {"Eighth" 8 "Quarter" 4 "Half" 2 "Whole" 1})

(def note-type->width
  {"Eighth"  (/ wave-width 8)
   "Quarter" (/ wave-width 4)
   "Half"    (/ wave-width 2)
   "Whole"   wave-width})

(def note-types (keys note-type->width))

(let [db {:compositions []
          :tracks []
          :track-samples []
          :samples []
          :sounds []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :ui {:buffers-visible true
               :selected-track-id nil
               :selected-track-idx nil
               :is-playing false}}]

  (defonce app-state (atom db)))


(defn sounds []
  (om/ref-cursor (:sounds (om/root-cursor app-state))))

(defn samples []
  (om/ref-cursor (:samples (om/root-cursor app-state))))

(defn tracks []
  (om/ref-cursor (:tracks (om/root-cursor app-state))))

(defn track-samples []
  (om/ref-cursor (:track-samples (om/root-cursor app-state))))

(defn ui []
  (om/ref-cursor (:ui (om/root-cursor app-state))))

(defonce audio-context (js/window.AudioContext.))

(defn play-buffer! [audio-context buffer-data offset duration]
  (let [source (.createBufferSource audio-context)
        buffer (.createBuffer audio-context 1
                              (.-length buffer-data)
                              (.-sampleRate audio-context))
        chan-data (.getChannelData buffer 0)]
    (.set chan-data buffer-data)
    (aset source "buffer" buffer)
    (.log js/console "PLAY!")
    (.connect source (.-destination audio-context))
    (.start source 0 offset duration)))

(defn make-new-sound [audio-buffer sound-name]
  {:id (uuid/make-random)
   :name sound-name
   :audio-buffer audio-buffer
   :current-offset 0
   :current-note-type "Quarter"})

(defn make-new-sample [sound]
  {:id (uuid/make-random)
   :name (:name sound)
   :audio-buffer (:audio-buffer sound)
   :offset (:current-offset sound)
   :type (:current-note-type sound)
   :sound (:id sound)})

(defn save-sound! [app-state sound-name]
  (let [{:keys [audio-recorder analyser-node]} @app-state
        buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffer audio-recorder
      (fn [buffers]
        (om/transact! app-state :sounds
          #(conj % (make-new-sound (aget buffers 0) sound-name)))
        (.clear audio-recorder)))))

(defn handle-toggle-recording [app-state sound-name]
  (let [{:keys [is-recording audio-recorder]} @app-state]
    (if is-recording
      (save-sound! app-state sound-name)
      (.record audio-recorder))
    (om/transact! app-state :is-recording not)))

(defn handle-update-sound-note-type [app-state sound note-type]
  (let [i (last (om/path sound))
        current-selector-width (note-type->width note-type wave-width)
        x-offset (.clamp goog.math (:current-offset sound) 0 (- wave-width current-selector-width))
        new-sound (assoc sound :current-note-type note-type :current-offset x-offset)]
    (om/transact! (sounds) #(assoc % i new-sound))))

(defn handle-update-sound-offset [sound-idx x-offset]
  (let [snds (sounds)
        new-sound (assoc (get snds sound-idx) :current-offset x-offset)]
    (om/transact! snds #(assoc % sound-idx new-sound))))

(defn start-actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
    (match [action-vec]
      [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
      [[:set-sound-note-type sound note-type]]
           (handle-update-sound-note-type app-state sound note-type)
      [[:set-sound-offset sound-index x-offset]]
           (handle-update-sound-offset sound-index x-offset)
      [[:new-sample sound]] (om/transact! (samples) #(conj % (make-new-sample sound)))
      :else (.error js/console "Unknown handler: " (clj->js action-vec)))
    (recur (<! actions-chan))))
