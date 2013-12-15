(ns wikirick2.helper.page-storage
  (:require [clojure.core.match :refer [match]]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [slingshot.slingshot :refer :all]
            [wikirick2.types :refer :all]
            [wikirick2.wiki-parser :as wiki-parser]))

(defmacro with-rw-lock [storage lock-type & forms]
  `(do
     (.. ~storage ~'rw-lock ~lock-type lock)
     (try
       ~@forms
       (finally
         (.. ~storage ~'rw-lock ~lock-type unlock)))))

(defn nlinks-per-page-size [page]
  (let [dests (referring-titles page)]
    (Math/round (float (* (/ (count dests) (count (page-source page nil))) 1e6)))))

(defn validate-page-title [title]
  (when (not (wiki-parser/valid-page-name? title))
    (throw+ {:type :invalid-page-title})))

(defn validate-page-source [source]
  (when (empty? (.trim source))
    (throw+ {:type :empty-source})))

(defn trim-end-of-source [source]
  (str (string/trimr source) "\n"))
