(defproject wikirick2 "0.1.0-SNAPSHOT"
  :description "A wiki software for my site."
  :url "http://mizondev.net/wiki/wikirick2"
  :license {:name "BSD 3-Clause License"
            :url "http://opensource.org/licenses/BSD-3-Clause"
            :distribution :repo}
  :dependencies [[clj-time "0.6.0"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]
                 [org.clojars.runa/conjure "2.1.3"]
                 [org.clojure/algo.monads "0.1.4"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1934"]
                 [org.clojure/core.match "0.2.0"]
                 [org.clojure/java.jdbc "0.3.0-alpha5"]
                 [org.van-clj/zetta-parser "0.0.4"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-ring "0.8.5"]
            [speclj "2.5.0"]
            [lein-cljsbuild "0.3.4"]]
  :main wikirick2.main
  :aot [wikirick2.main]
  :ring {:handler wikirick2.main/application}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :test-paths ["test"]
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/js/index.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
