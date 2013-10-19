(ns wikirick2.spec.spec-helper
  (:use speclj.core
        wikirick2.service
        wikirick2.types)
  (:require [clojure.java.shell :as shell]))

(def test-repo "test-repo")

(defn cleanup-test-repo []
  (shell/sh "rm" "-rf" test-repo))

(def testing-service
  (->WikiService
   {:repository-dir test-repo
    :base-path "/"}))

(defn service [getter]
  (getter testing-service))

(defn should-be-full-rendered [res template]
  (should= (render-full (service get-screen) template) (res :body)))
