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
      (it "expands prefix style"
        (should-be-rendered [[:h1 "News"]] "# News")
        (should-be-rendered [[:h2 "News"]] "## News")
        (should-be-rendered [[:h3 "News"]] "### News")
        (should-be-rendered [[:h4 "News"]] "#### News")
        (should-be-rendered [[:h5 "News"]] "##### News")
        (should-be-rendered [[:h6 "News"]] "###### News")

        (should-be-rendered [[:h1 "News"]] "# News #")
        (should-be-rendered [[:h2 "News"]] "## News ##")
        (should-be-rendered [[:h3 "News"]] "### News ###")
        (should-be-rendered [[:h4 "News"]] "#### News ####")
        (should-be-rendered [[:h5 "News"]] "##### News #####")
        (should-be-rendered [[:h6 "News"]] "###### News ######"))

      (it "expands underline style"
        (should-be-rendered [[:h1 "News"]] "News
==")
        (should-be-rendered [[:h2 "News"]] "News
--")))

    (it "expands parapraphs" (should-be-rendered [[:p "foo\nbar"] [:p "foobar"]] "foo
bar

foobar"))

    (describe "unordered list"
      (it "expands basic style items"
        (should-be-rendered [[:ul
                              [:li "foo"]
                              [:li "bar"]
                              [:li "foobar"]]] "
* foo
+ bar
- foobar
"))
      (it "expands rest style items"
        (should-be-rendered [[:ul
                              [:li "foo
bar
foobar"]
                              [:li "bar"]
                              [:li "foo
bar"]]] "* foo
bar
foobar
* bar
* foo
  bar"
)))))
