(defproject three-js-puppet "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.5"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring "1.1.8"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.3"]]
  :main three-js-puppet.core
  :ring {:handler three-js-puppet.core/main-routes}
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
              :builds [{:source-paths ["src-cljs"]
                        :compiler {
                                   :output-to "resources/public/app.js"
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :jar true}}]})
