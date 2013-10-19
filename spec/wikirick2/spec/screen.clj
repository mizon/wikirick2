(ns wikirick2.spec.screen
  (:use speclj.core
        wikirick2.types
        wikirick2.service
        wikirick2.spec.spec-helper)
  (:require [hiccup.core :as core]
            [hiccup.page :as page]
            [wikirick2.screen :as screen]))

(def screen (screen/->Screen testing-service))

(describe "screen componets"
  (describe "screen"
    (it "render template fragments as full html"
      (let [template (->Template nil [:p "foo"])
            rendered (page/html5 (.body (screen/base template testing-service)))]
        (should= rendered (render-full screen template))))

    (it "render template fragments"
      (let [template (->Template nil [:p "foo"])]
        (should= (core/html (.body template)) (render-fragment screen template))))))
