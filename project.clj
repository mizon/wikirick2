(defproject wikirick2 "0.1.0-SNAPSHOT"
  :description "A wiki software for my site."
  :url "http://mizondev.net/wiki/wikirick2"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [parse-ez "0.3.6"]]
  :plugins [[lein-ring "0.8.5"]
            [speclj "2.5.0"]]
  :ring {:handler wikirick2.handler/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]
                                  [speclj "2.5.0"]]}}
  :test-paths ["spec"])
