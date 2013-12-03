(ns wikirick2.screen
  (:require [hiccup.core :refer :all]
            [hiccup.page :as page]
            [wikirick2.helper.screen :refer :all]
            [wikirick2.types :refer :all]))

(deftype Screen [storage url-mapper renderer config]
  IScreen
  (read-view [self page]
    (base-view self
               (.title page)
               [(navigation self page {:read {:enabled? true :selected? true}
                                       :source {:enabled? true :selected? false}
                                       :edit {:enabled? true :selected? false}
                                       :history {:enabled? true :selected? false}})
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (.title page))]]
                  ~@(renderer page)
                  [:h2 "Related Pages"]
                  [:ul ~@(map #(title-to-li self %) (referred-titles page))]]]))

  (new-view [self page]
    (base-view self
               (.title page)
               [(navigation self page {:read {:enabled? false :selected? false}
                                       :source {:enabled? false :selected? false}
                                       :edit {:enabled? true :selected? true}
                                       :history {:enabled? false :selected? false}})
                [:p {:class "page-info"} [:em (h (.title page))] ": (new page)"]
                `[:article
                  [:header [:h1 ~(h (format "%s: New" (.title page)))]]
                  [:section
                   {:class "edit"}
                   [:textarea {:placeholder ~(page-source page)}]
                   [:button {:type "submit"} "Preview"]
                   [:button {:type "submit"} "Submit"]]]]))

  (edit-view [self page]
    (base-view self
               (.title page)
               [(navigation self page {:read {:enabled? true :selected? false}
                                       :source {:enabled? true :selected? false}
                                       :edit {:enabled? true :selected? true}
                                       :history {:enabled? true :selected? false}})
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (format "%s: Edit" (.title page)))]]
                  [:form {:class "edit"
                          :method "post"
                          :action ~(page-action-path url-mapper (.title page) "edit")}
                   [:input {:type "hidden"
                            :name "base-rev"
                            :value ~(page-revision page)}]
                   [:textarea {:name "source"} ~(h (page-source page))]
                   [:button {:type "submit"} "Preview"]
                   [:button {:type "submit"} "Submit"]]]]))

  (history-view [self page]
    (base-view self
               (.title page)
               [(navigation self page {:read {:enabled? true :selected? false}
                                       :source {:enabled? true :selected? false}
                                       :edit {:enabled? true :selected? false}
                                       :history {:enabled? true :selected? true}})
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
               [[:nav
                 [:ul
                  [:li "Read"]
                  [:li "Source"]
                  [:li "Edit"]
                  [:li "History"]]]
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
    (letfn [(do-render [page]
              (let [key (str (.title page) "/" (page-revision page))]
                (or (@cache key)
                    (let [rendered (render-f (page-source page))]
                      (dosync (alter cache #(assoc % key rendered)))
                      (do-render page)))))]
      do-render)))
