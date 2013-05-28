(ns three-js-puppet.engine
  (:require [three-js-puppet.connection :as conn]
            [three-js-puppet.graphics :as gfx]
            [three-js-puppet.util :refer [log on-load listen $id]]
            [clojure.string :as str]))

(def world (atom nil))

(defn move-cube [{cube :cube [prev-x prev-y] :move-start-pos :as old-world} new-x new-y]
  (let [[dx dy] [(/ (- new-x prev-x) 100.0)
                 (/ (- new-y prev-y) 100.0)]
        [x' y' z'] [(+ (.-x (.-position cube)) dx)
                    (.-y (.-position cube))
                    (- (.-z (.-position cube)) dy)]]
    (conn/send (pr-str {:x x' :y y' :z z'}))
    (gfx/move-to cube x' y' z')
    (assoc old-world :move-start-pos [new-x new-y])))

(defn start-moving-cube [x y]
  (swap! world assoc :moving? true :move-start-pos [x y]))

(defn stop-moving-cube [event]
  (.preventDefault event)
  (swap! world assoc :moving? false))

(defn mouse-down [event]
  (.preventDefault event)
  (start-moving-cube (.-clientX event) (.-clientY event)))

(defn touch-start [event]
  (.preventDefault event)
  (let [target (aget (.-targetTouches event) 0)]
    (start-moving-cube (.-pageX target) (.-pageY target))))

(defn mouse-move [event]
  (swap! world
         (fn [{:keys [moving?] :as old-world}]
           (if moving?
             (move-cube old-world (.-clientX event) (.-clientY event))
             old-world))))

(defn touch-move [event]
  (swap! world
         (fn [{:keys [moving?] :as old-world}]
           (if moving?
             (let [target (aget (.-targetTouches event) 0)]
               (move-cube old-world (.-pageX target) (.-pageY target)))
             old-world))))

(defn init-world []
  (let [aspect (/ window/innerWidth window/innerHeight)
        camera (gfx/camera 75 aspect 0.1 1000)
        cube (gfx/mesh (gfx/quadrilateral 1 1 1) (gfx/material :lambert :color 0xD128E8))
        light (gfx/light :point :color 0xFFFFFF)
        scene (gfx/scene light cube)
        renderer (gfx/renderer window/innerWidth window/innerHeight)]
    (gfx/move-to camera 0 0 2)
    (gfx/move-to light 50 50 130)

    ;; put the cube off screen until we get an update from the server
    (gfx/move-to cube 0 0 1e6)

    (listen (.-domElement renderer)
            :mousedown mouse-down
            :mouseup stop-moving-cube
            :mousemove mouse-move
            :touchstart touch-start
            :touchmove touch-move
            :touchend stop-moving-cube)

    {:scene scene
     :camera camera
     :light light
     :cube cube
     :renderer renderer
     :moving? false
     :move-start-pos nil}))

(defn render []
  (let [{:keys [cube renderer scene camera]} @world]
                                        ;(gfx/rotate cube 0.01 0.012 0)
    (.render renderer scene camera)))

(defn show-message [& messages]
  (if-let [message-div ($id "message")]
    (set! (.-innerText message-div) (str messages))))

(defn show-error [& messages]
  (if-let [error-div ($id "error")]
    (set! (.-innerText error-div) (str/join messages))))

(defn format-coordinates [c]
  (let [[x y z] [(.-x c) (.-y c) (.-z c)]]
    (str (.toFixed x 2) ", " (.toFixed y 2) ", " (.toFixed z 2))))

(defn format-status-message []
  (let [{:keys [camera light cube moving?]} @world]
    (str "cube: @" (format-coordinates (.-position cube))
         " rot "(format-coordinates (.-rotation cube))
         "; light: " (format-coordinates (.-position light))
         "; camera: " (format-coordinates (.-position camera))
         "; moving?: " moving?)))

(defn cube-position-received [msg]
  (let [{:keys [x y z]} (reader/read-string msg)
        cube (:cube @world)]
    (gfx/move-to cube x y z)))

(defn cube-position-error [error]
  (log "error: " error))

(defn connection-closed [code reason was-clean?]
  (log "close - code: " code
       ", reason: " reason
       ", was-clean?: " was-clean?))

(defn animation-loop []
  (js/requestAnimationFrame animation-loop)
  (show-message (format-status-message))
  (render))

(defn ^:export init []
  (on-load
   (try
     (reset! world (init-world))
     (conn/connect cube-position-received connection-closed cube-position-error)
     (animation-loop)
     (catch js/Error ex
       (show-error "Failed to initialize: " (.toString ex) (.-stack ex))
       (throw ex)))))
