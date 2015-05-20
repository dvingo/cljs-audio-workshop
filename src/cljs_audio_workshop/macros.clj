(ns cljs-audio-workshop.macros)

(defmacro send!! [owner action & data]
  `(cljs.core.async/put! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))

(defmacro send! [owner action & data]
  `(cljs.core.async/>! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))

(defmacro build-button [disp-name on-click label]
  `(om.core/build
     (fn [_# owner#]
       (reify
         om.core/IDisplayName
         (~'display-name [_#] ~disp-name)
         om.core/IRender
         (~'render [_#]
           (om.dom/button (cljs.core/js-obj "onClick" ~on-click) ~label))))
     nil))
