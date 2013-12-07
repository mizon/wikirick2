(ns wikirick2.helper.screen
  (:require [clj-time.format :as format]
            [hiccup.core :refer :all]
            [hiccup.page :as page]
            [wikirick2.types :refer :all]
            [wikirick2.wiki-parser :refer :all]))

(defn navigation [screen page selected]
  (let [urls (.url-mapper screen)
        selected? #(= % selected)]
    [:nav
     [:ul
      (cond (selected? :read) [:li {:class "selected"} "Read"]
            (page-exists? page) [:li [:a {:href (page-path urls (.title page))} "Read"]]
            :else [:li "Read"])
      (cond (selected? :edit) [:li {:class "selected"} "Edit"]
            (page-exists? page) [:li
                                 [:a {:href (page-action-path urls (.title page) "edit")}
                                  "Edit"]]
            :else [:li "Edit"])
      (cond (selected? :diff) [:li {:class "selected"} "Diff"]
            (and (page-exists? page)
                 (not= (latest-revision page) 1))
            [:li
             [:a {:href (page-diff-path urls
                                        (.title page)
                                        (dec (latest-revision page))
                                        (latest-revision page))}
              "Diff"]]
            :else [:li "Diff"])
      (cond (selected? :history) [:li {:class "selected"} "History"]
            (page-exists? page) [:li [:a {:href (page-action-path urls (.title page) "history")}
                                      "History"]]
            :else [:li "History"])]]))

(defn all-disabled-navigation [screen]
  [:nav [:ul [:li "Read"] [:li "Edit"] [:li "Diff"] [:li "History"]]])

(defn show-modified-at [page revision]
  (format/unparse (format/formatter "yyyy/MM/dd HH:mm") (modified-at page revision)))

(defn show-date [date]
  (format/unparse (format/formatter "yyyy/MM/dd HH:mm") date))

(defn show-revision [page revision]
  (if (latest-revision? page revision)
    (format "Latest" revision)
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
     ~(if-let [lines (history :lines)]
        [:a {:class "changes"
             :href (page-diff-path (.url-mapper screen)
                                   (.title page)
                                   (dec (history :revision))
                                   (history :revision))}
         [:em {:class (if (> (lines :deleted) 0) "deleted" "zero")}
          (str "-" (lines :deleted))]
         " "
         [:em {:class (if (> (lines :added) 0) "added" "zero")}
          (str "+" (lines :added))]])]
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
