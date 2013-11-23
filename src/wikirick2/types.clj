(ns wikirick2.types)

(defprotocol IRepository
  (new-page [self title])
  (select-page [self title])
  (select-page-by-revision [self title rev])
  (select-all-pages [self]))

(defprotocol IPage
  (save-page [self])
  (page-source [self])
  (page-revision [self])
  (page-exists? [self])
  (modified-at [self])
  (diff-with-other-revision [self rev])
  (referring-titles [self])
  (referred-titles [self]))

(defprotocol IPageRelation
  (update-relations [self page])
  (do-referred-titles [self page]))

(defprotocol IURLMapper
  (index-path [self])
  (page-path [self page])
  (page-action-path [self page-title action-name])
  (theme-path [self])
  (expand-path [self path]))

(defprotocol IScreen
  (read-view [self page])
  (new-view [self page])
  (edit-view [self page])
  (source-view [self page]))

(defrecord Template [title body])

(defrecord WikiService [config repository url-mapper screen])
