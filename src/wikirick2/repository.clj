(ns wikirick2.repository
  (:use wikirick2.types
        slingshot.slingshot
        blancas.kern.core)
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import java.io.File))

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

(defn- check-out-rcs-file [repo article]
  (shell/sh "co" "-l" (.title article) :dir (.base-dir repo)))

(deftype Repository [base-dir]
  IRepository
  (select-article [self title]
    (select-article- self title ["-p" title]))

  (select-article-by-revision [self title rev]
    (select-article- self title [(format "-r1.%s" rev) "-p" title]))

  (select-all-article-titles [self]
    (for [rcs-file (string/split-lines (:out (shell/sh "ls" "-t" (format "%s/RCS" base-dir))))]
      (let [[_ article-name] (re-find #"(.+),v" rcs-file)]
        article-name)))

  (post-article [self article]
    (check-out-rcs-file self article)
    (let [path (format "%s/%s" base-dir (.title article))]
      (spit path (.source article)))
    (shell/sh "ci" (.title article) :in (.edit-comment article) :dir base-dir)))
