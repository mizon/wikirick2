(ns wikirick2.main
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as jetty]
            [wikirick2.handler :refer :all]
            [wikirick2.service :refer :all]
            [wikirick2.types :refer :all]))

(def wikirick-config (load-file "./wikirick-config.clj"))

(def main-service (make-wiki-service wikirick-config))

(defroutes application
  (wrap-with-wiki-service (handler/site wikirick-routes) main-service))

(defn -main []
  (jetty/run-jetty application {:port (wikirick-config :production-port)}))
