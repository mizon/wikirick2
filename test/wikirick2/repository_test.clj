(ns wikirick2.repository-test
  (:use clojure.test
        wikirick2.repository
        wikirick2.testing-helper
        wikirick2.types)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [clojure.java.shell :as shell]))

(def test-repo-name "test-repo")
(def repo (.repository testing-service))
(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "test.sqlite3"})

(defn- page= [actual expected]
  (and (= (.title actual) (.title expected))
       (= (.source actual) (.source expected))))

(defn- cleanup-page-relation [testcase]
  (try
    (testcase)
    (finally
      (jdbc/execute! db-spec (sql/delete :page_relation []) :multi? true))))

(defmacro testing-repo [name & forms]
  `(testing ~name
     (with-repository (fn []
                        (cleanup-page-relation (fn []
                                                 ~@forms))))))

(deftest pape
  (testing "save-page"
    (testing-repo "saves some pages"
      (let [foo (new-page repo "FooPage" "foo content")
            bar (new-page repo "BarPage" "bar content")]
        (save-page foo)
        (save-page bar)
        (is (page= (select-page repo "FooPage") foo))
        (is (page= (select-page repo "BarPage") bar))))

    (testing-repo "increments revisions of a saved page"
      (let [rev1 (new-page repo "FooBar" "some content rev 1")
            rev2 (new-page repo "FooBar" "some content rev 2")]
        (save-page rev1)
        (is (= (.revision (select-page repo "FooBar")) 1))
        (save-page rev2)
        (is (= (.revision (select-page repo "FooBar")) 2))))

    (testing-repo "fails to save an invalid title page"
      (let [page (new-page repo "FooBar" "some content")
            invalid-page (assoc page :title "Foo/Page")]
        (throw+? (save-page invalid-page) [:type :invalid-page-title]))))

  (testing "referring-titles"
    (testing-repo "gets referring titles"
      (let [page (new-page repo "SomePage" "[[Foo]] [[Bar]]")]
        (is (= (referring-titles page) #{"Foo" "Bar"})))))

  (testing "referred-titles"
    (testing-repo "gets referred titles"
      (let [foo-page (new-page repo "FooPage" "[[SomePage]]")
            bar-page (new-page repo "BarPage" "[[SomePage]]")
            some-page (new-page repo "SomePage" "some content")]
        (save-page foo-page)
        (save-page bar-page)
        (is (= (referred-titles some-page) ["FooPage" "BarPage"]))))

    (testing-repo "forgots old referred titles"
      (let [foo-page (new-page repo "FooPage" "[[BarPage]]")
            bar-page (new-page repo "BarPage" "some content")]
        (save-page foo-page)
        (is (= (referred-titles bar-page) ["FooPage"]))
        (save-page (assoc foo-page :source "some content"))
        (is (= (referred-titles bar-page) []))))

    (testing-repo "considers the referred page priority"
      (let [densed-page (new-page repo "Densed" "short content [[TargetPage]]")
            linkful-page (new-page repo "LinkFul" "[[TargetPage]] [[Foo]]")
            sparsed-page (new-page repo "Sparsed" "blah blah blah -- long content [[TargetPage]]")
            target-page (new-page repo "TargetPage" "some content")]
        (save-page densed-page)
        (save-page linkful-page)
        (save-page sparsed-page)
        (is (= (referred-titles target-page) ["LinkFul" "Densed" "Sparsed"]))))))

(deftest page-repository
  (testing "new-page"
    (testing "makes new page"
      (let [page (new-page repo "NewPage" "new page content")]
        (is (= (.title page) "NewPage"))
        (is (= (.source page) "new page content"))))

    (testing "failes to make a invalid title page"
      (throw+? (new-page repo "New/Page" "new page content") [:type :invalid-page-title])))

  (testing "select-page"
    (testing-repo "selects a page"
      (let [page (new-page repo "SomePage" "some content")]
        (save-page page)
        (is (page= (select-page repo "SomePage") page))))

    (testing-repo "failes to select non-existed page"
      (is (throw+? (select-page repo "FooPage") [:type :page-not-found])))

    (testing-repo "failes to select with a invalid title"
      (is (throw+? (select-page repo "Foo/Page") [:type :invalid-page-title]))))

  (testing "select-page-by-revision"
    (testing-repo "selects some revision"
      (let [rev1 (new-page repo "RevPage" "some content rev 1")
            rev2 (new-page repo "RevPage" "some content rev 2")]
        (save-page rev1)
        (save-page rev2)
        (is (page= (select-page-by-revision repo "RevPage" 1) rev1))
        (is (page= (select-page-by-revision repo "RevPage" 2) rev2))))

    (testing-repo "failes to select non-existed page"
      (is (throw+? (select-page-by-revision repo "FooPage" 1) [:type :page-not-found])))

    (testing-repo "failes to select with a invalid title"
      (is (throw+? (select-page-by-revision repo "Foo/Page" 1) [:type :invalid-page-title]))))

  (testing "select-all-page-titles"
    (testing-repo "selects all saved page titles"
      (is (= (select-all-page-titles repo) []))

      (save-page (new-page repo "FirstPage" "first content"))
      (is (= (select-all-page-titles repo) ["FirstPage"]))

      (save-page (new-page repo "SencondPage" "sencond content"))
      (is (= (select-all-page-titles repo) ["SencondPage" "FirstPage"])))))
