(ns wikirick2.types)

(defprotocol IRepository
  (select-page [self title])
  (select-page-by-revision [self title rev])
  (select-all-page-titles [self])
  (post-page [self page]))

(defprotocol IURLMapper
  (index-path [self])
  (page-path [self page])
  (theme-path [self])
  (expand-path [self path]))

(defprotocol IScreen
  (render-full [self template])
  (render-fragment [self template]))

(defrecord Template [title body])

(defrecord Page [title source revision edit-comment])

(defn make-page [title source]
  (->Page title source nil nil))

(defrecord WikiService [config repository url-mapper screen])
