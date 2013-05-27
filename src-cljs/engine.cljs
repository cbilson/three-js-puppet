(ns three-js-puppet.engine
  (:require [cljs.reader :as reader]
            [three-js-puppet.polyfill :as polyfill]
            [three-js-puppet.connection :as conn]
            [three-js-puppet.util :refer [log on-load listen]]))

(def THREE js/THREE)

(defn move-to [thing x y z]
  (let [pos (.-position thing)]
    (set! (.-x pos) x)
    (set! (.-y pos) y)
    (set! (.-z pos) z)))

(defn move [thing x y z]
  (let [pos (.-position thing)]
    (move-to thing
             (+ x (.-x thing))
             (+ y (.-y thing))
             (+ z (.-z thing)))))

(defn mod-2-pi [x]
  (mod x (* 2 Math/PI)))

(defn rotate [obj x y z]
  (let [rotation (.-rotation obj)]
    (set! (.-x rotation) (mod-2-pi (+ (.-x rotation) x)))
    (set! (.-y rotation) (mod-2-pi (+ (.-y rotation) y)))
    (set! (.-z rotation) (mod-2-pi (+ (.-z rotation) z)))))

(def world (atom nil))
(def cube-position-socket (atom nil))

(defn move-cube [{cube :cube [prev-x prev-y] :move-start-pos :as old-world} new-x new-y]
  (let [[dx dy] [(/ (- new-x prev-x) 100.0)
                 (/ (- new-y prev-y) 100.0)]
        [x' y' z'] [(+ (.-x (.-position cube)) dx)
                    (.-y (.-position cube))
                    (- (.-z (.-position cube)) dy)]]
    (.send @cube-position-socket (pr-str {:x x' :y y' :z z'}))
    (move-to cube x' y' z')
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
        camera (THREE.PerspectiveCamera. 75 aspect 0.1 1000)
        geometry (THREE.CubeGeometry. 1 1 1)
        material (THREE.MeshLambertMaterial. (js-obj "color" 0xD128E8))
        cube (THREE.Mesh. geometry material)
        light (THREE.PointLight. 0xFFFFFF)
        scene (THREE.Scene.)
        renderer (polyfill/make-renderer)]
    (move-to camera 0 0 2)
    (move-to light 50 50 130)

    ;; put the cube off screen until we get an update from the server
    (move-to cube 0 0 1e6)

    (.add scene light)
    (.add scene cube)
    (.setSize renderer window/innerWidth window/innerHeight)
    (let [renderer-element (.-domElement renderer)]
      (.appendChild (.-body js/document) renderer-element)
      (listen renderer-element
              :mousedown mouse-down
              :mouseup stop-moving-cube
              :mousemove mouse-move
              :touchstart touch-start
              :touchmove touch-move
              :touchend stop-moving-cube))

    {:scene scene
     :camera camera
     :light light
     :cube cube
     :renderer renderer
     :moving? false
     :move-start-pos nil}))

(defn render []
  (let [{:keys [cube renderer scene camera]} @world]
                                        ;(rotate cube 0.01 0.012 0)
    (.render renderer scene camera)))

(defn show-message [& messages]
  (if-let [message-div (.getElementById js/document "message")]
    (set! (.-innerText message-div) (str messages))))

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
    (move-to cube x y z)))

(defn cube-position-error [error]
  (log "error: " error))

(defn connection-closed [code reason was-clean?]
  (log "close - code: " code
       ", reason: " reason
       ", was-clean?: " was-clean?))

(defn make-cube-position-connection []
  (let [loc (.-location js/window)
        ws-base-path (str "ws://" (.-host loc))
        cube-position-uri (str ws-base-path "/cube-position")]
    (reset!  cube-position-socket
             (conn/make-connection cube-position-uri
                                   cube-position-received
                                   cube-position-error
                                   connection-closed))))

(defn animation-loop []
  (js/requestAnimationFrame animation-loop)
  (show-message (format-status-message))
  (render))

(defn ^:export init []
  (reset! world (init-world))
  (on-load
   (try
     (make-cube-position-connection)
     (catch js/Error ex
       (show-message "Failed to initialize: " (.toString ex) (.-stack ex)))))
  (animation-loop))
