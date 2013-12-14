(ns wikirick2.page-storage
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.ddl :as ddl]
            [clojure.java.jdbc.sql :as sql]
            [clojure.string :as string]
            [slingshot.slingshot :refer :all]
            [wikirick2.helper.page-storage :refer :all]
            [wikirick2.shell :as shell]
            [wikirick2.types :refer :all]
            [wikirick2.wiki-parser :as wiki-parser])
  (:import java.sql.SQLException
           java.util.concurrent.locks.ReentrantReadWriteLock))

(declare map->Page)

(deftype PageStorage [shell db rw-lock]
  IPageStorage
  (new-page [self title]
    (validate-page-title title)
    (map->Page {:storage self :title title :latest-revision-cache (ref nil)}))

  (select-page [self title]
    (let [page (new-page self title)]
      (with-rw-lock self readLock
        (if (not (page-exists? page))
          (throw+ {:type :page-not-found})
          page))))

  (select-all-pages [self]
    (with-rw-lock self readLock
      (map #(new-page self %) (shell/ls-rcs-files shell))))

  (select-recent-pages [self n-pages]
    (take n-pages (select-all-pages self)))

  (search-pages [self word]
    (with-rw-lock self readLock
      (shell/grep-iF shell word))))

(defrecord Page [storage title source edit-comment latest-revision-cache]
  IPage
  (save-page [self]
    (letfn [(update-page-relation [db]
              (let [priority (nlinks-per-page-size self)]
                (jdbc/delete! db :page_relation (sql/where {:source title}))
                (doseq [d (referring-titles self)]
                  (jdbc/insert! db
                                :page_relation
                                {:source title
                                 :destination d
                                 :priority priority}))))]
      (validate-page-title title)
      (jdbc/db-transaction [db (.db storage)]
        (with-rw-lock storage writeLock
          (update-page-relation db)
          (shell/co-l (.shell storage) title)
          (try+
            (shell/ci (.shell storage) title (page-source self nil) (or edit-comment ""))
            (catch Object e
              (when (page-exists? self)
                (shell/co-u (.shell storage) title)
                (shell/touch-rcs-file (.shell storage) title (modified-at self nil)))
              (throw+ e)))))))

  (page-source [self revision]
    (or source (with-rw-lock storage readLock
                 (shell/co-p (.shell storage)
                             title
                             (or revision (latest-revision self))))))

  (latest-revision [self]
    (or @latest-revision-cache
        (let [rev (with-rw-lock storage readLock
                    (shell/rlog-head (.shell storage) title))]
          (dosync (ref-set latest-revision-cache rev))
          (latest-revision self))))

  (latest-revision? [self revision]
    (= revision (latest-revision self)))

  (page-exists? [self]
    (with-rw-lock storage readLock
      (shell/test-f (.shell storage) title)))

  (page-history [self]
    (with-rw-lock storage readLock
      (shell/rlog (.shell storage) title)))

  (modified-at [self revision]
    (with-rw-lock storage readLock
      (shell/rlog-date (.shell storage) title (or revision (latest-revision self)))))

  (diff-revisions [self from-rev to-rev]
    (with-rw-lock storage readLock
      (shell/rcsdiff (.shell storage) title from-rev to-rev)))

  (referring-titles [self]
    (wiki-parser/scan-wiki-links (page-source self nil)))

  (referred-titles [self]
    (jdbc/query (.db storage)
                (sql/select [:source]
                            :page_relation
                            (sql/where {:destination title})
                            "ORDER BY priority DESC")
                :row-fn :source))

  (orphan-page? [self]
    (zero? (count (referred-titles self)))))

(defn create-page-storage [base-dir db]
  (letfn [(table-exists? [table-name]
            (try
              (jdbc/query db (sql/select * table-name))
              true
              (catch SQLException e
                false)))]
    (when (not (table-exists? :page_relation))
      (jdbc/db-do-commands db
        (ddl/create-table :page_relation
                          [:source "text"]
                          [:destination "text"]
                          [:priority "integer"])
        (ddl/create-index :pr_source_index :page_relation [:source])
        (ddl/create-index :pr_destination_index :page_relation [:destination])
        (ddl/create-index :pr_priority_index :page_relation [:priority])))
    (let [shell (shell/->Shell base-dir)]
      (shell/make-rcs-dir shell)
      (PageStorage. shell db (ReentrantReadWriteLock.)))))
