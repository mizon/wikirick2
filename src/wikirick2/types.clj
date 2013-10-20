(ns wikirick2.types)

(defprotocol IRepository
  (select-article [self title])
  (select-article-by-revision [self title rev])
  (select-all-article-titles [self])
  (post-article [self article]))

(defprotocol IURLMapper
  (index-path [self])
  (article-path [self article])
  (theme-path [self])
  (expand-path [self path]))

(defprotocol IScreen
  (render-full [self template])
  (render-fragment [self template]))

(defrecord Template [title body])

(defrecord Article [title source revision edit-comment])

(defn make-article [title source]
  (->Article title source nil nil))

(defrecord WikiService [config repository url-mapper screen])
