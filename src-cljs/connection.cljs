(ns three-js-puppet.connection
  (:require [three-js-puppet.util :refer [log]]))

(defn ^:export make-socket [uri on-message on-close on-error]
  (let [socket
        (if (nil? (.-MozWebSocket js/window))
          (js/WebSocket. uri)
          (js/MozWebSocket. uri))]
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
