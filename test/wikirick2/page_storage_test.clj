(ns wikirick2.page-storage-test
  (:use clojure.test
        wikirick2.page-storage
        wikirick2.testing-helper
        wikirick2.types)
  (:require [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [clojure.java.shell :as shell])
  (:import org.joda.time.DateTime))

(def test-storage-name "test-storage")
(def storage (.storage testing-service))
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

(defn- create-page [storage title source]
  (assoc (new-page storage title) :source source))

(defmacro testing-storage [name & forms]
  `(testing ~name
     (with-page-storage (fn []
                          (cleanup-page-relation (fn []
                                                   ~@forms))))))

(deftest page-storage
  (testing "new-page"
    (testing "makes new page"
      (let [page (new-page storage "NewPage")]
        (is (= (.title page) "NewPage"))))

    (testing "failes to make a invalid title page"
      (throw+? (new-page storage "New/Page") [:type :invalid-page-title])))

  (testing "select-page"
    (testing-storage "selects a page"
      (let [page (create-page storage "SomePage" "some content")]
        (save-page page)
        (is (page= (select-page storage "SomePage") page))))

    (testing-storage "failes to select non-existed page"
      (is (throw+? (select-page storage "FooPage") [:type :page-not-found])))

    (testing-storage "failes to select with a invalid title"
      (is (throw+? (select-page storage "Foo/Page") [:type :invalid-page-title]))))

  (testing "select-page-by-revision"
    (testing-storage "selects some revision"
      (let [rev1 (create-page storage "RevPage" "some content rev 1")
            rev2 (create-page storage "RevPage" "some content rev 2")]
        (save-page rev1)
        (save-page rev2)
        (is (page= (select-page-by-revision storage "RevPage" 1) rev1))
        (is (page= (select-page-by-revision storage "RevPage" 2) rev2))))

    (testing-storage "failes to select non-existed page"
      (is (throw+? (select-page-by-revision storage "FooPage" 1) [:type :page-not-found])))

    (testing-storage "failes to select with a invalid title"
      (is (throw+? (select-page-by-revision storage "Foo/Page" 1) [:type :invalid-page-title]))))

  (testing "select-all-pages"
    (testing-storage "selects all pages"
      (is (= (select-all-pages storage) []))

      (save-page (create-page storage "FirstPage" "first content"))
      (is (= (map :title (select-all-pages storage)) ["FirstPage"]))

      (save-page (create-page storage "SencondPage" "sencond content"))
      (is (= (map :title (select-all-pages storage)) ["SencondPage" "FirstPage"]))))

  (testing "select-recent-pages"
    (testing-storage "selects recent pages"
      (save-page (create-page storage "FirstPage" "first content"))
      (save-page (create-page storage "SecondPage" "sencond content"))
      (save-page (create-page storage "ThirdPage" "third content"))

      (is (= (map :title (select-recent-pages storage 2)) '("ThirdPage" "SecondPage")))
      (is (= (map :title (select-recent-pages storage 3)) '("ThirdPage" "SecondPage" "FirstPage")))
      (is (= (map :title (select-recent-pages storage 10)) '("ThirdPage" "SecondPage" "FirstPage")))))

  (testing-storage "search-pages"
    (save-page (create-page storage "FooPage" "
some foo
some bar
some 'foo
"))
    (save-page (create-page storage "BarPage" "
some foo
some bar
foo: foobar
"))

    (testing "returns a empty set when matched no pages"
      (is (= (search-pages storage "nooo") #{})))

    (testing "hits the first matched line only in each page"
      (is (= (search-pages storage "some") #{["FooPage" "some foo"] ["BarPage" "some foo"]})))

    (testing "accepts words containing single quotes"
      (is (= (search-pages storage "'foo") #{["FooPage" "some 'foo"]})))

    (testing "hits lines containing colons"
      (is (= (search-pages storage "foobar") #{["BarPage" "foo: foobar"]})))

    (testing "hits titles and has first line of the page"
      ;; FIXME: Skipped: This feature is not implemented yet
      ;; (is (= (search-pages storage "barpage") #{["BarPage" "some foo"]}))
      )))

(deftest pape
  (testing "save-page"
    (testing-storage "saves some pages"
      (let [foo (create-page storage "FooPage" "foo content")
            bar (create-page storage "BarPage" "bar content")]
        (save-page foo)
        (save-page bar)
        (is (page= (select-page storage "FooPage") foo))
        (is (page= (select-page storage "BarPage") bar))))

    (testing-storage "increments revisions of a saved page"
      (let [rev1 (create-page storage "FooBar" "some content rev 1")
            rev2 (create-page storage "FooBar" "some content rev 2")]
        (save-page rev1)
        (is (= (latest-revision (select-page storage "FooBar")) 1))
        (save-page rev2)
        (is (= (latest-revision (select-page storage "FooBar")) 2))))

    (testing-storage "fails to save an invalid title page"
      (let [page (create-page storage "FooBar" "some content")
            invalid-page (assoc page :title "Foo/Page")]
        (throw+? (save-page invalid-page) [:type :invalid-page-title]))))

  (testing "page-revision"
    (testing-storage "returns the latest revision when the slot is set nil"
      (let [page (create-page storage "Foo" "foo content")]
        (save-page page)
        (is (= (page-revision page) 1))))

    (testing "returns the specified revision when the slot is set non-nil"
      (let [page (create-page storage "Foo" "foo content")
            specified (assoc page :revision 10)]
        (is (= (page-revision specified) 10)))))

  (testing "latest-revision"
    (testing-storage "returns the latest revision"
      (let [rev1 (create-page storage "FooPage" "foo content")
            rev2 (create-page storage "FooPage" "bar content")]
        (save-page rev1)
        (is (= (latest-revision (select-page storage "FooPage")) 1))
        (save-page rev2)
        (is (= (latest-revision (select-page storage "FooPage")) 2)))))

  (testing "page-exists?"
    (testing-storage "knows if page exists"
      (let [page (create-page storage "SomePage" "some content")]
        (is (not (page-exists? page)))
        (save-page page)
        (is (page-exists? page)))))

  (testing-storage "page-history"
    (let [before (DateTime/now)]
      (save-page (create-page storage "SomePage" "some content"))
      (save-page (create-page storage "SomePage" "foo content"))

      (testing "returns the revisions of the commits"
        (let [hist (page-history (select-page storage "SomePage"))]
          (is (= (map :revision hist) [2 1]))))

      (testing "returns numbers of edited lines"
        (let [hist (page-history (select-page storage "SomePage"))]
          (is (= ((first hist) :lines) {:added 1 :deleted 1}))))

      (testing "doesn't returns num of edited lines of first revision"
        (let [hist (page-history (select-page storage "SomePage"))]
          (is (= ((last hist) :lines) nil))))

      (testing "returns the commited dates"
        (let [hist (page-history (select-page storage "SomePage"))
              committed-date ((first hist) :date)]
          (time/after? committed-date before)
          (time/before? committed-date (DateTime/now))))))

  (testing "modified-at"
    (testing-storage "tells when it be modified"
      (let [page (create-page storage "SomePage" "some content")
            before (DateTime/now)]
        (save-page page)
        (time/after? (modified-at page) before)
        (time/before? (modified-at page) (DateTime/now))))

    (testing-storage "throws exception when called before saved"
      (let [page (create-page storage "SomePage" "some content")]
        (throw+? (modified-at page) [:type :head-revision-failed]))))

  (testing "referring-titles"
    (testing-storage "gets referring titles"
      (let [page (create-page storage "SomePage" "[[Foo]] [[Bar]]")]
        (is (= (referring-titles page) #{"Foo" "Bar"})))))

  (testing "referred-titles"
    (testing-storage "gets referred titles"
      (let [foo-page (create-page storage "FooPage" "[[SomePage]]")
            bar-page (create-page storage "BarPage" "[[SomePage]]")
            some-page (create-page storage "SomePage" "some content")]
        (save-page foo-page)
        (save-page bar-page)
        (is (= (referred-titles some-page) ["FooPage" "BarPage"]))))

    (testing-storage "forgots old referred titles"
      (let [foo-page (create-page storage "FooPage" "[[BarPage]]")
            bar-page (create-page storage "BarPage" "some content")]
        (save-page foo-page)
        (is (= (referred-titles bar-page) ["FooPage"]))
        (save-page (assoc foo-page :source "some content"))
        (is (= (referred-titles bar-page) []))))

    (testing-storage "considers the referred page priority"
      (let [densed-page (create-page storage "Densed" "short content [[TargetPage]]")
            linkful-page (create-page storage "LinkFul" "[[TargetPage]] [[Foo]]")
            sparsed-page (create-page storage "Sparsed" "blah blah blah -- long content [[TargetPage]]")
            target-page (create-page storage "TargetPage" "some content")]
        (save-page densed-page)
        (save-page linkful-page)
        (save-page sparsed-page)
        (is (= (referred-titles target-page) ["LinkFul" "Densed" "Sparsed"]))))))
