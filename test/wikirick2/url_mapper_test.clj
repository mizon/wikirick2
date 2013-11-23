(ns wikirick2.url-mapper-test
  (:use clojure.test
        wikirick2.url-mapper
        wikirick2.types))

(def urlm (->URLMapper "/wiki"))

(defrecord MockedPage [title source])

(deftest url-mapper
  (testing "expands index pathes"
    (is (= (index-path urlm) "/")))

  (testing "expands page pathes"
    (is (= (page-path urlm "SomePage") "/w/SomePage")))

  (testing "expands page action pathes"
    (is (= (page-action-path urlm "SomePage" "Edit") "/w/SomePage/edit"))
    (is (= (page-action-path urlm "SomePage" "Source") "/w/SomePage/source")))

  (testing "expands some pathes"
    (is (= (expand-path urlm "foo") "/foo")))

  (testing "expands search pathes"
    (is (= (search-path urlm "foobar") "/search?word=foobar")))

  (testing "expands the theme path"
    (is (= (theme-path urlm) "/theme.css"))))
