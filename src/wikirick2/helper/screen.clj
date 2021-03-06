(ns wikirick2.helper.screen
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [hiccup.core :refer :all]
            [hiccup.page :as page]
            [wikirick2.types :refer :all]
            [wikirick2.wiki-parser :refer :all])
  (:import org.joda.time.DateTime))

(defn navigation [screen page selected]
  (let [urls (.url-mapper screen)
        selected? #(= % selected)]
    [:nav.page-actions
     [:ul
      (cond (selected? :read) [:li.selected "Read"]
            (new-page? page) [:li "Read"]
            :else [:li [:a {:href (page-path urls (.title page))} "Read"]])
      (cond (selected? :edit) [:li.selected "Edit"]
            (new-page? page) [:li "Edit"]
            :else [:li
                   [:a {:href (page-action-path urls (.title page) "edit")}
                    "Edit"]])
      (cond (selected? :diff) [:li.selected "Diff"]
            (or (new-page? page) (= (latest-revision page) 1)) [:li "Diff"]
            :else [:li
                   [:a {:href (page-diff-path urls
                                              (.title page)
                                              (dec (latest-revision page))
                                              (latest-revision page))}
                    "Diff"]])
      (cond (selected? :history) [:li.selected "History"]
            (new-page? page) [:li "History"]
            :else [:li [:a {:href (page-action-path urls (.title page) "history")}
                        "History"]])]]))

(defn all-disabled-navigation [screen]
  [:nav.page-actions [:ul [:li "Read"] [:li "Edit"] [:li "Diff"] [:li "History"]]])

(defn show-date [date]
  (format/unparse (format/formatter "yyyy/MM/dd HH:mm z") date))

(defn show-modified-at [page revision]
  (show-date (modified-at page revision)))

(defn show-revision [page revision]
  (if (latest-revision? page revision)
    (format "Latest" revision)
    (format "rev%s" revision)))

(defn page-info [screen page]
  `[:p.page-info
    ~[:em [:a {:href (page-path (.url-mapper screen) (.title page))} (h (.title page))]]
    ~@(if (not (new-page? page))
        [": Last modified: " (show-modified-at page nil)]
        [": (new page)"])])

(defn special-page-info [title message]
  [:p.page-info
   [:em (h title)] (h (format ": (%s)" message))])

(defn search-box [screen default-word]
  [:section.search
   [:form
    {:method "get" :action (search-path (.url-mapper screen))}
    [:input {:type "text" :name "word" :value default-word}]
    [:button {:type "submit"} "Search"]]])

(defn editable-sidebar [screen]
  (let [render-page (.render-page screen)
        storage (.storage screen)
        url-mapper (.url-mapper screen)]
    `[:section
      ~@(render-page (select-page storage "Sidebar") nil true)
      [:p "[" [:a {:href ~(page-action-path url-mapper "Sidebar" "edit")} "Edit"] "]"]]))

(defn last-modified-digest [page]
  (let [i (time/interval (modified-at page nil) (DateTime/now))]
    (cond (zero? (time/in-days i)) "today"
          (zero? (time/in-months i)) (format "%sd" (time/in-days i))
          (zero? (time/in-years i)) (format "%sm" (time/in-months i))
          :else (format "%sy" (time/in-years i)))))

(defn recent-changes [screen]
  (let [pages (select-recent-pages (.storage screen) 10)]
    [:section
     [:h2 "Recent Changes"]
     [:ul
      (for [p pages]
        [:li
         [:a {:href (page-path (.url-mapper screen) (.title p))} (h (.title p))]
         (h (format " - %s" (last-modified-digest p)))])]]))

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
                   (editable-sidebar screen)
                   (recent-changes screen)]
                  [:footer ((.config screen) :footer-message)]]])))

(defn error-notification [screen messages]
  `[:ul.errors ~@(map (fn [message]
                        [:li (h message)])
                      messages)])

(defn editor-form [screen page placeholder source base-revision]
  [:form {:method "post"
          :action (page-action-path (.url-mapper screen)
                                    (.title page)
                                    "edit")}
   (if base-revision
     [:input {:name "base-rev" :type "hidden" :value base-revision}])
   [:textarea {:name "source" :placeholder placeholder} (h source)]
   [:button {:name "preview" :type "submit"} "Preview"]
   [:button {:type "submit"} "Submit"]])

(defn page-editor [screen page new-or-edit placeholder source base-revision errors]
  (let [url-mapper (.url-mapper screen)]
    (base-view screen
               (.title page)
               [(navigation screen page :edit)
                (page-info screen page)
                [:article.edit
                 [:header [:h1 (h (format "%s: %s" (.title page) new-or-edit))]]
                 (when (not (empty? errors))
                   [:ul.errors
                    (for [e errors]
                      [:li (h e)])])
                 (editor-form screen page placeholder source base-revision)]])))

(defn title-to-li [screen title]
  (let [url-mapper (.url-mapper screen)]
    `[:li [:a {:href ~(page-path url-mapper title)} ~(h title)]]))

(defn search-line [screen [title content] line-no]
  [:tr {:class (if (odd? line-no) "odd" "even")}
   [:td.title [:a {:href (page-path (.url-mapper screen) title)} (h title)]]
   [:td.line (h content)]])

(defn history-line [screen page history line-no]
  [:tr {:class (if (odd? line-no) "odd" "even")}
   [:td (h (show-date (history :date)))]
   [:td [:a.revision {:href (page-revision-path (.url-mapper screen)
                                                (.title page)
                                                (history :revision))}
         (history :revision)]]
   [:td
    (if-let [lines (history :lines)]
      [:a.changes {:href (page-diff-path (.url-mapper screen)
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
