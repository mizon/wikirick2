(ns wikirick2.service
  (:use wikirick2.types)
  (:require [wikirick2.repository :as repository]
            [wikirick2.screen :as screen]
            [wikirick2.url-mapper :as url-mapper]))

(def ^:dynamic wiki-service nil)

(defn ws [key]
  (key wiki-service))

(defn wrap-with-wiki-service [app service]
  (fn [req]
    (binding [wiki-service service]
      (app req))))

(defn make-wiki-service [config]
  (let [service
        (map->WikiService
         {:config config
          :repository (repository/create-repository (config :repository-dir))
          :url-mapper (url-mapper/->URLMapper (config :base-path))})]
    (assoc service :screen (screen/->Screen service))))
