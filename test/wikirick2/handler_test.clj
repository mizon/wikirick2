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
            [wikirick2.screen :as screen]))

(compojure/defroutes app
  (handler/site wikirick-routes))

(use-fixtures :each with-repository with-testing-service)

(def repo (.repository testing-service))

(deftest application-handler
  (let [front-page (new-page repo "FrontPage" "front page content")]
    (save-page front-page)

    (testing "handles GET /"
      (let [res (app (request :get "/"))]
        (is (= (res :status) 200)))))

  (let [foo-page (new-page repo "FooPage" "some content")
        bar-page (new-page repo "FooPage" "some content")]
    (save-page foo-page)
    (save-page bar-page)

    (testing "handles GET /w/FooPage"
      (let [res (app (request :get "/w/FooPage"))]
        (is (= (res :status) 200))))))
