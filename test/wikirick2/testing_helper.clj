(ns wikirick2.testing-helper
  (:use slingshot.slingshot
        wikirick2.service
        wikirick2.types)
  (:require [clojure.java.shell :as shell]))

(def test-storage "test-storage")

(defn setup-test-storage []
  (shell/sh "mkdir" "-p" (format "%s/RCS" test-storage)))

(defn cleanup-test-storage []
  (shell/sh "rm" "-rf" test-storage))

(def testing-service
  (make-wiki-service {:page-storage-dir test-storage
                      :base-path "/"
                      :sqlite-path "test.sqlite3"}))

(defn with-page-storage [testcase]
  (try
    (setup-test-storage)
    (testcase)
    (finally
      (cleanup-test-storage))))

(defn with-testing-service [testcase]
  (binding [*wiki-service* testing-service]
    (testcase)))

(defmacro throw+? [try-form catch-form]
  `(try+
     ~try-form
     false
     (catch ~catch-form {} true)))
