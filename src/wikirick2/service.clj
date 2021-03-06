(ns wikirick2.service
  (:require [wikirick2.page-storage :as page-storage]
            [wikirick2.screen :as screen]
            [wikirick2.url-mapper :as url-mapper]
            [wikirick2.wiki-parser :as wiki-parser]
            [wikirick2.types :refer :all]))

(def ^:dynamic *wiki-service* nil)

(defn wrap-with-wiki-service [app service]
  (fn [req]
    (binding [*wiki-service* service]
      (app req))))

(defmacro with-wiki-service [& forms]
  (list* `let [{'storage :storage
                'screen :screen
                'url-mapper :url-mapper} `*wiki-service*]
         forms))

(defn make-wiki-service [config]
  (let [storage (page-storage/create-page-storage (config :page-storage-dir)
                                                  {:classname "org.sqlite.JDBC"
                                                   :subprotocol "sqlite"
                                                   :subname (config :sqlite-path)})
        urlm (url-mapper/->URLMapper (config :base-path))
        render-source (wiki-parser/make-wiki-source-renderer #(page-path urlm %))
        render-page (screen/make-page-renderer render-source)
        screen (screen/->Screen storage urlm render-page config)]
    (map->WikiService {:config config
                       :storage storage
                       :url-mapper urlm
                       :screen screen})))
