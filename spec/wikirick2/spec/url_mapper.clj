(ns wikirick2.spec.url-mapper
  (:use wikirick2.url-mapper
        wikirick2.types
        speclj.core))

(def urlm (->URLMapper "/wiki"))

(describe "url mapper"
  (it "expands index pathes"
    (should= "/" (index-path urlm)))

  (it "exapads an page path"
    (let [page (make-page "SomePage" "some content")]
      (should= "/w/SomePage" (page-path urlm page))))

  (it "expands some pathes"
    (should= "/foo" (expand-path urlm "foo")))

  (it "expands the theme path"
    (should= "/theme.css" (theme-path urlm))))
