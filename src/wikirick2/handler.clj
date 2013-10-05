(ns wikirick2.handler
  (:use compojure.core
        wikirick2.service)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn- handle-route []
  "<h1>Hello World</h1>")

(defroutes app-routes
  (GET "/" [] (handle-route))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
