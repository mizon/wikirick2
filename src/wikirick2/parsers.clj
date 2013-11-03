(ns wikirick2.parsers
  (:use [hiccup.core :only [h]]
        zetta.core)
  (:require [clojure.string :as string]
            [zetta.combinators :as c]
            [zetta.parser.seq :as s]))

(declare wiki)

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(defn render-wiki-source [wiki-source]
  (let [source (string/split-lines wiki-source)
        result (parse-once wiki source)
        value (:result result)]
    (if value
      value
      result)))

(defmacro def- [& forms]
  `(def ^:private ~@forms))

(defn- match? [reg]
  (s/satisfy? #(re-matches reg %)))

(defn- try-parser [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(err-fn0 [_ more0 stack msg]
              (err-fn input more0 stack msg))]
      (parser input more err-fn0 ok-fn))))

(defn- not-followed-by [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(ok-fn* [_ more* result]
              (err-fn input more* [] "not followed by"))

            (err-fn* [_ more* _ _]
              (s/any-token input more* err-fn ok-fn))]
      (parser input more err-fn* ok-fn*))))

(def- special-prefix-chars
  "#>\\*\\+\\-")

(def- empty-line
  (match? #"\s*"))

(def- atx-header
  (let [regex #"(#{1,6}) *(.*?) *#*"]
    (do-parser [line (match? regex)]
      (let [[_ syms content] (re-matches regex line)]
        [(keyword (str "h" (count syms))) content]))))

(def- settext-header
  (try-parser (do-parser [content s/any-token
                          underline (match? #"(=+|-+) *")]
                (case (first underline)
                  \= [:h1 content]
                  \- [:h2 content]
                  (assert false "must not happen")))))

(def- paragraph
  (let [regex (re-pattern (format "[^%s\\s].+" special-prefix-chars))]
    (do-parser [lines (c/many1 (match? regex))]
      [:p (string/join "\n" lines)])))

(def- li-rest-line
  (let [regex (re-pattern (format "[^%s]\\s*[^\\s\\*\\+\\-].+" special-prefix-chars))]
    (do-parser [line (match? regex)]
      (.trim line))))

(def- ul-item
  (let [regex #"(\s*)[\*\+\-]\s+(.+)"]
    (do-parser [first* (match? regex)
                rest* (c/many li-rest-line)]
      (let [[_ spaces first**] (re-matches regex first*)]
        [(count spaces) (string/join "\n" (cons first** rest*))]))))

(def- unordered-list
  (do-parser [items (c/many1 ul-item)]
    `[:ul ~@(for [[_ content] items]
              [:li content])]))

(def- code
  (let [regex #"(\t|    )(.+)"]
    (do-parser [first* (match? regex)
                rest* (c/many (<|> (match? regex)
                                   (*> empty-line (always "    "))))]
      (let [code-lines (cons first* rest*)
            trim-left #(.replaceAll % "^(\t|    )" "")
            trim-right #(.replaceAll % "\\s*$" "")]
        [:pre
         [:code
          (trim-right (string/join "\n" (map trim-left code-lines)))]]))))

(def- bq-marked-line
  (let [regex #"\s*> ?(.+)"]
    (do-parser [line (match? regex)]
      ((re-matches regex line) 1))))

(def- bq-no-marked-line
  (let [regex #"\s*"]
    nil))

(def- blockquote
  (let [regex #">(.*)"]
    (do-parser [lines (c/many1 (match? regex))]
      nil)))

(def- block
  (reduce <|> [unordered-list
               code
               atx-header
               settext-header
               paragraph]))

(def- wiki
  (do-parser [bs (c/many (*> (c/many empty-line) block))
              _ (c/many empty-line)
              _ s/end-of-input]
    bs))
