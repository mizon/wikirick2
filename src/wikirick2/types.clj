(ns wikirick2.types)

(defprotocol IRepository
  (select-page [self title])
  (select-page-by-revision [self title rev])
  (select-all-page-titles [self])
  (post-page [self title source edit-comment])
  (new-page [self title source]))

(defprotocol IURLMapper
  (index-path [self])
  (page-path [self page])
  (theme-path [self])
  (expand-path [self path]))

(defprotocol IScreen
  (render-full [self template])
  (render-fragment [self template]))

(defrecord Template [title body])

(defprotocol IPage
  (save-page [self])
  (diff-with-other-revision [self rev])
  (referring-titles [self])
  (referred-titles [self]))

(defprotocol IPageRelation
  (update-relations [self page])
  (do-referred-titles [self page]))

(defrecord WikiService [config repository url-mapper screen])
