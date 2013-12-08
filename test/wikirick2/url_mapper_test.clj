(ns wikirick2.url-mapper-test
  (:require [clojure.test :refer :all]
            [wikirick2.types :refer :all]
            [wikirick2.url-mapper :refer :all]))

(def urlm (->URLMapper "/wiki"))

(defrecord MockedPage [title source])

(deftest url-mapper
  (testing "index-path"
    (is (= (index-path urlm) "/wiki/")))

  (testing "page-path"
    (testing "returns a path to SomePage"
      (is (= (page-path urlm "SomePage") "/wiki/w/SomePage")))

    (testing "returns a encoded path to Some Page"
      (is (= (page-path urlm "Some Page") "/wiki/w/Some%20Page"))))

  (testing "page-revision-path"
    (is (= (page-revision-path urlm "SomePage" 3) "/wiki/w/SomePage?rev=3")))

  (testing "page-diff-path"
    (is (= (page-diff-path urlm "SomePage" 3 10) "/wiki/w/SomePage/diff/3-10")))

  (testing "diff-from-previous-path"
    (is (= (diff-from-previous-path urlm "SomePage" 5) "/wiki/w/SomePage/diff/4-5")))

  (testing "diff-from-next-path"
    (is (= (diff-from-next-path urlm "SomePage" 5) "/wiki/w/SomePage/diff/5-6")))

  (testing "page-action-path"
    (is (= (page-action-path urlm "SomePage" "Edit") "/wiki/w/SomePage/edit"))
    (is (= (page-action-path urlm "SomePage" "Source") "/wiki/w/SomePage/source")))

  (testing "search-path"
    (is (= (search-path urlm) "/wiki/search")))

  (testing "theme-path"
    (is (= (theme-path urlm) "/wiki/theme.css"))))
