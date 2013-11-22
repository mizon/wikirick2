(ns wikirick2.handler
  (:use compojure.core
        slingshot.slingshot
        wikirick2.service
        wikirick2.types)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [wikirick2.screen :as screen]))

(defn- open-read-view [title]
  (with-wiki-service
    (try+
      (let [page (select-page repository title)]
        (read-view screen page))
      (catch [:type :page-not-found] _
        (response/redirect (page-action-path url-mapper title "new"))))))

(defn- open-new-view [title]
  (with-wiki-service
    (new-view screen (assoc (new-page repository title) :source "new content"))))

(defn- open-edit-view [title]
  (with-wiki-service
    (try+
      (let [page (select-page repository title)]
        (edit-view screen page))
      (catch [:type :page-not-found] _
        (response/redirect (page-action-path url-mapper title "new"))))))

(defn- register-new-page [{:keys [title source]}]
  (with-wiki-service
    (save-page (assoc (new-page repository title) :source source))))

(defn- update-page [{:keys [title source base-ver]}]
  (with-wiki-service
    (let [base-ver- (Integer/parseInt base-ver)
          page (select-page-by-version repository title base-ver-)]
      (save-page (assoc page :source source))
      (response/redirect-after-post (page-path url-mapper title)))))

(defroutes wikirick-routes
  (GET "/" [] (open-read-view "FrontPage"))
  (GET "/w/:title" [title] (open-read-view title))
  (GET "/w/:title/new" [title] (open-new-view title))
  (POST "/w/:title/new" {params :params} (register-new-page params))
  (GET "/w/:title/edit" [title] (open-edit-view title))
  (POST "/w/:title/edit" {params :params} (update-page params))
  (route/resources "/")
  (route/not-found "Not Found"))
