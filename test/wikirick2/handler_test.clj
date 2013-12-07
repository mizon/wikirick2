(ns wikirick2.handler-test
  (:require [clojure.java.shell :as shell]
            [clojure.test :refer :all]
            [compojure.core :as compojure]
            [compojure.handler :as handler]
            [ring.mock.request :refer :all]
            [ring.util.response :as response]
            [wikirick2.handler :refer :all]
            [wikirick2.screen :as screen]
            [wikirick2.service :refer :all]
            [wikirick2.testing-helper :refer :all]
            [wikirick2.types :refer :all]))

(compojure/defroutes app
  (handler/site wikirick-routes))

(use-fixtures :each with-page-storage with-testing-service)

(def storage (.storage testing-service))

(deftest wikirick-routes-
  (let [front-page (assoc (new-page storage "FrontPage") :source "some content")]
    (save-page front-page)

    (testing "handles GET /"
      (let [res (app (request :get "/"))]
        (is (= (res :status) 200)))))

  (save-page (create-page storage "FooPage" "some content"))
  (save-page (assoc (select-page storage "FooPage")
               :source "foo content"))
  (save-page (create-page storage "BarPage" "some content"))

  (let [foo-page (select-page storage "FooPage")
        bar-page (select-page storage "BarPage")]
    (testing "handles GET /w/FooPage"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (read-view screen (select-page storage "FooPage") nil))))))

    (testing "handles GET /w/FooPage?rev=1"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage?rev=1"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (read-view screen (select-page storage "FooPage") 1))))))

    (testing "handles GET /w/SomePage/new"
      (with-wiki-service
        (let [res (app (request :get "/w/SomePage/new"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (new-view screen
                           (assoc (new-page storage "SomePage") :source "new content")))))))

    (testing "handles GET /w/FooPage/edit"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage/edit"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (edit-view screen
                            (select-page storage "FooPage")))))))

    (testing "redirects when GET /w/SomePage"
      (let [res (app (request :get "/w/SomePage"))]
        (is (= (res :status) 302))
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "redirects when GET /w/SomePage/edit"
      (let [res (app (request :get "/w/SomePage/edit"))]
        (is (= (res :status) 302))
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "handles POST /w/FooPage/edit"
      (let [page-content "some content"
            res (app (request :post "/w/FooPage/edit"
                              {:source page-content}))]
        (is (= (res :status) 303))
        (let [foo-page (select-page storage "FooPage")]
          (is (= (page-source foo-page nil) page-content))
          (is (= ((res :headers) "Location") "/w/FooPage")))))

    (testing "handles GET /w/FooPage/diff/from-to"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage/diff/1-2"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (diff-view screen foo-page 1 2))))))

    (testing "handles GET /w/FooPage/history"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage/history"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (history-view screen foo-page))))))

    (testing "redirects when GET /w/SomePage/history"
      (let [res (app (request :get "/w/SomePage/history"))]
        (is (= (res :status) 302))
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "handles GET /search"
      (with-wiki-service
        (let [res (app (request :get "/search" {:word "some"}))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (search-view screen
                              "some"
                              (search-pages storage "some")))))))))
