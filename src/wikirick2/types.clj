(ns wikirick2.types)

(defprotocol IPageStorage
  (new-page [self title])
  (select-page [self title])
  (select-page-by-revision [self title rev])
  (select-all-pages [self])
  (select-recent-pages [self n-pages])
  (search-pages [self word]))

(defprotocol IPage
  (save-page [self])
  (page-source [self])
  (page-revision [self])
  (latest-revision [self])
  (latest-revision? [self])
  (page-exists? [self])
  (page-history [self])
  (modified-at [self])
  (diff-from-previous-revision [self])
  (diff-from-latest-revision [self])
  (referring-titles [self])
  (referred-titles [self]))

(defprotocol IPageRelation
  (update-relations [self page])
  (do-referred-titles [self page]))

(defprotocol IURLMapper
  (index-path [self])
  (page-path [self page])
  (page-revision-path [self title revision])
  (page-diff-path [self title src-rev dest-rev])
  (page-action-path [self page-title action-name])
  (theme-path [self])
  (search-path [self])
  (expand-path [self path]))

(defprotocol IScreen
  (read-view [self page])
  (new-view [self page])
  (source-view [self page])
  (edit-view [self page])
  (history-view [self page])
  (search-view [self word result]))

(defrecord Template [title body])

(defrecord WikiService [config storage url-mapper screen])
