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

(def repo (.repository testing-service))

(describe "application handler"
  (before
    (setup-test-repo))
  (after
    (cleanup-test-repo))
  (around [example]
    (binding [wiki-service testing-service]
      (example)))

  (context "with the FrontPage page"
    (with front-page (new-page repo "FrontPage" "front page content"))
    (before
      (save-page @front-page))

    (it "handles GET /"
      (let [res (app (request :get "/"))]
        (should= (res :status) 200)
        (should-be-full-rendered res (screen/page @front-page)))))

  (context "with two pages"
    (with foo-page (new-page repo "FooPage" "some content"))
    (with bar-page (new-page repo "FooPage" "some content"))
    (before
      (save-page @foo-page)
      (save-page @bar-page))

    (it "handles GET /w/FooPage"
      (let [res (app (request :get "/w/FooPage"))]
        (should= (res :status) 200)
        (should-be-full-rendered res (screen/page @foo-page))))))
