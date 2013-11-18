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
                                       :source {:enabled? false :selected? false}
                                       :edit {:enabled? true :selected? false}
                                       :history {:enabled? false :selected? false}})
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (.title page))]]
                  ~@(render-wiki-source (.source page))
                  [:h2 "Related Pages"]
                  [:ul ~@(map #(title-to-li self %) (referred-titles page))]]]))

  (edit-view [self page]
    (base-view self
               (.title page)
               [(navigation self page {:read {:enabled? true :selected? false}
                                       :source {:enabled? false :selected? false}
                                       :edit {:enabled? true :selected? true}
                                       :history {:enabled? false :selected? false}})
                (page-info page)
                `[:article
                  [:header [:h1 ~(h (format "Edit: %s" (.title page)))]]
                  [:textarea ~(h (.source page))]
                  [:button {:type "submit"} "Submit"]]])))
