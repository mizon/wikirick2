(ns wikirick2.handler
  (:use compojure.core
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [wikirick2.screen :as screen]))

(def ^:dynamic wiki-service nil)

(defn wrap-with-service [app service]
  (fn [req]
    (binding [wiki-service service]
      (app req))))

(defn- service [getter]
  (getter wiki-service))

(defn- handle-route []
  "<h1>Hello World</h1>")

(defn- handle-navigation [])

(defn- handle-article [title]
  (let [article (select-article (service get-repository) title)]
    (render-full (service get-screen) (screen/article article))))

(defroutes wikirick-routes
  (GET "/" [] (handle-route))
  (GET "/:title" [title] (handle-article title))
  (route/resources "/")
  (route/not-found "Not Found"))
