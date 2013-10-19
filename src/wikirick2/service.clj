(ns wikirick2.service
  (:use wikirick2.types)
  (:require [wikirick2.repository :as repository]
            [wikirick2.screen :as screen]
            [wikirick2.url-mapper :as url-mapper]))

(deftype WikiService [config]
  IService
  (get-config [self]
    config)

  (get-repository [self]
    (repository/create-repository (config :repository-dir)))

  (get-url-mapper [self]
    (url-mapper/->URLMapper (config :base-path)))

  (get-screen [self]
    (screen/->Screen self)))

(def ^:dynamic wiki-service nil)

(defn wrap-with-service [app service]
  (fn [req]
    (binding [wiki-service service]
      (app req))))

(defn repository []
  (get-repository wiki-service))

(defn url-mapper []
  (get-url-mapper wiki-service))

(defn screen []
  (get-screen wiki-service))
