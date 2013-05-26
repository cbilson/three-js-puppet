(ns three-js-puppet.pages
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn index-page []
  (html5
   [:head
    (include-css "app.css")]
   [:body
    [:div#message "ohai!"]
    (include-js "three.js"
                "app.js")]))
