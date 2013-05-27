(ns three-js-puppet.server
  (:require [org.httpkit.server :as hk]
            [ring.middleware.reload :as reload]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [three-js-puppet.pages :as pages]
            [clojure.edn :as edn]))

(def cube-channels (agent #{}))
(def cube-position (atom {:x 0.0 :y 0.0 :z 0.0}))

(defn notify-other-channels [channels source-channel]
  (let [serialized-cube-position (pr-str @cube-position)]
    (doseq [ch (disj channels source-channel)]
      (hk/send! ch {:status 200
                    :headers {"Content-type" "text/edn"}
                    :body serialized-cube-position})))
  channels)

(defn cube-position-channel-closed [ch status]
  (println "channel closed:" status)
  (send cube-channels disj ch))

(defn receive-cube-position [ch data]
  (println "data: " data)
  (let [new-pos (edn/read-string data)]
    (reset! cube-position new-pos)
    (send cube-channels notify-other-channels ch)))

(defn cube-position-handler [request]
  (hk/with-channel request channel
    (println "connected")
    (send cube-channels conj channel)
    (hk/on-close channel (partial cube-position-channel-closed channel))
    (hk/on-receive channel (partial receive-cube-position channel))))

(defroutes all-routes
  (GET "/" [] (pages/index-page))
  (GET "/cube-position" [] cube-position-handler)
  (route/resources "/")
  (route/not-found "Not found"))

(def stop-fn (atom nil))

(defn start []
  (let [server (hk/run-server (reload/wrap-reload (site #'all-routes)) {:port 5000})]
    (reset! stop-fn server)))

(defn stop []
  (swap! stop-fn (fn [server]
                   (server)
                   nil)))
