(ns wikirick2.spec.url-mapper
  (:use wikirick2.url-mapper
        wikirick2.types
        speclj.core))

(def urlm (->URLMapper "/wiki"))

(describe "url mapper"
  (it "expands index pathes"
    (should= "/" (index-path urlm)))

  (it "exapads an article path"
    (let [article (make-article "SomePage" "some content")]
      (should= "/wiki/SomePage" (article-path urlm article))))

  (it "expands some pathes"
    (should= "/foo" (expand-path urlm "foo")))

  (it "expands the css path"
    (should= "/wikirick.css" (css-path urlm))))
