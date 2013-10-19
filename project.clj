(defproject wikirick2 "0.1.0-SNAPSHOT"
  :description "A wiki software for my site."
  :url "http://mizondev.net/wiki/wikirick2"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [org.clojure/clojurescript "0.0-1934"]
                 [hiccup "1.0.4"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-ring "0.8.5"]
            [speclj "2.5.0"]
            [lein-cljsbuild "0.3.4"]]
  :ring {:handler wikirick2.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [speclj "2.5.0"]]}}
  :test-paths ["spec"]
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/js/index.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
