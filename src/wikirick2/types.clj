(ns wikirick2.types)

(defprotocol IRepository
  (select-page [self title])
  (select-page-by-revision [self title rev])
  (select-all-page-titles [self])
  (new-page [self title source]))

(defprotocol IURLMapper
  (index-path [self])
  (page-path [self page])
  (page-action-path [self page-title action-name])
  (theme-path [self])
  (expand-path [self path]))

(defprotocol IScreen
  (read-view [self page])
  (edit-view [self page])
  (source-view [self page]))

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
