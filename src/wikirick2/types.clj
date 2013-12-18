(ns wikirick2.types)

(defprotocol IPageStorage
  (new-page [self title])
  (select-page [self title])
  (select-all-pages [self])
  (select-recent-pages [self n-pages])
  (search-pages [self word])
  (has-page? [self title]))

(defprotocol IPage
  (save-page [self base-revision])
  (page-source [self revision])
  (latest-revision [self])
  (latest-revision? [self revision])
  (new-page? [self])
  (page-history [self])
  (modified-at [self revision])
  (diff-revisions [self from-rev to-rev])
  (referring-titles [self])
  (referred-titles [self])
  (orphan-page? [self])
  (remove-page [self]))

(defprotocol IURLMapper
  (index-path [self])
  (page-path [self page])
  (page-revision-path [self title revision])
  (page-diff-path [self title src-rev dest-rev])
  (diff-from-previous-path [self page-title revision])
  (diff-from-next-path [self page-title revision])
  (page-action-path [self page-title action-name])
  (theme-path [self])
  (search-path [self]))

(defprotocol IScreen
  (read-view [self page revision])
  (new-view [self page errors])
  (source-view [self page])
  (edit-view [self page errors base-rev])
  (preview-view [self page base-rev])
  (diff-view [self page from-rev to-rev])
  (history-view [self page])
  (search-view [self word result]))

(defrecord WikiService [config storage url-mapper screen])
