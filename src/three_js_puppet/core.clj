(ns three-js-puppet.core
  (:require [org.httpkit.server :as hk]
            [compojure.handler :as compojure]
            [three-js-puppet.server :as server])
  (:gen-class :main true))

(defn -main [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "5000"))]
    (hk/run-server (compojure/site server/all-routes) {:port port})))
