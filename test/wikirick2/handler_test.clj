(ns wikirick2.handler-test
  (:use clojure.test
        ring.mock.request
        wikirick2.handler
        wikirick2.service
        wikirick2.testing-helper
        wikirick2.types)
  (:require [clojure.java.shell :as shell]
            [compojure.core :as compojure]
            [compojure.handler :as handler]
            [ring.util.response :as response]
            [wikirick2.screen :as screen]))

(compojure/defroutes app
  (handler/site wikirick-routes))

(use-fixtures :each with-repository with-testing-service)

(def repo (.repository testing-service))

(deftest wikirick-routes-
  (let [front-page (assoc (new-page repo "FrontPage") :source "some content")]
    (save-page front-page)

    (testing "handles GET /"
      (let [res (app (request :get "/"))]
        (is (= (res :status) 200)))))

  (let [foo-page (assoc (new-page repo "FooPage") :source "some content")
        bar-page (assoc (new-page repo "FooPage") :source "some content")]
    (save-page foo-page)
    (save-page bar-page)

    (testing "handles GET /w/FooPage"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (read-view screen (select-page repository "FooPage")))))))

    (testing "handles GET /w/SomePage/new"
      (with-wiki-service
        (let [res (app (request :get "/w/SomePage/new"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (new-view screen
                           (assoc (new-page repository "SomePage") :source "new content")))))))

    (testing "handles GET /w/FooPage/edit"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage/edit"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (edit-view screen
                            (select-page repository "FooPage")))))))

    (testing "redirects when GET /w/SomePage"
      (let [res (app (request :get "/w/SomePage"))]
        (is (= (res :status) 302))
        (prn res)
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "redirects when GET /w/SomePage/edit"
      (let [res (app (request :get "/w/SomePage/edit"))]
        (is (= (res :status) 302))
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "handles POST /w/FooPage/edit"
      (let [page-content "some content"
            res (app (request :post "/w/FooPage/edit"
                              {:source page-content
                               :base-ver "1"}))]
        (is (= (res :status)))
        (let [foo-page (select-page repo "FooPage")]
          (is (= (page-source foo-page) page-content))
          (is (= ((res :headers) "Location") "/w/FooPage")))))))
