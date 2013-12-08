(ns wikirick2.screen
  (:require [clojure.string :as string]
            [hiccup.core :refer :all]
            [hiccup.page :as page]
            [wikirick2.helper.screen :refer :all]
            [wikirick2.types :refer :all]))

(deftype Screen [storage url-mapper render-page config]
  IScreen
  (read-view [self page revision]
    (base-view self
               (.title page)
               [(navigation self page :read)
                (page-info self page)
                `[:article.read
                  [:header
                   [:h1 ~(h (.title page))
                    ~(when revision
                       (h (format ": Revision %s" revision)))]]
                  ~@(render-page page revision)
                  ~(when (not (or revision (orphan-page? page)))
                     [:nav.related-pages
                      [:h2 "Related Pages:"]
                      `[:ul ~@(map #(title-to-li self %) (referred-titles page))]])]]))

  (new-view [self page]
    (page-editor self
                 page
                 "New"
                 (special-page-info (.title page) "new page")
                 (page-source page nil)
                 nil))

  (edit-view [self page]
    (page-editor self
                 page
                 "Edit"
                 (page-info self page)
                 nil
                 (page-source page nil)))

  (diff-view [self page from-rev to-rev]
    (let [diff-lines (string/split-lines (diff-revisions page from-rev to-rev))
          colorlize-line #(cond (re-find #"\A@@" %) [:em.section-header (h %)]
                                (re-find #"\A\+" %) [:em.added (h %)]
                                (re-find #"\A-" %) [:em.removed (h %)]
                                :else (h %))
          diff-result (map colorlize-line
                           (list* (format "--- %s %s %s"
                                          (.title page)
                                          (show-modified-at page from-rev)
                                          (show-revision page from-rev))
                                  (format "+++ %s %s %s"
                                          (.title page)
                                          (show-modified-at page to-rev)
                                          (show-revision page to-rev))
                                  diff-lines))]
      (base-view self
                 (.title page)
                 [(navigation self page :diff)
                  (page-info self page)
                  `[:article.diff
                    [:header [:h1 ~(h (format "%s: Diff" (.title page)))]]
                    [:h2
                     "Changes between "
                     ~[:a {:href (page-revision-path url-mapper (.title page) from-rev)}
                       (h (show-revision page from-rev))]
                     " and "
                     ~[:a {:href (page-revision-path url-mapper (.title page) to-rev)}
                       (h (show-revision page to-rev))]]
                    [:pre
                     ~@(interpose "\n" diff-result)]]])))

  (history-view [self page]
    (base-view self
               (.title page)
               [(navigation self page :history)
                (page-info self page)
                `[:article.history
                  [:header [:h1 ~(h (format "%s: History" (.title page)))]]
                  [:table.tabular
                   [:tr [:th "Timestamp"] [:th "Revision"] [:th "Changes"] [:th "Diff from"]]
                   ~@(map #(history-line self page % %2) (page-history page) (range))]]]))

  (search-view [self word result]
    (base-view self
               "Search"
               [(all-disabled-navigation self)
                (special-page-info "Search" "special page")
                [:p.page-info [:em "Search"] ": (special page)"]
                `[:article.search
                  [:header [:h1 "Search"]]
                  ~(search-box self word)
                  [:table.tabular
                   [:tr [:th.title "Title"] [:th.line "Line"]]
                   ~@(map #(search-line self % %2) result (range))]]])))

(defn cached-page-renderer [render-source]
  (let [cache (ref {})]
    (letfn [(do-render [page revision]
              (let [key (str (.title page) "/" (or revision (latest-revision page)))]
                (or (@cache key)
                    (let [rendered (render-source (page-source page revision))]
                      (dosync (alter cache #(assoc % key rendered)))
                      (do-render page revision)))))]
      do-render)))
