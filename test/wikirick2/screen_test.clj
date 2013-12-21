(ns wikirick2.screen-test
  (:require [clojure.test :refer :all]
            [conjure.core :refer :all]
            [wikirick2.screen :as screen]
            [wikirick2.types :refer :all]))

(defn- prepare-renderer []
  (letfn [(fake-render-source [source]
            [:p source])]
    (screen/make-page-renderer fake-render-source)))

(defn- fake-render-source [source]
  [:p source])

(defrecord FakePage [title])

(deftest page-renderer
  (let [foo-page (->FakePage "FooPage")]
    (testing "renders the latest revisions of given pages"
      (let [render (prepare-renderer)]
        (testing "cache mode"
          (stubbing [page-source "foobar" latest-revision 5]
            (is (= (render foo-page nil true) [:p "foobar"]))
            (verify-called-once-with-args page-source foo-page nil)))

        (testing "no cache mode"
          (stubbing [page-source "foobar" latest-revision 5]
            (is (= (render foo-page nil false) [:p "foobar"]))
            (verify-called-once-with-args page-source foo-page nil)))))

    (testing "renders the specified revisions of given pages"
      (let [render (prepare-renderer)]
        (stubbing [page-source "foobar"]
          (is (= (render foo-page 5 true) [:p "foobar"]))
          (verify-called-once-with-args page-source foo-page 5))))

    (testing "caches rendered pages"
      (let [render (prepare-renderer)]
        (stubbing [page-source "foo" latest-revision 5]
          (is (= (render foo-page nil true) [:p "foo"])))
        (stubbing [page-source "foobar"
                   latest-revision 5]
          (is (= (render foo-page nil true) [:p "foo"])))))

    (testing "caches any revisions of each page"
      (let [render (prepare-renderer)]
        (stubbing [page-source "foo" latest-revision 5]
          (is (= (render foo-page nil true) [:p "foo"])))
        (stubbing [page-source "foobar" latest-revision 6]
          (is (= (render foo-page nil true) [:p "foobar"])))))

    (testing "don't cache rendered pages"
      (let [render (prepare-renderer)
            latest-revision- 5]
        (stubbing [page-source "foo" latest-revision latest-revision-]
          (is (= (render foo-page nil false) [:p "foo"])))
        (stubbing [page-source "foobar" latest-revision latest-revision-]
          (is (= (render foo-page nil false) [:p "foobar"])))))))
