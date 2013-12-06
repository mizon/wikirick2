(ns wikirick2.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [slingshot.slingshot :refer :all]
            [wikirick2.screen :as screen]
            [wikirick2.service :refer :all]
            [wikirick2.types :refer :all]))

(defn- open-read-view [{title :title revision :rev}]
  (with-wiki-service
    (try+
      (let [page (select-page storage title)]
        (read-view screen page (when revision (Integer/parseInt revision))))
      (catch [:type :page-not-found] _
        (response/redirect (page-action-path url-mapper title "new"))))))

(defn- open-new-view [title]
  (with-wiki-service
    (new-view screen (assoc (new-page storage title) :source "new content"))))

(defn- open-edit-view [title]
  (with-wiki-service
    (try+
      (let [page (select-page storage title)]
        (edit-view screen page))
      (catch [:type :page-not-found] _
        (response/redirect (page-action-path url-mapper title "new"))))))

(defn- open-search-view [{:keys [word]}]
  (with-wiki-service
    (search-view screen word (search-pages storage word))))

(defn- open-history-view [title]
  (with-wiki-service
    (try+
      (let [page (select-page storage title)]
        (history-view screen page))
      (catch [:type :page-not-found] _
        (response/redirect (page-action-path url-mapper title "new"))))))

(defn- open-previous-diff-view [{title :title revision :rev}]
  (with-wiki-service
    ))

(defn- open-latest-diff-view [{title :title revision :rev}]
  (with-wiki-service
    ))

(defn- register-new-page [{:keys [title source]}]
  (with-wiki-service
    (save-page (assoc (new-page storage title) :source source))))

(defn- update-page [{:keys [title source base-rev]}]
  (with-wiki-service
    (let [base-rev- (Integer/parseInt base-rev)
          page (select-page-by-revision storage title base-rev-)]
      (save-page (assoc page :source source))
      (response/redirect-after-post (page-path url-mapper title)))))

(defroutes wikirick-routes
  (GET "/" {params :params} (open-read-view (assoc params :title "FrontPage")))
  (GET "/w/:title" {params :params} (open-read-view params))
  (GET "/w/:title/new" [title] (open-new-view title))
  (POST "/w/:title/new" {params :params} (register-new-page params))
  (GET "/w/:title/edit" [title] (open-edit-view title))
  (POST "/w/:title/edit" {params :params} (update-page params))
  (GET "/w/:title/history" [title] (open-history-view title))
  (GET "/search" {params :params} (open-search-view params))
  (route/resources "/")
  (route/not-found "Not Found"))
