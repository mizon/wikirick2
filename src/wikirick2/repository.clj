(ns wikirick2.repository
  (:use slingshot.slingshot
        wikirick2.repository.helper
        wikirick2.types)
  (:require [clojure.java.shell :as shell]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.ddl :as ddl]
            [clojure.java.jdbc.sql :as sql]
            [clojure.string :as string]
            [wikirick2.parsers :as parsers])
  (:import java.sql.SQLException
           java.util.concurrent.locks.ReentrantReadWriteLock))

(defrecord Page [repo db rw-lock title source revision edit-comment]
  IPage
  (save-page [self]
    (post-page repo self))

  (referring-titles [self]
    (parsers/scan-wiki-links source))

  (referred-titles [self]
    (jdbc/query db
      (sql/select [:source] :page_relation
        (sql/where {:destination title}) "ORDER BY priority DESC")
      :row-fn :source)))

(deftype Repository [base-dir db rw-lock]
  IRepository
  (select-page [self title]
    (with-rw-lock readLock
      (select-page- self title ["-p" title])))

  (select-page-by-revision [self title rev]
    (with-rw-lock readLock
      (select-page- self title [(format "-r1.%s" rev) "-p" title])))

  (select-all-page-titles [self]
    (letfn
      [(ls-rcs-dir []
         (:out (shell/sh "ls" "-t" (format "%s/RCS" base-dir))))]

      (with-rw-lock readLock
        (for [rcs-file (string/split-lines (ls-rcs-dir)) :when (not (empty? rcs-file))]
          (let [[_ page-name] (re-find #"(.+),v" rcs-file)]
            page-name)))))

  (post-page [self page]
    (letfn
      [(check-out-rcs-file []
         (shell/sh "co" "-l" (.title page) :dir base-dir))

       (update-page-relation []
         (let [priority (nlinks-per-page-size page)]
           (doseq [d (referring-titles page)]
             (jdbc/insert! db
                           :page_relation
                           {:source (.title page)
                            :destination d
                            :priority priority}))))]

      (jdbc/db-transaction [_ db]
        (with-rw-lock writeLock
          (update-page-relation)
          (check-out-rcs-file)
          (let [path (format "%s/%s" base-dir (.title page))]
            (spit path (.source page)))
          (shell/sh "ci" (.title page) :in (.edit-comment page) :dir base-dir)))))

  (new-page [self title source]
    (->Page self db rw-lock title source nil nil)))

(defn create-repository [base-dir db]
  (letfn
    [(table-exists? []
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
                          [:priority "integer"])))
    (shell/sh "mkdir" "-p" (format "%s/RCS" base-dir))
    (Repository. base-dir db (ReentrantReadWriteLock.))))
