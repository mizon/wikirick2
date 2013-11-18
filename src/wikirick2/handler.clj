(ns wikirick2.handler
  (:use compojure.core
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [wikirick2.screen :as screen]))

(defn- open-read-view [title]
  (with-wiki-service
    (let [page (select-page repository title)]
      (read-view screen page))))

(defn- open-edit-view [title]
  (with-wiki-service
    (let [page (select-page repository title)]
      (edit-view screen page))))

(defroutes wikirick-routes
  (GET "/" [] (open-read-view "FrontPage"))
  (GET "/w/:title" [title] (open-read-view title))
  (GET "/w/:title/edit" [title] (open-edit-view title))
  (route/resources "/")
  (route/not-found "Not Found"))
