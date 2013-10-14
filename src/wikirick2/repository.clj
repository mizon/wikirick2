(ns wikirick2.repository
  (:use wikirick2.types
        slingshot.slingshot
        blancas.kern.core)
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import java.util.concurrent.locks.ReentrantReadWriteLock))

(defn- select-article- [repo title co-args]
  (defn parse-revision [text]
    (let [[_ message] (string/split-lines text)
          [_ rev] (re-find #"^revision \d+\.(\d+)$" message)]
      (Integer. rev)))

  (let [result (apply shell/sh `("co" ~@co-args :dir ~(.base-dir repo)))]
    (if (= (:exit result) 0)
      (let [rev (parse-revision (:err result))
            article (make-article title (:out result))]
        (assoc article :revision rev))
      (throw (RuntimeException. (:err result))))))

(defn- with-read-lock [repo f]
  (do
    (-> repo .rwlock .readLock .lock)
    (try
      (f)
      (finally
        (-> repo .rwlock .readLock .unlock)))))

(defn- with-write-lock [repo f]
  (do
    (-> repo .rwlock .writeLock .lock)
    (try
      (f)
      (finally
        (-> repo .rwlock .writeLock .unlock)))))

(deftype Repository [base-dir rwlock]
  IRepository
  (select-article [self title]
    (with-read-lock self
      (fn []
        (select-article- self title ["-p" title]))))

  (select-article-by-revision [self title rev]
    (with-read-lock self
      (fn []
        (select-article- self title [(format "-r1.%s" rev) "-p" title]))))

  (select-all-article-titles [self]
    (with-read-lock self
      (fn []
        (for [rcs-file (string/split-lines (:out (shell/sh "ls" "-t" (format "%s/RCS" base-dir))))]
          (let [[_ article-name] (re-find #"(.+),v" rcs-file)]
            article-name)))))

  (post-article [self article]
    (defn check-out-rcs-file []
      (shell/sh "co" "-l" (.title article) :dir (.base-dir self)))

    (with-write-lock self
      (fn []
        (check-out-rcs-file)
        (let [path (format "%s/%s" base-dir (.title article))]
          (spit path (.source article)))
        (shell/sh "ci" (.title article) :in (.edit-comment article) :dir base-dir)))))

(defn create-repository [base-dir]
  (shell/sh "mkdir" "-p" (format "%s/RCS" base-dir))
  (Repository. base-dir (ReentrantReadWriteLock.)))
