(ns wikirick2.url-mapper-test
  (:use clojure.test
        wikirick2.url-mapper
        wikirick2.types))

(def urlm (->URLMapper "/wiki"))

(defrecord MockedPage [title source])

(deftest url-mapper
  (testing "index-path"
    (is (= (index-path urlm) "/")))

  (testing "page-path"
    (is (= (page-path urlm "SomePage") "/w/SomePage")))

  (testing "page-revision-path"
    (is (= (page-revision-path urlm "SomePage" 3) "/w/SomePage?rev=3")))

  (testing "page-action-path"
    (is (= (page-action-path urlm "SomePage" "Edit") "/w/SomePage/edit"))
    (is (= (page-action-path urlm "SomePage" "Source") "/w/SomePage/source")))

  (testing "expand-path"
    (is (= (expand-path urlm "foo") "/foo")))

  (testing "search-path"
    (is (= (search-path urlm) "/search")))

  (testing "theme-path"
    (is (= (theme-path urlm) "/theme.css"))))
