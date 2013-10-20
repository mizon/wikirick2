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
    (expand-path self (concat-paths "w" (.title article))))

  (expand-path [self path]
    (.toString (.resolve (URI. base-path) path)))

  (css-path [self]
    (expand-path self "wikirick.css")))
