(ns wikirick2.main
  (:use compojure.core
        wikirick2.handler
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]))

(def main-service (->WikiService (load-file "./wikirick-config.clj")))

(defroutes application
  (wrap-with-service (handler/site wikirick-routes) main-service))
