(ns three-js-puppet.connection
  (:require [three-js-puppet.util :refer [log listen timeout kvargs->obj]]
            [cljs.reader :as reader]
            [goog.net.XhrIo :as xhr]
            [goog.net.WebSocket :as ws]
            [clojure.string :as str]))

(defprotocol Connection
  (send-message [this data]))

(def connection (atom nil))

(defrecord LongPollConnection [uri on-message on-error])

(defn- long-poll-send-connection-handler [event]
  (let [target (.-target event)]
    (when (not (.isSuccess target))
      ((:on-error @connection) (.getLastError target)))))

(defn- long-poll-connection-error [event]
  ((:on-error @connection) event))

(extend-type LongPollConnection
  Connection
  (send-message [this message]
    (.send this (:post-uri this)
           long-poll-send-connection-handler
           "POST" message
           (kvargs->obj :content-type "text/edn")
           60000)))

(if (not (nil? (aget js/window "WebSocket")))
  (extend-type js/WebSocket
    Connection
    (send-message [this message]
      (.send this message))))

(if (not (nil? (aget js/window "MozWebSocket")))
  (extend-type js/MozWebSocket
    Connection
    (send-message [this message]
      (.send this message))))

(defn- create-long-poll-connection [uri on-message on-close on-error]
  (let [req (goog.net.XhrIo.)
        uri (str (.. js/window -location toString) "/cube-position")]
    (listen req :on-success long-poll-send-connection-handler)
    (listen req :on-error long-poll-connection-error)
    (LongPollConnection. uri on-message on-error on-close)))

(defn- long-poll-connection-success [fake-socket response]
  (if-let [onmessage (.-onmessage fake-socket)]
    (onmessage (.getResponseText response)))
  (timeout create-long-poll-connection 500))

(defn- create-web-socket-connection [impl uri on-message on-close on-error]
  (let [uri (str (str/replace (.. js/window -location toString) #"^http" "ws") "/cube-position")
        socket (impl. uri)]
    (set! (.-onopen socket)
          (fn []
            (log "opened...")))
    (set! (.-onmessage socket)
          (fn [event]
            (log "message: " (.-data event))
            (on-message (.-data event))))
    (set! (.-onerror socket)
          (fn [event]
            (log "error: " event)
            (on-error event)))
    (set! (.-onclose socket)
          (fn [event]
            (log "close: " event)
            (on-close (.-code event) (.-reason event)
                      (.-wasClean event))))
    socket))

(defn connect [uri on-message on-close on-error]
  (reset! connection
          (cond
           (.-WebSocket js/window)
           (create-web-socket-connection js/WebSocket uri on-message on-close on-error)

           (.-MozWebSocket js/window)
           (create-web-socket-connection js/MozWebSocket uri on-message on-close on-error)

           :otherwise
           (create-long-poll-connection uri on-message on-close on-error))))

(defn send [data]
  (send-message @connection (pr-str data)))
