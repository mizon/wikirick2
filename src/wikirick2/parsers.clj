(ns wikirick2.parsers
  (:use blancas.kern.core
        blancas.kern.lexer.basic)
  (:require [clojure.string :as string]))

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(defn- headline []
  )

(defn render-wiki-source [wiki-source]
  )
