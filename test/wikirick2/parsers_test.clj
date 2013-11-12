(ns wikirick2.parsers-test
  (:use clojure.test)
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

(use-fixtures :each
  (fn [example]
    (example)))

(defn- render? [sxml source]
  (= (parsers/render-wiki-source source) sxml))

(deftest scan-wiki-links
  (testing "scans wiki links from the wiki source"
    (is (= (parsers/scan-wiki-links wiki-source) #{"SomePage" "FooPage" "BarPage"}))))

(deftest render-wiki-source
  (testing "header"
    (testing "atx style"
      (is (render? [[:h1 "News"]] "# News"))
      (is (render? [[:h2 "News"]] "## News"))
      (is (render? [[:h3 "News"]] "### News"))
      (is (render? [[:h4 "News"]] "#### News"))
      (is (render? [[:h5 "News"]] "##### News"))
      (is (render? [[:h6 "News"]] "###### News"))

      (is (render? [[:h1 "News"]] "# News #"))
      (is (render? [[:h2 "News"]] "## News ##"))
      (is (render? [[:h3 "News"]] "### News ###"))
      (is (render? [[:h4 "News"]] "#### News ####"))
      (is (render? [[:h5 "News"]] "##### News #####"))
      (is (render? [[:h6 "News"]] "###### News ######"))

      (testing "don't output h7"
        (is (render? [[:h6 "# News"]] "####### News"))))

    (testing "settext style"
      (is (render? [[:h1 "News"]] "
News
====
"))
      (is (render? [[:h2 "News"]] "
News
----
"))))

  (testing "parapraph"
    (is (render? [[:p "foo\nbar"] [:p "foobar"]] "
foo
bar

foobar
")))

  (testing "unordered list"
    (testing "expands basic style items"
      (is (render? [[:ul
                     [:li "foo"]
                     [:li "bar"]
                     [:li "foobar"]]] "
* foo
+ bar
- foobar
")))

    (testing "expands lazy style items"
      (is (render? [[:ul
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

    (testing "expands nested items"
      (is (render? [[:ul
                     [:li
                      "foo"
                      [:ul
                       [:li "bar"]
                       [:li "foobar"]]]
                     [:li "bar"]]] "
+ foo
  - bar
  - foobar
+ bar
")))

    (testing "expands paragraph style"
      (is (render? [[:ul
                     [:li [:p "foo\nbar"]]
                     [:li [:p "foobar\nfoobar"]]]] "
* foo
bar

* foobar
foobar
")))

    (testing "expands paragraphs in each item"
      (is (render? [[:ul
                     [:li [:p "foobar\nfoobar"] [:p "foobar"]]
                     [:li [:p "foobar"]]]] "
-   foobar
    foobar

    foobar
-   foobar
"))))

  (testing "ordered list"
    (testing "expands items"
      (is (render? [[:ol
                     [:li "foo"]
                     [:li "bar"]
                     [:li "foobar"]]] "
1. foo
2. bar
3. foobar
")))

    (testing "expands nested items"
      (is (render? [[:ol
                     [:li "foo"
                      [:ol
                       [:li "bar"]]]]] "
1. foo
  2. bar
")))

    (testing "expands mixed with ul items"
      (is (render? [[:ol
                     [:li
                      "foo"
                      [:ul
                       [:li "foobar"]]]]] "
1. foo
  * foobar
"))))

  (testing "code block"
    (is (render? [[:pre
                   [:code
                    "#include <stdio.h>\n\nint main(void)\n{\n    return 0;\n}"]]] "
    #include <stdio.h>

    int main(void)
    {
        return 0;
    }
")))

  (testing "blockquote"
    (testing "expands from strict style markups"
      (is (render? [[:blockquote [:p "foo bar\nfoobar"]]] "
> foo bar
> foobar")))

    (testing "expands from from lazy style markups"
      (is (render? [[:blockquote [:p "foo bar\nfoobar"]]] "
> foo bar
foobar
")))

    (testing "expands some paragraphs"
      (is (render? [[:blockquote [:p "foo\nbar"] [:p "foo\nbar"]]] "
> foo
bar

> foo
bar
")))

    (testing "expands some elements"
      (is (render? [[:blockquote [:h1 "Section"] [:h2 "Subsection"] [:p "foobar"]]] "
> Section
> =======
>
> Subsection
> ----------
>
> foobar
"))))

  (testing "expands some elements"
    (is (render? [[:h1 "Section"] [:h2 "Subsection"] [:p "foobar"]] "
Section
=======

Subsection
----------

foobar
"))))
