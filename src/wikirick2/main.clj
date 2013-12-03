(ns wikirick2.main
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [wikirick2.handler :refer :all]
            [wikirick2.service :refer :all]
            [wikirick2.types :refer :all]))

(def main-service (make-wiki-service (load-file "./wikirick-config.clj")))

(defroutes application
  (wrap-with-wiki-service (handler/site wikirick-routes) main-service))
