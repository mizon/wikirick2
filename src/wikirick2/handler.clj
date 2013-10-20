(ns wikirick2.handler
  (:use compojure.core
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [wikirick2.screen :as screen]))

(defn- handle-root []
  "<h1>Hello World</h1>")

(defn- handle-navigation [])

(defn- handle-article [title]
  (let [article (select-article (ws :repository) title)]
    (render-full (ws :screen) (screen/article article))))

(defroutes wikirick-routes
  (GET "/" [] (handle-root))
  (GET "/w/:title" [title] (handle-article title))
  (route/resources "/")
  (route/not-found "Not Found"))
