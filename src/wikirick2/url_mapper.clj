(ns wikirick2.url-mapper
  (:use wikirick2.types)
  (:require [clojure.string :as string])
  (:import java.net.URI))

(declare concat-paths)

(deftype URLMapper [base-path]
  IURLMapper
  (index-path [self]
    (expand-path self ""))

  (page-path [self page-title]
    (expand-path self (concat-paths "w" page-title)))

  (page-revision-path [self page-title revision]
    (expand-path self (format "%s?rev=%s" (concat-paths "w" page-title) revision)))

  (page-diff-path [self page-title src-rev dest-rev]
    (expand-path self (concat-paths "w"
                                    page-title
                                    "diff"
                                    (format "%s-%s" src-rev dest-rev))))

  (page-action-path [self page-title action-name]
    (expand-path self (concat-paths "w" page-title (.toLowerCase action-name))))

  (theme-path [self]
    (expand-path self "theme.css"))

  (search-path [self]
    (expand-path self "search"))

  (expand-path [self path]
    (concat-paths base-path path)))

(defn- concat-paths [& paths]
  (string/join "/" paths))
