(ns wikirick2.testing-helper
  (:use slingshot.slingshot
        wikirick2.service
        wikirick2.types)
  (:require [clojure.java.shell :as shell]))

(def test-repo "test-repo")

(defn setup-test-repo []
  (shell/sh "mkdir" "-p" (format "%s/RCS" test-repo)))

(defn cleanup-test-repo []
  (shell/sh "rm" "-rf" test-repo))

(def testing-service
  (make-wiki-service {:repository-dir test-repo
                      :base-path "/"
                      :sqlite-path "test.sqlite3"}))

(defn with-repository [testcase]
  (try
    (setup-test-repo)
    (testcase)
    (finally
      (cleanup-test-repo))))

(defn with-testing-service [testcase]
  (binding [*wiki-service* testing-service]
    (testcase)))

(defmacro throw+? [try-form catch-form]
  `(try+
     ~try-form
     false
     (catch ~catch-form {} true)))
