(ns wikirick2.screen
  (:use hiccup.core
        wikirick2.types)
  (:require [hiccup.page :as page]))

(defn base [fragment service]
  (->Template (.title fragment)
    (let [url-mapper (.url-mapper service)
          config (.config service)]
      [:html
       [:head
        [:link {:href (css-path url-mapper) :type "text-css" :rel "stylesheet"}]
        [:title (config :site-title)]]])))

(defn article [article-]
  (->Template (.title article-)
    [:h3 (.source article-)]))

(deftype Screen [service]
  IScreen
  (render-full [self template]
    (page/html5 (.body (base template service))))

  (render-fragment [self template]
    (html (.body template))))
