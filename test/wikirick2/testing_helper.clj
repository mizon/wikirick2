(ns wikirick2.testing-helper
  (:use wikirick2.service
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

(defn render-full? [res template]
  (= (render-full (ws :screen) template) (res :body)))

(defn with-repository [testcase]
  (try
    (setup-test-repo)
    (testcase)
    (finally
      (cleanup-test-repo))))

(defn with-testing-service [testcase]
  (binding [wiki-service testing-service]
    (testcase)))
