(ns wikirick2.repository
  (:use wikirick2.types
        slingshot.slingshot)
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import java.util.concurrent.locks.ReentrantReadWriteLock))

(defn- parse-revision [text]
  (let [[_ message] (string/split-lines text)
        [_ rev] (re-find #"^revision \d+\.(\d+)$" message)]
    (Integer. rev)))

(defn- select-page- [repo title co-args]
  (let [result (apply shell/sh `("co" ~@co-args :dir ~(.base-dir repo)))]
    (if (= (:exit result) 0)
      (let [rev (parse-revision (:err result))
            page (make-page title (:out result))]
        (assoc page :revision rev))
      (throw (RuntimeException. (:err result))))))

(defmacro with-rw-lock [lock-type & forms]
  `(do
     (.. ~'rw-lock ~lock-type lock)
     (try
       ~@forms
       (finally
         (.. ~'rw-lock ~lock-type unlock)))))

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
         (shell/sh "co" "-l" (.title page) :dir (.base-dir self)))]
      (with-rw-lock writeLock
        (check-out-rcs-file)
        (let [path (format "%s/%s" base-dir (.title page))]
          (spit path (.source page)))
        (shell/sh "ci" (.title page) :in (.edit-comment page) :dir base-dir)))))

(defn create-repository [base-dir]
  (shell/sh "mkdir" "-p" (format "%s/RCS" base-dir))
  (Repository. base-dir (ReentrantReadWriteLock.)))
