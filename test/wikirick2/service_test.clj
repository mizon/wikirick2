(ns wikirick2.service-test
  (:use clojure.test)
  (:require [wikirick2.service :as service]))

(deftest wrap-with-wiki-service
  (testing "wraps in a service"
    (let [app (fn [req]
                (is (= req :dummy-request))
                (is (= service/wiki-service :dummy-service))
                :dummy-response)
          wrapped-app (service/wrap-with-wiki-service app :dummy-service)]
      (is (= (wrapped-app :dummy-request) :dummy-response)))))
