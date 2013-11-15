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

(defn- render? [sxml source]
  (= (parsers/render-wiki-source source) sxml))

(defn- render-inline? [sxml source]
  (render? `[[:p ~@sxml]] source))

(deftest scan-wiki-links
  (testing "scans wiki links from the wiki source"
    (is (= (parsers/scan-wiki-links wiki-source) #{"SomePage" "FooPage" "BarPage"}))))

(deftest render-wiki-source-inline-level
  (testing "text"
    (is (render-inline? ["text ** __ * _"] "text \\*\\* \\_\\_ \\* \\_"))
    (is (render-inline? ["text\\text"] "text\\\\text")))

  (testing "inline link"
    (is (render-inline? [[:a {:href "http://www.w3.org/"} "W3C"]]
                        "[W3C](http://www.w3.org/)"))
    (is (render-inline? [[:a {:href "http://www.w3.org/" :title "World Wide Web Consortium"} "W3C"]]
                        "[W3C](http://www.w3.org/ \"World Wide Web Consortium\")")))

  (testing "reference link"
    (is (render-inline? [[:a {:href "http://www.w3.org/"} "W3C"]] "
[W3C][]

[W3C]: http://www.w3.org/
"))
    (is (render-inline? [[:a {:href "http://www.w3.org/"} "W3C"]] "
[W3C][1]

  [1]:http://www.w3.org/ 
"))
    (is (render-inline? ["[W3C][]"] "[W3C][]"))
    (is (render-inline? [[:a {:href "http://www.w3.org/" :title "World Wide Web Consortium"} "W3C"]] "
[W3C][]

[w3c]: http://www.w3.org/ \"World Wide Web Consortium\"
")))

  (testing "strong"
    (is (render-inline? [[:strong "important!"]] "**important!**"))
    (is (render-inline? [[:strong "important!"]] "__important!__"))
    (is (render-inline? [[:strong "**important!**"]] "**\\*\\*important!\\*\\***")))

  (testing "emphasis"
    (is (render-inline? [[:em "important"]] "*important*"))
    (is (render-inline? [[:em "important"]] "_important_"))
    (is (render-inline? [[:em "_important_"]] "_\\_important\\__")))

  (testing "code"
    (is (render-inline? [[:code "printf(&quot;hello&quot;);"]] "`printf(\"hello\");`")))

  (testing "html escapes"
    (is (render-inline? ["foo&amp;bar"] "foo&bar"))
    (is (render-inline? [[:em "foo&amp;bar"]] "*foo&bar*"))
    (is (render-inline? [[:strong "foo&amp;bar"]] "**foo&bar**"))
    (is (render-inline? [[:code "foo&amp;bar"]] "`foo&bar`"))
    (is (render-inline? [[:a {:href "http://www.w3.org/?k0=v0&k1=v1" :title "W3C&"} "W3C&amp;"]]
                        "[W3C&](http://www.w3.org/?k0=v0&k1=v1 \"W3C&\")"))
    (is (render-inline? [[:a {:href "http://www.w3.org/?k0=v0&k1=v1" :title "W3C&"} "W3C&amp;"]] "
[W3C&][]

[W3C&]: http://www.w3.org/?k0=v0&k1=v1 \"W3C&\"
"))))

(deftest render-wiki-source-block-level
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
                    "#include &lt;stdio.h&gt;\n\nint main(void)\n{\n    return 0;\n}"]]] "
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
    (is (render? [[:h1 "Section"] [:h2 "Subsection"] [:ul [:li "foo"] [:li "bar"]] [:p "foobar"]] "
Section
=======

Subsection
----------

* foo
* bar

foobar
"))))
