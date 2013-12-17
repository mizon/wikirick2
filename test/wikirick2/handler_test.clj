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
  (save-page (create-page storage "FooPage" "some content") nil)
  (save-page (assoc (select-page storage "FooPage")
               :source "foo content")
             nil)
  (save-page (create-page storage "BarPage" "some content") nil)
  (save-page (create-page storage "Front Page" "front page content") nil)
  (save-page (create-page storage "Sidebar" "## Sidebar") nil)

  (testing "handles GET /"
    (with-wiki-service
      (let [res (app (request :get "/"))]
        (is (= (res :status) 200))
        (is (= (res :body)
               (read-view screen (select-page storage "Front Page") nil))))))

  (let [foo-page (select-page storage "FooPage")
        bar-page (select-page storage "BarPage")]
    (testing "handles GET /FooPage"
      (with-wiki-service
        (let [res (app (request :get "/FooPage"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (read-view screen (select-page storage "FooPage") nil))))))

    (testing "handles GET /FooPage?rev=1"
      (with-wiki-service
        (let [res (app (request :get "/FooPage?rev=1"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (read-view screen (select-page storage "FooPage") 1))))))

    (testing "handles GET /FooPage/edit"
      (with-wiki-service
        (testing "shows edit view"
          (let [res (app (request :get "/FooPage/edit"))]
            (is (= (res :status) 200))
            (is (= (res :body)
                   (edit-view screen
                              (select-page storage "FooPage")
                              [])))))

        (testing "shows new view"
          (let [res (app (request :get "/NewPage/edit"))]
            (is (= (res :status) 200))
            (is (= (res :body)
                   (new-view screen
                             (create-page storage "NewPage" "new content")
                             [])))))))

    (testing "redirects when GET /SomePage"
      (let [res (app (request :get "/SomePage"))]
        (is (= (res :status) 302))
        (is (= (-> res :headers (get "Location")) "/SomePage/edit"))))

    (testing "handles POST /FooPage/edit"
      (with-wiki-service
        (testing "opens the preview view"
          (let [foo-page (create-page storage "FooPage" "foo content")
                res (app (-> (request :post "/FooPage/edit"
                                      {:source (page-source foo-page nil)
                                       :preview true})
                             (header "referer" "/FooPage/edit")))]
            (is (= (res :status) 200))
            (is (= (res :body) (preview-view screen foo-page)))))

        (testing "register the posted page"
          (let [page-content "some content\n"
                res (app (-> (request :post "/FooPage/edit"
                                      {:source page-content})
                             (header "referer" "/FooPage/edit")))]
            (is (= (res :status) 303))
            (let [foo-page (select-page storage "FooPage")]
              (is (= (page-source foo-page nil) page-content))
              (is (= (-> res :headers (get "Location")) "/FooPage")))))

        (testing "denies requests without editor referer"
          (let [res (app (request :post
                                  "/FooPage/edit"
                                  {:source "foo content"}))]
            (is (= (res :status) 404))
            (is (= (res :body) "Not Found"))))

        (testing "reopen the editor if posted unchanged source"
          (let [_ (app (-> (request :post "/FooPage/edit" {:source "foo content"})
                           (header "referer" "/FooPage/edit")))
                res (app (-> (request :post "/FooPage/edit" {:source "foo content"})
                             (header "referer" "/FooPage/edit")))]
            (is (= (res :status) 200))
            (is (= (res :body) (edit-view screen
                                          (create-page storage "FooPage" "foo content")
                                          ["Source is unchanged."])))))

        (testing "reopen the editor if posted empty source"
          (let [res (app (-> (request :post "/FooPage/edit" {:source "  "})
                             (header "referer" "/FooPage/edit")))]
            (is (= (res :status) 200))
            (is (= (res :body) (edit-view screen
                                          (create-page storage "FooPage" "  ")
                                          ["Source is empty."])))))))

    (testing "handles GET /FooPage/diff/from-to"
      (with-wiki-service
        (let [res (app (request :get "/FooPage/diff/1-2"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (diff-view screen foo-page 1 2))))))

    (testing "handles GET /FooPage/history"
      (with-wiki-service
        (let [res (app (request :get "/FooPage/history"))]
          (is (= (res :status) 200))
          (is (= (res :body)
                 (history-view screen foo-page))))))

    (testing "redirects when GET /SomePage/history"
      (let [res (app (request :get "/SomePage/history"))]
        (is (= (res :status) 302))
        (is (= (-> res :headers (get "Location")) "/SomePage/edit"))))

    (testing "handles GET /search"
      (testing "shows search results"
        (with-wiki-service
          (let [res (app (request :get "/search" {:word "some"}))]
            (is (= (res :status) 200))
            (is (= (res :body)
                   (search-view screen
                                "some"
                                (search-pages storage "some")))))))

      (testing "opens an editor for the 'search' wiki page"
        (with-wiki-service
          (let [res (app (request :get "/search"))]
            (is (= (res :status) 302))
            (is (= (-> res :headers (get "Location")) "/search/edit"))))))

    (testing "handles invalid title requests"
      (let [res (app (request :get "/Foo%20%20Bar"))]
        (is (= (res :status) 404))
        (is (= (res :body) "Not Found"))))))
