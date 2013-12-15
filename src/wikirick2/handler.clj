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
        (response/redirect (page-action-path url-mapper title "edit"))))))

(defn- open-edit-view [title]
  (with-wiki-service
    (try+
      (let [page (select-page storage title)]
        (edit-view screen page []))
      (catch [:type :page-not-found] _
        (new-view screen (assoc (new-page storage title) :source "new content") [])))))

(defn- open-search-view [{:keys [word]}]
  (with-wiki-service
    (search-view screen word (search-pages storage word))))

(defn- open-history-view [title]
  (with-wiki-service
    (try+
      (let [page (select-page storage title)]
        (history-view screen page))
      (catch [:type :page-not-found] _
        (response/redirect (page-action-path url-mapper title "edit"))))))

(defn- open-diff-view [{title :title revision-range :range}]
  (with-wiki-service
    (if-let [[_ from-rev to-rev] (re-matches #"(\d+)-(\d+)" revision-range)]
      (diff-view screen
                 (select-page storage title)
                 (Integer/parseInt from-rev)
                 (Integer/parseInt to-rev)))))

(defn- deny-direct-post [req title]
  (with-wiki-service
    (let [referer (-> req :headers (get "referer"))
          editor-path (page-action-path url-mapper title "edit")]
      (if (not (and referer (.endsWith referer editor-path)))
        (throw+ {:type :direct-post-denied})))))

(defn- register-page [page]
  (with-wiki-service
    (letfn [(reopen-editor [messages]
              (if (page-exists? page)
                (edit-view screen page messages)
                (new-view screen page messages)))]
      (try+
        (save-page page)
        (response/redirect-after-post (page-path url-mapper (.title page)))
        (catch [:type :empty-source] _
          (reopen-editor ["Source is empty."]))
        (catch [:type :unchanged-source] _
          (reopen-editor ["Source is unchanged."]))))))

(defn- post-page [req]
  (with-wiki-service
    (let [{title :title source :source preview? :preview} (req :params)]
      (deny-direct-post req title)
      (let [page (assoc (new-page storage title) :source source)]
        (if preview?
          (preview-view screen page)
          (register-page page))))))

(defn- catch-known-exceptions [app]
  (fn [req]
    (try+
      (app req)
      (catch [:type :invalid-page-title] _
        ((route/not-found "Not Found") req))
      (catch [:type :direct-post-denied] _
        ((route/not-found "Not Found") req)))))

(def wikirick-routes
  (-> (routes (GET "/" {params :params} (open-read-view (assoc params :title "Front Page")))
              (GET "/w/:title" {params :params} (open-read-view params))
              (GET "/w/:title/edit" [title] (open-edit-view title))
              (POST "/w/:title/edit" req (post-page req))
              (GET "/w/:title/diff/:range" {params :params} (open-diff-view params))
              (GET "/w/:title/history" [title] (open-history-view title))
              (GET "/search" {params :params} (open-search-view params))
              (route/resources "/")
              (route/not-found "Not Found"))
      catch-known-exceptions))
