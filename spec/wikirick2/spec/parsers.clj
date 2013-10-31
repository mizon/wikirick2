(ns wikirick2.spec.parsers
  (:use speclj.core)
  (:require [wikirick2.parsers :as parsers]))

(def wiki-source "
FrontPage
================

SomePage -> [[SomePage]]
FooPage -> [[FooPage]]
BarPage -> [[BarPage]]

[SomeSite][]

[[SomePage]] is funny.
")

(defn- should-be-rendered [expected source]
  (should= expected (parsers/render-wiki-source source)))

(describe "parsers"
  (describe "scan-wiki-links"
    (it "scans wiki links from wiki sources"
      (should= #{"SomePage" "FooPage" "BarPage"} (parsers/scan-wiki-links wiki-source))))

  (describe "render-wiki-source"
    (describe "headlines"
      (it "expands underline style headlines"
        (should-be-rendered [[:h1 "News"]] "
News
====
")))))
