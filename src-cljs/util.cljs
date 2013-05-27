(ns three-js-puppet.util)

(defn log [& args]
  (.log (.-console js/window) (str args)))

(defn on-load [f]
  (.addEventListener js/document "load" f))

(defn listen [elem event f & event-fns]
  (.addEventListener elem (name event) f)
  (if (next event-fns)
    (recur elem (first event-fns) (second event-fns) (nnext event-fns))))
