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
  (handler/site wikirick-routes))

(describe "application handler"
  (before
    (setup-test-repo))
  (after
    (cleanup-test-repo))
  (around [example]
    (binding [wiki-service testing-service]
      (example)))

  (context "with the FrontPage article"
    (with front-page (make-article "FrontPage" "front page content"))
    (before
      (post-article (ws :repository) @front-page))

    (it "handles GET /"
      (let [res (app (request :get "/"))]
        (should= (res :status) 200)
        (should-be-full-rendered res (screen/article @front-page)))))

  (context "with two articles"
    (with foo-page (make-article "FooPage" "some content"))
    (with bar-page (make-article "FooPage" "some content"))
    (before
      (post-article (ws :repository) @foo-page)
      (post-article (ws :repository) @bar-page))

    (it "handles GET /w/FooPage"
      (let [res (app (request :get "/w/FooPage"))]
        (should= (res :status) 200)
        (should-be-full-rendered res (screen/article @foo-page))))))
