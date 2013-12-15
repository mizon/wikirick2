(ns wikirick2.handler-test
  (:require [clojure.java.shell :as shell]
            [clojure.test :refer :all]
            [compojure.core :as compojure]
            [compojure.handler :as handler]
            [conjure.core :refer :all]
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
  (save-page (create-page storage "FooPage" "some content"))
  (save-page (assoc (select-page storage "FooPage")
               :source "foo content"))
  (save-page (create-page storage "BarPage" "some content"))
  (save-page (create-page storage "FrontPage" "front page content"))
  (save-page (create-page storage "Sidebar" "## Sidebar"))

  (testing "handles GET /"
    (with-wiki-service
      (let [res (app (request :get "/"))]
        (is (= (res :status) 200))
        (is (= (res :body)
               (read-view screen (select-page storage "FrontPage") nil))))))

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
                           (assoc (new-page storage "SomePage") :source "new content")
                           []))))))

    (testing "handles GET /w/FooPage/edit"
      (with-wiki-service
        (let [res (app (request :get "/w/FooPage/edit"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (edit-view screen
                            (select-page storage "FooPage")
                            []))))))

    (testing "redirects when GET /w/SomePage"
      (let [res (app (request :get "/w/SomePage"))]
        (is (= (res :status) 302))
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "redirects when GET /w/SomePage/edit"
      (let [res (app (request :get "/w/SomePage/edit"))]
        (is (= (res :status) 302))
        (is (= ((res :headers) "Location") "/w/SomePage/new"))))

    (testing "handles POST /w/FooPage/edit"
      (with-wiki-service
        (testing "opens the preview view"
          (let [foo-page (create-page storage "FooPage" "foo content")
                res (app (-> (request :post "/w/FooPage/edit"
                                      {:source (page-source foo-page nil)
                                       :preview true})
                             (header "referer" "/w/FooPage/edit")))]
            (is (= (res :status) 200))
            (is (= (res :body) (preview-view screen foo-page)))))

        (testing "register a posted page"
          (let [page-content "some content"
                res (app (-> (request :post "/w/FooPage/edit"
                                      {:source page-content})
                             (header "referer" "/w/FooPage/edit")))]
            (is (= (res :status) 303))
            (let [foo-page (select-page storage "FooPage")]
              (is (= (page-source foo-page nil) page-content))
              (is (= (-> (res :headers) (get "Location")) "/w/FooPage")))))

        (testing "denies requests without editor referer"
          (let [res (app (request :post
                                  "/w/FooPage/edit"
                                  {:source "foo content"}))]
            (is (= (res :status) 404))
            (is (= (res :body) "Not Found"))))

        (testing "reopen the editor if posted unchanged source"
          (let [_ (app (-> (request :post "/w/FooPage/edit" {:source "foo content"})
                           (header "referer" "/w/FooPage/edit")))
                res (app (-> (request :post "/w/FooPage/edit" {:source "foo content"})
                             (header "referer" "/w/FooPage/edit")))]
            (is (= (res :status) 200))
            (is (= (res :body) (edit-view screen
                                          (create-page storage "FooPage" "foo content")
                                          ["Source is unchanged."])))))))

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
                              (search-pages storage "some")))))))

    (testing "handles invalid title requests"
      (let [res (app (request :get "/w/Foo%20%20Bar"))]
        (is (= (res :status) 404))
        (is (= (res :body) "Not Found"))))))
