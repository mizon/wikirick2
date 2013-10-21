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
        [:aside
         [:header {:id "main-title"} [:h1 (h (config :site-title))]]
         [:section#search
          [:form
           [:input {:class "text-box" :type "text"}]
           [:button {:class "submit-button" :type "submit"} "Search"]]]
         [:section
          [:h2 "Recent Updates"]]]
        [:div#wrapper
         [:div.content
          [:nav
           [:ul
            [:li {:class "selected"} "Read"]
            [:li "Source"]
            [:li "Edit"]
            [:li "History"]]]
          [:p {:class "article-info"} [:em {:class "selected"} (h (.title fragment))] ": Last modified: " "2013/10/20 19:30"]
          (.body fragment)]]
        [:footer "Powered by Clojure Programming Language"]]]])))

(defn page [page-]
  (->Template
   (.title page-)
   [:article
    [:header [:h1 (.title page-)]]
    [:p (.source page-)]]))

(deftype Screen [service]
  IScreen
  (render-full [self template]
    (page/html5 (.body (base template service))))

  (render-fragment [self template]
    (html (.body template))))
