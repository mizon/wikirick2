(ns wikirick2.service-test
  (:use clojure.test
        wikirick2.types)
  (:require [wikirick2.service :as service]))

(deftest wrap-with-wiki-service
  (testing "wraps in a service"
    (let [app (fn [req]
                (is (= req :dummy-request))
                (is (= service/*wiki-service* :dummy-service))
                :dummy-response)
          wrapped-app (service/wrap-with-wiki-service app :dummy-service)]
      (is (= (wrapped-app :dummy-request) :dummy-response)))))

(deftest with-wiki-service
  (testing "binds the service components"
    (binding [service/*wiki-service* (map->WikiService {:config :dummy-config
                                                        :storage :dummy-page-storage
                                                        :url-mapper :dummy-url-mapper
                                                        :screen :dummy-screen})]
      (service/with-wiki-service
        (is (= storage :dummy-page-storage))
        (is (= screen :dummy-screen))
        (is (= url-mapper :dummy-url-mapper))))))
