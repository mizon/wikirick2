(ns wikirick2.helper.repository
  (:use slingshot.slingshot
        wikirick2.types)
  (:require [clojure.core.match :refer [match]]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [wikirick2.parsers :as parsers]))

(defmacro with-rw-lock [repo lock-type & forms]
  `(do
     (.. ~repo ~'rw-lock ~lock-type lock)
     (try
       ~@forms
       (finally
         (.. ~repo ~'rw-lock ~lock-type unlock)))))

(defn nlinks-per-page-size [page]
  (let [dests (referring-titles page)]
    (Math/round (float (* (/ (count dests) (count (.source page))) 1e6)))))

(defn validate-page-title [title]
  (when (not (parsers/valid-page-name? title))
    (throw+ {:type :invalid-page-title})))
