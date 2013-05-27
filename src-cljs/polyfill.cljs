(ns three-js-puppet.polyfill)

(def last-time (atom 0))

(defn fallback-request-animation-frame [callback element]
  (let [current-time (.. (js/Date.) getTime)
        time-to-call (Math/max 0 (- 16 current-time @last-time))
        id (.setTimeout js/window
                        (fn []
                          (callback (+ current-time time-to-call))))]
    (reset! last-time (+ current-time time-to-call))
    id))

(defn fallback-cancel-animation-frame [id]
  (window/clearTimeout id))

(defn polyfill-fn [obj candidate-names new-name fallback-impl]
  (let [existing-fn (->> candidate-names
                         (map #(aget obj %))
                         (some #(when % %)))]
    (aset obj new-name (or existing-fn fallback-impl))))

(defn animation-frame []
  (let [vendors #{"ms" "moz" "webkit" "o"}
        candidate-request-fns (map #(str % "RequestAnimationFrame") vendors)
        candidate-cancel-fns (concat (map #(str % "CancelAnimationFrame") vendors)
                                     (map #(str % "CancelRequestAnimationFrame") vendors))]
    (polyfill-fn js/window candidate-request-fns "requestAnimationFrame"
                  fallback-request-animation-frame)
    (polyfill-fn js/window candidate-request-fns "cancelAnimationFrame"
                  fallback-cancel-animation-frame)))

(defn make-renderer []
  (if (.-WebGLRenderingContext js/window)
    (THREE.WebGLRenderer.)
    (THREE.CanvasRenderer.)))
