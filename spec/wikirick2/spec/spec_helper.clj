(ns wikirick2.spec.spec-helper
  (:use speclj.core
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
                      :base-path "/"}))

(defn should-be-full-rendered [res template]
  (should= (render-full (ws :screen) template) (res :body)))
