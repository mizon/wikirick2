(ns wikirick2.main
  (:use compojure.core
        wikirick2.handler
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]))

(def main-service (make-wiki-service (load-file "./wikirick-config.clj")))

(defroutes application
  (wrap-with-wiki-service (handler/site wikirick-routes) main-service))
