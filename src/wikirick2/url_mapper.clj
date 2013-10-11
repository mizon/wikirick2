(ns wikirick2.url-mapper
  (:use wikirick2.types)
  (:require [clojure.string :as string])
  (:import java.net.URI))

(defn- concat-paths [& paths]
  (string/join "/" paths))

(deftype URLMapper [base-path]
  IURLMapper
  (index-path [self]
    (expand-path self ""))

  (article-path [self article]
    (expand-path self (concat-paths "wiki" (.title article))))

  (expand-path [self path]
    (concat-paths base-path path)))
