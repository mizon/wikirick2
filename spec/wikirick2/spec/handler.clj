(ns wikirick2.spec.handler
  (:use ring.mock.request
        speclj.core
        wikirick2.handler
        wikirick2.service
        wikirick2.types
        wikirick2.spec.spec-helper)
  (:require [clojure.java.shell :as shell]
            [compojure.core :as compojure]
            [compojure.handler :as handler]
            [wikirick2.screen :as screen]))

(compojure/defroutes app
  (wrap-with-service (handler/site wikirick-routes) testing-service))

(describe "application handler"
  (after
    (cleanup-test-repo))

  (it "handles GET /"
    (let [res (app (request :get "/"))]
      (should= (res :status) 200)
      (should= (res :body) "<h1>Hello World</h1>")))

  (context "with two articles"
    (with foo-page (make-article "FooPage" "some content"))
    (with bar-page (make-article "FooPage" "some content"))
    (before
      (post-article (service get-repository) @foo-page)
      (post-article (service get-repository) @bar-page))

    (it "handles GET /FooPage"
      (let [res (app (request :get "/FooPage"))]
        (should= (res :status) 200)
        (should-be-full-rendered res (screen/article @foo-page))))))
