(ns wikirick2.screen
  (:require [clojure.string :as string]
            [hiccup.core :refer :all]
            [hiccup.page :as page]
            [wikirick2.helper.screen :refer :all]
            [wikirick2.types :refer :all]))

(deftype Screen [storage url-mapper renderer config]
  IScreen
  (read-view [self page revision]
    (base-view self
               (.title page)
               [(navigation self page :read)
                (page-info page)
                `[:article
                  {:class "read"}
                  [:header
                   [:h1 ~(h (.title page))
                    ~(when revision
                       [:em {:class "old-revision"}
                        (h (format ": Revision %s" revision))])]]
                  ~@(renderer page revision)
                  ~@(if (not revision)
                      [[:h2 "Related Pages"]
                       `[:ul ~@(map #(title-to-li self %) (referred-titles page))]]
                      [])]]))

  (new-view [self page]
    (base-view self
               (.title page)
               [(navigation self page :edit)
                [:p {:class "page-info"} [:em (h (.title page))] ": (new page)"]
                `[:article
                  [:header [:h1 ~(h (format "%s: New" (.title page)))]]
                  [:section
                   {:class "edit"}
                   [:textarea {:placeholder ~(page-source page nil)}]
                   [:button {:type "submit"} "Preview"]
                   [:button {:type "submit"} "Submit"]]]]))

  (edit-view [self page]
    (base-view self
               (.title page)
               [(navigation self page :edit)
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (format "%s: Edit" (.title page)))]]
                  [:form {:class "edit"
                          :method "post"
                          :action ~(page-action-path url-mapper (.title page) "edit")}
                   [:input {:type "hidden"
                            :name "base-rev"
                            :value ~(latest-revision page)}]
                   [:textarea {:name "source"} ~(h (page-source page nil))]
                   [:button {:type "submit"} "Preview"]
                   [:button {:type "submit"} "Submit"]]]]))

  (diff-view [self page from-rev to-rev]
    (let [diff-lines (string/split-lines (diff-revisions page from-rev to-rev))
          colorlize-line #(cond (re-find #"\A@@" %) [:em {:class "section-header"} (h %)]
                                (re-find #"\A\+" %) [:em {:class "added"} (h %)]
                                (re-find #"\A-" %) [:em {:class "removed"} (h %)]
                                :else (h %))
          diff-result (map colorlize-line
                           (list* (format "--- %s %s %s"
                                          (.title page)
                                          (show-modified-at page from-rev)
                                          (show-revision page
                                                         from-rev))
                                  (format "+++ %s %s %s"
                                          (.title page)
                                          (show-modified-at page to-rev)
                                          (show-revision page
                                                         to-rev))
                                  diff-lines))]
      (base-view self
                 (.title page)
                 [(navigation self page :diff)
                  (page-info page)
                  `[:article
                    {:class "diff"}
                    [:header [:h1 ~(h (format "%s: Diff" (.title page)))]]
                    [:h2 ~(h (format "Changes between %s and %s"
                                     (show-revision page from-rev)
                                     (show-revision page to-rev)))]
                    [:pre
                     ~@(interpose "\n" diff-result)]]])))

  (history-view [self page]
    (base-view self
               (.title page)
               [(navigation self page :history)
                (page-info page)
                `[:article
                  {:class "history"}
                  [:header [:h1 ~(h (format "%s: History" (.title page)))]]
                  [:table {:class "tabular"}
                   [:tr [:th "Timestamp"] [:th "Revision"] [:th "Changes"] [:th "Diff to"]]
                   ~@(map #(history-line self page % %2) (page-history page) (range))]]]))

  (search-view [self word result]
    (base-view self
               "Search"
               [(all-disabled-navigation self)
                [:p {:class "page-info"} [:em "Search"] ": (special page)"]
                `[:article
                  {:class "search"}
                  [:header [:h1 "Search"]]
                  ~(search-box self word)
                  [:table {:class "tabular"}
                   [:tr [:th {:class "title"} "Title"] [:th {:class "line"} "Line"]]
                   ~@(map #(search-line self % %2) result (range))]]])))

(defn cached-page-renderer [render-f]
  (let [cache (ref {})]
    (letfn [(do-render [page revision]
              (let [key (str (.title page) "/" (or revision (latest-revision page)))]
                (or (@cache key)
                    (let [rendered (render-f (page-source page revision))]
                      (dosync (alter cache #(assoc % key rendered)))
                      (do-render page revision)))))]
      do-render)))
