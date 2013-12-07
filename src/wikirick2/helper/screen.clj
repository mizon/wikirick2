(ns wikirick2.helper.screen
  (:require [clj-time.format :as format]
            [hiccup.core :refer :all]
            [hiccup.page :as page]
            [wikirick2.types :refer :all]
            [wikirick2.wiki-parser :refer :all]))

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

(defn show-modified-at [page revision]
  (format/unparse (format/formatter "yyyy/MM/dd HH:mm") (modified-at page revision)))

(defn show-date [date]
  (format/unparse (format/formatter "yyyy/MM/dd HH:mm") date))

(defn show-revision [page revision]
  (if (latest-revision? page revision)
    (format "rev%s[Latest]" revision)
    (format "rev%s" revision)))

(defn page-info [page]
  [:p
   {:class "page-info"}
   [:em (h (.title page))]
   ": Last modified: "
   (show-modified-at page nil)])

(defn search-box [screen default-word]
  [:section.search
   [:form
    {:method "get" :action (search-path (.url-mapper screen))}
    [:input {:type "text" :name "word" :value default-word}]
    [:button {:type "submit"} "Search"]]])

(defn recent-changes [screen]
  (let [pages (select-recent-pages (.storage screen) 10)]
    [:section
     [:h2 "Recent Changes"]
     [:ol
      (for [p pages]
        [:li [:a {:href (page-path (.url-mapper screen) (.title p))} (h (.title p))]])]]))

(defn base-view [screen title content]
  (let [url-mapper (.url-mapper screen)
        config (.config screen)]
    (page/html5 [:head
                 [:meta {:charset "UTF-8"}]
                 [:title (h (format "%s - %s" title (config :site-title)))]
                 [:link {:rel "stylesheet"
                         :type "text/css"
                         :href (h (theme-path url-mapper))}]]
                [:body
                 [:div#container
                  [:div#wrapper
                   `[:div.content
                     ~@content]]
                  [:aside
                   [:header [:h1 [:a {:href (index-path url-mapper)} (h (config :site-title))]]]
                   (search-box screen "")
                   (recent-changes screen)]
                  [:footer "Made with Clojure Programming Language"]]])))

(defn title-to-li [screen title]
  (let [url-mapper (.url-mapper screen)]
    `[:li [:a {:href ~(page-path url-mapper title)} ~(h title)]]))

(defn search-line [screen [title content] line-no]
  [:tr {:class (if (odd? line-no) "odd" "even")}
   [:td {:class "title"} [:a {:href (page-path (.url-mapper screen) title)} (h title)]]
   [:td {:class "line"} (h content)]])

(defn history-line [screen page history line-no]
  [:tr {:class (if (odd? line-no) "odd" "even")}
   [:td (h (show-date (history :date)))]
   [:td [:a {:href (page-revision-path (.url-mapper screen) (.title page) (history :revision))
             :class "revision"}
         (history :revision)]]
   `[:td
     ~@(if-let [lines (history :lines)]
         [[:em {:class (if (> (lines :added) 0) "added" "zero")}
           (str "+" (lines :added))]
          " "
          [:em {:class (if (> (lines :deleted) 0) "deleted" "zero")}
           (str "-" (lines :deleted))]])]
   [:td
    (if (latest-revision? page (history :revision))
      "Latest"
      [:a {:href (page-diff-path (.url-mapper screen)
                                 (.title page)
                                 (history :revision)
                                 (latest-revision page))}
       "Latest"])
    " | "
    (if (> (history :revision) 1)
      [:a {:href (page-diff-path (.url-mapper screen)
                               (.title page)
                               (dec (history :revision))
                               (history :revision))}
       "Previous"]
      "Previous")]])
