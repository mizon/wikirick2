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
       (= (page-source actual) (page-source expected))))

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

(deftest page-repository
  (testing "new-page"
    (testing "makes new page"
      (let [page (new-page repo "NewPage" "new page content")]
        (is (= (.title page) "NewPage"))
        (is (= (page-source page) "new page content"))))

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

  (testing "select-page-by-version"
    (testing-repo "selects some version"
      (let [ver1 (new-page repo "VerPage" "some content ver 1")
            ver2 (new-page repo "VerPage" "some content ver 2")]
        (save-page ver1)
        (save-page ver2)
        (is (page= (select-page-by-version repo "VerPage" 1) ver1))
        (is (page= (select-page-by-version repo "VerPage" 2) ver2))))

    (testing-repo "failes to select non-existed page"
      (is (throw+? (select-page-by-version repo "FooPage" 1) [:type :page-not-found])))

    (testing-repo "failes to select with a invalid title"
      (is (throw+? (select-page-by-version repo "Foo/Page" 1) [:type :invalid-page-title]))))

  (testing "select-all-pages"
    (testing-repo "select all pages"
      (is (= (select-all-pages repo) []))

      (save-page (new-page repo "FirstPage" "first content"))
      (is (= (map :title (select-all-pages repo)) ["FirstPage"]))

      (save-page (new-page repo "SencondPage" "sencond content"))
      (is (= (map :title (select-all-pages repo)) ["SencondPage" "FirstPage"])))))

(deftest pape
  (testing "save-page"
    (testing-repo "saves some pages"
      (let [foo (new-page repo "FooPage" "foo content")
            bar (new-page repo "BarPage" "bar content")]
        (save-page foo)
        (save-page bar)
        (is (page= (select-page repo "FooPage") foo))
        (is (page= (select-page repo "BarPage") bar))))

    (testing-repo "increments versions of a saved page"
      (let [ver1 (new-page repo "FooBar" "some content ver 1")
            ver2 (new-page repo "FooBar" "some content ver 2")]
        (save-page ver1)
        (is (= (page-version (select-page repo "FooBar")) 1))
        (save-page ver2)
        (is (= (page-version (select-page repo "FooBar")) 2))))

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
