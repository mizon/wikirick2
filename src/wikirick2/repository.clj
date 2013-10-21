(ns wikirick2.repository
  (:use slingshot.slingshot
        wikirick2.repository.helper
        wikirick2.types)
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import java.util.concurrent.locks.ReentrantReadWriteLock))

(deftype Repository [base-dir rw-lock]
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
         (shell/sh "co" "-l" (.title page) :dir base-dir))]
      (with-rw-lock writeLock
        (check-out-rcs-file)
        (let [path (format "%s/%s" base-dir (.title page))]
          (spit path (.source page)))
        (shell/sh "ci" (.title page) :in (.edit-comment page) :dir base-dir)))))

(defn create-repository [base-dir]
  (shell/sh "mkdir" "-p" (format "%s/RCS" base-dir))
  (Repository. base-dir (ReentrantReadWriteLock.)))
