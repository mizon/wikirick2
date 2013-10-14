(ns wikirick2.spec.repository
  (:use speclj.core
        wikirick2.repository
        wikirick2.types)
  (:require [clojure.java.shell :as shell]))

(def test-repo-name "test-repo")
(def repo (atom nil))

(defn- should-article= [expected actual]
  (should= (.title expected) (.title actual))
  (should= (.source expected) (.source actual)))

(describe "article repository"
  (before
    (reset! repo (create-repository test-repo-name)))
  (after
    (shell/sh "rm" "-rf" test-repo-name :dir "."))

  (it "saves an article"
    (let [foo (make-article "FooPage" "foo content")
          bar (make-article "BarPage" "bar content")]
      (post-article @repo foo)
      (post-article @repo bar)
      (should-article= foo (select-article @repo "FooPage"))
      (should-article= bar (select-article @repo "BarPage"))))

  (it "selects an article"
    (let [article (make-article "SomePage" "some content")]
      (post-article @repo article)
      (should-article= article (select-article @repo "SomePage"))))

  (it "selects an article by specified revision"
    (let [rev1 (make-article "SomePage" "some content rev 1")
          rev2 (make-article "SomePage" "some content rev 2")]
      (post-article @repo rev1)
      (post-article @repo rev2)
      (should-article= rev1 (select-article-by-revision @repo "SomePage" 1))
      (should-article= rev2 (select-article-by-revision @repo "SomePage" 2))))

  (it "increments revisions of saved articles"
    (let [rev1 (make-article "SomePage" "some content rev 1")
          rev2 (make-article "SomePage" "some content rev 2")]
      (post-article @repo rev1)
      (should= 1 (.revision (select-article @repo "SomePage")))
      (post-article @repo rev2)
      (should= 2 (.revision (select-article @repo "SomePage")))))

  (it "selects titles of all saved articles"
    (should= [] (select-all-article-titles @repo))

    (post-article @repo (make-article "FooPage" "foo content"))
    (should= ["FooPage"] (select-all-article-titles @repo))

    (post-article @repo (make-article "BarPage" "bar content"))
    (should= ["BarPage" "FooPage"] (select-all-article-titles @repo))))
