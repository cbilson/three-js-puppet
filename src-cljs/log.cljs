(ns three-js-puppet.log)

(defn ^export log [& args]
  (.log (.-console js/window) (str args)))
