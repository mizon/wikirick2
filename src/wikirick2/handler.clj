(ns wikirick2.handler
  (:use compojure.core
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [wikirick2.screen :as screen]))

(defn- handle-navigation [])

(defn- handle-page [title]
  (let [page (select-page (ws :repository) title)]
    (render-full (ws :screen) (screen/page page))))

(defroutes wikirick-routes
  (GET "/" [] (handle-page "FrontPage"))
  (GET "/w/:title" [title] (handle-page title))
  (route/resources "/")
  (route/not-found "Not Found"))
