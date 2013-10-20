(ns wikirick2.screen
  (:use hiccup.core
        wikirick2.types)
  (:require [hiccup.page :as page]))

(defn base [fragment service]
  (let [urlm (.url-mapper service)
        config (.config service)]
    (->Template
     (.title fragment)
     [:html
      [:head
       [:title (h (config :site-title))]
       [:link {:rel "stylesheet" :type "text/css" :href (theme-path urlm)}]]
      [:body
       [:div#container
        [:div#wrapper
         [:div.content
          [:nav
           [:p {:class "locator"} "Top > " [:em {:class "selected"} (h (.title fragment))]]
           [:ul
            [:li {:class "selected"} "Article"]
            [:li "Source"]
            [:li "Edit"]
            [:li "History"]]]
          (.body fragment)]
         [:aside
          [:header {:id "main-title"} [:h1 (h (config :site-title))]]
          [:section#search
           [:form
            [:input {:class "search-box" :type "text" :placeholder "Search"}]]]
          [:section
           [:h2 "Recent Updates"]]]]
        [:footer "Powered by Clojure Programming Language"]]]])))

(defn article [article-]
  (->Template
   (.title article-)
   [:article
    [:header [:h1 (.title article-)]]
    [:p (.source article-)]]))

(deftype Screen [service]
  IScreen
  (render-full [self template]
    (page/html5 (.body (base template service))))

  (render-fragment [self template]
    (html (.body template))))
