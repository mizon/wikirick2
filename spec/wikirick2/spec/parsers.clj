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

(describe "parsers"
  (describe "scan-wiki-links"
    (it "scans wiki links from wiki sources"
      (should= (hash-set "SomePage" "FooPage" "BarPage") (parsers/scan-wiki-links wiki-source)))))
