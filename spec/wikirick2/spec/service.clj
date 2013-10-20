(ns wikirick2.spec.service
  (:use speclj.core
        wikirick2.service))

(describe "wiki service"
  (describe "wrap-with-wiki-service"
    (it "wraps in a service"
      (let [app (fn [req]
                  (should= :dummy-request req)
                  (should= :dummy-service wiki-service)
                  :dummy-response)
            wrapped-app (wrap-with-wiki-service app :dummy-service)]
        (should= :dummy-response (wrapped-app :dummy-request))))))
