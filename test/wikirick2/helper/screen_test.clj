(ns wikirick2.helper.screen_test
  (:require [clj-time.core :as time]
            [clojure.test :refer :all]
            [conjure.core :refer :all]
            [wikirick2.helper.screen :as screen]
            [wikirick2.types :refer :all]))

(deftest last-modified-digest
  (testing "prints 'a day ago'"
    (stubbing [modified-at (time/from-now (time/hours -23))]
      (is (= (screen/last-modified-digest :fake-page) "today"))))

  (testing "prints 'a day ago'"
    (stubbing [modified-at (time/from-now (time/days -1))]
      (is (= (screen/last-modified-digest :fake-page) "1d"))))

  (testing "prints '15 days ago'"
    (stubbing [modified-at (time/from-now (time/days -15))]
      (is (= (screen/last-modified-digest :fake-page) "15d"))))

  (testing "prints 'a month go'"
    (stubbing [modified-at (time/from-now (time/months -1))]
      (is (= (screen/last-modified-digest :fake-page) "1m"))))

  (testing "prints 'a year ago'"
    (stubbing [modified-at (time/from-now (time/years -1))]
      (is (= (screen/last-modified-digest :fake-page) "1y"))))

  (testing "calls modified-at correctly"
    (mocking [modified-at]
      (screen/last-modified-digest :fake-page)
      (verify-called-once-with-args modified-at :fake-page nil))))
