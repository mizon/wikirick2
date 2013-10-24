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

  (it "makes new page"
    (let [page (new-page @repo "NewPage" "new page content")]
      (should= "NewPage" (.title page))
      (should= "new page content" (.source page))))

  (it "saves pages"
    (let [foo (new-page @repo "FooPage" "foo content")
          bar (new-page @repo "BarPage" "bar content")]
      (save-page foo)
      (save-page bar)
      (should-page= foo (select-page @repo "FooPage"))
      (should-page= bar (select-page @repo "BarPage"))))

  (it "selects an page"
    (let [page (new-page @repo "SomePage" "some content")]
      (save-page page)
      (should-page= page (select-page @repo "SomePage"))))

  (it "selects an page by specified revision"
    (let [rev1 (new-page @repo "SomePage" "some content rev 1")
          rev2 (new-page @repo "SomePage" "some content rev 2")]
      (save-page rev1)
      (save-page rev2)
      (should-page= rev1 (select-page-by-revision @repo "SomePage" 1))
      (should-page= rev2 (select-page-by-revision @repo "SomePage" 2))))

  (it "increments revisions of saved pages"
    (let [rev1 (new-page @repo "SomePage" "some content rev 1")
          rev2 (new-page @repo "SomePage" "some content rev 2")]
      (save-page rev1)
      (should= 1 (.revision (select-page @repo "SomePage")))
      (save-page rev2)
      (should= 2 (.revision (select-page @repo "SomePage")))))

  (it "selects titles of all saved pages"
    (should= [] (select-all-page-titles @repo))

    (save-page (new-page @repo "FooPage" "foo content"))
    (should= ["FooPage"] (select-all-page-titles @repo))

    (save-page (new-page @repo "BarPage" "bar content"))
    (should= ["BarPage" "FooPage"] (select-all-page-titles @repo))))
