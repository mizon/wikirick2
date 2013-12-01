(ns wikirick2.screen
  (:use hiccup.core
        wikirick2.helper.screen
        wikirick2.types)
  (:require [hiccup.page :as page]))

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
                  [:table
                   [:tr [:th "Timestamp"] [:th "Revision"] [:th "Changes"] [:th "Diff to"]]
                   ~@(apply concat
                            (for [[odd-hist even-hist] (partition 2 (page-history page))]
                              [(history-line self page odd-hist "odd")
                               (history-line self page even-hist "even")]))]]]))

  (search-view [self word result]
    (base-view self
               "Search"
               [[:nav
                 [:ul
                  [:li "Read"]
                  [:li "Source"]
                  [:li "Edit"]
                  [:li "History"]]]
                [:p {:class "page-info"} [:em "Search"] ": " (h word)]
                `[:article
                  {:class "search"}
                  [:header [:h1 "Search"]]
                  ~(search-box self word)
                  [:table
                   ~@(for [[title content] result]
                       [:tr
                        [:th [:a {:href (page-path url-mapper title)} (h title)]]
                        [:td (h content)]])]]])))

(defn cached-page-renderer [render-f]
  (let [cache (ref {})]
    (letfn [(do-render [page]
              (let [key (str (.title page) "/" (page-revision page))]
                (or (@cache key)
                    (let [rendered (render-f (page-source page))]
                      (dosync (alter cache #(assoc % key rendered)))
                      (do-render page)))))]
      do-render)))
