(ns wikirick2.types)

(defprotocol IRepository
  (new-page [self title source])
  (select-page [self title])
  (select-page-by-version [self title ver])
  (select-all-pages [self]))

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

(defprotocol IPage
  (save-page [self])
  (page-source [self])
  (page-version [self])
  (diff-with-other-version [self ver])
  (referring-titles [self])
  (referred-titles [self]))

(defprotocol IPageRelation
  (update-relations [self page])
  (do-referred-titles [self page]))

(defrecord WikiService [config repository url-mapper screen])
