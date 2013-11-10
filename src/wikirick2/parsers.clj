(ns wikirick2.parsers
  (:use [hiccup.core :only [h]]
        zetta.core)
  (:require [clojure.string :as string]
            [zetta.combinators :as c]
            [zetta.parser.seq :as s]))

(declare wiki li-level)

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(defn render-wiki-source [wiki-source]
  (let [source (string/split-lines wiki-source)
        result (parse-once wiki source)
        value (:result result)]
    (if value
      value
      result)))

(defmacro def- [name value]
  (list `def (with-meta name {:private true}) value))

(defn- match? [reg]
  (s/satisfy? #(re-matches reg %)))

(defn- try-parser [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(err-fn* [_ more* stack msg]
              (err-fn input more* stack msg))]
      (parser input more err-fn* ok-fn))))

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

(declare ul-item-cont)

(def- ul-indented-cont-line
  (let [regex #" {4}(.+)"]
    (try-parser (do-parser [es (c/many empty-line)
                            line (match? regex)]
                  (let [content (second (re-matches regex line))]
                    (if (empty? es)
                      content
                      ["" content]))))))

(defn- ul-no-indended-cont-line [level]
  (do-parser [line (not-followed-by (<|> (ul-item-cont level)
                                         empty-line))]
    line))

(def- ul-item
  (let [start (let [regex #"( {0,3})[\*\+\-]\s+(.*)"]
                (do-parser [line (match? regex)]
                  (let [[_ spaces content] (re-matches regex line)]
                    [(count spaces) content])))]
    (do-parser [[level l] start
                ls (c/many (<|> ul-indented-cont-line
                                (ul-no-indended-cont-line level)))
                blanks (c/many empty-line)]
      [level (flatten (if (empty? blanks)
                        (cons l ls)
                        (list* "" l ls)))])))

(defn- ul-item-cont [level]
  (let [start (let [regex (re-pattern (format " {%s}[\\*\\+\\-]\\s+(.*)" level))]
                (do-parser [line (match? regex)]
                  (second (re-matches regex line))))]
    (do-parser [l start
                ls (c/many (<|> ul-indented-cont-line
                                (ul-no-indended-cont-line level)))
                blanks (c/many empty-line)]
      (flatten (if (empty? blanks)
                 (cons l ls)
                 (list* "" l ls))))))

(declare unordered-list)

(def- ul-plain-lines
  (do-parser [lines (c/many1 (not-followed-by unordered-list))]
    (string/join "\n" (map #(.trim %) lines))))

(def- unordered-list
  (letfn [(plain-mode [liness]
            (for [lines liness]
              `[:li ~@(:result (parse-once (c/many (<|> unordered-list ul-plain-lines)) lines))]))

          (paragraph-mode [liness]
            (for [lines liness]
              `[:li ~@(:result (parse-once wiki lines))]))]
    (do-parser [[level ls] ul-item
                lss (c/many (ul-item-cont level))]
      (let [liness (cons ls lss)]
        `[:ul ~@(if (empty? (filter #(= % "") (flatten liness)))
                  (plain-mode liness)
                  (paragraph-mode liness))]))))

(def- code
  (let [regex #"(\t|    )(.+)"]
    (do-parser [l (match? regex)
                ls (c/many (<|> (match? regex) empty-line))]
      (let [code-lines (cons l ls)
            trim-left #(.replaceAll % "^(\t|    )" "")
            trim-right #(.replaceAll % "\\s*$" "")]
        [:pre
         [:code
          (trim-right (string/join "\n" (map trim-left code-lines)))]]))))

(def- bq-marked-line
  (let [regex #"\s*> ?(.*)"]
    (do-parser [line (match? regex)]
      ((re-matches regex line) 1))))

(def- bq-no-marked-line
  (not-followed-by empty-line))

(def- bq-fragment
  (do-parser [l bq-marked-line
              ls (c/many (<|> bq-marked-line bq-no-marked-line))
              _ (c/skip-many empty-line)]
    `(~l ~@ls "")))

(def- blockquote
  (do-parser [fragments (c/many1 bq-fragment)]
    (let [inners (parse-once wiki (apply concat fragments))]
      (if (:result inners)
        `[:blockquote ~@(:result inners)]
        (assert false "blockquote: must not happen")))))

(def- block
  (reduce <|> [unordered-list
               code
               atx-header
               settext-header
               blockquote
               paragraph]))

(def- wiki
  (do-parser [bs (c/many (*> (c/many empty-line) block))
              _ (c/many empty-line)
              _ s/end-of-input]
    bs))
