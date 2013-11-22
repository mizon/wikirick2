(ns wikirick2.screen
  (:use hiccup.core
        wikirick2.helper.screen
        wikirick2.parsers
        wikirick2.types)
  (:require [hiccup.page :as page]))

(deftype Screen [url-mapper render-wiki-source config]
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
                  ~@(render-wiki-source (page-source page))
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
                            :value ~(str (page-revision page))}]
                   [:textarea {:name "source"} ~(h (page-source page))]
                   [:button {:type "submit"} "Preview"]
                   [:button {:type "submit"} "Submit"]]]])))
