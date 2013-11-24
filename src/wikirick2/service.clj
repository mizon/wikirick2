(ns wikirick2.service
  (:use wikirick2.types)
  (:require [wikirick2.parsers :as parsers]
            [wikirick2.repository :as repository]
            [wikirick2.screen :as screen]
            [wikirick2.url-mapper :as url-mapper]))

(def ^:dynamic *wiki-service* nil)

(defn wrap-with-wiki-service [app service]
  (fn [req]
    (binding [*wiki-service* service]
      (app req))))

(defmacro with-wiki-service [& forms]
  `(let [~'repository (.repository *wiki-service*)
         ~'screen (.screen *wiki-service*)
         ~'url-mapper (.url-mapper *wiki-service*)]
     ~@forms))

(defn make-wiki-service [config]
  (let [repo (repository/create-repository (config :repository-dir)
                                           {:classname "org.sqlite.JDBC"
                                            :subprotocol "sqlite"
                                            :subname (config :sqlite-path)})
        urlm (url-mapper/->URLMapper (config :base-path))
        screen (screen/->Screen repo
                                urlm
                                (parsers/make-wiki-source-renderer #(page-path urlm %))
                                config)]
    (map->WikiService {:config config :repository repo :url-mapper urlm :screen screen})))
