(ns three-js-puppet.engine
  (:require [cljs.reader :as reader]
            [three-js-puppet.polyfill :as polyfill]
            [three-js-puppet.web-socket :as ws]
            [three-js-puppet.log :refer [log]]))

(def THREE js/THREE)

(defn set-pos [thing x y z]
  (let [pos (.-position thing)]
    (set! (.-x pos) x)
    (set! (.-y pos) y)
    (set! (.-z pos) z)))

(defn move [thing x y z]
  (let [pos (.-position thing)]
    (set-pos thing
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

(defn move-cube [event]
  (swap! world
         (fn [old]
           (let [{moving? :moving? cube :cube [prev-mouse-x prev-mouse-y] :mouse-down-pos} old]
             (if moving?
               (let [[mouse-x mouse-y] [(.-clientX event) (.-clientY event)]
                     [dx dy] [(/ (- mouse-x prev-mouse-x) 100.0)
                              (/ (- mouse-y prev-mouse-y) 100.0)]
                     [x' y' z'] [(+ (.-x (.-position cube)) dx)
                                 (.-y (.-position cube))
                                 (- (.-z (.-position cube)) dy)]]
                 (.preventDefault event)
                 (.send @cube-position-socket (pr-str {:x x' :y y' :z z'}))
                 (set-pos cube x' y' z')
                 (assoc old :mouse-down-pos [mouse-x mouse-y]))
               old)))))

(defn start-moving-cube [event]
  (.preventDefault event)
  (swap! world assoc
         :moving? true
         :mouse-down-pos [(.-clientX event) (.-clientY event)]))

(defn stop-moving-cube [event]
  (.preventDefault event)
  (swap! world assoc :moving? false))

(defn init-world []
  (let [aspect (/ window/innerWidth window/innerHeight)
        camera (THREE.PerspectiveCamera. 75 aspect 0.1 1000)
        geometry (THREE.CubeGeometry. 1 1 1)
        material (THREE.MeshLambertMaterial. (js-obj "color" 0xD128E8))
        cube (THREE.Mesh. geometry material)
        light (THREE.PointLight. 0xFFFFFF)
        scene (THREE.Scene.)
        renderer (THREE.WebGLRenderer.)]
    (set-pos camera 0 0 2)
    (set-pos light 50 50 130)
    (.add scene light)
    (.add scene cube)
    (.setSize renderer window/innerWidth window/innerHeight)
    (let [renderer-element (.-domElement renderer)]
      (.appendChild (.-body js/document) renderer-element)
      (.addEventListener renderer-element "mousedown" start-moving-cube)
      (.addEventListener renderer-element "mouseup" stop-moving-cube)
      (.addEventListener renderer-element "mousemove" move-cube))

    {:scene scene
     :camera camera
     :light light
     :cube cube
     :renderer renderer
     :moving? false
     :mouse-down-pos nil}))

(defn render []
  (let [{:keys [cube renderer scene camera]} @world]
    ;(rotate cube 0.01 0.012 0)
    (.render renderer scene camera)))

(defn show-message [message]
  (if-let [message-div (.getElementById js/document "message")]
    (set! (.-innerText message-div) message)))

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

(defn animation-loop []
  (js/requestAnimationFrame animation-loop)
  (show-message (format-status-message))
  (render))

(defn ^:export init []
  (polyfill/animation-frame)
  (reset! world (init-world))
  (let [loc (.-location js/window)
        ws-base-path (str "ws://" (.-host loc))
        cube-position-uri (str ws-base-path "/cube-position")]
    (reset!  cube-position-socket (ws/make-socket cube-position-uri
                                                  (fn [msg]
                                                    (let [{:keys [x y z]} (reader/read-string msg)
                                                          cube (:cube @world)]
                                                      (set-pos cube x y z)))
                                                  (fn [error]
                                                    (log "error: " error))
                                                  (fn [code reason was-clean?]
                                                    (log "close - code: " code
                                                         ", reason: " reason
                                                         ", was-clean?: " was-clean?)))))
  (animation-loop))
