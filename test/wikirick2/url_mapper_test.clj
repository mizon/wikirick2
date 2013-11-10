(ns wikirick2.url-mapper-test
  (:use clojure.test
        wikirick2.url-mapper
        wikirick2.types))

(def urlm (->URLMapper "/wiki"))

(defrecord MockedPage [title source])

(deftest url-mapper
  (testing "expands index pathes"
    (is (= (index-path urlm) "/")))

  (testing "expands an page path"
    (let [page (->MockedPage "SomePage" "some content")]
      (is (= (page-path urlm page) "/w/SomePage"))))

  (testing "expands some pathes"
    (is (= (expand-path urlm "foo") "/foo")))

  (testing "expands the theme path"
    (is (= (theme-path urlm) "/theme.css"))))
