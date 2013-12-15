(ns wikirick2.page-storage-test
  (:require [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [wikirick2.page-storage :refer :all]
            [wikirick2.testing-helper :refer :all]
            [wikirick2.types :refer :all])
  (:import org.joda.time.DateTime))

(def test-storage-name "test-storage")
(def storage (.storage testing-service))
(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "test.sqlite3"})

(defn- page= [actual expected]
  (and (= (.title actual) (.title expected))
       (= (string/trimr (page-source actual nil))
          (string/trimr (page-source expected nil)))))

(defn- cleanup-page-relation [testcase]
  (try
    (testcase)
    (finally
      (jdbc/execute! db-spec (sql/delete :page_relation []) :multi? true))))

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
      (is (throw+? (new-page storage "New/Page") [:type :invalid-page-title]))))

  (testing "select-page"
    (testing-storage "selects a page"
      (let [page (create-page storage "SomePage" "some content")]
        (save-page page)
        (is (page= (select-page storage "SomePage") page))))

    (testing-storage "failes to select non-existed page"
      (is (throw+? (select-page storage "FooPage") [:type :page-not-found])))

    (testing-storage "failes to select with a invalid title"
      (is (throw+? (select-page storage "Foo/Page") [:type :invalid-page-title]))))

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
      (save-page (create-page storage "FooBar" "some content rev 1"))
      (is (= (latest-revision (select-page storage "FooBar")) 1))
      (save-page (assoc (select-page storage "FooBar")
                   :source "some content rev 2"))
      (is (= (latest-revision (select-page storage "FooBar")) 2)))

    (testing-storage "fails to save an invalid title page"
      (let [page (create-page storage "FooBar" "some content")
            invalid-page (assoc page :title "Foo/Page")]
        (is (throw+? (save-page invalid-page) [:type :invalid-page-title]))))

    (testing-storage "fails to save same content"
      (save-page (create-page storage "Foo" "foo content"))
      (is (throw+? (save-page (select-page storage "Foo")) [:type :unchanged-source])))

    (testing-storage "failed to save, doesn't change recent page results"
      (save-page (create-page storage "Foo" "foo content"))
      (save-page (create-page storage "Bar" "bar content"))
      (is (throw+? (save-page (select-page storage "Foo")) [:type :unchanged-source]))

      (is (= (map :title (select-recent-pages storage 10))
             ["Bar" "Foo"]))))

  (testing-storage "page-source"
    (save-page (create-page storage "SomePage" "some content"))
    (save-page (assoc (select-page storage "SomePage")
                 :source "some some content"))
    (let [some-page (select-page storage "SomePage")]
      (testing "returns the source on latest revision"
        (is (= (page-source some-page nil) "some some content\n")))

      (testing "returns the source on specific revision"
        (is (= (page-source some-page 1) "some content\n")))

      (testing "returns the assigned source"
        (is (= (page-source (create-page storage "SomePage" "new content") nil)
               "new content\n"))))

    (testing "trims end of space chars"
      (is (page-source (create-page storage "FooPage" "foobar    \n   \n \t   ") nil)
          "foobar\n")))

  (testing "latest-revision"
    (testing-storage "returns the latest revision"
      (save-page (create-page storage "FooPage" "foo content"))
      (is (= (latest-revision (select-page storage "FooPage")) 1))
      (save-page (assoc (select-page storage "FooPage")
                   :source "bar content"))
      (is (= (latest-revision (select-page storage "FooPage")) 2))))

  (testing "latest-revision?"
    (testing-storage "checks the argument is the latest revision"
      (save-page (create-page storage "FooPage" "foo content"))
      (save-page (assoc (select-page storage "FooPage")
                   :source "bar content"))
      (let [page (select-page storage "FooPage")]
        (is (not (latest-revision? page 1)))
        (is (latest-revision? page 2)))))

  (testing "new-page?"
    (testing-storage "knows itself is whether new page or not"
      (let [page (create-page storage "SomePage" "some content")]
        (is (new-page? page))
        (save-page page)
        (is (not (new-page? page))))))

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
        (time/after? (modified-at page nil) before)
        (time/before? (modified-at page nil) (DateTime/now))))

    (testing-storage "throws exception when called before saved"
      (let [page (create-page storage "SomePage" "some content")]
        (is (throw+? (modified-at page nil) [:type :head-revision-failed])))))

  (testing-storage "diff-revisions"
    (save-page (create-page storage "SomePage" "
foo
bar
foobar
"))
    (save-page (assoc (select-page storage "SomePage")
                 :source "
foo
foobar
foobar
"))

    (testing "gets diff from previous revision"
      (let [some-page (select-page storage "SomePage")]
        (is (= (diff-revisions some-page 1 2) "@@ -1,4 +1,4 @@
 
 foo
-bar
+foobar
 foobar
")))))

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
        (is (= (referred-titles target-page) ["LinkFul" "Densed" "Sparsed"])))))

  (testing "orphan-page?"
    (testing-storage "knows if self is an orphan page or not"
      (save-page (create-page storage "FooPage" "foo content"))
      (is (orphan-page? (select-page storage "FooPage")))
      (save-page (create-page storage "SomePage" "[[FooPage]]"))
      (is (not (orphan-page? (select-page storage "FooPage"))))))

  (testing "remove-page"
    (testing-storage "removes a page"
      (save-page (create-page storage "FooPage" "foo content"))
      (remove-page (select-page storage "FooPage"))
      (is (throw+? (select-page storage "FooPage") [:type :page-not-found])))))
