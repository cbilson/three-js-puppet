(ns three-js-puppet.util)

(defn log [& args]
  (.log (.-console js/window) (str args)))

(defn on-load [f]
  (.addEventListener js/document "load" f))

(defn $id [id]
  (.getElementById js/document id))

(defn- keyword->event-name [x]
  (.replace (name x) "-" ""))

(defn listen [elem event f & event-fns]
  (.addEventListener elem (keyword->event-name event) f)
  (if (next event-fns)
    (recur elem (first event-fns) (second event-fns) (nnext event-fns))))

(defn timeout [f ms]
  (.setTimeout js/window f ms))

(defn kvargs->obj [& kvargs]
  (let [obj (js-obj)]
    (if (next kvargs)
      (do
        (aset obj (name (first kvargs)) (second kvargs))
        (recur (nnext kvargs)))
      obj)))
