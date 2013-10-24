(ns wikirick2.parsers
  (:require [clojure.string :as string]))

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))
