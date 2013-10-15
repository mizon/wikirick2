(ns wikirick2.spec.handler
  (:use speclj.core
        ring.mock.request
        wikirick2.handler))

(describe "application handler"
  (it "handles GET /"
    (let [res (app (request :get "/"))]
      (should= (:status res) 200)
      (should= (:body res) "<h1>Hello World</h1>")))

  (it "handles invalid requests"
    (let [res (app (request :get "/invalid"))]
      (should= (:status res) 404))))
