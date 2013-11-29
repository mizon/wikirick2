(ns wikirick2.repository
  (:use slingshot.slingshot
        wikirick2.helper.repository
        wikirick2.types)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.ddl :as ddl]
            [clojure.java.jdbc.sql :as sql]
            [clojure.string :as string]
            [wikirick2.shell :as shell]
            [wikirick2.wiki-parser :as wiki-parser])
  (:import java.sql.SQLException
           java.util.concurrent.locks.ReentrantReadWriteLock))

(declare map->Page)

(deftype Repository [shell db rw-lock]
  IRepository
  (new-page [self title]
    (validate-page-title title)
    (map->Page {:repo self :title title}))

  (select-page [self title]
    (let [page (new-page self title)]
      (if (not (page-exists? page))
        (throw+ {:type :page-not-found})
        page)))

  (select-page-by-revision [self title rev]
    (assoc (select-page self title) :revision rev))

  (select-all-pages [self]
    (with-rw-lock self readLock
      (map #(new-page self %) (shell/ls-rcs-files shell))))

  (select-recent-pages [self n-pages]
    (take n-pages (select-all-pages self)))

  (search-pages [self word]
    (with-rw-lock self readLock
      (shell/grep-iF shell word))))

(defrecord Page [repo title source revision edit-comment]
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
      (jdbc/db-transaction [db (.db repo)]
        (with-rw-lock repo writeLock
          (update-page-relation db)
          (shell/co-l (.shell repo) title)
          (shell/ci (.shell repo) title (page-source self) (or edit-comment ""))))))

  (page-source [self]
    (or source (with-rw-lock repo readLock
                 (shell/co-p (.shell repo) title (page-revision self)))))

  (page-revision [self]
    (or revision (with-rw-lock repo readLock
                   (shell/head-revision (.shell repo) title))))

  (page-exists? [self]
    (with-rw-lock repo readLock
      (shell/test-f (.shell repo) title)))

  (modified-at [self]
    (with-rw-lock repo readLock
      (shell/rlog-date (.shell repo) title (page-revision self))))

  (referring-titles [self]
    (wiki-parser/scan-wiki-links (page-source self)))

  (referred-titles [self]
    (jdbc/query (.db repo)
                (sql/select [:source]
                            :page_relation
                            (sql/where {:destination title})
                            "ORDER BY priority DESC")
                :row-fn :source)))

(defn create-repository [base-dir db]
  (letfn [(table-exists? []
            (try
              (jdbc/query db (sql/select * :page_relation))
              true
              (catch SQLException e
                false)))]
    (when (not (table-exists?))
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
      (Repository. shell db (ReentrantReadWriteLock.)))))
