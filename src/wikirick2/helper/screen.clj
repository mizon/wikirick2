(ns wikirick2.helper.screen
  (:use hiccup.core
        wikirick2.parsers
        wikirick2.types)
  (:require [hiccup.page :as page]))

(defn- nav-item [screen action-name action-path spec]
  (let [url-mapper (.url-mapper screen)
        key (-> action-name .toLowerCase keyword)]
    (cond (-> spec key :selected?) [:li {:class "selected"} action-name]
          (-> spec key :enabled?) [:li [:a {:href action-path} action-name]]
          :else [:li action-name])))

(defn navigation [screen page spec]
  (let [urls (.url-mapper screen)]
    `[:nav
      [:ul
       ~(nav-item screen "Read" (page-path urls (.title page)) spec)
       ~(nav-item screen "Source" (page-action-path urls (.title page) "source") spec)
       ~(nav-item screen "Edit" (page-action-path urls (.title page) "edit") spec)
       ~(nav-item screen "History" (page-action-path urls (.title page) "history") spec)]]))

(defn page-info [page]
  [:p
   {:class "article-info"}
   [:em (h (.title page))]
   ": Last modified: "
   "2013/10/20 19:30"])

(defn base-view [screen title content]
  (let [url-mapper (.url-mapper screen)
        config (.config screen)]
    (page/html5 [:head
                 [:meta {:charset "UTF-8"}]
                 [:title (h (format "%s - %s" title (config :site-title)))]
                 [:link {:rel "stylesheet" :type "text/css" :href (h (theme-path url-mapper))}]]
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
                   `[:div.content
                     ~@content]]
                  [:footer "Made with Clojure Programming Language"]]])))

(defn title-to-li [screen title]
  (let [url-mapper (.url-mapper screen)]
    `[:li [:a {:href ~(page-path url-mapper title)} ~(h title)]]))
