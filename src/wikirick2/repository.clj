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

(defn- select-article- [repo title co-args]
  (let [result (apply shell/sh `("co" ~@co-args :dir ~(.base-dir repo)))]
    (if (= (:exit result) 0)
      (let [rev (parse-revision (:err result))
            article (make-article title (:out result))]
        (assoc article :revision rev))
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
  (select-article [self title]
    (with-rw-lock readLock
      (select-article- self title ["-p" title])))

  (select-article-by-revision [self title rev]
    (with-rw-lock readLock
      (select-article- self title [(format "-r1.%s" rev) "-p" title])))

  (select-all-article-titles [self]
    (letfn
      [(ls-rcs-dir []
         (:out (shell/sh "ls" "-t" (format "%s/RCS" base-dir))))]
      (with-rw-lock readLock
        (for [rcs-file (string/split-lines (ls-rcs-dir)) :when (not (empty? rcs-file))]
          (let [[_ article-name] (re-find #"(.+),v" rcs-file)]
            article-name)))))

  (post-article [self article]
    (letfn
      [(check-out-rcs-file []
         (shell/sh "co" "-l" (.title article) :dir (.base-dir self)))]
      (with-rw-lock writeLock
        (check-out-rcs-file)
        (let [path (format "%s/%s" base-dir (.title article))]
          (spit path (.source article)))
        (shell/sh "ci" (.title article) :in (.edit-comment article) :dir base-dir)))))

(defn create-repository [base-dir]
  (shell/sh "mkdir" "-p" (format "%s/RCS" base-dir))
  (Repository. base-dir (ReentrantReadWriteLock.)))
