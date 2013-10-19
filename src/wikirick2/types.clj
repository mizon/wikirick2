(ns wikirick2.types)

(defprotocol IService
  (get-repository [self])
  (get-url-mapper [self])
  (get-config [self])
  (get-screen [self]))

(defprotocol IRepository
  (select-article [self title])
  (select-article-by-revision [self title rev])
  (select-all-article-titles [self])
  (post-article [self article]))

(defprotocol IURLMapper
  (index-path [self])
  (article-path [self article])
  (expand-path [self path])
  (css-path [self]))

(defprotocol IScreen
  (render-full [self template])
  (render-fragment [self template]))

(defrecord Template [title body])

(defrecord Article [title source revision edit-comment])

(defn make-article [title source]
  (->Article title source nil nil))
