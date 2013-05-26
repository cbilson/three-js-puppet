(ns three-js-puppet.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [three-js-puppet.pages :as pages]))

(defroutes main-routes
  (GET "/" [] (pages/index-page))
  (route/resources "/")
  (route/not-found "Not found"))

(def app (handler/site main-routes))

(defonce server (atom nil))
(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "5000"))]
    (jetty/run-jetty main-routes {:port port})))


;;; for use in the repl
(defn start []
  (reset! server (jetty/run-jetty app {:port 5000 :join? false})))

(defn stop []
  (swap! server (fn [old]
                  (when old
                    (.stop old))
                  nil)))
