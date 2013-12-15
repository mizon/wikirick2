(ns wikirick2.main
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as jetty]
            [wikirick2.handler :refer :all]
            [wikirick2.service :refer :all]
            [wikirick2.types :refer :all]))

(def wikirick-config (load-file "./wikirick-config.clj"))

(defn- initialize-service []
  (let [service (make-wiki-service wikirick-config)
        storage (.storage service)]
    (if (not (has-page? storage "Front Page"))
      (let [front-page (new-page storage "Front Page")
            front-page (assoc front-page :source "Welcome to Wikirick2 Wiki Engine!")]
        (save-page front-page)))
    (if (not (has-page? storage "Sidebar"))
      (let [sidebar (new-page storage "Sidebar")
            sidebar (assoc sidebar :source "Menu
----
You can edit here.
")]
        (save-page sidebar)))
    service))

(defroutes application
  (wrap-with-wiki-service (handler/site wikirick-routes) (initialize-service)))

(defn -main []
  (jetty/run-jetty application {:port (wikirick-config :production-port)}))
