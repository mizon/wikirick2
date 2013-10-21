(ns wikirick2.spec.repository
  (:use speclj.core
        wikirick2.repository
        wikirick2.types)
  (:require [clojure.java.shell :as shell]))

(def test-repo-name "test-repo")
(def repo (atom nil))

(defn- should-page= [expected actual]
  (should= (.title expected) (.title actual))
  (should= (.source expected) (.source actual)))

(describe "page repository"
  (before
    (reset! repo (create-repository test-repo-name)))
  (after
    (shell/sh "rm" "-rf" test-repo-name :dir "."))

  (it "saves pages"
    (let [foo (make-page "FooPage" "foo content")
          bar (make-page "BarPage" "bar content")]
      (post-page @repo foo)
      (post-page @repo bar)
      (should-page= foo (select-page @repo "FooPage"))
      (should-page= bar (select-page @repo "BarPage"))))

  (it "selects an page"
    (let [page (make-page "SomePage" "some content")]
      (post-page @repo page)
      (should-page= page (select-page @repo "SomePage"))))

  (it "selects an page by specified revision"
    (let [rev1 (make-page "SomePage" "some content rev 1")
          rev2 (make-page "SomePage" "some content rev 2")]
      (post-page @repo rev1)
      (post-page @repo rev2)
      (should-page= rev1 (select-page-by-revision @repo "SomePage" 1))
      (should-page= rev2 (select-page-by-revision @repo "SomePage" 2))))

  (it "increments revisions of saved pages"
    (let [rev1 (make-page "SomePage" "some content rev 1")
          rev2 (make-page "SomePage" "some content rev 2")]
      (post-page @repo rev1)
      (should= 1 (.revision (select-page @repo "SomePage")))
      (post-page @repo rev2)
      (should= 2 (.revision (select-page @repo "SomePage")))))

  (it "selects titles of all saved pages"
    (should= [] (select-all-page-titles @repo))

    (post-page @repo (make-page "FooPage" "foo content"))
    (should= ["FooPage"] (select-all-page-titles @repo))

    (post-page @repo (make-page "BarPage" "bar content"))
    (should= ["BarPage" "FooPage"] (select-all-page-titles @repo))))
