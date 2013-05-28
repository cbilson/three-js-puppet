(ns three-js-puppet.graphics
  (:require [three-js-puppet.util :refer [kvargs->obj on-load]]
            [three-js-puppet.polyfill :as poly]))

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

(defn camera [fov aspect near far]
  (THREE.PerspectiveCamera. fov aspect near far))

(defn quadrilateral [width height depth]
  (THREE.CubeGeometry. width height depth))

(defmulti material (fn [type & _] type))

(defmethod material :lambert [_ & kvprops]
  (THREE.MeshLambertMaterial. (kvargs->obj kvprops)))

(defn mesh [geometry material]
  (THREE.Mesh. geometry material))

(defmulti light (fn [type & _] type))

(defmethod light :point [& kvargs]
  (let [options (apply hash-map kvargs)]
    (THREE.PointLight. (:color options)
                       (:intensity options)
                       (:distance options))))

(defn scene [& objects]
  (let [s (THREE.Scene.)]
    (doseq [o objects]
      (.add s o))
    s))

(defn renderer [width height]
  (let [r (poly/make-renderer)]
    (.setSize r width height)
    (on-load (fn []
               (.appendChild (.-body js/document) (.-domElement renderer))))
    r))
