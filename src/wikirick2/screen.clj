(ns wikirick2.screen
  (:use hiccup.core
        wikirick2.parsers
        wikirick2.types)
  (:require [hiccup.page :as page]))

(defn- navigation [& items]
  `[:nav [:ul ~@items]])

(defn- nav-item [screen page action-name enabled? selected?]
  (let [url-mapper (-> screen .service .url-mapper)]
    (cond selected? [:li {:class "selected"} action-name]
          enabled? [:li [:a {:href (page-action-path url-mapper (.title page) action-name)} action-name]]
          :else [:li action-name])))

(defn- page-info [page]
  [:p
   {:class "article-info"}
   [:em (h (.title page))]
   ": Last modified: "
   "2013/10/20 19:30"])

(defn- base-view [screen title content]
  (let [url-mapper (-> screen .service .url-mapper)
        config (-> screen .service .config)
        repo (-> screen .service .repository)]
    (page/html5 [:head
                 [:meta {:charset "UTF-8"}]
                 [:title (h (config :site-title))]
                 [:link {:rel "stylesheet" :type "text/css" :href (h (theme-path url-mapper))}]]
                [:body
                 [:div#container
                  [:aside
                   [:header {:id "main-title"} [:h1 (h (format "%s - %s" title (config :site-title)))]]
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

(deftype Screen [service render-wiki-source]
  IScreen
  (read-view [self page]
    (base-view self
               (.title page)
               [(navigation (nav-item self page "Read" true true)
                            (nav-item self page "Source" false false)
                            (nav-item self page "Edit" true false)
                            (nav-item self page "History" false false))
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (.title page))]]
                  ~@(render-wiki-source (.source page))
                  [:h2 "Related Pages"
                   [:ul ~@(map #(title-to-li (.url-mapper service) %)
                               (referred-titles page))]]]]))

  (edit-view [self page]
    (base-view self
               (.title page)
               [(navigation (nav-item self page "Read" true false)
                            (nav-item self page "Source" false false)
                            (nav-item self page "Edit" true true)
                            (nav-item self page "History" false false))
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (format "Edit: %s" (.title page)))]]
                  [:textarea ~(h (.source page))]
                  [:button {:type "submit"} "Submit"]]]))

  (render-full [self template]
    (page/html5 (.body (base template service))))

  (render-fragment [self template]
    (html (.body template))))

(defn make-screen [service]
  (let [url-mapper (.url-mapper service)]
    (->Screen service (make-wiki-source-renderer #(page-path url-mapper %)))))
