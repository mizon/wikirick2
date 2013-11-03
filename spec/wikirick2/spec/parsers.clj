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

(defn- should-render [expected source]
  (should= expected (parsers/render-wiki-source source)))

(describe "parsers"
  (describe "scan-wiki-links"
    (it "scans wiki links from a wiki source"
      (should= #{"SomePage" "FooPage" "BarPage"} (parsers/scan-wiki-links wiki-source))))

  (describe "render-wiki-source"
    (describe "header"
      (it "expands atx style"
        (should-render [[:h1 "News"]] "# News")
        (should-render [[:h2 "News"]] "## News")
        (should-render [[:h3 "News"]] "### News")
        (should-render [[:h4 "News"]] "#### News")
        (should-render [[:h5 "News"]] "##### News")
        (should-render [[:h6 "News"]] "###### News")

        (should-render [[:h1 "News"]] "# News #")
        (should-render [[:h2 "News"]] "## News ##")
        (should-render [[:h3 "News"]] "### News ###")
        (should-render [[:h4 "News"]] "#### News ####")
        (should-render [[:h5 "News"]] "##### News #####")
        (should-render [[:h6 "News"]] "###### News ######"))

      (it "should not output too low-level header elements"
        (should-render [[:h6 "# News"]] "####### News"))

      (it "expands settext style"
        (should-render [[:h1 "News"]] "
News
====
")
        (should-render [[:h2 "News"]] "
News
----
")))

    (it "expands parapraphs" (should-render [[:p "foo\nbar"] [:p "foobar"]] "
foo
bar

foobar
"))

    (describe "unordered list"
      (it "expands basic style items"
        (should-render [[:ul
                         [:li "foo"]
                         [:li "bar"]
                         [:li "foobar"]]] "
* foo
+ bar
- foobar
"
))

      (it "expands lazy style items"
        (should-render [[:ul
                         [:li "foo\nbar\nfoobar"]
                         [:li "bar"]
                         [:li "foo\nbar"]]] "
* foo
bar
foobar
* bar
* foo
  bar
")))

    (it "expands code blocks"
      (should-render [[:pre
                       [:code
                        "#include <stdio.h>\n\nint main(void)\n{\n    return 0;\n}"]]] "
    #include <stdio.h>

    int main(void)
    {
        return 0;
    }
"))

    (describe "blockquote"
      (it "expands from strict style markups"
        (should-render [[:blockquote [:p "foo bar\nfoobar"]]] "
> foo bar
> foobar"))

      (it "expands from from lazy style markups"
        (should-render [[:blockquote [:p "foo bar\nfoobar"]]] "
> foo bar
foobar
"))

      (it "expands some paragraphs"
        (should-render [[:blockquote [:p "foo\nbar"] [:p "foo\nbar"]]] "
> foo
bar

> foo
bar
")))))
