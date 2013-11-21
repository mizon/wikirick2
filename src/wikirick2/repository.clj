(ns wikirick2.repository
  (:use slingshot.slingshot
        wikirick2.helper.repository
        wikirick2.types)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.ddl :as ddl]
            [clojure.java.jdbc.sql :as sql]
            [clojure.string :as string]
            [wikirick2.parsers :as parsers]
            [wikirick2.shell :as shell])
  (:import java.sql.SQLException
           java.util.concurrent.locks.ReentrantReadWriteLock))

(declare map->Page)

(deftype Repository [shell db rw-lock]
  IRepository
  (new-page [self title]
    (validate-page-title title)
    (map->Page {:repo self :title title}))

  (select-page [self title]
    (new-page self title))

  (select-page-by-version [self title ver]
    (assoc (new-page self title) :version ver))

  (select-all-pages [self]
    (with-rw-lock self readLock
      (map #(new-page self %) (shell/ls-rcs-files shell)))))

(defrecord Page [repo title source version edit-comment]
  IPage
  (save-page [self]
    (letfn [(update-page-relation []
              (let [priority (nlinks-per-page-size self)]
                (jdbc/delete! (.db repo) :page_relation (sql/where {:source title}))
                (doseq [d (referring-titles self)]
                  (jdbc/insert! (.db repo)
                                :page_relation
                                {:source title
                                 :destination d
                                 :priority priority}))))]
      (validate-page-title title)
      (jdbc/db-transaction [db (.db repo)]
        (with-rw-lock repo writeLock
          (update-page-relation)
          (shell/lock-rcs-file (.shell repo) title)
          (shell/ci (.shell repo) title source (or edit-comment ""))))))

  (page-source [self]
    (or source (with-rw-lock repo readLock
                 (shell/co-p (.shell repo) title (page-version self)))))

  (page-version [self]
    (or version (with-rw-lock repo readLock
                  (shell/head-version (.shell repo) title))))

  (referring-titles [self]
    (parsers/scan-wiki-links source))

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
